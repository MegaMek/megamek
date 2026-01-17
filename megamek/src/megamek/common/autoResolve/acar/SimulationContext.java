/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.autoResolve.acar;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import megamek.common.Player;
import megamek.common.Team;
import megamek.common.annotations.Nullable;
import megamek.common.autoResolve.acar.action.Action;
import megamek.common.autoResolve.acar.action.ActionHandler;
import megamek.common.autoResolve.acar.order.Orders;
import megamek.common.autoResolve.component.AcTurn;
import megamek.common.autoResolve.component.Formation;
import megamek.common.autoResolve.component.FormationTurn;
import megamek.common.autoResolve.converter.SetupForces;
import megamek.common.autoResolve.damage.DamageApplierChooser;
import megamek.common.autoResolve.damage.EntityFinalState;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.enums.SkillLevel;
import megamek.common.event.GameEvent;
import megamek.common.event.GameListener;
import megamek.common.force.Forces;
import megamek.common.game.IGame;
import megamek.common.game.InGameObject;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.interfaces.PlanetaryConditionsUsing;
import megamek.common.interfaces.ReportEntry;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.units.BTObject;
import megamek.common.units.Deployable;
import megamek.common.units.Entity;
import megamek.common.units.EntitySelector;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import megamek.server.scriptedEvents.TriggeredEvent;
import org.apache.commons.lang3.NotImplementedException;

/**
 * @author Luana Coppio
 */
public class SimulationContext implements IGame, PlanetaryConditionsUsing {

    private static final MMLogger logger = MMLogger.create(SimulationContext.class);
    private static final int MAX_ROUND_LIMIT = 1000;
    private final long seed;
    private final SimulationOptions options;

    /**
     * Objectives that must be considered during the game
     */
    private static final int AWAITING_FIRST_TURN = -1;
    private final List<Action> pendingActions = new ArrayList<>();

    /**
     * Game Phase and rules
     */
    private GamePhase phase = GamePhase.UNKNOWN;
    private GamePhase lastPhase = GamePhase.UNKNOWN;

    private final Map<Integer, SkillLevel> playerSkillLevels = new HashMap<>();
    private final Map<Integer, Integer> unitsPerPlayerAtStart = new HashMap<>();
    private int lastEntityId;
    /**
     * Report and turn list
     */
    private final List<AcTurn> turnList = new ArrayList<>();
    protected final ConcurrentHashMap<Integer, InGameObject> inGameObjects = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();
    protected final List<Team> teams = new ArrayList<>();

    protected Forces forces = new Forces(this);
    private final Map<Integer, List<Deployable>> deploymentTable = new HashMap<>();
    protected int currentRound = -1;
    protected int turnIndex = AWAITING_FIRST_TURN;
    private final Map<Integer, List<Formation>> board = new HashMap<>();
    private final Board originalBoard;
    private final PlanetaryConditions planetaryConditions;
    /**
     * Tools for the game
     */
    private final List<ActionHandler> actionHandlers = new ArrayList<>();

    private final Orders orders = new Orders();

    /**
     * Contains all units that have left the game by any means.
     */
    private final Vector<Entity> graveyard = new Vector<>();

    public SimulationContext(SimulationOptions gameOptions, SetupForces setupForces, Board board,
          PlanetaryConditions planetaryConditions) {
        this(gameOptions, setupForces, board, planetaryConditions, System.nanoTime());
    }

    public SimulationContext(SimulationOptions gameOptions, SetupForces setupForces, Board board,
          PlanetaryConditions planetaryConditions, long seed) {
        this.seed = seed;
        this.options = gameOptions;
        this.originalBoard = board;
        this.planetaryConditions = new PlanetaryConditions(planetaryConditions);
        setBoard(0, board);
        setupForces.createForcesOnSimulation(this);
        setupForces.addOrdersToForces(this);
    }

    public Orders getOrders() {
        return orders;
    }

    public void addUnit(InGameObject unit) {
        int id = unit.getId();
        if (inGameObjects.containsKey(id) || isOutOfGame(id) || (Entity.NONE == id)) {
            id = getNextEntityId();
            unit.setId(id);
        }
        unitsPerPlayerAtStart.put(unit.getOwnerId(), unitsPerPlayerAtStart.getOrDefault(unit.getOwnerId(), 0) + 1);
        inGameObjects.put(id, unit);
    }

    /** @return The TW Units (Entity) currently in the game. */
    public List<Entity> inGameTWEntities() {
        return filterToEntity(inGameObjects.values());
    }

    private List<Entity> filterToEntity(Collection<? extends BTObject> objects) {
        return objects.stream().filter(Entity.class::isInstance).map(o -> (Entity) o).toList();
    }

