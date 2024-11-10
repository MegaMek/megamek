/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import megamek.common.actions.EntityAction;
import megamek.common.annotations.Nullable;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameNewActionEvent;
import megamek.common.force.Forces;
import megamek.common.hexarea.HexArea;
import megamek.logging.MMLogger;
import megamek.server.scriptedevent.TriggeredEvent;

/**
 * This is a base class to derive all types of Game (TW, AS, BF, SBF...) from.
 * Any such game will have players, units
 * (InGameObjects) and Forces (even if empty); the base class manages these.
 */
public abstract class AbstractGame implements IGame {

    private static final MMLogger LOGGER = MMLogger.create(AbstractGame.class);

    private static final int AWAITING_FIRST_TURN = -1;

    /** The players present in the game mapped to their id as key */
    protected final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();

    /**
     * The InGameObjects (units such as Entity and others) present in the game
     * mapped to their id as key
     */
    protected final ConcurrentHashMap<Integer, InGameObject> inGameObjects = new ConcurrentHashMap<>();

    /** The teams present in the game */
    protected final CopyOnWriteArrayList<Team> teams = new CopyOnWriteArrayList<>();

    protected transient Vector<GameListener> gameListeners = new Vector<>();

    /** The currently pending actions such as attacks */
    protected final List<EntityAction> pendingActions = new ArrayList<>();

    /**
     * This Map holds all game boards together with a unique ID for each.
     * For the "legacy" Game that currently only allows a single board, that board
     * always uses ID 0.
     * To support game types that use board types other than hex boards, a
     * superclass or interface should
     * be used instead of Board in the future.
     */
    private final Map<Integer, Board> gameBoards = new HashMap<>();

    /**
     * The forces present in the game. The top level force holds all forces and
     * force-less entities
     * and should therefore not be shown.
     */
    protected Forces forces = new Forces(this);

    /**
     * This map links deployment rounds to lists of Deployables that deploy in
     * respective rounds. It only contains
     * units/objects that are not yet deployed or will redeploy (returning Aeros,
     * units going from one board to
     * another if implemented). For those, the list is updated every round.
     */
    private final Map<Integer, List<Deployable>> deploymentTable = new HashMap<>();

    /**
     * The round counter. It gets incremented before initiative; round 0 is initial
     * deployment only.
     */
    protected int currentRound = -1;

    protected int turnIndex = AWAITING_FIRST_TURN;

    /**
     * This list contains all scripted events that may happen during the course of
     * the game. This list
     * should only ever be present on the server. Only the results of events should
     * be sent to clients.
     */
    protected final List<TriggeredEvent> scriptedEvents = new CopyOnWriteArrayList<>();

    /**
     * Piles of carry-able objects, sorted by coordinates
     */
    protected Map<Coords, List<ICarryable>> groundObjects = new HashMap<>();

    @Override
    public Forces getForces() {
        return forces;
    }

    @Override
    public Player getPlayer(int id) {
        return players.get(id);
    }

    @Override
    public void addPlayer(int id, Player player) {
        players.put(id, player);
    }

    @Override
    public List<Player> getPlayersList() {
        return new ArrayList<>(players.values());
    }

    @Override
    public int getNoOfPlayers() {
        return players.size();
    }

    @Override
    public List<Team> getTeams() {
        return new ArrayList<>(teams);
    }

    @Override
    public int getNoOfTeams() {
        return teams.size();
    }

    @Override
    public List<InGameObject> getInGameObjects() {
        return new ArrayList<>(inGameObjects.values());
    }

    @Override
    public void addGameListener(GameListener listener) {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        gameListeners.add(listener);
    }

    @Override
    public void removeGameListener(GameListener listener) {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        gameListeners.remove(listener);
    }

    /**
     * @return All registered GameListeners.
     */
    public List<GameListener> getGameListeners() {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        return Collections.unmodifiableList(gameListeners);
    }

    @Override
    public void fireGameEvent(GameEvent event) {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        // The iteration must allow and support concurrent modification of the list!
        // Testing shows that a CopyOnWriteArrayList does not work
        for (Enumeration<GameListener> e = gameListeners.elements(); e.hasMoreElements();) {
            event.fireEvent(e.nextElement());
        }
    }

