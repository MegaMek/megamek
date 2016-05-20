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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.common.Aero;
import megamek.common.Board;
import megamek.common.Building;
import megamek.common.ComputeECM;
import megamek.common.Coords;
import megamek.common.ECMInfo;
import megamek.common.Entity;
import megamek.common.Flare;
import megamek.common.Game;
import megamek.common.GameTurn;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.IPlayer;
import megamek.common.Minefield;
import megamek.common.Mounted;
import megamek.common.PlanetaryConditions;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.TagInfo;
import megamek.common.UnitLocation;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.DodgeAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameCFREvent;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEvent;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.GameVictoryEvent;
import megamek.common.logging.LogLevel;
import megamek.common.net.Packet;
import megamek.common.options.GameOptions;
import megamek.common.preference.PreferenceManager;
import megamek.server.SmokeCloud;

/**
 * unit_potential_locations keeps track of all the potential coordinates and
 * facings a unit could reach It tries to keep all the calculations up to date,
 * and do most of the work when the opponent is moving
 */
public class Precognition implements Runnable {

    private final Princess owner;
    
    /**
     *  Precognition's version of the game, which should mirror the game in
     *  Princess, but should not be the same reference.  If Precognition and
     *  Princess share the same game reference, than this will cause concurrency
     *  issues. 
     */
    private IGame game;
    private final ReentrantReadWriteLock GAME_LOCK = new ReentrantReadWriteLock();

    /**
     * Computing ECMInfo requires iterating over all Entities in the Game and 
     * this can be an expensive operation, so it's cheaper to use cache it and
     * re-use the cache.
     */
    private List<ECMInfo> ecmInfo;
        
    private PathEnumerator pathEnumerator;
    private final ReentrantReadWriteLock PATH_ENUMERATOR_LOCK = new ReentrantReadWriteLock();


    // units who's path I need to update
    private final ConcurrentSkipListSet<Integer> dirtyUnits = new ConcurrentSkipListSet<>();

    // events that may affect which units are dirty
    private final ConcurrentLinkedQueue<GameEvent> eventsToProcess = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean waitWhenDone = new AtomicBoolean(false); // used for pausing
    private final AtomicBoolean waiting = new AtomicBoolean(false);
    private final AtomicBoolean done = new AtomicBoolean(false);

    public Precognition(Princess owner) {
        this.owner = owner;
        this.game = new Game();
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
        setPathEnumerator(new PathEnumerator(owner, getGame()));
        // Initialize ECM Info, especially important if Princess added mid-game
        ecmInfo = ComputeECM.computeAllEntitiesECMInfo(
                getGame().getEntitiesVector());
    }
    