    public List<Deployable> deployableInGameObjects() {
        return getInGameObjects().stream()
              .filter(Deployable.class::isInstance)
              .map(Deployable.class::cast)
              .collect(Collectors.toList());
    }

    public int getNoOfEntities() {
        return inGameTWEntities().size();
    }

    public int getStartingNumberOfUnits(int playerId) {
        return unitsPerPlayerAtStart.getOrDefault(playerId, 0);
    }


    public int getSelectedEntityCount(EntitySelector selector) {
        int retVal = 0;

        // If no selector was supplied, return the count of all game entities.
        if (null == selector) {
            retVal = getNoOfEntities();
        }

        // Otherwise, count the entities that meet the selection criteria.
        else {
            for (Entity entity : inGameTWEntities()) {
                if (selector.accept(entity)) {
                    retVal++;
                }
            }

        } // End use-selector

        // Return the number of selected entities.
        return retVal;
    }

    public SkillLevel getPlayerSkill(int playerId) {
        return playerSkillLevels.getOrDefault(playerId, SkillLevel.ULTRA_GREEN);
    }

    @Override
    public int getNextEntityId() {
        return inGameObjects.isEmpty() ? 0 : Collections.max(inGameObjects.keySet()) + 1;
    }

    /** @return The entity with the given id number, if any. */
    public Optional<Entity> getEntity(final int id) {
        InGameObject possibleEntity = inGameObjects.get(id);
        if (possibleEntity instanceof Entity) {
            return Optional.of((Entity) possibleEntity);
        }
        return Optional.empty();
    }

    public void addEntity(Entity entity) {
        int id = entity.getId();
        if (isIdUsed(id)) {
            id = getNextEntityId();
            entity.setId(id);
        }

        inGameObjects.put(id, entity);
        if (id > lastEntityId) {
            lastEntityId = id;
        }

        if (entity instanceof Mek mek) {
            mek.setAutoEject(true);
            mek.setCondEjectAmmo(!entity.hasCase() && !entity.hasCASEII());
            mek.setCondEjectEngine(true);
            mek.setCondEjectCTDest(true);
            mek.setCondEjectHeadshot(true);
        }

        entity.setInitialBV(entity.calculateBattleValue(false, false));
    }

    public boolean isOutOfGame(int id) {
        for (Entity entity : graveyard) {
            if (entity.getId() == id) {
                return true;
            }
        }

        return false;
    }


    private boolean isIdUsed(int id) {
        return inGameObjects.containsKey(id) || isOutOfGame(id);
    }

    @Override
    public List<AcTurn> getTurnsList() {
        return Collections.unmodifiableList(turnList);
    }


    @Override
    public SimulationOptions getOptions() {
        if (options != null) {
            return options;
        }
        return SimulationOptions.EMPTY;
    }

    @Override
    public GamePhase getPhase() {
        return phase;
    }

    public void addActionHandler(ActionHandler handler) {
        if (actionHandlers.contains(handler)) {
            logger.error("Tried to re-add action handler {}!", handler);
        } else {
            actionHandlers.add(handler);
        }
    }

    @Override
    public AcTurn getTurn() {
        if ((turnIndex < 0) || (turnIndex >= turnList.size())) {
            return null;
        }
        return turnList.get(turnIndex);
    }

    public Optional<AcTurn> getCurrentTurn() {
        if ((turnIndex < 0) || (turnIndex >= turnList.size())) {
            return Optional.empty();
        }
        return Optional.of(turnList.get(turnIndex));
    }

    @Override
    public boolean hasMoreTurns() {
        return getTurnsList().size() > turnIndex + 1;
    }

    public void setTurns(List<AcTurn> turns) {
        this.turnList.clear();
        this.turnList.addAll(turns);
    }

    @Override
    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    @Override
    public void setLastPhase(GamePhase lastPhase) {
        this.lastPhase = this.phase;
    }

    @Override
    public void receivePhase(GamePhase phase) {
        setLastPhase(this.phase);
        setPhase(phase);
    }


    @Override
    public boolean isCurrentPhasePlayable() {
        return true;
    }


    @Override
    public void setPlayer(int id, Player player) {
        player.setGame(this);
        players.put(id, player);
        setupTeams();
    }

    @Override
    public void removePlayer(int id) {
        // not implemented
    }