    @Override
    public void receiveBoard(int boardId, Board board) {
        Board oldBoard = getBoard(boardId);
        setBoard(boardId, board);
        fireGameEvent(new GameBoardNewEvent(this, oldBoard, board));
    }

    @Override
    public void receiveBoards(Map<Integer, Board> boards) {
        // cycle the entries so an event can be fired for each to allow listeners to
        // register and unregister
        boards.forEach(this::receiveBoard);
        // some old boards might not have been replaced by new ones, so clear the map
        // and refill
        gameBoards.clear();
        gameBoards.putAll(boards);
    }

    @Override
    public void setBoard(int boardId, Board board) {
        gameBoards.put(boardId, board);
    }

    @Override
    public Map<Integer, Board> getBoards() {
        return Collections.unmodifiableMap(gameBoards);
    }

    @Override
    public int getCurrentRound() {
        return currentRound;
    }

    @Override
    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    /**
     * Empties the list of pending EntityActions completely.
     *
     * @see #getActionsVector()
     */
    public void clearActions() {
        pendingActions.clear();
    }

    /**
     * Removes all pending EntityActions by the InGameObject (Entity, unit) of the
     * given ID from the list
     * of pending actions.
     */
    public void removeActionsFor(int id) {
        pendingActions.removeIf(action -> action.getEntityId() == id);
    }

    /**
     * Remove the given EntityAction from the list of pending actions.
     */
    public void removeAction(EntityAction action) {
        pendingActions.remove(action);
    }

    /**
     * Returns the pending EntityActions. Do not use to modify the actions; Arlith
     * said: I will be
     * angry. &gt;:[
     */
    public List<EntityAction> getActionsVector() {
        return Collections.unmodifiableList(pendingActions);
    }

    /**
     * Adds the specified action to the list of pending EntityActions for this phase
     * and fires a GameNewActionEvent.
     */
    public void addAction(EntityAction action) {
        pendingActions.add(action);
        fireGameEvent(new GameNewActionEvent(this, action));
    }

    /**
     * Clears and re-calculates the deployment table, i.e. assembles all
     * units/objects in the game
     * that are undeployed (that includes returning units or reinforcements)
     * together with the game
     * round that they are supposed to deploy on. This method can be called at any
     * time in the game
     * and will assemble deployment according to the present game state.
     */
    public void setupDeployment() {
        deploymentTable.clear();
        for (Deployable unit : deployableInGameObjects()) {
            if (!unit.isDeployed()) {
                deploymentTable.computeIfAbsent(unit.getDeployRound(), k -> new ArrayList<>()).add(unit);
            }
        }
    }

    protected List<Deployable> deployableInGameObjects() {
        return inGameObjects.values().stream()
                .filter(unit -> unit instanceof Deployable)
                .map(unit -> (Deployable) unit)
                .collect(Collectors.toList());
    }

    public int lastDeploymentRound() {
        return deploymentTable.isEmpty() ? -1 : Collections.max(deploymentTable.keySet());
    }

    public boolean isDeploymentComplete() {
        return lastDeploymentRound() < currentRound;
    }

    /**
     * Check to see if we should deploy this round
     */
    public boolean shouldDeployThisRound() {
        return shouldDeployForRound(currentRound);
    }

    public boolean shouldDeployForRound(int round) {
        return deploymentTable.containsKey(round);
    }

    /**
     * Clear this round from this list of entities to deploy
     */
    public void clearDeploymentThisRound() {
        deploymentTable.remove(currentRound);
    }

    /**
     * Resets the turn index to {@link #AWAITING_FIRST_TURN}
     */
    public void resetTurnIndex() {
        turnIndex = AWAITING_FIRST_TURN;
    }

    @Override
    public int getTurnIndex() {
        return turnIndex;
    }

    @Override
    public int getNextEntityId() {
        return inGameObjects.isEmpty() ? 0 : Collections.max(inGameObjects.keySet()) + 1;
    }