    /**
     * Pared down version of Client.handlePacket; essentially it's only looking
     * for packets that update Game.  This ensures that Precognition's Game
     * instance stays up-to-date with Princess's instance of Game.
     * @param c
     */
    @SuppressWarnings("unchecked")
    protected void handlePacket(Packet c) {
        if (c == null) {
            System.out.println("client: got null packet"); //$NON-NLS-1$
            return;
        }
        switch (c.getCommand()) {
            case Packet.COMMAND_PLAYER_UPDATE:
                receivePlayerInfo(c);
                break;
            case Packet.COMMAND_PLAYER_READY:
                getPlayer(c.getIntValue(0)).setDone(c.getBooleanValue(1));
                break;
            case Packet.COMMAND_PLAYER_ADD:
                receivePlayerInfo(c);
                break;
            case Packet.COMMAND_PLAYER_REMOVE:
                getGame().removePlayer(c.getIntValue(0));
                break;
            case Packet.COMMAND_CHAT:
                getGame().processGameEvent(new GamePlayerChatEvent(this, null, 
                        (String) c.getObject(0)));
                break;
            case Packet.COMMAND_ENTITY_ADD:
                receiveEntityAdd(c);
                break;
            case Packet.COMMAND_ENTITY_UPDATE:
                receiveEntityUpdate(c);
                break;
            case Packet.COMMAND_ENTITY_REMOVE:
                receiveEntityRemove(c);
                break;
            case Packet.COMMAND_ENTITY_VISIBILITY_INDICATOR:
                receiveEntityVisibilityIndicator(c);
                break;
            case Packet.COMMAND_SENDING_MINEFIELDS:
                receiveSendingMinefields(c);
                break;
            case Packet.COMMAND_SENDING_ILLUM_HEXES:
                receiveIlluminatedHexes(c);
                break;
            case Packet.COMMAND_CLEAR_ILLUM_HEXES:
                getGame().clearIlluminatedPositions();
                break;
            case Packet.COMMAND_UPDATE_MINEFIELDS:
                receiveUpdateMinefields(c);
                break;
            case Packet.COMMAND_DEPLOY_MINEFIELDS:
                receiveDeployMinefields(c);
                break;
            case Packet.COMMAND_REVEAL_MINEFIELD:
                receiveRevealMinefield(c);
                break;
            case Packet.COMMAND_REMOVE_MINEFIELD:
                receiveRemoveMinefield(c);
                break;
            case Packet.COMMAND_ADD_SMOKE_CLOUD:
                SmokeCloud cloud = (SmokeCloud) c.getObject(0);
                getGame().addSmokeCloud(cloud);
                break;
            case Packet.COMMAND_CHANGE_HEX:
                getGame().getBoard().setHex((Coords) c.getObject(0),
                                       (IHex) c.getObject(1));
                break;
            case Packet.COMMAND_CHANGE_HEXES:
                List<Coords> coords = new ArrayList<Coords>(
                        (Set<Coords>) c.getObject(0));
                List<IHex> hexes = new ArrayList<IHex>(
                        (Set<IHex>) c.getObject(1));
                getGame().getBoard().setHexes(coords, hexes);
                break;
            case Packet.COMMAND_BLDG_UPDATE:
                receiveBuildingUpdate(c);
                break;
            case Packet.COMMAND_BLDG_COLLAPSE:
                receiveBuildingCollapse(c);
                break;
            case Packet.COMMAND_PHASE_CHANGE:
                getGame().setPhase((IGame.Phase) c.getObject(0));
                break;
            case Packet.COMMAND_TURN:
                getGame().setTurnIndex(c.getIntValue(0));
                break;
            case Packet.COMMAND_ROUND_UPDATE:
                getGame().setRoundCount(c.getIntValue(0));
                break;
            case Packet.COMMAND_SENDING_TURNS:
                receiveTurns(c);
                break;
            case Packet.COMMAND_SENDING_BOARD:
                receiveBoard(c);
                break;
            case Packet.COMMAND_SENDING_ENTITIES:
                receiveEntities(c);
                break;
            case Packet.COMMAND_SENDING_REPORTS:
            case Packet.COMMAND_SENDING_REPORTS_TACTICAL_GENIUS:
                getGame().addReports((Vector<Report>) c.getObject(0));
                break;
            case Packet.COMMAND_SENDING_REPORTS_ALL:
                Vector<Vector<Report>> allReports = (Vector<Vector<Report>>) c
                        .getObject(0);
                getGame().setAllReports(allReports);
                break;
            case Packet.COMMAND_ENTITY_ATTACK:
                receiveAttack(c);
                break;
            case Packet.COMMAND_SENDING_GAME_SETTINGS:
                getGame().setOptions((GameOptions) c.getObject(0));
                break;
            case Packet.COMMAND_SENDING_PLANETARY_CONDITIONS:
                getGame().setPlanetaryConditions((PlanetaryConditions) c
                        .getObject(0));
                getGame().processGameEvent(new GameSettingsChangeEvent(this));
                break;
            case Packet.COMMAND_SENDING_TAGINFO:
                Vector<TagInfo> vti = (Vector<TagInfo>) c.getObject(0);
                for (TagInfo ti : vti) {
                    getGame().addTagInfo(ti);
                }
                break;
            case Packet.COMMAND_RESET_TAGINFO:
                getGame().resetTagInfo();
                break;
            case Packet.COMMAND_SENDING_ARTILLERYATTACKS:
                Vector<ArtilleryAttackAction> v = (Vector<ArtilleryAttackAction>) c
                        .getObject(0);
                getGame().setArtilleryVector(v);
                break;
            case Packet.COMMAND_SENDING_FLARES:
                Vector<Flare> v2 = (Vector<Flare>) c.getObject(0);
                getGame().setFlares(v2);
                break;
            case Packet.COMMAND_SENDING_SPECIAL_HEX_DISPLAY:
                getGame().getBoard().setSpecialHexDisplayTable(
                        (Hashtable<Coords, Collection<SpecialHexDisplay>>) c
                                .getObject(0));
                getGame().processGameEvent(new GameBoardChangeEvent(this));
                break;
            case Packet.COMMAND_ENTITY_NOVA_NETWORK_CHANGE:
                receiveEntityNovaNetworkModeChange(c);
                break;
            case Packet.COMMAND_CLIENT_FEEDBACK_REQUEST:
                int cfrType = (int) c.getData()[0];
                GameCFREvent cfrEvt = new GameCFREvent(this, cfrType);
                switch (cfrType) {
                    case (Packet.COMMAND_CFR_DOMINO_EFFECT):
                        cfrEvt.setEntityId((int) c.getData()[1]);
                        break;
                    case Packet.COMMAND_CFR_AMS_ASSIGN:
                        cfrEvt.setEntityId((int) c.getData()[1]);
                        cfrEvt.setAmsEquipNum((int) c.getData()[2]);
                        cfrEvt.setWAAs((List<WeaponAttackAction>) c.getData()[3]);
                        break;
                    case Packet.COMMAND_CFR_APDS_ASSIGN:
                        cfrEvt.setEntityId((int) c.getData()[1]);
                        cfrEvt.setApdsDists((List<Integer>) c.getData()[2]);
                        cfrEvt.setWAAs((List<WeaponAttackAction>) c.getData()[3]);
                        break;
                }
                getGame().processGameEvent(cfrEvt);
                break;
            case Packet.COMMAND_GAME_VICTORY_EVENT:
                GameVictoryEvent gve = new GameVictoryEvent(this, getGame());
                getGame().processGameEvent(gve);
                break;
        }
    }