    @Override
    public void setupTeams() {
        Vector<Team> initTeams = new Vector<>();

        // Now, go through all the teams, and add the appropriate player
        for (int t = Player.TEAM_NONE + 1; t < Player.TEAM_NAMES.length; t++) {
            Team newTeam = null;
            for (Player player : getPlayersList()) {
                if (player.getTeam() == t) {
                    if (newTeam == null) {
                        newTeam = new Team(t);
                    }
                    newTeam.addPlayer(player);
                }
            }

            if (newTeam != null) {
                initTeams.addElement(newTeam);
            }
        }

        for (Team newTeam : initTeams) {
            for (Team oldTeam : teams) {
                if (newTeam.equals(oldTeam)) {
                    newTeam.setInitiative(oldTeam.getInitiative());
                }
            }
        }

        // Carry over faction settings
        for (Team newTeam : initTeams) {
            for (Team oldTeam : teams) {
                if (newTeam.equals(oldTeam)) {
                    newTeam.setFaction(oldTeam.getFaction());
                }
            }
        }

        teams.clear();
        teams.addAll(initTeams);
    }

    @Override
    public void replaceUnits(List<InGameObject> units) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public List<InGameObject> getGraveyard() {
        return new ArrayList<>(graveyard);
    }

    public List<Entity> getRetreatingUnits() {
        return this.graveyard.stream()
              .filter(entity -> entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_IN_RETREAT)
              .toList();
    }

    public int getLiveDeployedEntitiesOwnedBy(Player player) {
        var res = getActiveFormations(player).stream()
              .filter(Formation::isDeployed)
              .count();

        return (int) res;
    }

    @Override
    public ReportEntry getNewReport(int messageId) {
        throw new RuntimeException("Not implemented and not to be used in this context.");
    }

    @Override
    public List<TriggeredEvent> scriptedEvents() {
        return List.of();
    }

    public boolean gameTimerIsExpired() {
        return getRoundCount() >= MAX_ROUND_LIMIT;
    }

    private int getRoundCount() {
        return currentRound;
    }

    public List<ActionHandler> getActionHandlers() {
        return actionHandlers;
    }

    public Optional<AcTurn> changeToNextTurn() {
        turnIndex++;
        return getCurrentTurn();
    }

    public boolean hasEligibleFormation(FormationTurn turn) {
        return (turn != null) && getActiveFormations().stream().anyMatch(f -> turn.isValidEntity(f, this));
    }

    /**
     * Returns the formation of the given ID, if one can be found.
     *
     * @param formationID the ID to look for
     *
     * @return The formation or an empty Optional
     */
    public Optional<Formation> getFormation(int formationID) {
        Optional<InGameObject> unit = getInGameObject(formationID);
        if (unit.isPresent() && unit.get() instanceof Formation formation) {
            return Optional.of(formation);
        } else {
            return Optional.empty();
        }
    }

    public GamePhase getLastPhase() {
        return lastPhase;
    }

    // check current turn, phase, formation
    private boolean isEligibleForAction(Formation formation) {
        return (getTurn() instanceof FormationTurn)
              && getTurn().isValidEntity(formation, this);
    }

    /**
     * Returns the list of formations that are in the game's InGameObject list, i.e. that aren't destroyed or otherwise
     * removed from play.
     *
     * @return The currently active formations
     */
    public List<Formation> getActiveFormations() {
        return getInGameObjects().stream()
              .filter(u -> u instanceof Formation)
              .map(u -> (Formation) u)
              .toList();
    }

    public List<Formation> getActiveDeployedFormations() {
        return getActiveFormations().stream()
              .filter(Formation::isDeployed)
              .toList();
    }

    public List<Formation> getActiveFormations(Player player) {
        return this.getActiveFormations(player.getId());
    }


    public List<Formation> getActiveFormations(int playerId) {
        return getActiveFormations().stream()
              .filter(f -> f.getOwnerId() == playerId)
              .toList();
    }

    public void addUnitToGraveyard(Entity entity) {
        if (!inGameObjects.containsKey(entity.getId())) {
            logger.error("Tried to add entity {} to graveyard, but it's not in the game!", entity);
            return;
        }
        removeEntity(entity);
        graveyard.add(entity);
    }

    public void setPlayerSkillLevel(int playerId, SkillLevel averageSkillLevel) {
        playerSkillLevels.put(playerId, averageSkillLevel);
    }

    public Player getLocalPlayer() {
        return getPlayer(0);
    }

    @Override
    public synchronized Forces getForces() {
        return forces;
    }

    @Override
    public Player getPlayer(int id) {
        var player = players.get(id);
        if (player == null) {
            throw new IllegalArgumentException("No player with ID " + id + " found.");
        }
        return player;
    }