    @Override
    public synchronized void setForces(Forces fs) {
        forces = fs;
        forces.setGame(this);
    }

    @Override
    public void incrementCurrentRound() {
        currentRound++;
    }

    /**
     * Sets the turn index to the given value.
     *
     * @param turnIndex the new turn index
     */
    protected void setTurnIndex(int turnIndex) {
        this.turnIndex = turnIndex;
    }

    public boolean hasBoardLocation(@Nullable BoardLocation boardLocation) {
        return hasBoardLocation(boardLocation.coords(), boardLocation.boardId());
    }

    public boolean hasBoardLocation(Coords coords, int boardId) {
        return hasBoard(boardId) && getBoard(boardId).contains(coords);
    }

    public boolean hasBoard(@Nullable BoardLocation boardLocation) {
        return (boardLocation != null) && hasBoard(boardLocation.boardId());
    }

    public boolean hasBoard(int boardId) {
        return gameBoards.containsKey(boardId);
    }

    /**
     * Place a carryable object on the ground at the given coordinates
     */
    public void placeGroundObject(Coords coords, ICarryable carryable) {
        getGroundObjects().computeIfAbsent(coords, k -> new ArrayList<>()).add(carryable);
    }

    /**
     * Remove the given carryable object from the ground at the given coordinates
     */
    public void removeGroundObject(Coords coords, ICarryable carryable) {
        if (getGroundObjects().containsKey(coords)) {
            getGroundObjects().get(coords).remove(carryable);
        }
    }

    /**
     * Get a list of all the objects on the ground at the given coordinates
     * guaranteed to return non-null, but may return empty list
     */
    public List<ICarryable> getGroundObjects(Coords coords) {
        return getGroundObjects().getOrDefault(coords, new ArrayList<>());
    }

    /**
     * @return Collection of objects on the ground. Best to use
     *         getGroundObjects(Coords)
     *         if looking for objects in specific hex
     */
    public Map<Coords, List<ICarryable>> getGroundObjects() {
        return groundObjects;
    }

    /**
     * @param groundObjects the groundObjects to set
     */
    public void setGroundObjects(Map<Coords, List<ICarryable>> groundObjects) {
        this.groundObjects = groundObjects;
    }

    @Override
    public final List<TriggeredEvent> scriptedEvents() {
        return Collections.unmodifiableList(scriptedEvents);
    }

    @Override
    public final void addScriptedEvent(TriggeredEvent event) {
        scriptedEvents.add(event);
    }

    public final void clearScriptedEvents() {
        scriptedEvents.clear();
    }

    /**
     * Resets this game, i.e. prepares it for a return to the lobby.
     */
    public void reset() {
        clearScriptedEvents();
        clearActions();
        inGameObjects.clear();
        turnIndex = AWAITING_FIRST_TURN;
        groundObjects.clear();
        currentRound = -1;
        forces = new Forces(this);
    }

    /**
     * Returns true when the given unit can flee from the given coords, as set either for the unit itself or for its owner.
     *
     * @param unit   The unit that wants to flee
     * @param coords The hex coords it wants to flee from
     * @return True when it can indeed flee
     */
    public boolean canFleeFrom(Deployable unit, Coords coords) {
        if ((unit == null) || (coords == null)) {
            LOGGER.warn("Received null unit or coords!");
            return false;
        } else {
            return getFleeZone(unit).containsCoords(coords, getBoard());
        }
    }

    /**
     * Returns the {@link HexArea} a given unit can flee from, as set either for the unit itself or for its owner.
     *
     * @param unit The unit that wants to flee
     * @return The area it may flee from
     */
    public HexArea getFleeZone(Deployable unit) {
        if (unit == null) {
            LOGGER.warn("Received null unit!");
            return HexArea.EMPTY_AREA;
        } else if (unit.hasFleeZone()) {
            return unit.getFleeZone();
        } else if ((unit instanceof InGameObject inGameObject) && hasPlayer(inGameObject.getOwnerId())) {
            return getPlayer(inGameObject.getOwnerId()).getFleeZone();
        } else {
            return HexArea.EMPTY_AREA;
        }
    }
}