    public void pause() {
        final String METHOD_NAME = "pause()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            getWaitWhenDone().set(true);
            while (!getWaiting().get() && !getDone().get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    public synchronized void unPause() {
        final String METHOD_NAME = "unpause()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            getWaitWhenDone().set(false);
            notifyAll();
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Tells the thread there's something to do Note, you can't just call
     * notifyAll in the event listener because it doesn't have the thread
     * something something.
     */
    public synchronized void wakeUp() {
        final String METHOD_NAME = "wake_up()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            notifyAll();
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    private boolean isEntityOnMap(final Entity entity) {
        return entity.isDeployed() && !entity.isOffBoard();
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
            for (Entity entity : getGame().getEntitiesVector()) {
                // If Precog is done, just exit
                if (getDone().get()) {
                    return;
                }
                if (!isEntityOnMap(entity)) {
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
                // If Precog is done, just exit
                if (getDone().get()) {
                    return;
                }
                
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
                    ecmInfo = ComputeECM.computeAllEntitiesECMInfo(
                            getGame().getEntitiesVector());
                } else if (!getDirtyUnits().isEmpty()) {
                    Entity entity = getGame().getEntity(getDirtyUnits().pollFirst());
                    if ((entity != null) && isEntityOnMap(entity)) {
                        unPause();
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
    public synchronized void waitForUnpause() {
        final String METHOD_NAME = "wait_for_unpause()";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            while (!getDone().get() &&
                   (getWaitWhenDone().get() ||
                    (getEventsToProcess().isEmpty() &&
                     getDirtyUnits().isEmpty()))) {
                getOwner().log(getClass(), METHOD_NAME,
                               "waitWhenDone = " + getWaitWhenDone() +
                               " :: eventsToProcess = " +
                               getEventsToProcess().size() +
                               " :: dirtyUnits = " + getDirtyUnits().size());
                getWaiting().set(true);
                try {
                    wait();
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
                        for (Entity entity : getGame().getEntitiesVector()) {
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
            getOwner().log(getClass(), "getPathEnumerator()()", LogLevel.DEBUG, "PATH_ENUMERATOR_LOCK read locked.");
            return pathEnumerator;
        } finally {
            PATH_ENUMERATOR_LOCK.readLock().unlock();
            getOwner().log(getClass(), "getPathEnumerator()()", LogLevel.DEBUG, "PATH_ENUMERATOR_LOCK read unlocked.");
        }
    }

    private void setPathEnumerator(PathEnumerator pathEnumerator) {
        PATH_ENUMERATOR_LOCK.writeLock().lock();
        try {
            getOwner().log(getClass(), "setPathEnumerator(PathEnumerator)", LogLevel.DEBUG,
                           "PATH_ENUMERATOR_LOCK write locked.");
            this.pathEnumerator = pathEnumerator;
        } finally {
            getOwner().log(getClass(), "setPathEnumerator(PathEnumerator)", LogLevel.DEBUG,
                           "PATH_ENUMERATOR_LOCK write unlocked.");
            PATH_ENUMERATOR_LOCK.writeLock().unlock();
        }
    }
    
    public List<ECMInfo> getECMInfo() {
        return Collections.unmodifiableList(ecmInfo);
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
    
    public void resetGame() {
        GAME_LOCK.writeLock();
        try {
            getOwner().log(getClass(), "resetGame()", LogLevel.DEBUG, "GAME_LOCK write locked.");
            game.reset();
        } finally {
            GAME_LOCK.writeLock().unlock();
            getOwner().log(getClass(), "resetGame()", LogLevel.DEBUG, "GAME_LOCK write unlocked.");
        }
    }

    private IGame getGame() {
        GAME_LOCK.readLock().lock();
        try {
            getOwner().log(getClass(), "getGame()", LogLevel.DEBUG, "GAME_LOCK read locked.");
            return game;
        } finally {
            GAME_LOCK.readLock().unlock();
            getOwner().log(getClass(), "getGame()", LogLevel.DEBUG, "GAME_LOCK read unlocked.");
        }
    }
   
    /**
     * Returns the individual player assigned the index parameter.
     */
    protected IPlayer getPlayer(int idx) {
        return getGame().getPlayer(idx);
    }

    /**
     * Receives player information from the message packet.
     */
    protected void receivePlayerInfo(Packet c) {
        int pindex = c.getIntValue(0);
        IPlayer newPlayer = (IPlayer) c.getObject(1);
        if (getPlayer(newPlayer.getId()) == null) {
            getGame().addPlayer(pindex, newPlayer);
        } else {
            getGame().setPlayer(pindex, newPlayer);
        }

        PreferenceManager.getClientPreferences().setLastPlayerColor(
                newPlayer.getColorIndex());
        PreferenceManager.getClientPreferences().setLastPlayerCategory(
                newPlayer.getCamoCategory());
        PreferenceManager.getClientPreferences().setLastPlayerCamoName(
                newPlayer.getCamoFileName());
    }

    /**
     * Loads the turn list from the data in the packet
     */
    @SuppressWarnings("unchecked")
    protected void receiveTurns(Packet packet) {
        getGame().setTurnVector((List<GameTurn>) packet.getObject(0));
    }

    /**
     * Loads the board from the data in the net command.
     */
    protected void receiveBoard(Packet c) {
        Board newBoard = (Board) c.getObject(0);
        getGame().setBoard(newBoard);
    }

    /**
     * Loads the entities from the data in the net command.
     */
    @SuppressWarnings("unchecked")
    protected void receiveEntities(Packet c) {
        List<Entity> newEntities = (List<Entity>) c.getObject(0);
        List<Entity> newOutOfGame = (List<Entity>) c.getObject(1);

        // Replace the entities in the game.
        getGame().setEntitiesVector(newEntities);
        if (newOutOfGame != null) {
            getGame().setOutOfGameEntitiesVector(newOutOfGame);
        }
    }

    /**
     * Loads entity update data from the data in the net command.
     */
    @SuppressWarnings("unchecked")
    protected void receiveEntityUpdate(Packet c) {
        int eindex = c.getIntValue(0);
        Entity entity = (Entity) c.getObject(1);
        Vector<UnitLocation> movePath = (Vector<UnitLocation>) c.getObject(2);
        // Replace this entity in the game.
        getGame().setEntity(eindex, entity, movePath);
    }

    protected void receiveEntityAdd(Packet packet) {
        @SuppressWarnings("unchecked")
        List<Integer> entityIds = (List<Integer>) packet.getObject(0);
        @SuppressWarnings("unchecked")
        List<Entity> entities = (List<Entity>) packet.getObject(1);

        assert (entityIds.size() == entities.size());
        for (int i = 0; i < entityIds.size(); i++) {
            assert (entityIds.get(i) == entities.get(i).getId());
        }
        getGame().addEntities(entities);
    }

    protected void receiveEntityRemove(Packet packet) {
        @SuppressWarnings("unchecked")
        List<Integer> entityIds = (List<Integer>) packet.getObject(0);
        int condition = packet.getIntValue(1);
        // Move the unit to its final resting place.
        getGame().removeEntities(entityIds, condition);
    }

    @SuppressWarnings("unchecked")
    protected void receiveEntityVisibilityIndicator(Packet packet) {
        Entity e = getGame().getEntity(packet.getIntValue(0));
        if (e != null) { // we may not have this entity due to double blind
            e.setEverSeenByEnemy(packet.getBooleanValue(1));
            e.setVisibleToEnemy(packet.getBooleanValue(2));
            e.setDetectedByEnemy(packet.getBooleanValue(3));
            e.setWhoCanSee((Vector<IPlayer>)packet.getObject(4));
            e.setWhoCanDetect((Vector<IPlayer>)packet.getObject(5));
            // this next call is only needed sometimes, but we'll just
            // call it everytime
            getGame().processGameEvent(new GameEntityChangeEvent(this, e));
        }
    }

    @SuppressWarnings("unchecked")
    protected void receiveDeployMinefields(Packet packet) {
        getGame().addMinefields((Vector<Minefield>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveSendingMinefields(Packet packet) {
        getGame().setMinefields((Vector<Minefield>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveIlluminatedHexes(Packet p) {
        getGame().setIlluminatedPositions((HashSet<Coords>) p.getObject(0));
    }

    protected void receiveRevealMinefield(Packet packet) {
        getGame().addMinefield((Minefield) packet.getObject(0));
    }

    protected void receiveRemoveMinefield(Packet packet) {
        getGame().removeMinefield((Minefield) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveUpdateMinefields(Packet packet) {
        // only update information if you know about the minefield
        Vector<Minefield> newMines = new Vector<Minefield>();
        for (Minefield mf : (Vector<Minefield>) packet.getObject(0)) {
            if (getOwner().getLocalPlayer().containsMinefield(mf)) {
                newMines.add(mf);
            }
        }
        if (newMines.size() > 0) {
            getGame().resetMinefieldDensity(newMines);
        }
    }

    @SuppressWarnings("unchecked")
    protected void receiveBuildingUpdate(Packet packet) {
        getGame().getBoard().updateBuildings((Vector<Building>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveBuildingCollapse(Packet packet) {
        getGame().getBoard().collapseBuilding((Vector<Coords>) packet.getObject(0));
    }

    /**
     * Loads entity firing data from the data in the net command
     */
    @SuppressWarnings("unchecked")
    protected void receiveAttack(Packet c) {
        List<EntityAction> vector = (List<EntityAction>) c.getObject(0);
        int charge = c.getIntValue(1);
        boolean addAction = true;
        for (EntityAction ea : vector) {
            int entityId = ea.getEntityId();
            if ((ea instanceof TorsoTwistAction) && getGame().hasEntity(entityId)) {
                TorsoTwistAction tta = (TorsoTwistAction) ea;
                Entity entity = getGame().getEntity(entityId);
                entity.setSecondaryFacing(tta.getFacing());
            } else if ((ea instanceof FlipArmsAction)
                    && getGame().hasEntity(entityId)) {
                FlipArmsAction faa = (FlipArmsAction) ea;
                Entity entity = getGame().getEntity(entityId);
                entity.setArmsFlipped(faa.getIsFlipped());
            } else if ((ea instanceof DodgeAction) && getGame().hasEntity(entityId)) {
                Entity entity = getGame().getEntity(entityId);
                entity.dodging = true;
                addAction = false;
            } else if (ea instanceof AttackAction) {
                // The equipment type of a club needs to be restored.
                if (ea instanceof ClubAttackAction) {
                    ClubAttackAction caa = (ClubAttackAction) ea;
                    Mounted club = caa.getClub();
                    club.restore();
                }
            }

            if (addAction) {
                // track in the appropriate list
                if (charge == 0) {
                    getGame().addAction(ea);
                } else if (charge == 1) {
                    getGame().addCharge((AttackAction) ea);
                }
            }
        }
    }
    
    /**
     * receive and process an entity nova network mode change packet
     *
     * @param c
     */
    protected void receiveEntityNovaNetworkModeChange(Packet c) {
        try {
            int entityId = c.getIntValue(0);
            String networkID = c.getObject(1).toString();
            Entity e = getGame().getEntity(entityId);
            if (e != null) {
                e.setNewRoundNovaNetworkString(networkID);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