    @Override
    public void addPlayer(int id, Player player) {
        players.put(id, player);
        player.setGame(this);
        setupTeams();
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

    public void removeFormation(Formation formation) {
        inGameObjects.remove(formation.getId());
    }

    public void applyDamageToEntityFromUnit(SBFUnit unit, Entity entity, EntityFinalState entityFinalState) {
        var percent = (double) unit.getCurrentArmor() / unit.getArmor();
        var crits = Math.min(9, unit.getTargetingCrits() + unit.getMpCrits() + unit.getDamageCrits());
        percent -= percent * (crits / 15.0);
        percent = Math.min(0.85, percent);
        var totalDamage = (int) ((entity.getTotalArmor() + entity.getTotalInternal()) * (1 - percent));
        var clusterSize = -1;
        if (entity instanceof Infantry) {
            clusterSize = 1;
        }
        DamageApplierChooser.choose(entity, entityFinalState)
              .applyDamageInClusters(totalDamage, clusterSize);
    }

    public void removeEntity(Entity entity) {
        inGameObjects.remove(entity.getId());
    }

    @Override
    public void addGameListener(GameListener listener) {}

    @Override
    public void removeGameListener(GameListener listener) {}

    @Override
    public boolean isForceVictory() {
        return false;
    }

    @Override
    public void fireGameEvent(GameEvent event) {}

    @Override
    public void receiveBoard(int boardId, Board board) {}

    @Override
    public void receiveBoards(Map<Integer, Board> boards) {}

    @Override
    public void setBoard(int boardId, Board board) {
        var linearSize = new Coords(0, 0).distance(board.getHeight(), board.getWidth());
        for (var i = 0; i < linearSize; i++) {
            this.board.put(i, new ArrayList<>());
        }
    }

    @Override
    public Map<Integer, Board> getBoards() {
        return Map.of(0, originalBoard);
    }

    public int getBoardSize() {
        return board.size();
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
     * Removes all pending EntityActions by the InGameObject (Entity, unit) of the given ID from the list of pending
     * actions.
     */
    public void removeActionsFor(int id) {
        pendingActions.removeIf(action -> action.getEntityId() == id);
    }

    /**
     * Remove the given EntityAction from the list of pending actions.
     */
    public void removeAction(Action action) {
        pendingActions.remove(action);
    }

    /**
     * Returns the pending EntityActions. Do not use to modify the actions; Arlith said: I will be angry. &gt;:[
     */
    public List<Action> getActionsVector() {
        return Collections.unmodifiableList(pendingActions);
    }

    /**
     * Adds the specified action to the list of pending EntityActions for this phase and fires a GameNewActionEvent.
     */
    public void addAction(Action action) {
        pendingActions.add(action);
    }

    /**
     * Clears and re-calculates the deployment table, i.e. assembles all units/objects in the game that are un-deployed
     * (that includes returning units or reinforcements) together with the game round that they are supposed to deploy
     * on. This method can be called at any time in the game and will assemble deployment according to the present game
     * state.
     */
    public void setupDeployment() {
        deploymentTable.clear();
        for (Deployable unit : deployableInGameObjects()) {
            if (!unit.isDeployed()) {
                deploymentTable.computeIfAbsent(unit.getDeployRound(), k -> new ArrayList<>()).add(unit);
            }
        }
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

    @Override
    public boolean hasBoardLocation(@Nullable BoardLocation boardLocation) {
        return hasBoardLocation(boardLocation.coords(), boardLocation.boardId());
    }

    @Override
    public boolean hasBoardLocation(Coords coords, int boardId) {
        return hasBoard(boardId) && coords.getX() < board.size();
    }

    @Override
    public boolean hasBoard(@Nullable BoardLocation boardLocation) {
        return (boardLocation != null) && hasBoard(boardLocation.boardId());
    }

    @Override
    @SuppressWarnings("unused")
    public boolean hasBoard(int boardId) {
        return true;
    }

    public void setFormationAt(Formation formation, BoardLocation position) {
        setBoardLocation(position, formation);
    }

    public void setBoardLocation(BoardLocation boardLocation, Formation formation) {
        boardLocation = clamp(boardLocation);
        if (formation.getPosition() != null) {
            board.get(formation.getPosition().coords().getX()).remove(formation);
        }
        formation.setPosition(boardLocation);
        board.get(formation.getPosition().coords().getX()).add(formation);
    }

    public BoardLocation clamp(BoardLocation location) {
        var x = location.coords().getX();
        if (x >= board.size()) {
            x = board.size() - 1;
        } else if (x < 0) {
            x = 0;
        }
        return BoardLocation.of(new Coords(x, 0), location.boardId());
    }

    /**
     * Resets this game, i.e. prepares it for a return to the lobby.
     */
    public void reset() {
        clearActions();
        inGameObjects.clear();
        turnIndex = AWAITING_FIRST_TURN;
        currentRound = -1;
        forces = new Forces(this);
    }

    public long getSeed() {
        return seed;
    }

    @Override
    public PlanetaryConditions getPlanetaryConditions() {
        return this.planetaryConditions;
    }

    @Override
    public void setPlanetaryConditions(PlanetaryConditions conditions) {
        this.planetaryConditions.alterConditions(planetaryConditions);
    }
}
