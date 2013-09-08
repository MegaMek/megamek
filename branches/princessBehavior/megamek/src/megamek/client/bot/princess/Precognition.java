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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEvent;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.util.Logger;

/**
 * unit_potential_locations keeps track of all the potential coordinates and
 * facings a unit could reach It tries to keep all the calculations up to date,
 * and do most of the work when the opponent is moving
 */
public class Precognition implements Runnable {

    private Princess owner;

    private PathEnumerator path_enumerator;
    private final Object PATH_ENUM_LOCK = new Object();

    private IGame game;

    // units who's path I need to update
    private final TreeSet<Integer> dirty_units = new TreeSet<Integer>();

    // events that may affect which units are dirty
    private final LinkedList<GameEvent> events_to_process = new LinkedList<GameEvent>();

    private boolean wait_when_done = false; // used for pausing
    private boolean waiting = false;

    public Precognition(Princess owner) {
        this.owner = owner;
        synchronized (PATH_ENUM_LOCK) {
            path_enumerator = new PathEnumerator(owner);
        }
    }

    void setGame(IGame g) {
        final String METHOD_NAME = "setGame(IGame)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            game = g;
            synchronized (PATH_ENUM_LOCK) {
                path_enumerator.game = game;
            }

            game.addGameListener(new GameListenerAdapter() {
                @Override
                public void gameEntityChange(GameEntityChangeEvent e) {
                    synchronized (events_to_process) {
                        events_to_process.addLast(e);
                    }
                    wake_up();
                }

                @Override
                public void gamePhaseChange(GamePhaseChangeEvent e) {
                    synchronized (events_to_process) {
                        events_to_process.addLast(e);
                    }
                    wake_up();
                }

            });
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public void pause() {
        final String METHOD_NAME = "pause()";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            wait_when_done = true;
            while (!waiting) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public synchronized void unpause() {
        final String METHOD_NAME = "unpause()";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            wait_when_done = false;
            notifyAll();
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Tells the thread there's something to do Note, you can't just call
     * notifyAll in the event listener because it doesn't have the thread
     * something something.
     */
    public synchronized void wake_up() {
        final String METHOD_NAME = "wake_up()";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            notifyAll();
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Makes sure path_enumerator has up to date information about other units
     * locations call this right before making a move. automatically pauses.
     */
    public void insureUpToDate() {
        final String METHOD_NAME = "insureUpToDate()";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            pause();
            for (Enumeration<Entity> ents = game.getEntities(); ents
                    .hasMoreElements(); ) {
                Entity e = ents.nextElement();
                if (!e.isDeployed() || e.isOffBoard()) {
                    continue;
                }
                if (((!path_enumerator.last_known_location.containsKey(e.getId()))
                     || (!path_enumerator.last_known_location.get(e.getId()).equals(new CoordFacingCombo(e))))) {
                    // System.err.println("entity "+e.getDisplayName()+" not where I left it");
                    // if(path_enumerator.last_known_location.containsKey(e.getId()))
                    // System.err.println("  I thought it was at "+path_enumerator.last_known_location.get(e.getId())
                    // .coords+" but its actually at "+e.getPosition());
                    // else
                    // System.err.println("  I had no idea where it was");
                    dirtifyUnit(e.getId());
                }
            }
            while (!dirty_units.isEmpty()) {
                Integer entity_id;
                synchronized (dirty_units) {
                    entity_id = dirty_units.pollFirst();
                }
                Entity e = game.getEntity(entity_id);
                if (e != null) {
                    Logger.log(getClass(), METHOD_NAME, "recalculating paths for " + e.getDisplayName());
                    synchronized (PATH_ENUM_LOCK) {
                        path_enumerator.recalculateMovesFor(game, e);
                    }
                    Logger.log(getClass(), METHOD_NAME, "finished recalculating paths for " + e.getDisplayName());
                }
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public void run() {
        final String METHOD_NAME = "run()";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            // todo There's probably a better way to handle this than a loop that only exits on an error.
            //noinspection InfiniteLoopStatement
            while (true) {
                if (!events_to_process.isEmpty()) {
                    processGameEvents();
                } else if (!dirty_units.isEmpty()) {
                    Entity e;
                    synchronized (dirty_units) {
                        e = game.getEntity(dirty_units.pollFirst());
                    }
                    if (e != null) {
                        Logger.log(getClass(), METHOD_NAME, "recalculating paths for " + e.getDisplayName());
                        synchronized (PATH_ENUM_LOCK) {
                            path_enumerator.recalculateMovesFor(game, e);
                        }
                        Logger.log(getClass(), METHOD_NAME, "finished recalculating paths for " + e.getDisplayName());
                    }
                } else if (wait_when_done) {
                    wait_for_unpause(); // paused for a reason
                } else {
                    wait_for_unpause(); // idling because there's nothing to do
                }
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Waits until the thread is not paused, and there's indication that it has
     * something to do
     */
    public synchronized void wait_for_unpause() {
        final String METHOD_NAME = "wait_for_unpause()";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            while (wait_when_done
                   || (events_to_process.isEmpty() && dirty_units.isEmpty())) {
                StringBuilder msg = new StringBuilder("wait_when_done = " + wait_when_done);
                msg.append(" :: events_to_process = ").append(events_to_process.size());
                msg.append(" :: dirty_units = ").append(dirty_units.size());
                Logger.log(getClass(), METHOD_NAME, msg.toString());
                waiting = true;
                try {
                    wait();
                } catch (InterruptedException ignored) {
                }
                // System.err.println("checking WAIT conditions");
            }
            waiting = false;
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Process game events that have happened since the thread last checked i.e.
     * if a unit has moved, my precaculated paths are no longer valid
     */
    public synchronized void processGameEvents() {
        final String METHOD_NAME = "processGameEvents()";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            LinkedList<GameEvent> eventsToProcessIterator;
            synchronized (events_to_process) {
                eventsToProcessIterator = new LinkedList<GameEvent>(events_to_process);
            }
            int numEvents = eventsToProcessIterator.size();
            for (int count = 0; count < numEvents; count++) {
                Logger.log(getClass(), METHOD_NAME, "Processing event " + (count + 1) + " out of " + numEvents);
                GameEvent event = eventsToProcessIterator.get(count);
                if (event == null) {
                    continue;
                }
                Logger.log(getClass(), METHOD_NAME, "Processing " + event.toString());
                synchronized (events_to_process) {
                    events_to_process.remove(event);
                }
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
                    Entity onentity = game.getEntity(changeevent.getEntity().getId());
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
                    if (onentity.getPosition().equals(path_enumerator.getLastKnownCoords(onentity.getId()))) {
                        continue; // no sense in updating a unit if it hasn't moved
                    }
                    Logger.log(getClass(), METHOD_NAME, "Received entity change event for "
                                                        + changeevent.getEntity().getDisplayName() + " (ID "
                                                        + onentity.getId() + ")");
                    Integer entity = changeevent.getEntity().getId();
                    dirtifyUnit(entity);
                } else if (event instanceof GamePhaseChangeEvent) {
                    GamePhaseChangeEvent phasechange = (GamePhaseChangeEvent) event;
                    Logger.log(getClass(), METHOD_NAME, "Phase change detected: " + phasechange.getNewPhase().name());
                    // this marks when I can all I can start recalculating paths.
                    // All units are dirty
                    if (phasechange.getNewPhase() == IGame.Phase.PHASE_MOVEMENT) {
                        synchronized (PATH_ENUM_LOCK) {
                            path_enumerator.clear();
                        }
                        for (Enumeration<Entity> ents = game.getEntities(); ents.hasMoreElements(); ) {
                            Entity e = ents.nextElement();
                            if (e.isActive() && e.isDeployed()) {
                                synchronized (dirty_units) {
                                    dirty_units.add(e.getId());
                                }
                            }
                        }
                    }
                }
            }
            Logger.log(getClass(), METHOD_NAME, "Events still to process: " + events_to_process.size());
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Called when a unit has moved and should be put on the dirty list, as well
     * as any units who's moves contain that unit
     */
    public synchronized void dirtifyUnit(int id) {
        final String METHOD_NAME = "dirtifyUnit(int)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            // first of all, if a unit has been removed, remove it from the list and
            // stop
            if (game.getEntity(id) == null) {
                synchronized (PATH_ENUM_LOCK) {
                    path_enumerator.last_known_location.remove(id);
                    path_enumerator.unit_movable_areas.remove(id);
                    path_enumerator.unit_paths.remove(id);
                    path_enumerator.unit_potential_locations.remove(id);
                }
                return;
            }
            // if a unit has moved or deployed, then it becomes dirty, and any units
            // with its initial or final position
            // in their list become dirty
            if (!(game.getEntity(id) instanceof Aero)) {
                TreeSet<Integer> to_dirty = path_enumerator.getEntitiesWithLocation(game.getEntity(id).getPosition(),
                                                                                    true);
                if (path_enumerator.last_known_location.containsKey(id)) {
                    if ((game.getEntity(id) != null) && game.getEntity(id).isSelectableThisTurn()) {
                        to_dirty.addAll(path_enumerator.getEntitiesWithLocation(path_enumerator.last_known_location
                                                                                               .get(id).coords, true));
                    }
                }
                // no need to dirty units that aren't selectable this turn
                List<Integer> toRemove = new ArrayList<Integer>();
                for (Integer index : to_dirty) {
                    if ((game.getEntity(index) == null) || (!game.getEntity(index).isSelectableThisTurn())
                                                           && (game.getPhase() == IGame.Phase.PHASE_MOVEMENT)) {
                        toRemove.add(index);
                    }
                }
                for (Integer i : toRemove) {
                    to_dirty.remove(i);
                }

                if (to_dirty.size() != 0) {
                    String msg = "The following units have become dirty";
                    if (game.getEntity(id) != null) {
                        msg += " as a result of a nearby move of " + game.getEntity(id).getDisplayName();
                    }

                    Iterator<Integer> dirtyIterator = to_dirty.descendingIterator();
                    while (dirtyIterator.hasNext()) {
                        Integer i = dirtyIterator.next();
                        Entity e = game.getEntity(i);
                        if (e != null)
                            msg += "\n  " + e.getDisplayName();
                    }
                    Logger.log(getClass(), METHOD_NAME, msg);
                }
                synchronized (dirty_units) {
                    dirty_units.addAll(to_dirty);
                }
            }
            Entity e = game.getEntity(id);
            if ((e != null) && (e.isSelectableThisTurn())
                || (game.getPhase() != IGame.Phase.PHASE_MOVEMENT)) {
                synchronized (dirty_units) {
                    dirty_units.add(id);
                }
            } else if (e != null) {
                synchronized (PATH_ENUM_LOCK) {
                    path_enumerator.last_known_location.put(id, new CoordFacingCombo(e));
                }
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public PathEnumerator getPathEnumerator() {
        return path_enumerator;
    }
}
