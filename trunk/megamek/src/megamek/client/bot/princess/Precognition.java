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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.common.Aero;
import megamek.common.Coords;
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

    private final Princess owner;

    private IGame game;
    private final ReentrantReadWriteLock GAME_LOCK = new ReentrantReadWriteLock();

    private PathEnumerator pathEnumerator;
    private final ReentrantReadWriteLock PATH_ENUMERATOR_LOCK = new ReentrantReadWriteLock();


    // units who's path I need to update
    private final ConcurrentSkipListSet<Integer> dirtyUnits = new ConcurrentSkipListSet<>();

    // events that may affect which units are dirty
    private final ConcurrentLinkedQueue<GameEvent> eventsToProcess = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean waitWhenDone = new AtomicBoolean(false); // used for pausing
    private final AtomicBoolean waiting = new AtomicBoolean(false);
    private final AtomicBoolean done = new AtomicBoolean(false);

    public Precognition(Princess owner, IGame game) {
        this.owner = owner;
        setGame(game);
        setPathEnumerator(new PathEnumerator(owner, getGame()));
    }

    void updateGame(IGame game) {
        final String METHOD_NAME = "setGame(IGame)";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        setGame(game);
        try {
            getGame().addGameListener(new GameListenerAdapter() {
                @Override
                public void gameEntityChange(GameEntityChangeEvent changeEvent) {
                    getEventsToProcess().add(changeEvent);
                    wakeUp();
                }

                @Override
                public void gamePhaseChange(GamePhaseChangeEvent changeEvent) {
                    getEventsToProcess().add(changeEvent);
                    wakeUp();
                }

            });
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    public void pause() {
        final String METHOD_NAME = "pause()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            getWaitWhenDone().set(true);
            while (!getWaiting().get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    public void unPause() {
        final String METHOD_NAME = "unpause()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            getWaitWhenDone().set(false);
            synchronized (this) {
                notifyAll();
            }
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Tells the thread there's something to do Note, you can't just call
     * notifyAll in the event listener because it doesn't have the thread
     * something something.
     */
    public void wakeUp() {
        final String METHOD_NAME = "wake_up()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            synchronized (this) {
                notifyAll();
            }
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Makes sure pathEnumerator has up to date information about other units
     * locations call this right before making a move. automatically pauses.
     */
    public void insureUpToDate() {
        final String METHOD_NAME = "insureUpToDate()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            pause();
            for (Enumeration<Entity> entities = getGame().getEntities(); entities.hasMoreElements(); ) {
                Entity entity = entities.nextElement();
                if (!entity.isDeployed() || entity.isOffBoard()) {
                    continue;
                }
                if (((!getPathEnumerator().getLastKnownLocations().containsKey(entity.getId()))
                     || (!getPathEnumerator().getLastKnownLocations().get(entity.getId())
                                             .equals(CoordFacingCombo.createCoordFacingCombo(entity))))) {
                    // System.err.println("entity "+entity.getDisplayName()+" not where I left it");
                    // if(pathEnumerator.last_known_location.containsKey(entity.getId()))
                    // System.err.println("  I thought it was at "+pathEnumerator.last_known_location.get(entity
                    // .getId()).coords+" but its actually at "+entity.getPosition());
                    // else
                    // System.err.println("  I had no idea where it was");
                    dirtifyUnit(entity.getId());
                }
            }
            while (!getDirtyUnits().isEmpty()) {
                Integer entityId = getDirtyUnits().pollFirst();
                Entity entity = getGame().getEntity(entityId);
                if (entity != null) {
                    getOwner().log(getClass(), METHOD_NAME, "recalculating paths for " + entity.getDisplayName());
                    getPathEnumerator().recalculateMovesFor(entity);
                    getOwner().log(getClass(), METHOD_NAME, "finished recalculating paths for " + entity
                            .getDisplayName());
                }
            }
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    public void run() {
        final String METHOD_NAME = "run()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            // todo There's probably a better way to handle this than a loop that only exits on an error.
            //noinspection InfiniteLoopStatement
            while (!getDone().get()) {
                if (!getEventsToProcess().isEmpty()) {
                    processGameEvents();
                } else if (!getDirtyUnits().isEmpty()) {
                    Entity entity = getGame().getEntity(getDirtyUnits().pollFirst());
                    if (entity != null) {
                        getOwner().log(getClass(), METHOD_NAME, "recalculating paths for " + entity.getDisplayName());
                        getPathEnumerator().recalculateMovesFor(entity);
                        getOwner().log(getClass(), METHOD_NAME, "finished recalculating paths for " + entity
                                .getDisplayName());
                    }
                } else if (getWaitWhenDone().get()) {
                    waitForUnpause(); // paused for a reason
                } else {
                    waitForUnpause(); // idling because there's nothing to do
                }
            }
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    public void signalDone() {
        getDone().set(true);
    }

    /**
     * Waits until the thread is not paused, and there's indication that it has
     * something to do
     */
    public void waitForUnpause() {
        final String METHOD_NAME = "wait_for_unpause()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            while (!getDone().get() &&
                   (getWaitWhenDone().get() || (getEventsToProcess().isEmpty() && getDirtyUnits().isEmpty()))) {
                getOwner().log(getClass(), METHOD_NAME, "waitWhenDone = " + getWaitWhenDone() +
                                                        " :: eventsToProcess = " + getEventsToProcess().size() +
                                                        " :: dirtyUnits = " + getDirtyUnits().size());
                getWaiting().set(true);
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException ignored) {
                }
                // System.err.println("checking WAIT conditions");
            }
            getWaiting().set(false);
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Process game events that have happened since the thread last checked i.e.
     * if a unit has moved, my precaculated paths are no longer valid
     */
    public void processGameEvents() {
        final String METHOD_NAME = "processGameEvents()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            LinkedList<GameEvent> eventsToProcessIterator = new LinkedList<>(getEventsToProcess());
            int numEvents = eventsToProcessIterator.size();
            for (int count = 0; count < numEvents; count++) {
                getOwner().log(getClass(), METHOD_NAME, "Processing event " + (count + 1) + " out of " + numEvents);
                GameEvent event = eventsToProcessIterator.get(count);
                if (event == null) {
                    continue;
                }
                getOwner().log(getClass(), METHOD_NAME, "Processing " + event.toString());
                getEventsToProcess().remove(event);
                if (event instanceof GameEntityChangeEvent) {
                    // for starters, ignore entity changes that don't happen during
                    // the movement phase
                    if (getGame().getPhase() != IGame.Phase.PHASE_MOVEMENT) {
                        continue;
                    }
                    GameEntityChangeEvent changeEvent = (GameEntityChangeEvent) event;
                    if (changeEvent.getEntity() == null) {
                        continue; // just to be safe
                    }
                    Entity entity = getGame().getEntity(changeEvent.getEntity().getId());
                    if (entity == null) {
                        continue; // not sure how this can happen, but just to be
                        // safe
                    }
                    // a lot of odd entity changes are send during the firing phase,
                    // none of which are relevant
                    if (getGame().getPhase() == IGame.Phase.PHASE_FIRING) {
                        continue;
                    }
                    Coords position = entity.getPosition();
                    if (position == null) {
                        continue;
                    }
                    if (position.equals(getPathEnumerator().getLastKnownCoords(entity.getId()))) {
                        continue; // no sense in updating a unit if it hasn't moved
                    }
                    getOwner().log(getClass(), METHOD_NAME, "Received entity change event for "
                                                            + changeEvent.getEntity().getDisplayName() + " (ID "
                                                            + entity.getId() + ")");
                    Integer entityId = changeEvent.getEntity().getId();
                    dirtifyUnit(entityId);

                } else if (event instanceof GamePhaseChangeEvent) {
                    GamePhaseChangeEvent phaseChange = (GamePhaseChangeEvent) event;
                    getOwner().log(getClass(), METHOD_NAME, "Phase change detected: " + phaseChange.getNewPhase()
                                                                                                   .name());
                    // this marks when I can all I can start recalculating paths.
                    // All units are dirty
                    if (phaseChange.getNewPhase() == IGame.Phase.PHASE_MOVEMENT) {
                        getPathEnumerator().clear();
                        for (Enumeration<Entity> entities = getGame().getEntities(); entities.hasMoreElements(); ) {
                            Entity entity = entities.nextElement();
                            if (entity.isActive() && entity.isDeployed() && entity.getPosition() != null) {
                                getDirtyUnits().add(entity.getId());
                            }
                        }
                    }
                }
            }
            getOwner().log(getClass(), METHOD_NAME, "Events still to process: " + getEventsToProcess().size());
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Called when a unit has moved and should be put on the dirty list, as well
     * as any units who's moves contain that unit
     */
    public void dirtifyUnit(int id) {
        final String METHOD_NAME = "dirtifyUnit(int)";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            // first of all, if a unit has been removed, remove it from the list and
            // stop
            if (getGame().getEntity(id) == null) {
                getPathEnumerator().getLastKnownLocations().remove(id);
                getPathEnumerator().getUnitMovableAreas().remove(id);
                getPathEnumerator().getUnitPaths().remove(id);
                getPathEnumerator().getUnitPotentialLocations().remove(id);
                return;
            }
            // if a unit has moved or deployed, then it becomes dirty, and any units
            // with its initial or final position
            // in their list become dirty
            if (!(getGame().getEntity(id) instanceof Aero)) {
                TreeSet<Integer> toDirty = new TreeSet<>(getPathEnumerator().getEntitiesWithLocation(getGame().getEntity
                                                                                                             (id)
                                                                                                              .getPosition(),
                                                                                                     true));
                if (getPathEnumerator().getLastKnownLocations().containsKey(id)) {
                    if ((getGame().getEntity(id) != null) && getGame().getEntity(id).isSelectableThisTurn()) {
                        toDirty.addAll(getPathEnumerator().getEntitiesWithLocation(getPathEnumerator()
                                                                                           .getLastKnownLocations()
                                                                                           .get(id).getCoords(), true));
                    }
                }
                // no need to dirty units that aren't selectable this turn
                List<Integer> toRemove = new ArrayList<>();
                for (Integer index : toDirty) {
                    if ((getGame().getEntity(index) == null) || (!getGame().getEntity(index).isSelectableThisTurn())
                                                                && (getGame().getPhase() == IGame.Phase
                            .PHASE_MOVEMENT)) {
                        toRemove.add(index);
                    }
                }
                for (Integer i : toRemove) {
                    toDirty.remove(i);
                }

                if (toDirty.size() != 0) {
                    String msg = "The following units have become dirty";
                    if (getGame().getEntity(id) != null) {
                        msg += " as a result of a nearby move of " + getGame().getEntity(id).getDisplayName();
                    }

                    Iterator<Integer> dirtyIterator = toDirty.descendingIterator();
                    while (dirtyIterator.hasNext()) {
                        Integer i = dirtyIterator.next();
                        Entity e = getGame().getEntity(i);
                        if (e != null)
                            msg += "\n  " + e.getDisplayName();
                    }
                    getOwner().log(getClass(), METHOD_NAME, msg);
                }
                getDirtyUnits().addAll(toDirty);
            }
            Entity entity = getGame().getEntity(id);
            if ((entity != null) && (entity.isSelectableThisTurn()) ||
                (getGame().getPhase() != IGame.Phase.PHASE_MOVEMENT)) {
                getDirtyUnits().add(id);
            } else if (entity != null) {
                getPathEnumerator().getLastKnownLocations().put(id, CoordFacingCombo.createCoordFacingCombo(entity
                                                                                                           ));
            }
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    public PathEnumerator getPathEnumerator() {
        PATH_ENUMERATOR_LOCK.readLock().lock();
        try {
            return pathEnumerator;
        } finally {
            PATH_ENUMERATOR_LOCK.readLock().unlock();
        }
    }

    private void setPathEnumerator(PathEnumerator pathEnumerator) {
        PATH_ENUMERATOR_LOCK.writeLock().lock();
        try {
            this.pathEnumerator = pathEnumerator;
        } finally {
            PATH_ENUMERATOR_LOCK.writeLock().unlock();
        }
    }

    private ConcurrentSkipListSet<Integer> getDirtyUnits() {
        return dirtyUnits;
    }

    private ConcurrentLinkedQueue<GameEvent> getEventsToProcess() {
        return eventsToProcess;
    }

    private AtomicBoolean getWaitWhenDone() {
        return waitWhenDone;
    }

    private AtomicBoolean getWaiting() {
        return waiting;
    }

    private AtomicBoolean getDone() {
        return done;
    }

    private Princess getOwner() {
        return owner;
    }

    private IGame getGame() {
        GAME_LOCK.readLock().lock();
        try {
            return game;
        } finally {
            GAME_LOCK.readLock().unlock();
        }
    }

    private void setGame(IGame game) {
        GAME_LOCK.writeLock().lock();
        try {
            this.game = game;
        } finally {
            GAME_LOCK.writeLock().unlock();
        }
    }
}
