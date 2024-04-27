/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.bot.princess;

import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;
import megamek.common.net.packets.Packet;
import megamek.common.net.enums.PacketCommand;
import megamek.common.options.GameOptions;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.server.SmokeCloud;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * unit_potential_locations keeps track of all the potential coordinates and
 * facings a unit could reach It tries to keep all the calculations up to date,
 * and do most of the work when the opponent is moving
 */
public class Precognition implements Runnable {

    private final Princess owner;

    /**
     * Precognition's version of the game, which should mirror the game in
     * Princess, but should not be the same reference. If Precognition and
     * Princess share the same game reference, then this will cause concurrency
     * issues.
     */
    private Game game;
    private final ReentrantLock GAME_LOCK = new ReentrantLock();

    /**
     * Computing ECMInfo requires iterating over all Entities in the Game and 
     * this can be an expensive operation, so it's cheaper to use cache it and
     * re-use the cache.
     */
    private List<ECMInfo> ecmInfo;

    private PathEnumerator pathEnumerator;
    private final ReentrantReadWriteLock PATH_ENUMERATOR_LOCK = new ReentrantReadWriteLock();

    // units whose path I need to update
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
     * @param c The packet to be handled.
     */
    @SuppressWarnings("unchecked")
    void handlePacket(Packet c) {
        if (c == null) {
            LogManager.getLogger().warn("Client: got null packet");
            return;
        }
        // Game isn't thread safe; other threads shouldn't use  game while
        // it may be being updated
        GAME_LOCK.lock();
        try {
            switch (c.getCommand()) {
                case PLAYER_UPDATE:
                    receivePlayerInfo(c);
                    break;
                case PLAYER_READY:
                    final Player player = getPlayer(c.getIntValue(0));
                    if (player != null) {
                        player.setDone(c.getBooleanValue(1));
                        game.processGameEvent(new GamePlayerChangeEvent(player, player));
                    }
                    break;
                case PLAYER_ADD:
                    receivePlayerInfo(c);
                    break;
                case PLAYER_REMOVE:
                    getGame().removePlayer(c.getIntValue(0));
                    break;
                case CHAT:
                    getGame().processGameEvent(new GamePlayerChatEvent(this, null,
                            (String) c.getObject(0)));
                    break;
                case ENTITY_ADD:
                    receiveEntityAdd(c);
                    break;
                case ENTITY_UPDATE:
                    receiveEntityUpdate(c);
                    break;
                case ENTITY_REMOVE:
                    receiveEntityRemove(c);
                    break;
                case ENTITY_VISIBILITY_INDICATOR:
                    receiveEntityVisibilityIndicator(c);
                    break;
                case SENDING_MINEFIELDS:
                    receiveSendingMinefields(c);
                    break;
                case SENDING_ILLUM_HEXES:
                    receiveIlluminatedHexes(c);
                    break;
                case CLEAR_ILLUM_HEXES:
                    getGame().clearIlluminatedPositions();
                    break;
                case UPDATE_MINEFIELDS:
                    receiveUpdateMinefields(c);
                    break;
                case DEPLOY_MINEFIELDS:
                    receiveDeployMinefields(c);
                    break;
                case REVEAL_MINEFIELD:
                    receiveRevealMinefield(c);
                    break;
                case REMOVE_MINEFIELD:
                    receiveRemoveMinefield(c);
                    break;
                case ADD_SMOKE_CLOUD:
                    SmokeCloud cloud = (SmokeCloud) c.getObject(0);
                    getGame().addSmokeCloud(cloud);
                    break;
                case CHANGE_HEX:
                    getGame().getBoard().setHex((Coords) c.getObject(0), (Hex) c.getObject(1));
                    break;
                case CHANGE_HEXES:
                    List<Coords> coords = new ArrayList<>((Set<Coords>) c.getObject(0));
                    List<Hex> hexes = new ArrayList<>((Set<Hex>) c.getObject(1));
                    getGame().getBoard().setHexes(coords, hexes);
                    break;
                case BLDG_UPDATE:
                    receiveBuildingUpdate(c);
                    break;
                case BLDG_COLLAPSE:
                    receiveBuildingCollapse(c);
                    break;
                case PHASE_CHANGE:
                    getGame().setPhase((GamePhase) c.getObject(0));
                    break;
                case TURN:
                    getGame().setTurnIndex(c.getIntValue(0), c.getIntValue(1));
                    break;
                case ROUND_UPDATE:
                    getGame().setRoundCount(c.getIntValue(0));
                    break;
                case SENDING_TURNS:
                    receiveTurns(c);
                    break;
                case SENDING_BOARD:
                    getGame().receiveBoards((Map<Integer, Board>) c.getObject(0));
                    break;
                case SENDING_ENTITIES:
                    receiveEntities(c);
                    break;
                case SENDING_REPORTS:
                case SENDING_REPORTS_TACTICAL_GENIUS:
                    getGame().addReports((Vector<Report>) c.getObject(0));
                    break;
                case SENDING_REPORTS_ALL:
                    Vector<Vector<Report>> allReports = (Vector<Vector<Report>>) c.getObject(0);
                    getGame().setAllReports(allReports);
                    break;
                case ENTITY_ATTACK:
                    receiveAttack(c);
                    break;
                case SENDING_GAME_SETTINGS:
                    getGame().setOptions((GameOptions) c.getObject(0));
                    break;
                case SENDING_PLANETARY_CONDITIONS:
                    getGame().setPlanetaryConditions((PlanetaryConditions) c.getObject(0));
                    getGame().processGameEvent(new GameSettingsChangeEvent(this));
                    break;
                case SENDING_TAG_INFO:
                    Vector<TagInfo> vti = (Vector<TagInfo>) c.getObject(0);
                    for (TagInfo ti : vti) {
                        getGame().addTagInfo(ti);
                    }
                    break;
                case RESET_TAG_INFO:
                    getGame().resetTagInfo();
                    break;
                case SENDING_ARTILLERY_ATTACKS:
                    Vector<ArtilleryAttackAction> v = (Vector<ArtilleryAttackAction>) c.getObject(0);
                    getGame().setArtilleryVector(v);
                    break;
                case SENDING_FLARES:
                    Vector<Flare> v2 = (Vector<Flare>) c.getObject(0);
                    getGame().setFlares(v2);
                    break;
                case SENDING_SPECIAL_HEX_DISPLAY:
                    getGame().getBoard().setSpecialHexDisplayTable(
                            (Hashtable<Coords, Collection<SpecialHexDisplay>>) c.getObject(0));
                    getGame().processGameEvent(new GameBoardChangeEvent(this));
                    break;
                case ENTITY_NOVA_NETWORK_CHANGE:
                    receiveEntityNovaNetworkModeChange(c);
                    break;
                case CLIENT_FEEDBACK_REQUEST:
                    final PacketCommand cfrType = (PacketCommand) c.getData()[0];
                    GameCFREvent cfrEvt = new GameCFREvent(this, cfrType);
                    switch (cfrType) {
                        case CFR_DOMINO_EFFECT:
                            cfrEvt.setEntityId((int) c.getData()[1]);
                            break;
                        case CFR_AMS_ASSIGN:
                            cfrEvt.setEntityId((int) c.getData()[1]);
                            cfrEvt.setAmsEquipNum((int) c.getData()[2]);
                            cfrEvt.setWAAs((List<WeaponAttackAction>) c.getData()[3]);
                            break;
                        case CFR_APDS_ASSIGN:
                            cfrEvt.setEntityId((int) c.getData()[1]);
                            cfrEvt.setApdsDists((List<Integer>) c.getData()[2]);
                            cfrEvt.setWAAs((List<WeaponAttackAction>) c.getData()[3]);
                            break;
                        default:
                            break;
                    }
                    getGame().processGameEvent(cfrEvt);
                    break;
                case GAME_VICTORY_EVENT:
                    GameVictoryEvent gve = new GameVictoryEvent(this, getGame());
                    getGame().processGameEvent(gve);
                    break;
                case ENTITY_MULTIUPDATE:
                    receiveEntitiesUpdate(c);
                    break;
                case SERVER_GREETING:
                case SERVER_CORRECT_NAME:
                case CLOSE_CONNECTION:
                case SERVER_VERSION_CHECK:
                case ILLEGAL_CLIENT_VERSION:
                case LOCAL_PN:
                case PRINCESS_SETTINGS:
                case FORCE_UPDATE:
                case FORCE_DELETE:
                case SENDING_REPORTS_SPECIAL:
                case SENDING_MAP_SETTINGS:
                case END_OF_GAME:
                case SEND_SAVEGAME:
                case LOAD_SAVEGAME:
                case SENDING_AVAILABLE_MAP_SIZES:
                    LogManager.getLogger().debug("Intentionally ignoring PacketCommand: {}", c.getCommand().name());
                    break;
                default:
                    LogManager.getLogger().error("Attempted to parse unknown PacketCommand: {}", c.getCommand().name());
                    break;
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        } finally {
            GAME_LOCK.unlock();
        }
    }

    /**
     * Update multiple entities from the server. Used only in the lobby phase.
     */
    @SuppressWarnings("unchecked")
    protected void receiveEntitiesUpdate(Packet c) {
        Collection<Entity> entities = (Collection<Entity>) c.getObject(0);
        for (Entity entity: entities) {
            getGame().setEntity(entity.getId(), entity);
        }
    }

    private void pause() {
        getWaitWhenDone().set(true);
        while (!getWaiting().get() && !getDone().get()) {
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {

            }
        }
    }

    synchronized void unPause() {
        getWaitWhenDone().set(false);
        notifyAll();
    }

    /**
     * Tells the thread there's something to do Note, you can't just call
     * notifyAll in the event listener because it doesn't have the thread
     */
    private synchronized void wakeUp() {
        notifyAll();
    }

    private boolean isEntityOnMap(final Entity entity) {
        return entity.isDeployed() && !entity.isOffBoard();
    }

    /**
     * Makes sure pathEnumerator has up-to-date information about other units
     * locations call this right before making a move. automatically pauses.
     */
    void ensureUpToDate() {
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
                    LogManager.getLogger().debug("recalculating paths for " + entity.getDisplayName());
                    getPathEnumerator().recalculateMovesFor(entity);
                    LogManager.getLogger().debug("finished recalculating paths for " + entity.getDisplayName());
                }
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void run() {
        try {
            // todo There's probably a better way to handle this than a loop that only exits on an error.
            while (!getDone().get()) {
                if (!getEventsToProcess().isEmpty()) {
                    processGameEvents();
                    ecmInfo = ComputeECM.computeAllEntitiesECMInfo(
                            getGame().getEntitiesVector());
                } else if (!getDirtyUnits().isEmpty()) {
                    Entity entity = getGame().getEntity(getDirtyUnits().pollFirst());
                    if ((entity != null) && isEntityOnMap(entity)) {
                        unPause();
                        LogManager.getLogger().debug("recalculating paths for " + entity.getDisplayName());
                        getPathEnumerator().recalculateMovesFor(entity);
                        LogManager.getLogger().debug("finished recalculating paths for " + entity.getDisplayName());
                    }
                } else if (getWaitWhenDone().get()) {
                    waitForUnpause(); // paused for a reason
                } else {
                    waitForUnpause(); // idling because there's nothing to do
                }
            }
        } catch (Exception ignored) {
        }
    }

    void signalDone() {
        getDone().set(true);
    }

    /**
     * Waits until the thread is not paused, and there's indication that it has
     * something to do
     */
    private synchronized void waitForUnpause() {
        try {
            while (!getDone().get() &&
                   (getWaitWhenDone().get() ||
                    (getEventsToProcess().isEmpty() &&
                     getDirtyUnits().isEmpty()))) {
                LogManager.getLogger().debug("waitWhenDone = " + getWaitWhenDone() +
                               " :: eventsToProcess = " + getEventsToProcess().size() +
                               " :: dirtyUnits = " + getDirtyUnits().size());
                getWaiting().set(true);
                try {
                    wait();
                } catch (InterruptedException ignored) {

                }
            }
            getWaiting().set(false);
        } catch (Exception ignored) {

        }
    }

    /**
     * Process game events that have happened since the thread last checked i.e.
     * if a unit has moved, my precaculated paths are no longer valid
     */
    private void processGameEvents() {
        // We don't want Game to change while this is happening
        GAME_LOCK.lock();
        try {
            LinkedList<GameEvent> eventsToProcessIterator = new LinkedList<>(getEventsToProcess());
            int numEvents = eventsToProcessIterator.size();
            for (int count = 0; count < numEvents; count++) {
                LogManager.getLogger().debug("Processing event " + (count + 1) + " out of " + numEvents);
                GameEvent event = eventsToProcessIterator.get(count);
                if (event == null) {
                    continue;
                }
                LogManager.getLogger().debug("Processing " + event);
                getEventsToProcess().remove(event);
                if (event instanceof GameEntityChangeEvent) {
                    // Ignore entity changes that don't happen during movement
                    if (!getGame().getPhase().isMovement()) {
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

                    // a lot of odd entity changes are sent during the firing phase,
                    // none of which are relevant
                    if (getGame().getPhase().isFiring()) {
                        continue;
                    }
                    Coords position = entity.getPosition();
                    if (position == null) {
                        continue;
                    }
                    if (position.equals(getPathEnumerator().getLastKnownCoords(entity.getId()))) {
                        continue; // no sense in updating a unit if it hasn't moved
                    }
                    LogManager.getLogger().debug("Received entity change event for "
                                    + changeEvent.getEntity().getDisplayName()
                                    + " (ID " + entity.getId() + ")");
                    dirtifyUnit(changeEvent.getEntity().getId());
                } else if (event instanceof GamePhaseChangeEvent) {
                    GamePhaseChangeEvent phaseChange = (GamePhaseChangeEvent) event;
                    LogManager.getLogger().debug("Phase change detected: " + phaseChange.getNewPhase().name());
                    // this marks when I can all I can start recalculating paths.
                    // All units are dirty
                    if (phaseChange.getNewPhase().isMovement()) {
                        getPathEnumerator().clear();
                        for (Entity entity : getGame().getEntitiesVector()) {
                            if (entity.isActive() && entity.isDeployed() && entity.getPosition() != null) {
                                getDirtyUnits().add(entity.getId());
                            }
                        }
                    }
                }
            }
            LogManager.getLogger().debug("Events still to process: " + getEventsToProcess().size());
        } finally {
            GAME_LOCK.unlock();
        }
    }

    /**
     * Called when a unit has moved and should be put on the dirty list, as well
     * as any units who's moves contain that unit
     */
    private void dirtifyUnit(int id) {
        // Prevent Game from changing while processing
        GAME_LOCK.lock();
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
            if (!getGame().getEntity(id).isAero()) {
                TreeSet<Integer> toDirty = new TreeSet<>(
                        getPathEnumerator().getEntitiesWithLocation(
                                getGame().getEntity(id).getPosition(), true));
                if (getPathEnumerator().getLastKnownLocations()
                        .containsKey(id)) {
                    if ((getGame().getEntity(id) != null)
                            && getGame().getEntity(id).isSelectableThisTurn()) {
                        toDirty.addAll(getPathEnumerator()
                                .getEntitiesWithLocation(getPathEnumerator()
                                        .getLastKnownLocations().get(id)
                                        .getCoords(), true));
                    }
                }
                // no need to dirty units that aren't selectable this turn
                List<Integer> toRemove = new ArrayList<>();
                for (Integer index : toDirty) {
                    if ((getGame().getEntity(index) == null)
                            || (!getGame().getEntity(index).isSelectableThisTurn()
                                    && getGame().getPhase().isMovement())) {
                        toRemove.add(index);
                    }
                }

                for (Integer i : toRemove) {
                    toDirty.remove(i);
                }

                if (!toDirty.isEmpty()) {
                    StringBuilder msg = new StringBuilder("The following units have become dirty");
                    if (getGame().getEntity(id) != null) {
                        msg.append(" as a result of a nearby move of ")
                                .append(getGame().getEntity(id).getDisplayName());
                    }

                    Iterator<Integer> dirtyIterator = toDirty.descendingIterator();
                    while (dirtyIterator.hasNext()) {
                        Integer i = dirtyIterator.next();
                        Entity e = getGame().getEntity(i);
                        if (e != null) {
                            msg.append("\n  ").append(e.getDisplayName());
                        }
                    }
                    LogManager.getLogger().debug(msg.toString());
                }
                getDirtyUnits().addAll(toDirty);
            }
            Entity entity = getGame().getEntity(id);
            if (((entity != null) && entity.isSelectableThisTurn())
                    || !getGame().getPhase().isMovement()) {
                getDirtyUnits().add(id);
            } else if (entity != null) {
                getPathEnumerator().getLastKnownLocations().put(id,
                        CoordFacingCombo.createCoordFacingCombo(entity));
            }
        } finally {
            GAME_LOCK.unlock();
        }
    }

    PathEnumerator getPathEnumerator() {
        PATH_ENUMERATOR_LOCK.readLock().lock();
        try {
            LogManager.getLogger().debug("PATH_ENUMERATOR_LOCK read locked.");
            return pathEnumerator;
        } finally {
            PATH_ENUMERATOR_LOCK.readLock().unlock();
            LogManager.getLogger().debug("PATH_ENUMERATOR_LOCK read unlocked.");
        }
    }

    private void setPathEnumerator(PathEnumerator pathEnumerator) {
        PATH_ENUMERATOR_LOCK.writeLock().lock();
        try {
            LogManager.getLogger().debug("PATH_ENUMERATOR_LOCK write locked.");
            this.pathEnumerator = pathEnumerator;
        } finally {
            LogManager.getLogger().debug("PATH_ENUMERATOR_LOCK write unlocked.");
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

    void resetGame() {
        GAME_LOCK.lock();
        try {
            LogManager.getLogger().debug("GAME_LOCK write locked.");
            game.reset();
        } finally {
            GAME_LOCK.unlock();
            LogManager.getLogger().debug("GAME_LOCK write unlocked.");
        }
    }

    private Game getGame() {
        GAME_LOCK.lock();
        try {
            LogManager.getLogger().debug("GAME_LOCK read locked.");
            return game;
        } finally {
            GAME_LOCK.unlock();
            LogManager.getLogger().debug("GAME_LOCK read unlocked.");
        }
    }
   
    /**
     * Returns the individual player assigned the index parameter.
     */
    protected @Nullable Player getPlayer(final int idx) {
        return getGame().getPlayer(idx);
    }

    /**
     * Receives player information from the message packet.
     */
    private void receivePlayerInfo(Packet c) {
        int pindex = c.getIntValue(0);
        Player newPlayer = (Player) c.getObject(1);
        if (getPlayer(newPlayer.getId()) == null) {
            getGame().addPlayer(pindex, newPlayer);
        } else {
            getGame().setPlayer(pindex, newPlayer);
        }
    }

    /**
     * Loads the turn list from the data in the packet
     */
    @SuppressWarnings("unchecked")
    private void receiveTurns(Packet packet) {
        getGame().setTurnVector((List<GameTurn>) packet.getObject(0));
    }

    /**
     * Loads the board from the data in the net command.
     */
    private void receiveBoard(Packet c) {
        Board newBoard = (Board) c.getObject(0);
        getGame().setBoard(newBoard);
    }

    /**
     * Loads the entities from the data in the net command.
     */
    @SuppressWarnings("unchecked")
    private void receiveEntities(Packet c) {
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
    private void receiveEntityUpdate(Packet c) {
        int eindex = c.getIntValue(0);
        Entity entity = (Entity) c.getObject(1);
        Vector<UnitLocation> movePath = (Vector<UnitLocation>) c.getObject(2);
        // Replace this entity in the game.
        getGame().setEntity(eindex, entity, movePath);
    }

    private void receiveEntityAdd(Packet packet) {
        @SuppressWarnings(value = "unchecked")
        List<Entity> entities = (List<Entity>) packet.getObject(0);
        getGame().addEntities(entities);
    }

    private void receiveEntityRemove(Packet packet) {
        @SuppressWarnings("unchecked")
        List<Integer> entityIds = (List<Integer>) packet.getObject(0);
        int condition = packet.getIntValue(1);
        // Move the unit to its final resting place.
        getGame().removeEntities(entityIds, condition);
    }

    @SuppressWarnings("unchecked")
    private void receiveEntityVisibilityIndicator(Packet packet) {
        Entity e = getGame().getEntity(packet.getIntValue(0));
        if (e != null) { // we may not have this entity due to double blind
            e.setEverSeenByEnemy(packet.getBooleanValue(1));
            e.setVisibleToEnemy(packet.getBooleanValue(2));
            e.setDetectedByEnemy(packet.getBooleanValue(3));
            e.setWhoCanSee((Vector<Player>) packet.getObject(4));
            e.setWhoCanDetect((Vector<Player>) packet.getObject(5));
            // this next call is only needed sometimes, but we'll just
            // call it everytime
            getGame().processGameEvent(new GameEntityChangeEvent(this, e));
        }
    }

    @SuppressWarnings("unchecked")
    private void receiveDeployMinefields(Packet packet) {
        getGame().addMinefields((Vector<Minefield>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    private void receiveSendingMinefields(Packet packet) {
        getGame().setMinefields((Vector<Minefield>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    private void receiveIlluminatedHexes(Packet p) {
        getGame().setIlluminatedPositions((HashSet<Coords>) p.getObject(0));
    }

    private void receiveRevealMinefield(Packet packet) {
        getGame().addMinefield((Minefield) packet.getObject(0));
    }

    private void receiveRemoveMinefield(Packet packet) {
        getGame().removeMinefield((Minefield) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    private void receiveUpdateMinefields(Packet packet) {
        // only update information if you know about the minefield
        Vector<Minefield> newMines = new Vector<>();
        for (Minefield mf : (Vector<Minefield>) packet.getObject(0)) {
            if (getOwner().getLocalPlayer().containsMinefield(mf)) {
                newMines.add(mf);
            }
        }

        if (!newMines.isEmpty()) {
            getGame().resetMinefieldDensity(newMines);
        }
    }

    @SuppressWarnings("unchecked")
    private void receiveBuildingUpdate(Packet packet) {
        getGame().getBoard().updateBuildings((Vector<Building>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    private void receiveBuildingCollapse(Packet packet) {
        getGame().getBoard().collapseBuilding((Vector<Coords>) packet.getObject(0));
    }

    /**
     * Loads entity firing data from the data in the net command
     */
    @SuppressWarnings("unchecked")
    private void receiveAttack(Packet c) {
        List<EntityAction> vector = (List<EntityAction>) c.getObject(0);
        boolean isCharge = c.getBooleanValue(1);
        boolean addAction = true;
        for (EntityAction ea : vector) {
            int entityId = ea.getEntityId();
            if ((ea instanceof TorsoTwistAction) && game.hasEntity(entityId)) {
                TorsoTwistAction tta = (TorsoTwistAction) ea;
                Entity entity = game.getEntity(entityId);
                entity.setSecondaryFacing(tta.getFacing());
            } else if ((ea instanceof FlipArmsAction) && game.hasEntity(entityId)) {
                FlipArmsAction faa = (FlipArmsAction) ea;
                Entity entity = game.getEntity(entityId);
                entity.setArmsFlipped(faa.getIsFlipped());
            } else if ((ea instanceof DodgeAction) && game.hasEntity(entityId)) {
                Entity entity = game.getEntity(entityId);
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
                if (!isCharge) {
                    game.addAction(ea);
                } else {
                    game.addCharge((AttackAction) ea);
                }
            }
        }
    }

    /**
     * receive and process an entity nova network mode change packet
     *
     * @param c The packet containing the change.
     */
    private void receiveEntityNovaNetworkModeChange(Packet c) {
        try {
            int entityId = c.getIntValue(0);
            String networkID = c.getObject(1).toString();
            Entity e = getGame().getEntity(entityId);
            if (e != null) {
                e.setNewRoundNovaNetworkString(networkID);
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

    }
}
