/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEvent;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;

/**
 * unit_potential_locations keeps track of all the potential coordinates and
 * facings a unit could reach It tries to keep all the calculations up to date,
 * and do most of the work when the opponent is moving
 */
public class Precognition implements Runnable {

    PathEnumerator path_enumerator = new PathEnumerator();
    IGame game;
    // units who's path I need to update
    TreeSet<Integer> dirty_units = new TreeSet<Integer>();
    // events that may affect which units are dirty
    LinkedList<GameEvent> events_to_process = new LinkedList<GameEvent>();
    boolean wait_when_done = false; // used for pausing
    boolean waiting = false;
    boolean shouldquit = false;

    void setGame(IGame g) {
        game = g;
        path_enumerator.game = game;

        game.addGameListener(new GameListenerAdapter() {
            @Override
            public void gameEntityChange(GameEntityChangeEvent e) {
                events_to_process.addLast(e);
                wake_up();
            }

            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                events_to_process.addLast(e);
                wake_up();
            }

        });
    }

    public void pause() {
        wait_when_done = true;
        while (!waiting) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            ;
        }
    }

    public synchronized void unpause() {
        wait_when_done = false;
        notifyAll();
    }

    /**
     * Tells the thread there's something to do Note, you can't just call
     * notifyAll in the event listener because it doesn't have the thread
     * something something.
     */
    public synchronized void wake_up() {
        notifyAll();
    }

    /**
     * Makes sure path_enumerator has up to date information about other units
     * locations call this right before making a move. automatically pauses.
     */
    public void insureUpToDate() {

        System.err.println(System.currentTimeMillis()
                + ": insureUpToDate started");
        pause();
        for (Enumeration<Entity> ents = game.getEntities(); ents
                .hasMoreElements();) {
            Entity e = ents.nextElement();
            if (!e.isDeployed() || e.isOffBoard()) {
                continue;
            }
            if ((!path_enumerator.last_known_location.containsKey(e.getId()))
                    || (!path_enumerator.last_known_location.get(e.getId())
                            .equals(new CoordFacingCombo(e)))) {
                // System.err.println("entity "+e.getDisplayName()+" not where I left it");
                // if(path_enumerator.last_known_location.containsKey(e.getId()))
                // System.err.println("  I thought it was at "+path_enumerator.last_known_location.get(e.getId()).coords+" but its actually at "+e.getPosition());
                // else
                // System.err.println("  I had no idea where it was");
                dirtifyUnit(e.getId());
            }
        }
        while (!dirty_units.isEmpty()) {
            Integer entity_id = dirty_units.pollFirst();
            System.err.println("recalculating paths for "
                    + game.getEntity(entity_id).getDisplayName());
            path_enumerator
                    .recalculateMovesFor(game, game.getEntity(entity_id));
            System.err.println("finished recalculating paths for "
                    + game.getEntity(entity_id).getDisplayName());
        }
        System.err.println(System.currentTimeMillis()
                + ": insureUpToDate stopped");
    }

    @Override
    public void run() {
        System.err.println("starting precognition thread");
        while (true) {
            if (!events_to_process.isEmpty()) {
                processGameEvents();
            } else if (wait_when_done) {
                wait_for_unpause(); // paused for a reason
            } else if (!dirty_units.isEmpty()) {
                Integer entity_id = dirty_units.pollFirst();
                System.err.println("recalculating paths for "
                        + game.getEntity(entity_id).getDisplayName());
                path_enumerator.recalculateMovesFor(game,
                        game.getEntity(entity_id));
                System.err.println("finished recalculating paths for "
                        + game.getEntity(entity_id).getDisplayName());
            } else {
                wait_for_unpause(); // idling because there's nothing to do
            }
        }
    }

    /**
     * Waits until the thread is not paused, and there's indication that it has
     * something to do
     */
    public synchronized void wait_for_unpause() {
        while (wait_when_done
                || (events_to_process.isEmpty() && dirty_units.isEmpty())) {
            // System.err.println("STARTING TO WAIT");
            waiting = true;
            try {
                wait();
            } catch (InterruptedException e) {
            }
            // System.err.println("checking WAIT conditions");
        }
        waiting = false;
    }

    /**
     * Process game events that have happened since the thread last checked i.e.
     * if a unit has moved, my precaculated paths are no longer valid
     */
    public synchronized void processGameEvents() {
        while (!events_to_process.isEmpty()) {
            GameEvent event = events_to_process.pollFirst();
            if (event instanceof GameEntityChangeEvent) {
                // for starters, ignore entity changes that don't happen during
                // the movement phase
                if (game.getPhase() != IGame.Phase.PHASE_MOVEMENT) {
                    continue;
                }
                GameEntityChangeEvent changeevent = (GameEntityChangeEvent) event;
                if (changeevent.getEntity() == null) {
                    continue; // just to be safe
                }
                Entity onentity = game.getEntity(changeevent.getEntity()
                        .getId());
                if (onentity == null) {
                    continue; // not sure how this can happen, but just to be
                              // safe
                }
                // a lot of odd entity changes are send during the firing phase,
                // none of which are relevant
                if (game.getPhase() == IGame.Phase.PHASE_FIRING) {
                    continue;
                }
                if (onentity.getPosition() == null) {
                    continue;
                }
                if (onentity.getPosition().equals(
                        path_enumerator.getLastKnownCoords(onentity.getId()))) {
                    continue; // no sense in updating a unit if it hasn't moved
                }
                System.err.println(System.currentTimeMillis()
                        + ": received entity change event for "
                        + changeevent.getEntity().getDisplayName() + " (ID "
                        + onentity.getId() + ")");
                Integer entity = changeevent.getEntity().getId();
                dirtifyUnit(entity);
            } else if (event instanceof GamePhaseChangeEvent) {
                GamePhaseChangeEvent phasechange = (GamePhaseChangeEvent) event;
                System.err.println(System.currentTimeMillis()
                        + ": Phase change detected: "
                        + phasechange.getNewPhase().name());
                // this marks when I can all I can start recalculating paths.
                // All units are dirty
                if (phasechange.getNewPhase() == IGame.Phase.PHASE_MOVEMENT) {
                    path_enumerator.clear();
                    for (Enumeration<Entity> ents = game.getEntities(); ents
                            .hasMoreElements();) {
                        Entity e = ents.nextElement();
                        if (e.isActive() && e.isDeployed()) {
                            dirty_units.add(e.getId());
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when a unit has moved and should be put on the dirty list, as well
     * as any units who's moves contain that unit
     */
    public synchronized void dirtifyUnit(int id) {
        // first of all, if a unit has been removed, remove it from the list and
        // stop
        if (game.getEntity(id) == null) {
            path_enumerator.last_known_location.remove(id);
            path_enumerator.unit_movable_areas.remove(id);
            path_enumerator.unit_paths.remove(id);
            path_enumerator.unit_potential_locations.remove(id);
            return;
        }
        // if a unit has moved or deployed, then it becomes dirty, and any units
        // with its initial or final position
        // in their list become dirty
        if (!(game.getEntity(id) instanceof Aero)) {
            TreeSet<Integer> to_dirty = path_enumerator
                    .getEntitiesWithLocation(game.getEntity(id).getPosition(),
                            true);
            if (path_enumerator.last_known_location.containsKey(id)) {
                if (game.getEntity(id).isSelectableThisTurn()) {
                    to_dirty.addAll(path_enumerator.getEntitiesWithLocation(
                            path_enumerator.last_known_location.get(id).coords,
                            true));
                }
            }
            // no need to dirty units that aren't selectable this turn
            for (Iterator<Integer> it = to_dirty.iterator(); it.hasNext();) {
                if ((!game.getEntity(it.next()).isSelectableThisTurn())
                        && (game.getPhase() == IGame.Phase.PHASE_MOVEMENT)) {
                    it.remove();
                }
            }
            if (to_dirty.size() != 0) {
                System.err
                        .println("the following units have become dirty as a result of a nearby move of "
                                + game.getEntity(id).getDisplayName());
                for (Iterator<Integer> ondent = to_dirty.descendingIterator(); ondent
                        .hasNext();) {
                    Integer odid = ondent.next();
                    System.err.println("  "
                            + game.getEntity(odid).getDisplayName());
                }
            }
            dirty_units.addAll(to_dirty);
        }
        if (game.getEntity(id).isSelectableThisTurn()
                || (game.getPhase() != IGame.Phase.PHASE_MOVEMENT)) {
            dirty_units.add(id);
        } else {
            path_enumerator.last_known_location.put(id, new CoordFacingCombo(
                    game.getEntity(id)));
        }

    }
}
