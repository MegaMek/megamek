/*
 * MegaMek -
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
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
package megamek.common;

import megamek.MMConstants;
import megamek.Version;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.common.GameTurn.SpecificEntityTurn;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;
import megamek.common.force.Forces;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AttackHandler;
import megamek.server.SmokeCloud;
import megamek.server.victory.Victory;
import megamek.server.victory.VictoryResult;
import org.apache.logging.log4j.LogManager;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * The game class is the root of all data about the game in progress. Both the
 * Client and the Server should have one of these objects, and it is their job to
 * keep it synched.
 */
public class Game implements Serializable {
    private static final long serialVersionUID = 8376320092671792532L;

    /**
     * A UUID to identify this game instance.
     */
    public UUID uuid = UUID.randomUUID();

    /**
     * Stores the version of MM, so that it can be serialized in saved games.
     */
    public final Version version = MMConstants.VERSION;

    private GameOptions options = new GameOptions();

    private Board board = new Board();

    private final List<Entity> entities = new CopyOnWriteArrayList<>();
    private Hashtable<Integer, Entity> entityIds = new Hashtable<>();

    /**
     * Track entities removed from the game (probably by death)
     */
    private Vector<Entity> vOutOfGame = new Vector<>();

    private Vector<Player> players = new Vector<>();
    private Vector<Team> teams = new Vector<>();

    private Hashtable<Integer, Player> playerIds = new Hashtable<>();

    private final Map<Coords, HashSet<Integer>> entityPosLookup = new HashMap<>();

    /**
     * have the entities been deployed?
     */
    private boolean deploymentComplete = false;

    /**
     * how's the weather?
     */
    private PlanetaryConditions planetaryConditions = new PlanetaryConditions();

    /**
     * what round is it?
     */
    private int roundCount = 0;

    /**
     * The current turn list
     */
    private Vector<GameTurn> turnVector = new Vector<>();
    private int turnIndex = 0;

    /**
     * The present phase
     */
    private GamePhase phase = GamePhase.UNKNOWN;

    /**
     * The past phase
     */
    private GamePhase lastPhase = GamePhase.UNKNOWN;

    // phase state
    private Vector<EntityAction> actions = new Vector<>();
    private Vector<AttackAction> pendingCharges = new Vector<>();
    private Vector<AttackAction> pendingRams = new Vector<>();
    private Vector<AttackAction> pendingTeleMissileAttacks = new Vector<>();
    private Vector<PilotingRollData> pilotRolls = new Vector<>();
    private Vector<PilotingRollData> extremeGravityRolls = new Vector<>();
    private Vector<PilotingRollData> controlRolls = new Vector<>();
    private Vector<Team> initiativeRerollRequests = new Vector<>();

    // reports
    private GameReports gameReports = new GameReports();

    private boolean forceVictory = false;
    private int victoryPlayerId = Player.PLAYER_NONE;
    private int victoryTeam = Player.TEAM_NONE;

    private Hashtable<Integer, Vector<Entity>> deploymentTable = new Hashtable<>();
    private int lastDeploymentRound = 0;

    private Hashtable<Coords, Vector<Minefield>> minefields = new Hashtable<>();
    private Vector<Minefield> vibrabombs = new Vector<>();
    private Vector<AttackHandler> attacks = new Vector<>();
    private Vector<ArtilleryAttackAction> offboardArtilleryAttacks = new Vector<>();

    private int lastEntityId;

    private Vector<TagInfo> tagInfoForTurn = new Vector<>();
    private Vector<Flare> flares = new Vector<>();
    private HashSet<Coords> illuminatedPositions = new HashSet<>();

    private HashMap<String, Object> victoryContext = null;

    // internal integer value for an external game id link
    private int externalGameId = 0;

    // victory condition related stuff
    private Victory victory = null;

    // smoke clouds
    private List<SmokeCloud> smokeCloudList = new CopyOnWriteArrayList<>();

    /**
     * The forces present in the game. The top level force holds all forces and force-less entities
     * and should therefore not be shown.
     */
    private Forces forces = new Forces(this);

    private transient Vector<GameListener> gameListeners = new Vector<>();
    
    /** 
     * Stores princess behaviors for game factions. It does not indicate that a faction is currently
     * played by a bot, only that the most recent bot connected as that faction used these settings.
     * Used to add the settings to savegames and allow restoring bots to their previous settings.
     */
    private Map<String, BehaviorSettings> botSettings = new HashMap<>();

    /**
     * Constructor
     */
    public Game() {
        // empty
    }

    // Added public accessors for external game id
    public int getExternalGameId() {
        return externalGameId;
    }

    public void setExternalGameId(int value) {
        externalGameId = value;
    }

    public Version getVersion() {
        return version;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        Board oldBoard = this.board;
        setBoardDirect(board);
        processGameEvent(new GameBoardNewEvent(this, oldBoard, board));
    }

    public void setBoardDirect(final Board board) {
        this.board = board;
    }

    public boolean containsMinefield(Coords coords) {
        return minefields.containsKey(coords);
    }

    public Vector<Minefield> getMinefields(Coords coords) {
        Vector<Minefield> mfs = minefields.get(coords);
        return (mfs == null) ? new Vector<>() : mfs;
    }

    public int getNbrMinefields(Coords coords) {
        Vector<Minefield> mfs = minefields.get(coords);
        return (mfs == null) ? 0 : mfs.size();
    }

    /**
     * Get the coordinates of all mined hexes in the game.
     *
     * @return an <code>Enumeration</code> of the <code>Coords</code> containing
     * minefields. This will not be <code>null</code>.
     */
    public Enumeration<Coords> getMinedCoords() {
        return minefields.keys();
    }

    public void addMinefield(Minefield mf) {
        addMinefieldHelper(mf);
        processGameEvent(new GameBoardChangeEvent(this));
    }

    public void addMinefields(Vector<Minefield> mines) {
        for (int i = 0; i < mines.size(); i++) {
            Minefield mf = mines.elementAt(i);
            addMinefieldHelper(mf);
        }
        processGameEvent(new GameBoardChangeEvent(this));
    }

    public void setMinefields(Vector<Minefield> minefields) {
        clearMinefieldsHelper();
        for (int i = 0; i < minefields.size(); i++) {
            Minefield mf = minefields.elementAt(i);
            addMinefieldHelper(mf);
        }
        processGameEvent(new GameBoardChangeEvent(this));
    }

    public void resetMinefieldDensity(Vector<Minefield> newMinefields) {
        if (newMinefields.size() < 1) {
            return;
        }
        Vector<Minefield> mfs = minefields.get(newMinefields.firstElement().getCoords());
        mfs.clear();
        for (int i = 0; i < newMinefields.size(); i++) {
            Minefield mf = newMinefields.elementAt(i);
            addMinefieldHelper(mf);
        }
        processGameEvent(new GameBoardChangeEvent(this));
    }

    protected void addMinefieldHelper(Minefield mf) {
        Vector<Minefield> mfs = minefields.get(mf.getCoords());
        if (mfs == null) {
            mfs = new Vector<>();
            mfs.addElement(mf);
            minefields.put(mf.getCoords(), mfs);
            return;
        }
        mfs.addElement(mf);
    }

    public void removeMinefield(Minefield mf) {
        removeMinefieldHelper(mf);
        processGameEvent(new GameBoardChangeEvent(this));
    }

    public void removeMinefieldHelper(Minefield mf) {
        Vector<Minefield> mfs = minefields.get(mf.getCoords());
        if (mfs == null) {
            return;
        }

        Enumeration<Minefield> e = mfs.elements();
        while (e.hasMoreElements()) {
            Minefield mftemp = e.nextElement();
            if (mftemp.equals(mf)) {
                mfs.removeElement(mftemp);
                break;
            }
        }
        if (mfs.isEmpty()) {
            minefields.remove(mf.getCoords());
        }
    }

    public void clearMinefields() {
        clearMinefieldsHelper();
        processGameEvent(new GameBoardChangeEvent(this));
    }

    protected void clearMinefieldsHelper() {
        minefields.clear();
        vibrabombs.removeAllElements();

        Enumeration<Player> iter = getPlayers();
        while (iter.hasMoreElements()) {
            Player player = iter.nextElement();
            player.removeMinefields();
        }
    }

    public Vector<Minefield> getVibrabombs() {
        return vibrabombs;
    }

    public void addVibrabomb(Minefield mf) {
        vibrabombs.addElement(mf);
    }

    public void removeVibrabomb(Minefield mf) {
        vibrabombs.removeElement(mf);
    }

    /**
     * Checks if the game contains the specified Vibrabomb
     *
     * @param mf the Vibrabomb to check
     * @return true if the minefield contains a vibrabomb.
     */
    public boolean containsVibrabomb(Minefield mf) {
        return vibrabombs.contains(mf);
    }

    public GameOptions getOptions() {
        return options;
    }

    public void setOptions(final @Nullable GameOptions options) {
        if (options == null) {
            LogManager.getLogger().error("Can't set the game options to null!");
        } else {
            this.options = options;
            processGameEvent(new GameSettingsChangeEvent(this));
        }
    }

    /**
     * Return an enumeration of teams in the game
     */
    public Enumeration<Team> getTeams() {
        return teams.elements();
    }

    /**
     * Return the current number of teams in the game.
     */
    public int getNoOfTeams() {
        return teams.size();
    }

    /**
     * @return an immutable clone of the vector of teams. Each element is one of the teams in the
     * game.
     */
    public List<Team> getTeamsVector() {
        return Collections.unmodifiableList(teams);
    }

    /**
     * @return a player's team, which may be null if they do not have a team
     */
    public @Nullable Team getTeamForPlayer(Player p) {
        for (Team team : teams) {
            for (Enumeration<Player> j = team.getPlayers(); j.hasMoreElements(); ) {
                final Player player = j.nextElement();
                if (p == player) {
                    return team;
                }
            }
        }
        return null;
    }

    /**
     * Set up the teams vector. Each player on a team (Team 1 .. Team X) is
     * placed in the appropriate vector. Any player on 'No Team', is placed in
     * their own object
     */
    public void setupTeams() {
        Vector<Team> initTeams = new Vector<>();
        boolean useTeamInit = getOptions().getOption(OptionsConstants.BASE_TEAM_INITIATIVE)
                .booleanValue();

        // Get all NO_TEAM players. If team_initiative is false, all
        // players are on their own teams for initiative purposes.
        for (Enumeration<Player> i = getPlayers(); i.hasMoreElements(); ) {
            final Player player = i.nextElement();
            // Ignore players not on a team
            if (player.getTeam() == Player.TEAM_UNASSIGNED) {
                continue;
            }
            if (!useTeamInit || (player.getTeam() == Player.TEAM_NONE)) {
                Team new_team = new Team(Player.TEAM_NONE);
                new_team.addPlayer(player);
                initTeams.addElement(new_team);
            }
        }

        if (useTeamInit) {
            // Now, go through all the teams, and add the appropriate player
            for (int t = Player.TEAM_NONE + 1; t < Player.TEAM_NAMES.length; t++) {
                Team new_team = null;
                for (Enumeration<Player> i = getPlayers(); i.hasMoreElements(); ) {
                    final Player player = i.nextElement();
                    if (player.getTeam() == t) {
                        if (new_team == null) {
                            new_team = new Team(t);
                        }
                        new_team.addPlayer(player);
                    }
                }

                if (new_team != null) {
                    initTeams.addElement(new_team);
                }
            }
        }

        // May need to copy state over from previous teams, such as initiative
        if ((teams != null) && !getPhase().isLounge()) {
            for (Team newTeam : initTeams) {
                for (Team oldTeam : teams) {
                    if (newTeam.equals(oldTeam)) {
                        newTeam.setInitiative(oldTeam.getInitiative());
                    }
                }
            }
        }
        teams = initTeams;
    }

    /**
     * Return an enumeration of player in the game
     */
    public Enumeration<Player> getPlayers() {
        return players.elements();
    }

    /**
     * Return the players vector
     */
    public Vector<Player> getPlayersVector() {
        return players;
    }

    /**
     * Return the current number of active players in the game.
     */
    public int getNoOfPlayers() {
        return players.size();
    }

    /**
     * Returns the individual player assigned the id parameter.
     */
    public @Nullable Player getPlayer(final int id) {
        return (Player.PLAYER_NONE == id) ? null : playerIds.get(id);
    }

    public void addPlayer(int id, Player player) {
        player.setGame(this);
        players.addElement(player);
        playerIds.put(id, player);
        setupTeams();
        updatePlayer(player);
    }

    public void setPlayer(int id, Player player) {
        final Player oldPlayer = getPlayer(id);
        player.setGame(this);
        players.setElementAt(player, players.indexOf(oldPlayer));
        playerIds.put(id, player);
        setupTeams();
        updatePlayer(player);
    }

    protected void updatePlayer(Player player) {
        processGameEvent(new GamePlayerChangeEvent(this, player));
    }

    public void removePlayer(int id) {
        Player playerToRemove = getPlayer(id);
        players.removeElement(playerToRemove);
        playerIds.remove(id);
        setupTeams();
        processGameEvent(new GamePlayerChangeEvent(this, playerToRemove));
    }

    /**
     * Returns the number of entities owned by the player, regardless of their
     * status, as long as they are in the game.
     */
    public int getEntitiesOwnedBy(Player player) {
        int count = 0;
        for (Entity entity : entities) {
            if ((entity != null) && player.equals(entity.getOwner())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the number of entities owned by the player, regardless of their
     * status.
     */
    public int getAllEntitiesOwnedBy(Player player) {
        int count = 0;
        for (Entity entity : entities) {
            if (entity.getOwner().equals(player)) {
                count++;
            }
        }
        for (Entity entity : vOutOfGame) {
            if (entity.getOwner().equals(player)) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the number of non-destroyed entities owned by the player
     */
    public int getLiveEntitiesOwnedBy(Player player) {
        int count = 0;
        for (Entity entity : entities) {
            if (entity.getOwner().equals(player) && !entity.isDestroyed()
                    && !entity.isCarcass()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the number of non-destroyed entities owned by the player, including entities not yet
     * deployed. Ignores offboard units and captured Mek pilots.
     */
    public int getLiveDeployedEntitiesOwnedBy(Player player) {
        int count = 0;
        for (Entity entity : entities) {
            if (entity.getOwner().equals(player) && !entity.isDestroyed()
                && !entity.isCarcass()
                && !entity.isOffBoard() && !entity.isCaptured()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the number of non-destroyed commander entities owned by the player. Ignores offboard
     * units and captured Mek pilots.
     */
    public int getLiveCommandersOwnedBy(Player player) {
        int count = 0;
        for (Entity entity : entities) {
            if (entity.getOwner().equals(player) && !entity.isDestroyed()
                    && !entity.isCarcass()
                    && entity.isCommander() && !entity.isOffBoard()
                    && !entity.isCaptured()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return true if the player has a valid unit with the Tactical Genius pilot special ability.
     */
    public boolean hasTacticalGenius(Player player) {
        for (Entity entity : entities) {
            if (entity.hasAbility(OptionsConstants.MISC_TACTICAL_GENIUS)
                    && entity.getOwner().equals(player) && !entity.isDestroyed() && entity.isDeployed()
                    && !entity.isCarcass() && !entity.getCrew().isUnconscious()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a vector of entity objects that are "acceptable" to attack with this
     * entity
     */
    public List<Entity> getValidTargets(Entity entity) {
        List<Entity> ents = new ArrayList<>();

        boolean friendlyFire = getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);

        for (Entity otherEntity : entities) {
            // Even if friendly fire is acceptable, do not shoot yourself
            // Enemy units not on the board can not be shot.
            if ((otherEntity.getPosition() != null)
                    && !otherEntity.isOffBoard()
                    && otherEntity.isTargetable()
                    && !otherEntity.isHidden()
                    && !otherEntity.isSensorReturn(entity.getOwner())
                    && otherEntity.hasSeenEntity(entity.getOwner())
                    && (entity.isEnemyOf(otherEntity) || (friendlyFire && (entity
                            .getId() != otherEntity.getId())))) {
                // Air to Ground - target must be on flight path
                if (Compute.isAirToGround(entity, otherEntity)) {
                    if (entity.getPassedThrough().contains(
                            otherEntity.getPosition())) {
                        ents.add(otherEntity);
                    }                
                } else {
                    ents.add(otherEntity);
                }
            }
        }

        return Collections.unmodifiableList(ents);
    }

    /**
     * @return the current GameTurn object
     */
    public @Nullable GameTurn getTurn() {
        if ((turnIndex < 0) || (turnIndex >= turnVector.size())) {
            return null;
        }
        return turnVector.elementAt(turnIndex);
    }

    /**
     * @return the first GameTurn object for the specified player, or null if the player has no
     * turns to play
     */
    public @Nullable GameTurn getTurnForPlayer(int pn) {
        for (int i = turnIndex; i < turnVector.size(); i++) {
            GameTurn gt = turnVector.get(i);
            if (gt.isValid(pn, this)) {
                return gt;
            }
        }
        return null;
    }

    /**
     * Changes to the next turn, returning it.
     */
    public GameTurn changeToNextTurn() {
        turnIndex++;
        return getTurn();
    }

    /**
     * Resets the turn index to -1 (awaiting first turn)
     */
    public void resetTurnIndex() {
        turnIndex = -1;
    }

    /**
     * Returns true if there is a turn after the current one
     */
    public boolean hasMoreTurns() {
        return turnVector.size() > turnIndex;
    }

    /**
     * Inserts a turn that will come directly after the current one
     */
    public void insertNextTurn(GameTurn turn) {
        turnVector.insertElementAt(turn, turnIndex + 1);
    }

    /**
     * Inserts a turn after the specific index
     */
    public void insertTurnAfter(GameTurn turn, int index) {
        if ((index + 1) >= turnVector.size()) {
            turnVector.add(turn);
        } else {
            turnVector.insertElementAt(turn, index + 1);
        }
    }

    /**
     * Swaps the turn at index 1 with the turn at index 2.
     */
    public void swapTurnOrder(int index1, int index2) {
        GameTurn turn1 = turnVector.get(index1);
        GameTurn turn2 = turnVector.get(index2);
        turnVector.set(index2, turn1);
        turnVector.set(index1, turn2);
    }

    /**
     * Returns an Enumeration of the current turn list
     */
    public Enumeration<GameTurn> getTurns() {
        return turnVector.elements();
    }

    /**
     * Returns the current turn index
     */
    public int getTurnIndex() {
        return turnIndex;
    }

    /**
     * Sets the current turn index
     *
     * @param turnIndex The new turn index.
     * @param prevPlayerId  The ID of the player who triggered the turn index change.
     */
    public void setTurnIndex(int turnIndex, int prevPlayerId) {
        // FIXME: occasionally getTurn() returns null. Handle that case
        // intelligently.
        this.turnIndex = turnIndex;
        processGameEvent(new GameTurnChangeEvent(this, getPlayer(getTurn().getPlayerNum()), prevPlayerId));
    }

    /**
     * Returns the current turn vector
     */
    public List<GameTurn> getTurnVector() {
        return Collections.unmodifiableList(turnVector);
    }

    /**
     * Sets the current turn vector
     */
    public void setTurnVector(List<GameTurn> turnVector) {
        this.turnVector.clear();
        this.turnVector.addAll(turnVector);
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        final GamePhase oldPhase = this.phase;
        this.phase = phase;
        // Handle phase-specific items.
        switch (phase) {
            case LOUNGE:
                reset();
                break;
            case TARGETING:
            case PREMOVEMENT:
            case MOVEMENT:
            case PREFIRING:
            case FIRING:
            case PHYSICAL:
            case DEPLOYMENT:
                resetActions();
                break;
            case INITIATIVE:
                resetActions();
                resetCharges();
                resetRams();
                break;
            // TODO Is there better solution to handle charges?
            case PHYSICAL_REPORT:
            case END:
                resetCharges();
                resetRams();
                break;
            default:
        }

        processGameEvent(new GamePhaseChangeEvent(this, oldPhase, phase));
    }

    public GamePhase getLastPhase() {
        return lastPhase;
    }

    public void setLastPhase(GamePhase lastPhase) {
        this.lastPhase = lastPhase;
    }

    public void setDeploymentComplete(boolean deploymentComplete) {
        this.deploymentComplete = deploymentComplete;
    }

    public boolean isDeploymentComplete() {
        return deploymentComplete;
    }

    /**
     * Sets up the hashtable of who deploys when
     */
    public void setupRoundDeployment() {
        deploymentTable = new Hashtable<>();

        for (Entity ent : entities) {
            if (ent.isDeployed()) {
                continue;
            }

            Vector<Entity> roundVec = deploymentTable.computeIfAbsent(ent.getDeployRound(), k -> new Vector<>());
            roundVec.addElement(ent);
            lastDeploymentRound = Math.max(lastDeploymentRound, ent.getDeployRound());
        }
    }

    /**
     * Checks to see if we've past our deployment completion
     */
    public void checkForCompleteDeployment() {
        setDeploymentComplete(lastDeploymentRound < getRoundCount());
    }

    /**
     * Check to see if we should deploy this round
     */
    public boolean shouldDeployThisRound() {
        return shouldDeployForRound(getRoundCount());
    }

    public boolean shouldDeployForRound(int round) {
        Vector<Entity> vec = getEntitiesToDeployForRound(round);
        return (null != vec) && !vec.isEmpty();
    }

    private Vector<Entity> getEntitiesToDeployForRound(int round) {
        return deploymentTable.get(round);
    }

    /**
     * Clear this round from this list of entities to deploy
     */
    public void clearDeploymentThisRound() {
        deploymentTable.remove(getRoundCount());
    }

    /**
     * Returns a vector of entities that have not yet deployed
     */
    public List<Entity> getUndeployedEntities() {
        List<Entity> entList = new ArrayList<>();
        Enumeration<Vector<Entity>> iter = deploymentTable.elements();

        while (iter.hasMoreElements()) {
            Vector<Entity> vecTemp = iter.nextElement();

            for (int i = 0; i < vecTemp.size(); i++) {
                entList.add(vecTemp.elementAt(i));
            }
        }

        return Collections.unmodifiableList(entList);
    }

    /**
     * @return an enumeration of all the entities in the game.
     */
    public Iterator<Entity> getEntities() {
        return entities.iterator();
    }

    /**
     * @param current The <code>Entity</code> whose list position you wish to start from.
     * @return The previous <code>Entity</code> from the master list of entities. Will wrap around
     * to the end of the list if necessary, returning null if there are no entities.
     */
    public @Nullable Entity getPreviousEntityFromList(final @Nullable Entity current) {
        if ((current != null) && entities.contains(current)) {
            int prev = entities.indexOf(current) - 1;
            if (prev < 0) {
                prev = entities.size() - 1; // wrap around to end
            }
            return entities.get(prev);
        }
        return null;
    }

    /**
     * @param current The <code>Entity</code> whose list position you wish to start from.
     * @return The next <code>Entity</code> from the master list of entities. Will wrap around to
     * the beginning of the list if necessary, returning null if there are no entities.
     */
    public @Nullable Entity getNextEntityFromList(final @Nullable Entity current) {
        if ((current != null) && entities.contains(current)) {
            int next = entities.indexOf(current) + 1;
            if (next >= entities.size()) {
                next = 0; // wrap-around to beginning
            }
            return entities.get(next);
        }
        return null;
    }

    /**
     * @return the actual vector for the entities
     */
    public List<Entity> getEntitiesVector() {
        return Collections.unmodifiableList(entities);
    }

    public synchronized void setEntitiesVector(List<Entity> entities) {
        // checkPositionCacheConsistency();
        this.entities.clear();
        this.entities.addAll(entities);
        reindexEntities();
        resetEntityPositionLookup();
        processGameEvent(new GameEntityNewEvent(this, entities));
    }

    /**
     * @return the actual vector for the out-of-game entities
     */
    public Vector<Entity> getOutOfGameEntitiesVector() {
        return vOutOfGame;
    }

    /**
     * Swap out the current list of dead (or fled) units for a new one.
     *
     * @param vOutOfGame - the new <code>Vector</code> of dead or fled units. This
     *                   value should <em>not</em> be <code>null</code>.
     * @throws IllegalArgumentException if the new list is <code>null</code>.
     */
    public void setOutOfGameEntitiesVector(final List<Entity> vOutOfGame) {
        Objects.requireNonNull(vOutOfGame, "New out-of-game list should not be null.");
        Vector<Entity> newOutOfGame = new Vector<>();

        // Add entities for the existing players to the game.
        for (Entity entity : vOutOfGame) {
            int ownerId = entity.getOwnerId();
            if ((ownerId != Entity.NONE) && (getPlayer(ownerId) != null)) {
                entity.setGame(this);
                newOutOfGame.addElement(entity);
            }
        }
        this.vOutOfGame = newOutOfGame;
        processGameEvent(new GameEntityNewOffboardEvent(this));
    }

    /**
     * Returns an out-of-game entity.
     *
     * @param id the <code>int</code> ID of the out-of-game entity.
     * @return the out-of-game <code>Entity</code> with that ID. If no
     * out-of-game entity has that ID, returns a <code>null</code>.
     */
    public @Nullable Entity getOutOfGameEntity(int id) {
        Entity match = null;
        Enumeration<Entity> iter = vOutOfGame.elements();
        while ((null == match) && iter.hasMoreElements()) {
            Entity entity = iter.nextElement();
            if (id == entity.getId()) {
                match = entity;
            }
        }
        return match;
    }

    /**
     * Returns a <code>Vector</code> containing the <code>Entity</code>s that
     * are in the same C3 network as the passed-in unit. The output will contain
     * the passed-in unit, if the unit has a C3 computer. If the unit has no C3
     * computer, the output will be empty (but it will never be
     * <code>null</code>).
     *
     * @param entity - the <code>Entity</code> whose C3 network co- members is
     *               required. This value may be <code>null</code>.
     * @return a <code>Vector</code> that will contain all other
     * <code>Entity</code>s that are in the same C3 network as the
     * passed-in unit. This <code>Vector</code> may be empty, but it
     * will not be <code>null</code>.
     * @see #getC3SubNetworkMembers(Entity)
     */
    public Vector<Entity> getC3NetworkMembers(Entity entity) {
        Vector<Entity> members = new Vector<>();
        //WOR
        // Does the unit have a C3 computer?
        if ((entity != null) && entity.hasAnyC3System()) {

            // Walk throught the entities in the game, and add all
            // members of the C3 network to the output Vector.
            for (Entity unit : entities) {
                if (entity.equals(unit) || entity.onSameC3NetworkAs(unit)) {
                    members.addElement(unit);
                }
            }

        } // End entity-has-C3

        return members;
    }

    /**
     * Returns a <code>Vector</code> containing the <code>Entity</code>s that
     * are in the C3 sub-network under the passed-in unit. The output will
     * contain the passed-in unit, if the unit has a C3 computer. If the unit
     * has no C3 computer, the output will be empty (but it will never be
     * <code>null</code>). If the passed-in unit is a company commander or a
     * member of a C3i network, this call is the same as
     * <code>getC3NetworkMembers</code>.
     *
     * @param entity - the <code>Entity</code> whose C3 network sub- members is
     *               required. This value may be <code>null</code>.
     * @return a <code>Vector</code> that will contain all other
     * <code>Entity</code>s that are in the same C3 network under the
     * passed-in unit. This <code>Vector</code> may be empty, but it
     * will not be <code>null</code>.
     * @see #getC3NetworkMembers(Entity)
     */
    public Vector<Entity> getC3SubNetworkMembers(Entity entity) {
        //WOR
        // Handle null, C3i, NC3, and company commander units.
        if ((entity == null) || entity.hasC3i() || entity.hasNavalC3() || entity.hasActiveNovaCEWS() || entity.C3MasterIs(entity)) {
            return getC3NetworkMembers(entity);
        }

        Vector<Entity> members = new Vector<>();

        // Does the unit have a C3 computer?
        if (entity.hasC3()) {

            // Walk throught the entities in the game, and add all
            // sub-members of the C3 network to the output Vector.
            for (Entity unit : entities) {
                if (entity.equals(unit) || unit.C3MasterIs(entity)) {
                    members.addElement(unit);
                }
            }

        } // End entity-has-C3

        return members;
    }

    /**
     * Returns a <code>Hashtable</code> that maps the <code>Coords</code> of
     * each unit in this <code>Game</code> to a <code>Vector</code> of
     * <code>Entity</code>s at that positions. Units that have no position (e.g.
     * loaded units) will not be in the map.
     *
     * @return a <code>Hashtable</code> that maps the <code>Coords</code>
     * positions or each unit in the game to a <code>Vector</code> of
     * <code>Entity</code>s at that position.
     */
    public Hashtable<Coords, Vector<Entity>> getPositionMap() {
        Hashtable<Coords, Vector<Entity>> positionMap = new Hashtable<>();
        Vector<Entity> atPos;

        // Walk through the entities in this game.
        for (Entity entity : entities) {
            // Get the vector for this entity's position.
            final Coords coords = entity.getPosition();
            if (coords != null) {
                atPos = positionMap.get(coords);

                // If this is the first entity at this position,
                // create the vector and add it to the map.
                if (atPos == null) {
                    atPos = new Vector<>();
                    positionMap.put(coords, atPos);
                }

                // Add the entity to the vector for this position.
                atPos.addElement(entity);

            }
        } // Handle the next entity.

        // Return the map.
        return positionMap;
    }

    /**
     * Returns an enumeration of salvagable entities.
     */
    public Enumeration<Entity> getGraveyardEntities() {
        Vector<Entity> graveyard = new Vector<>();

        for (Entity entity : vOutOfGame) {
            if ((entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_SALVAGEABLE)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_EJECTED)) {
                graveyard.addElement(entity);
            }
        }

        return graveyard.elements();
    }

    /**
     * Returns an enumeration of wrecked entities.
     */
    public Enumeration<Entity> getWreckedEntities() {
        Vector<Entity> wrecks = new Vector<>();
        for (Entity entity : vOutOfGame) {
            if ((entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_SALVAGEABLE)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_EJECTED)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED)) {
                wrecks.addElement(entity);
            }
        }
        
        return wrecks.elements();
    }

    /**
     * Returns an enumeration of entities that have retreated
     */
 // TODO: Correctly implement "Captured" Entities
    public Enumeration<Entity> getRetreatedEntities() {
        Vector<Entity> sanctuary = new Vector<>();

        for (Entity entity : vOutOfGame) {
            if ((entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_IN_RETREAT)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_CAPTURED)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_PUSHED)) {
                sanctuary.addElement(entity);
            }
        }

        return sanctuary.elements();
    }

    /**
     * Returns an enumeration of entities that were utterly destroyed
     */
    public Enumeration<Entity> getDevastatedEntities() {
        Vector<Entity> smithereens = new Vector<>();

        for (Entity entity : vOutOfGame) {
            if (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED) {
                smithereens.addElement(entity);
            }
        }

        return smithereens.elements();
    }
    
    /**
     * Returns an enumeration of "carcass" entities, i.e., vehicles with dead
     * crews that are still on the map.
     */
    public Enumeration<Entity> getCarcassEntities() {
        Vector<Entity> carcasses = new Vector<>();
        
        for (Entity entity : entities) {
            if (entity.isCarcass()) {
                carcasses.addElement(entity);
            }
        }
        
        return carcasses.elements();
    }

    /**
     * Return the current number of entities in the game.
     */
    public int getNoOfEntities() {
        return entities.size();
    }

    /**
     * Returns the appropriate target for this game given a type and id
     */
    public @Nullable Targetable getTarget(int nType, int nID) {
        try {
            switch (nType) {
                case Targetable.TYPE_ENTITY:
                    return getEntity(nID);
                case Targetable.TYPE_HEX_CLEAR:
                case Targetable.TYPE_HEX_IGNITE:
                case Targetable.TYPE_HEX_BOMB:
                case Targetable.TYPE_MINEFIELD_DELIVER:
                case Targetable.TYPE_FLARE_DELIVER:
                case Targetable.TYPE_HEX_EXTINGUISH:
                case Targetable.TYPE_HEX_ARTILLERY:
                case Targetable.TYPE_HEX_SCREEN:
                case Targetable.TYPE_HEX_AERO_BOMB:
                case Targetable.TYPE_HEX_TAG:
                    return new HexTarget(HexTarget.idToCoords(nID), nType);
                case Targetable.TYPE_FUEL_TANK:
                case Targetable.TYPE_FUEL_TANK_IGNITE:
                case Targetable.TYPE_BUILDING:
                case Targetable.TYPE_BLDG_IGNITE:
                case Targetable.TYPE_BLDG_TAG:
                    if (getBoard().getBuildingAt(BuildingTarget.idToCoords(nID)) != null) {
                        return new BuildingTarget(BuildingTarget.idToCoords(nID), board, nType);
                    } else {
                        return null;
                    }
                case Targetable.TYPE_MINEFIELD_CLEAR:
                    return new MinefieldTarget(MinefieldTarget.idToCoords(nID));
                case Targetable.TYPE_INARC_POD:
                    return INarcPod.idToInstance(nID);
                default:
                    return null;
            }
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return null;
        }
    }

    /**
     * Returns the entity with the given id number, if any.
     */
    public @Nullable Entity getEntity(final int id) {
        return entityIds.get(id);
    }

    /**
     * looks for an entity by id number even if out of the game
     */
    public Entity getEntityFromAllSources(int id) {
        Entity en = getEntity(id);
        if (null == en) {
            for (Entity entity : vOutOfGame) {
                if (entity.getId() == id) {
                    return entity;
                }
            }
        }
        return en;
    }

    /**
     * Adds a collection of new Entities.
     * Only one GameEntityNewEvent is created for the whole list.
     *
     * @param entities the Entity objects to be added.
     */
    public void addEntities(List<Entity> entities) {
        for (int i = 0; i < entities.size(); i++) {
            addEntity(entities.get(i), false);
        }
        // We need to delay calculating BV until all units have been added because
        // C3 network connections will be cleared if the master is not in the game yet.
        entities.forEach(e -> e.setInitialBV(e.calculateBattleValue(false, false)));
        processGameEvent(new GameEntityNewEvent(this, entities));
    }

    /**
     * Adds a new Entity. The id parameter is ignored and addEntity(Entity) is called instead. This
     * is just to maintain compatibility.
     *
     * @param id Value that is ignored: the id is pulled from the passed Entity
     * @param entity The Entity to add to the game.
     */
    @Deprecated
    public void addEntity(int id, Entity entity) {
        // Disregard the passed id, addEntity(Entity) pulls the id from the
        //  Entity instance.
        addEntity(entity);
    }

    /**
     * Adds a new Entity to this Game object and generates a GameEntityNewEvent.
     *
     * @param entity The Entity to add.
     */
    public void addEntity(Entity entity) {
        addEntity(entity, true);
    }

    /**
     * Adds a new Entity to this Game object.
     *
     * @param entity The Entity to add.
     * @param genEvent A flag that determines whether a GameEntityNewEvent is generated.
     */
    public synchronized void addEntity(Entity entity, boolean genEvent) {
        entity.setGame(this);
        if (entity instanceof Mech) {
            ((Mech) entity).setBAGrabBars();
            ((Mech) entity).setProtomechClampMounts();
        } else if (entity instanceof Tank) {
            ((Tank) entity).setBAGrabBars();
            ((Tank) entity).setTrailerHitches();
        }

        // Add magnetic clamp mounts
        if ((entity instanceof Mech) && !entity.isOmni() && !entity.hasBattleArmorHandles()) {
            entity.addTransporter(new ClampMountMech());
        } else if ((entity instanceof Tank) && !entity.isOmni()
                && !entity.hasBattleArmorHandles()) {
            entity.addTransporter(new ClampMountTank());
        }

        entity.setGameOptions();
        if (entity.getC3UUIDAsString() == null) {
            // We don't want to be resetting a UUID that exists already!
            entity.setC3UUID();
        }
        // Add this Entity, ensuring that it's id is unique
        int id = entity.getId();
        if (!entityIds.containsKey(id)) {
            entityIds.put(id, entity);
        } else {
            id = getNextEntityId();
            entity.setId(id);
            entityIds.put(id, entity);
        }
        entities.add(entity);
        updateEntityPositionLookup(entity, null);

        if (id > lastEntityId) {
            lastEntityId = id;
        }

        // And... lets get this straight now.
        if ((entity instanceof Mech)
                && getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)) {
            ((Mech) entity).setAutoEject(true);
            ((Mech) entity).setCondEjectAmmo(!entity.hasCase() && !((Mech) entity).hasCASEIIAnywhere());
            ((Mech) entity).setCondEjectEngine(true);
            ((Mech) entity).setCondEjectCTDest(true);
            ((Mech) entity).setCondEjectHeadshot(true);
        }

        assert (entities.size() == entityIds.size()) : "Add Entity failed";
        if (genEvent) {
            entity.setInitialBV(entity.calculateBattleValue(false, false));
            processGameEvent(new GameEntityNewEvent(this, entity));
        }
    }

    public void setEntity(int id, Entity entity) {
        setEntity(id, entity, null);
    }

    public synchronized void setEntity(int id, Entity entity, Vector<UnitLocation> movePath) {
        final Entity oldEntity = getEntity(id);
        if (oldEntity == null) {
            addEntity(entity);
        } else {
            entity.setGame(this);
            entities.set(entities.indexOf(oldEntity), entity);
            entityIds.put(id, entity);
            // Get the collection of positions
            HashSet<Coords> oldPositions = oldEntity.getOccupiedCoords();
            // Update position lookup table
            updateEntityPositionLookup(entity, oldPositions);

            // Not sure if this really required
            if (id > lastEntityId) {
                lastEntityId = id;
            }

            processGameEvent(
                    new GameEntityChangeEvent(this, entity, movePath, oldEntity));
        }
        assert (entities.size() == entityIds.size()) : "Set Entity Failed";
    }

    /**
     * @return int containing an unused entity id
     */
    public int getNextEntityId() {
        return lastEntityId + 1;
    }

    /**
     * @return <code>true</code> if an entity with the specified id number exists in this game.
     */
    public boolean hasEntity(int entityId) {
        return entityIds.containsKey(entityId);
    }

    /**
     * Remove an entity from the master list. If we can't find that entity,
     * (probably due to double-blind) ignore it.
     */
    public synchronized void removeEntity(int id, int condition) {
        // always attempt to remove the entity with this ID from the entities collection
        // as it may have gotten stuck there.
        entities.removeIf(ent -> (ent.getId() == id));
        
        Entity toRemove = getEntity(id);
        if (toRemove == null) {
            return;
        }

        entityIds.remove(id);
        removeEntityPositionLookup(toRemove);

        toRemove.setRemovalCondition(condition);

        // do not keep never-joined entities
        if ((vOutOfGame != null)
            && (condition != IEntityRemovalConditions.REMOVE_NEVER_JOINED)) {
            vOutOfGame.addElement(toRemove);
        }

        // We also need to remove it from the list of things to be deployed...
        // we might still be in this list if we never joined the game
        if (!deploymentTable.isEmpty()) {
            Enumeration<Vector<Entity>> iter = deploymentTable.elements();

            while (iter.hasMoreElements()) {
                Vector<Entity> vec = iter.nextElement();

                for (int i = vec.size() - 1; i >= 0; i--) {
                    Entity en = vec.elementAt(i);

                    if (en.getId() == id) {
                        vec.removeElementAt(i);
                    }
                }
            }
        }
        processGameEvent(new GameEntityRemoveEvent(this, toRemove));
    }

    public void removeEntities(List<Integer> ids, int condition) {
        for (int i = 0; i < ids.size(); i++) {
            removeEntity(ids.get(i), condition);
        }
    }

    /**
     * Resets this game.
     */
    public synchronized void reset() {
        uuid = UUID.randomUUID();

        roundCount = 0;

        entities.clear();
        entityIds.clear();
        entityPosLookup.clear();

        vOutOfGame.removeAllElements();

        turnVector.clear();
        turnIndex = 0;

        resetActions();
        resetCharges();
        resetRams();
        resetPSRs();
        resetArtilleryAttacks();
        resetAttacks();
        // removeMinefields();  Broken and bad!
        clearMinefields();
        removeArtyAutoHitHexes();
        flares.removeAllElements();
        illuminatedPositions.clear();
        clearAllReports();
        smokeCloudList.clear();

        forceVictory = false;
        victoryPlayerId = Player.PLAYER_NONE;
        victoryTeam = Player.TEAM_NONE;
        lastEntityId = 0;
        planetaryConditions = new PlanetaryConditions();
    }

    private void removeArtyAutoHitHexes() {
        Enumeration<Player> iter = getPlayers();
        while (iter.hasMoreElements()) {
            Player player = iter.nextElement();
            player.removeArtyAutoHitHexes();
        }
    }

    /**
     * Regenerates the entities by id hashtable by going thru all entities in
     * the Vector
     */
    private void reindexEntities() {
        entityIds.clear();
        lastEntityId = 0;

        // Add these entities to the game.
        for (Entity entity : entities) {
            final int id = entity.getId();
            entityIds.put(id, entity);

            if (id > lastEntityId) {
                lastEntityId = id;
            }
        }
        // We need to ensure that each entity has the proper Game reference
        // however, the entityIds Hashmap must be fully formed before this
        // is called, since setGame also calls setGame for loaded Entities
        for (Entity entity : entities) {
            entity.setGame(this);
        }
    }

    /**
     * Returns the first entity at the given coordinate, if any. Only returns
     * targetable (non-dead) entities.
     *
     * @param c the coordinates to search at
     */
    public Entity getFirstEntity(Coords c) {
        for (Entity entity : entities) {
            if (c.equals(entity.getPosition()) && entity.isTargetable()) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Returns the first enemy entity at the given coordinate, if any. Only
     * returns targetable (non-dead) entities.
     *
     * @param c the coordinates to search at
     * @param currentEntity the entity that is firing
     */
    public Entity getFirstEnemyEntity(Coords c, Entity currentEntity) {
        for (Entity entity : entities) {
            if (c.equals(entity.getPosition()) && entity.isTargetable()
                && entity.isEnemyOf(currentEntity)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Returns an Enumeration of the active entities at the given coordinates.
     */
    public Iterator<Entity> getEntities(Coords c) {
        return getEntities(c, false);
    }

    /**
     * Returns an Enumeration of the active entities at the given coordinates.
     */
    public Iterator<Entity> getEntities(Coords c, boolean ignore) {
        return getEntitiesVector(c, ignore).iterator();
    }

    /**
     * Return an {@link Entity} <code>List</code> at {@link Coords} <code>c</code>, checking if
     * they can be targetted.
     *
     * @param c The coordinates to check
     * @return the {@link Entity} <code>List</code>
     */
    public List<Entity> getEntitiesVector(Coords c) {
        return getEntitiesVector(c, false);
    }

    /**
     * Return an {@link Entity} <code>List</code> at {@link Coords} <code>c</code>
     *
     * @param c The coordinates to check
     * @param ignore Flag that determines whether the ability to target is ignored
     * @return the {@link Entity} <code>List</code>
     */
    public synchronized List<Entity> getEntitiesVector(Coords c, boolean ignore) {
        // checkPositionCacheConsistency();
        // Make sure the look-up is initialized
        if (entityPosLookup.isEmpty() && !entities.isEmpty()) {
            resetEntityPositionLookup();
        }
        Set<Integer> posEntities = entityPosLookup.get(c);
        List<Entity> vector = new ArrayList<>();
        if (posEntities != null) {
            for (Integer eId : posEntities) {
                Entity e = getEntity(eId);
                
                // if the entity with the given ID doesn't exist, we will update the lookup table
                // and move on
                if (e == null) {
                    posEntities.remove(eId);
                    continue;
                }
                
                if (e.isTargetable() || ignore) {
                    vector.add(e);

                    // Sanity check
                    HashSet<Coords> positions = e.getOccupiedCoords();
                    if (!positions.contains(c)) {
                        LogManager.getLogger().error(e.getDisplayName() + " is not in " + c + "!");
                    }
                }
            }
        }
        return Collections.unmodifiableList(vector);
    }
    
    /**
     * Convenience function that gets a list of all off-board enemy entities.
     * @param player
     * @return
     */
    public synchronized List<Entity> getAllOffboardEnemyEntities(Player player) {
        List<Entity> vector = new ArrayList<>();
        for (Entity e : entities) {
            if (e.getOwner().isEnemyOf(player) && e.isOffBoard() && !e.isDestroyed() && e.isDeployed()) {
                vector.add(e);
            }
        }
        
        return Collections.unmodifiableList(vector);
    }

    /**
     * Return a Vector of gun emplacements at Coords <code>c</code>
     *
     * @param c The coordinates to check
     * @return the {@link GunEmplacement} <code>Vector</code>
     */
    public Vector<GunEmplacement> getGunEmplacements(Coords c) {
        Vector<GunEmplacement> vector = new Vector<>();

        // Only build the list if the coords are on the board.
        if (board.contains(c)) {
            for (Entity entity : getEntitiesVector(c, true)) {
                if (entity.hasETypeFlag(Entity.ETYPE_GUN_EMPLACEMENT)) {
                    vector.addElement((GunEmplacement) entity);
                }
            }
        }

        return vector;
    }
    
    /**
     * Determine if the given set of coordinates has a gun emplacement on the roof of a building.
     * @param c The coordinates to check
     */
    public boolean hasRooftopGunEmplacement(Coords c) {
        Building building = getBoard().getBuildingAt(c);
        if (building == null) {
            return false;
        }
        
        Hex hex = getBoard().getHex(c);
        
        for (Entity entity : getEntitiesVector(c, true)) {
            if (entity.hasETypeFlag(Entity.ETYPE_GUN_EMPLACEMENT) && entity.getElevation() == hex.ceiling()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Returns a Target for an Accidental Fall From above, or null if no
     * possible target is there
     *
     * @param c The <code>Coords</code> of the hex in which the accidental fall from above happens
     * @param ignore The entity who is falling, so shouldn't be returned
     * @return The <code>Entity</code> that should be an AFFA target.
     */
    public @Nullable Entity getAffaTarget(Coords c, Entity ignore) {
        Vector<Entity> vector = new Vector<>();
        if (board.contains(c)) {
            Hex hex = board.getHex(c);
            for (Entity entity : getEntitiesVector(c)) {
                if (entity.isTargetable()
                        && ((entity.getElevation() == 0) // Standing on hex surface
                                || (entity.getElevation() == -hex.depth())) // Standing on hex floor
                        && (entity.getAltitude() == 0)
                        && !(entity instanceof Infantry) && (entity != ignore)) {
                    vector.addElement(entity);
                }
            }
        }

        if (!vector.isEmpty()) {
            int count = vector.size();
            int random = Compute.randomInt(count);
            return vector.elementAt(random);
        }
        return null;
    }

    /**
     * Returns an <code>Iterator</code> of the enemy's active entities at the given coordinates.
     *
     * @param coords the <code>Coords</code> of the hex being examined.
     * @param currentEntity the <code>Entity</code> whose enemies are needed.
     * @return an <code>Enumeration</code> of <code>Entity</code>s at the given coordinates who are
     * enemies of the given unit.
     */
    public Iterator<Entity> getEnemyEntities(final Coords coords, final Entity currentEntity) {
        return getSelectedEntities(entity -> coords.equals(entity.getPosition())
                && entity.isTargetable() && entity.isEnemyOf(currentEntity));
    }

    /**
     * Returns an <code>Enumeration</code> of active enemy entities
     *
     * @param currentEntity the <code>Entity</code> whose enemies are needed.
     * @return an <code>Enumeration</code> of <code>Entity</code>s at the given coordinates who are
     * enemies of the given unit.
     */
    public Iterator<Entity> getAllEnemyEntities(final Entity currentEntity) {
        return getSelectedEntities(entity -> entity.isTargetable() && entity.isEnemyOf(currentEntity));
    }

    /**
     * Returns an <code>Iterator</code> of friendly active entities at the given coordinates.
     *
     * @param coords the <code>Coords</code> of the hex being examined.
     * @param currentEntity the <code>Entity</code> whose friends are needed.
     * @return an <code>Enumeration</code> of <code>Entity</code>s at the given coordinates who are
     * friends of the given unit.
     */
    public Iterator<Entity> getFriendlyEntities(final Coords coords, final Entity currentEntity) {
        return getSelectedEntities(entity -> coords.equals(entity.getPosition())
                && entity.isTargetable() && !entity.isEnemyOf(currentEntity));
    }

    /**
     * Moves an entity into the graveyard, so it stops getting sent out every phase.
     */
    public void moveToGraveyard(int id) {
        removeEntity(id, IEntityRemovalConditions.REMOVE_SALVAGEABLE);
    }

    /**
     * See if the <code>Entity</code> with the given ID is out of the game.
     *
     * @param id - the ID of the <code>Entity</code> to be checked.
     * @return <code>true</code> if the <code>Entity</code> is in the graveyard,
     * <code>false</code> otherwise.
     */
    public boolean isOutOfGame(int id) {
        for (Entity entity : vOutOfGame) {
            if (entity.getId() == id) {
                return true;
            }
        }

        return false;
    }

    /**
     * See if the <code>Entity</code> is out of the game.
     *
     * @param entity - the <code>Entity</code> to be checked.
     * @return <code>true</code> if the <code>Entity</code> is in the graveyard,
     * <code>false</code> otherwise.
     */
    public boolean isOutOfGame(Entity entity) {
        return isOutOfGame(entity.getId());
    }

    /**
     * @return the first entity that can act in the present turn, or null if none can.
     */
    public @Nullable Entity getFirstEntity() {
        return getFirstEntity(getTurn());
    }

    /**
     * @param turn the current game turn, which may be null
     * @return the first entity that can act in the specified turn, or null if none can.
     */
    public @Nullable Entity getFirstEntity(final @Nullable GameTurn turn) {
        return getEntity(getFirstEntityNum(turn));
    }

    /**
     * @return the id of the first entity that can act in the current turn, or -1 if none can.
     */
    public int getFirstEntityNum() {
        return getFirstEntityNum(getTurn());
    }

    /**
     * @param turn the current game turn, which may be null
     * @return the id of the first entity that can act in the specified turn, or -1 if none can.
     */
    public int getFirstEntityNum(final @Nullable GameTurn turn) {
        if (turn == null) {
            return -1;
        }

        for (Entity entity : entities) {
            if (turn.isValidEntity(entity, this)) {
                return entity.getId();
            }
        }

        return -1;
    }

    /**
     * @param start the index number to start at (not an Entity Id)
     * @return the next selectable entity that can act this turn, or null if none can.
     */
    public @Nullable Entity getNextEntity(int start) {
        if (entities.isEmpty()) {
            return null;
        }
        start = start % entities.size();
        int entityId = entities.get(start).getId();
        return getEntity(getNextEntityNum(getTurn(), entityId));
    }

    /**
     * @param turn the turn to use, which may be null
     * @param start the entity id to start at
     * @return the entity id of the next entity that can move during the specified turn
     */
    public int getNextEntityNum(final @Nullable GameTurn turn, int start) {
        // If we don't have a turn, return ENTITY_NONE
        if (turn == null) {
            return Entity.NONE;
        }
        boolean hasLooped = false;
        int i = (entities.indexOf(entityIds.get(start)) + 1) % entities.size();
        if (i == -1) {
            //This means we were given an invalid entity ID, punt
            return Entity.NONE;
        }
        int startingIndex = i;
        while (!((hasLooped == true) && (i == startingIndex))) {
            final Entity entity = entities.get(i);
            if (turn.isValidEntity(entity, this)) {
                return entity.getId();
            }
            i++;
            if (i == entities.size()) {
                i = 0;
                hasLooped = true;
            }
        }
        // return getFirstEntityNum(turn);
        return Entity.NONE;
    }

    /**
     * @param turn the turn to use
     * @param start the entity id to start at
     * @return the entity id of the previous entity that can move during the specified turn
     */
    public int getPrevEntityNum(GameTurn turn, int start) {
        boolean hasLooped = false;
        int i = (entities.indexOf(entityIds.get(start)) - 1) % entities.size();
        if (i == -2) {
            //This means we were given an invalid entity ID, punt
            return -1;
        }
        if (i == -1) {
            //This means we were given an invalid entity ID, punt
            i = entities.size() - 1;
        }
        int startingIndex = i;
        while (!((hasLooped == true) && (i == startingIndex))) {
            final Entity entity = entities.get(i);
            if (turn.isValidEntity(entity, this)) {
                return entity.getId();
            }
            i--;
            if (i < 0) {
                i = entities.size() - 1;
                hasLooped = true;
            }
        }
        // return getFirstEntityNum(turn);
        return -1;
    }

    /**
     * @param turn the current game turn, which may be null
     * @return the number of the first deployable entity that is valid for the specified turn
     */
    public int getFirstDeployableEntityNum(final @Nullable GameTurn turn) {
        // Repeat the logic from getFirstEntityNum.
        if (turn == null) {
            return -1;
        }
        for (Entity entity : entities) {
            if (turn.isValidEntity(entity, this) && entity.shouldDeploy(getRoundCount())) {
                return entity.getId();
            }
        }
        return -1;
    }

    /**
     * @return the number of the next deployable entity that is valid for the specified turn
     */
    public int getNextDeployableEntityNum(GameTurn turn, int start) {
        if (start >= 0) {
            for (int i = start; i < entities.size(); i++) {
                final Entity entity = entities.get(i);
                if (turn.isValidEntity(entity, this) && entity.shouldDeploy(getRoundCount())) {
                    return entity.getId();
                }
            }
        }
        return getFirstDeployableEntityNum(turn);
    }

    /**
     * Get the entities for the player.
     *
     * @param player - the <code>Player</code> whose entities are required.
     * @param hide   - should fighters loaded into squadrons be excluded?
     * @return a <code>Vector</code> of <code>Entity</code>s.
     */
    public ArrayList<Entity> getPlayerEntities(Player player, boolean hide) {
        ArrayList<Entity> output = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity.isPartOfFighterSquadron() && hide) {
                continue;
            }
            if (player.equals(entity.getOwner())) {
                output.add(entity);
            }
        }
        return output;
    }

    /**
     * Get the entities for the player.
     *
     * @param player - the <code>Player</code> whose entities are required.
     * @param hide   - should fighters loaded into squadrons be excluded from this list?
     * @return a <code>Vector</code> of <code>Entity</code>s.
     */
    public ArrayList<Integer> getPlayerEntityIds(Player player, boolean hide) {
        ArrayList<Integer> output = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity.isPartOfFighterSquadron() && hide) {
                continue;
            }
            if (player.equals(entity.getOwner())) {
                output.add(entity.getId());
            }
        }
        return output;
    }

    /**
     * Determines if the indicated entity is stranded on a transport that can't move.
     * <p>
     * According to
     * <a href="http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=555466&page=2&view=collapsed&sb=5&o=0&fpart=">Randall Bills</a>,
     * the "minimum move" rule allow stranded units to dismount at the start of the turn.
     *
     * @param entity the <code>Entity</code> that may be stranded
     * @return <code>true</code> if the entity is stranded <code>false</code> otherwise.
     */
    public boolean isEntityStranded(Entity entity) {

        // Is the entity being transported?
        final int transportId = entity.getTransportId();
        Entity transport = getEntity(transportId);
        if ((Entity.NONE != transportId) && (null != transport)) {

            // aero units don't count here
            if (transport instanceof Aero) {
                return false;
            }

            // Can that transport unload the unit?
            if (transport.isImmobile() || (0 == transport.getWalkMP())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param playerId the player's Id
     * @return number of infantry <code>playerId</code> has not selected yet this turn
     */
    public int getInfantryLeft(int playerId) {
        Player player = getPlayer(playerId);
        int remaining = 0;

        for (Entity entity : entities) {
            if (player.equals(entity.getOwner()) && entity.isSelectableThisTurn()
                    && (entity instanceof Infantry)) {
                remaining++;
            }
        }

        return remaining;
    }

    /**
     * @param playerId the player's Id
     * @return number of ProtoMechs <code>playerId</code> has not selected yet this turn
     */
    public int getProtomechsLeft(int playerId) {
        Player player = getPlayer(playerId);
        int remaining = 0;

        for (Entity entity : entities) {
            if (player.equals(entity.getOwner()) && entity.isSelectableThisTurn()
                    && (entity instanceof Protomech)) {
                remaining++;
            }
        }

        return remaining;
    }

    /**
     * @param playerId the player's Id
     * @return number of vehicles <code>playerId</code> has not selected yet this turn
     */
    public int getVehiclesLeft(int playerId) {
        Player player = getPlayer(playerId);
        int remaining = 0;

        for (Entity entity : entities) {
            if (player.equals(entity.getOwner()) && entity.isSelectableThisTurn()
                    && (entity instanceof Tank)) {
                remaining++;
            }
        }

        return remaining;
    }

    /**
     * @param playerId the player's Id
     * @return number of 'Mechs <code>playerId</code> has not selected yet this turn
     */
    public int getMechsLeft(int playerId) {
        Player player = getPlayer(playerId);
        int remaining = 0;

        for (Entity entity : entities) {
            if (player.equals(entity.getOwner()) && entity.isSelectableThisTurn()
                    && (entity instanceof Mech)) {
                remaining++;
            }
        }

        return remaining;
    }

    /**
     * Removes the first turn found that the specified entity can move in. Used
     * when a turn is played out of order
     */
    public @Nullable GameTurn removeFirstTurnFor(final Entity entity) throws Exception {
        if (getPhase().isMovement()) {
            throw new Exception("Cannot remove first turn for an entity when it is the movement phase");
        }

        for (int i = turnIndex; i < turnVector.size(); i++) {
            GameTurn turn = turnVector.elementAt(i);
            if (turn.isValidEntity(entity, this)) {
                turnVector.removeElementAt(i);
                return turn;
            }
        }
        return null;
    }

    /**
     * Removes the last, next turn found that the specified entity can move in.
     * Used when, say, an entity dies mid-phase.
     */
    public void removeTurnFor(Entity entity) {
        if (turnVector.isEmpty()) {
            return;
        }
        // If the game option "move multiple infantry per mech" is selected,
        // then we might not need to remove a turn at all.
        // A turn only needs to be removed when going from 4 inf (2 turns) to
        // 3 inf (1 turn)
        if (getOptions().booleanOption(OptionsConstants.INIT_INF_MOVE_MULTI)
                && (entity instanceof Infantry) && getPhase().isMovement()) {
            if ((getInfantryLeft(entity.getOwnerId()) % getOptions().intOption(
                    OptionsConstants.INIT_INF_PROTO_MOVE_MULTI)) != 1) {
                // exception, if the _next_ turn is an infantry turn, remove that
                // contrived, but may come up e.g. one inf accidentally kills another
                if (hasMoreTurns()) {
                    GameTurn nextTurn = turnVector.elementAt(turnIndex + 1);
                    if (nextTurn instanceof GameTurn.EntityClassTurn) {
                        GameTurn.EntityClassTurn ect =
                                (GameTurn.EntityClassTurn) nextTurn;
                        if (ect.isValidClass(GameTurn.CLASS_INFANTRY)
                            && !ect.isValidClass(~GameTurn.CLASS_INFANTRY)) {
                            turnVector.removeElementAt(turnIndex + 1);
                        }
                    }
                }
                return;
            }
        }
        // Same thing but for protos
        if (getOptions().booleanOption(OptionsConstants.INIT_PROTOS_MOVE_MULTI)
                && (entity instanceof Protomech) && getPhase().isMovement()) {
            if ((getProtomechsLeft(entity.getOwnerId()) % getOptions()
                    .intOption(OptionsConstants.INIT_INF_PROTO_MOVE_MULTI)) != 1) {
                // exception, if the _next_ turn is an protomek turn, remove that
                // contrived, but may come up e.g. one inf accidently kills another
                if (hasMoreTurns()) {
                    GameTurn nextTurn = turnVector.elementAt(turnIndex + 1);
                    if (nextTurn instanceof GameTurn.EntityClassTurn) {
                        GameTurn.EntityClassTurn ect =
                                (GameTurn.EntityClassTurn) nextTurn;
                        if (ect.isValidClass(GameTurn.CLASS_PROTOMECH)
                            && !ect.isValidClass(~GameTurn.CLASS_PROTOMECH)) {
                            turnVector.removeElementAt(turnIndex + 1);
                        }
                    }
                }
                return;
            }
        }

        // Same thing but for vehicles
        if (getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT)
                && (entity instanceof Tank) && getPhase().isMovement()) {
            if ((getVehiclesLeft(entity.getOwnerId()) % getOptions()
                    .intOption(OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT_NUMBER)) != 1) {
                // exception, if the _next_ turn is a tank turn, remove that
                // contrived, but may come up e.g. one tank accidentally kills another
                if (hasMoreTurns()) {
                    GameTurn nextTurn = turnVector.elementAt(turnIndex + 1);
                    if (nextTurn instanceof GameTurn.EntityClassTurn) {
                        GameTurn.EntityClassTurn ect =
                                (GameTurn.EntityClassTurn) nextTurn;
                        if (ect.isValidClass(GameTurn.CLASS_TANK)
                            && !ect.isValidClass(~GameTurn.CLASS_TANK)) {
                            turnVector.removeElementAt(turnIndex + 1);
                        }
                    }
                }
                return;
            }
        }

        // Same thing but for meks
        if (getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT)
                && (entity instanceof Mech) && getPhase().isMovement()) {
            if ((getMechsLeft(entity.getOwnerId()) % getOptions()
                    .intOption(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT_NUMBER)) != 1) {
                // exception, if the _next_ turn is a mech turn, remove that
                // contrived, but may come up e.g. one mech accidentally kills another
                if (hasMoreTurns()) {
                    GameTurn nextTurn = turnVector.elementAt(turnIndex + 1);
                    if (nextTurn instanceof GameTurn.EntityClassTurn) {
                        GameTurn.EntityClassTurn ect =
                                (GameTurn.EntityClassTurn) nextTurn;
                        if (ect.isValidClass(GameTurn.CLASS_MECH)
                            && !ect.isValidClass(~GameTurn.CLASS_MECH)) {
                            turnVector.removeElementAt(turnIndex + 1);
                        }
                    }
                }
                return;
            }
        }


        boolean useInfantryMoveLaterCheck = true;
        // If we have the "infantry move later" or "protos move later" optional
        //  rules, then we may be removing an infantry unit that would be
        //  considered invalid unless we don't consider the extra validity
        //  checks.
        if ((getOptions().booleanOption(OptionsConstants.INIT_INF_MOVE_LATER) &&
             (entity instanceof Infantry)) ||
            (getOptions().booleanOption(OptionsConstants.INIT_PROTOS_MOVE_LATER) &&
             (entity instanceof Protomech))) {
            useInfantryMoveLaterCheck = false;
        }

        for (int i = turnVector.size() - 1; i >= turnIndex; i--) {
            GameTurn turn = turnVector.elementAt(i);

            if (turn.isValidEntity(entity, this, useInfantryMoveLaterCheck)) {
                turnVector.removeElementAt(i);
                break;
            }
        }
    }

    /**
     * Removes any turns that can only be taken by the specified entity. Useful if the specified
     * Entity is being removed from the game to ensure any turns that only it can take are gone.
     *
     * @param entity the entity to remove turns for
     * @return The number of turns returned
     */
    public int removeSpecificEntityTurnsFor(Entity entity) {
        List<GameTurn> turnsToRemove = new ArrayList<>();
        
        for (GameTurn turn : turnVector) {
            if (turn instanceof SpecificEntityTurn) {
                int turnOwner = ((SpecificEntityTurn) turn).getEntityNum();
                if (entity.getId() == turnOwner) {
                    turnsToRemove.add(turn);
                }
            }
        }
        turnVector.removeAll(turnsToRemove);
        return turnsToRemove.size();
    }

    /**
     * Adds the specified action to the actions list for this phase.
     */
    public void addAction(EntityAction ea) {
        actions.addElement(ea);
        processGameEvent(new GameNewActionEvent(this, ea));
    }

    public void setArtilleryVector(Vector<ArtilleryAttackAction> v) {
        offboardArtilleryAttacks = v;
        processGameEvent(new GameBoardChangeEvent(this));
    }

    public void resetArtilleryAttacks() {
        offboardArtilleryAttacks.removeAllElements();
    }

    public Enumeration<ArtilleryAttackAction> getArtilleryAttacks() {
        return offboardArtilleryAttacks.elements();
    }

    public int getArtillerySize() {
        return offboardArtilleryAttacks.size();
    }

    /**
     * Returns an Enumeration of actions scheduled for this phase.
     */
    public Enumeration<EntityAction> getActions() {
        return actions.elements();
    }

    /**
     * Resets the actions list.
     */
    public void resetActions() {
        actions.removeAllElements();
    }

    /**
     * Removes all actions by the specified entity
     */
    public void removeActionsFor(int entityId) {
        // or rather, only keeps actions NOT by that entity
        Vector<EntityAction> toKeep = new Vector<>(actions.size());
        for (EntityAction ea : actions) {
            if (ea.getEntityId() != entityId) {
                toKeep.addElement(ea);
            }
        }
        actions = toKeep;
    }

    /**
     * Remove a specified action
     *
     * @param o The action to remove.
     */
    public void removeAction(Object o) {
        actions.removeElement(o);
    }

    public int actionsSize() {
        return actions.size();
    }

    /**
     * Returns the actions vector. Do not use to modify the actions; I will be
     * angry. &gt;:[ Used for sending all actions to the client.
     */
    public List<EntityAction> getActionsVector() {
        return Collections.unmodifiableList(actions);
    }

    public void addInitiativeRerollRequest(Team t) {
        initiativeRerollRequests.addElement(t);
    }

    public void rollInitAndResolveTies() {
        if (getOptions().booleanOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            Vector<TurnOrdered> vRerolls = new Vector<>();
            for (int i = 0; i < entities.size(); i++) {
                Entity e = entities.get(i);
                if (initiativeRerollRequests.contains(getTeamForPlayer(e.getOwner()))) {
                    vRerolls.add(e);
                }
            }
            TurnOrdered.rollInitAndResolveTies(getEntitiesVector(), vRerolls, false);
        } else {
            TurnOrdered.rollInitAndResolveTies(teams, initiativeRerollRequests,
                    getOptions().booleanOption(OptionsConstants.INIT_INITIATIVE_STREAK_COMPENSATION));
        }
        initiativeRerollRequests.removeAllElements();

    }
    
    public void handleInitiativeCompensation() {
        if (getOptions().booleanOption(OptionsConstants.INIT_INITIATIVE_STREAK_COMPENSATION)) {
            TurnOrdered.resetInitiativeCompensation(teams, getOptions().booleanOption(OptionsConstants.INIT_INITIATIVE_STREAK_COMPENSATION));
        }
    }

    public int getNoOfInitiativeRerollRequests() {
        return initiativeRerollRequests.size();
    }

    /**
     * Adds a pending displacement attack to the list for this phase.
     */
    public void addCharge(AttackAction ea) {
        pendingCharges.addElement(ea);
        processGameEvent(new GameNewActionEvent(this, ea));
    }

    /**
     * Returns an Enumeration of displacement attacks scheduled for the end of
     * the physical phase.
     */
    public Enumeration<AttackAction> getCharges() {
        return pendingCharges.elements();
    }

    /**
     * Resets the pending charges list.
     */
    public void resetCharges() {
        pendingCharges.removeAllElements();
    }

    /**
     * Returns the charges vector. Do not modify. &gt;:[ Used for sending all
     * charges to the client.
     */
    public List<AttackAction> getChargesVector() {
        return Collections.unmodifiableList(pendingCharges);
    }

    /**
     * Adds a pending ramming attack to the list for this phase.
     */
    public void addRam(AttackAction ea) {
        pendingRams.addElement(ea);
        processGameEvent(new GameNewActionEvent(this, ea));
    }

    /**
     * Returns an Enumeration of ramming attacks scheduled for the end of the
     * physical phase.
     */
    public Enumeration<AttackAction> getRams() {
        return pendingRams.elements();
    }

    /**
     * Resets the pending rams list.
     */
    public void resetRams() {
        pendingRams.removeAllElements();
    }

    /**
     * Returns the rams vector. Do not modify. &gt;:[ Used for sending all charges
     * to the client.
     */
    public List<AttackAction> getRamsVector() {
        return Collections.unmodifiableList(pendingRams);
    }

    /**
     * Adds a pending ramming attack to the list for this phase.
     */
    public void addTeleMissileAttack(AttackAction ea) {
        pendingTeleMissileAttacks.addElement(ea);
        processGameEvent(new GameNewActionEvent(this, ea));
    }

    /**
     * @return an Enumeration of telemissile attacks.
     */
    public Enumeration<AttackAction> getTeleMissileAttacks() {
        return pendingTeleMissileAttacks.elements();
    }

    /**
     * Resets the pending rams list.
     */
    public void resetTeleMissileAttacks() {
        pendingTeleMissileAttacks.removeAllElements();
    }

    /**
     * This is used to send all telemissile attacks to the client.
     * @return an unmodifiable list of pending telemissile attacks.
     */
    public List<AttackAction> getTeleMissileAttacksVector() {
        return Collections.unmodifiableList(pendingTeleMissileAttacks);
    }

    /**
     * Adds a pending PSR to the list for this phase.
     */
    public void addPSR(PilotingRollData psr) {
        pilotRolls.addElement(psr);
    }

    /**
     * Returns an Enumeration of pending PSRs.
     */
    public Enumeration<PilotingRollData> getPSRs() {
        return pilotRolls.elements();
    }

    /**
     * Adds a pending extreme Gravity PSR to the list for this phase.
     */
    public void addExtremeGravityPSR(PilotingRollData psr) {
        extremeGravityRolls.addElement(psr);
    }

    /**
     * Returns an Enumeration of pending extreme GravityPSRs.
     */
    public Enumeration<PilotingRollData> getExtremeGravityPSRs() {
        return extremeGravityRolls.elements();
    }

    /**
     * Resets the PSR list for a given entity.
     */
    public void resetPSRs(Entity entity) {
        PilotingRollData roll;
        Vector<Integer> rollsToRemove = new Vector<>();
        int i = 0;

        // first, find all the rolls belonging to the target entity
        for (i = 0; i < pilotRolls.size(); i++) {
            roll = pilotRolls.elementAt(i);
            if (roll.getEntityId() == entity.getId()) {
                rollsToRemove.addElement(i);
            }
        }

        // now, clear them out
        for (i = rollsToRemove.size() - 1; i > -1; i--) {
            pilotRolls.removeElementAt(rollsToRemove.elementAt(i));
        }
    }

    /**
     * Resets the extreme Gravity PSR list.
     */
    public void resetExtremeGravityPSRs() {
        extremeGravityRolls.removeAllElements();
    }

    /**
     * Resets the extreme Gravity PSR list for a given entity.
     */
    public void resetExtremeGravityPSRs(Entity entity) {
        PilotingRollData roll;
        Vector<Integer> rollsToRemove = new Vector<>();
        int i = 0;

        // first, find all the rolls belonging to the target entity
        for (i = 0; i < extremeGravityRolls.size(); i++) {
            roll = extremeGravityRolls.elementAt(i);
            if (roll.getEntityId() == entity.getId()) {
                rollsToRemove.addElement(i);
            }
        }

        // now, clear them out
        for (i = rollsToRemove.size() - 1; i > -1; i--) {
            extremeGravityRolls.removeElementAt(rollsToRemove.elementAt(i));
        }
    }

    /**
     * Resets the PSR list.
     */
    public void resetPSRs() {
        pilotRolls.removeAllElements();
    }

    /**
     * add an AttackHandler to the attacks list
     *
     * @param ah - The <code>AttackHandler</code> to add
     */
    public void addAttack(AttackHandler ah) {
        attacks.add(ah);
    }

    /**
     * remove an AttackHandler from the attacks list
     *
     * @param ah - The <code>AttackHandler</code> to remove
     */
    public void removeAttack(AttackHandler ah) {
        attacks.removeElement(ah);
    }

    /**
     * get the attacks
     *
     * @return a <code>Enumeration</code> of all <code>AttackHandler</code>s
     */
    public Enumeration<AttackHandler> getAttacks() {
        return attacks.elements();
    }

    /**
     * get the attacks vector
     *
     * @return the <code>Vector</code> containing the attacks
     */
    public Vector<AttackHandler> getAttacksVector() {
        return attacks;
    }

    /**
     * reset the attacks vector
     */
    public void resetAttacks() {
        attacks = new Vector<>();
    }

    /**
     * set the attacks vector
     *
     * @param v - the <code>Vector</code> that should be the new attacks
     *          vector
     */
    public void setAttacksVector(Vector<AttackHandler> v) {
        attacks = v;
    }

    /**
     * Getter for property roundCount.
     *
     * @return Value of property roundCount.
     */
    public int getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(int roundCount) {
        this.roundCount = roundCount;
    }

    /**
     * Increments the round counter
     */
    public void incrementRoundCount() {
        roundCount++;
    }

    /**
     * Getter for property forceVictory. This tells us that there is an active claim for victory.
     *
     * @return Value of property forceVictory.
     */
    public boolean isForceVictory() {
        return forceVictory;
    }

    /**
     * Setter for property forceVictory.
     *
     * @param forceVictory New value of property forceVictory.
     */
    public void setForceVictory(boolean forceVictory) {
        this.forceVictory = forceVictory;
    }

    /**
     * Adds the given reports vector to the GameReport collection.
     * @param v the reports vector
     */
    public void addReports(Vector<Report> v) {
        if (v.isEmpty()) {
            return;
        }
        gameReports.add(roundCount, v);
    }

    /**
     * @param r Round number
     * @return a vector of reports for the given round.
     */
    public Vector<Report> getReports(int r) {
        return gameReports.get(r);
    }

    /**
     * @return a vector of all the reports.
     */
    public Vector<Vector<Report>> getAllReports() {
        return gameReports.get();
    }

    /**
     * Used to populate previous game reports, e.g. after a client connects to an existing game.
     */
    public void setAllReports(Vector<Vector<Report>> v) {
        gameReports.set(v);
    }

    /**
     * Clears out all the current reports, paving the way for a new game.
     */
    public void clearAllReports() {
        gameReports.clear();
    }

    public void end(int winner, int winnerTeam) {
        setVictoryPlayerId(winner);
        setVictoryTeam(winnerTeam);
        processGameEvent(new GameEndEvent(this));

    }

    /**
     * Getter for property victoryPlayerId.
     *
     * @return Value of property victoryPlayerId.
     */
    public int getVictoryPlayerId() {
        return victoryPlayerId;
    }

    /**
     * Setter for property victoryPlayerId.
     *
     * @param victoryPlayerId New value of property victoryPlayerId.
     */
    public void setVictoryPlayerId(int victoryPlayerId) {
        this.victoryPlayerId = victoryPlayerId;
    }

    /**
     * Getter for property victoryTeam.
     *
     * @return Value of property victoryTeam.
     */
    public int getVictoryTeam() {
        return victoryTeam;
    }

    /**
     * Setter for property victoryTeam.
     *
     * @param victoryTeam New value of property victoryTeam.
     */
    public void setVictoryTeam(int victoryTeam) {
        this.victoryTeam = victoryTeam;
    }

    /**
     * @return true if the specified player is either the victor, or is on the winning team. Best
     * to call during GamePhase.VICTORY.
     */
    public boolean isPlayerVictor(Player player) {
        if (player.getTeam() == Player.TEAM_NONE) {
            return player.getId() == victoryPlayerId;
        }
        return player.getTeam() == victoryTeam;
    }

    /**
     * @return the currently active context-object for VictoryCondition checking. This should be a
     * mutable object, and it will be modified by the victory condition checkers. Whoever saves the
     * game state when doing saves is also responsible for saving this state. At the start of the
     * game this should be initialized to an empty HashMap
     */
    public HashMap<String, Object> getVictoryContext() {
        return victoryContext;
    }

    public void setVictoryContext(HashMap<String, Object> ctx) {
        victoryContext = ctx;
    }

    /**
     * Get all <code>Entity</code>s that pass the given selection criteria.
     *
     * @param selector the <code>EntitySelector</code> that implements test that an
     *                 entity must pass to be included. This value may be
     *                 <code>null</code> (in which case all entities in the game will
     *                 be returned).
     * @return an <code>Enumeration</code> of all entities that the selector
     * accepts. This value will not be <code>null</code> but it may be
     * empty.
     */
    public Iterator<Entity> getSelectedEntities(@Nullable EntitySelector selector) {
        Iterator<Entity> retVal;

        // If no selector was supplied, return all entities.
        if (null == selector) {
            retVal = this.getEntities();
        }

        // Otherwise, return an anonymous Enumeration
        // that selects entities in this game.
        else {
            final EntitySelector entry = selector;
            retVal = new Iterator<>() {
                private EntitySelector entitySelector = entry;
                private Entity current = null;
                private Iterator<Entity> iter = getEntities();

                // Do any more entities meet the selection criteria?
                @Override
                public boolean hasNext() {
                    // See if we have a pre-approved entity.
                    if (null == current) {

                        // Find the first acceptable entity
                        while ((null == current) && iter.hasNext()) {
                            current = iter.next();
                            if (!entitySelector.accept(current)) {
                                current = null;
                            }
                        }
                    }
                    return (null != current);
                }

                // Get the next entity that meets the selection criteria.
                @Override
                public Entity next() {
                    // Pre-approve an entity.
                    if (!hasNext()) {
                        return null;
                    }

                    // Use the pre-approved entity, and null out our reference.
                    Entity next = current;
                    current = null;
                    return next;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };

        } // End use-selector

        // Return the selected entities.
        return retVal;

    }

    /**
     * Count all <code>Entity</code>s that pass the given selection criteria.
     *
     * @param selector the <code>EntitySelector</code> that implements test that an
     *                 entity must pass to be included. This value may be
     *                 <code>null</code> (in which case the count of all entities in
     *                 the game will be returned).
     * @return the <code>int</code> count of all entities that the selector
     * accepts. This value will not be <code>null</code> but it may be
     * empty.
     */
    public int getSelectedEntityCount(EntitySelector selector) {
        int retVal = 0;

        // If no selector was supplied, return the count of all game entities.
        if (null == selector) {
            retVal = getNoOfEntities();
        }

        // Otherwise, count the entities that meet the selection criteria.
        else {
            Iterator<Entity> iter = this.getEntities();
            while (iter.hasNext()) {
                if (selector.accept(iter.next())) {
                    retVal++;
                }
            }

        } // End use-selector

        // Return the number of selected entities.
        return retVal;
    }

    /**
     * Get all out-of-game <code>Entity</code>s that pass the given selection
     * criteria.
     *
     * @param selector the <code>EntitySelector</code> that implements test that an
     *                 entity must pass to be included. This value may be
     *                 <code>null</code> (in which case all entities in the game will
     *                 be returned).
     * @return an <code>Enumeration</code> of all entities that the selector
     * accepts. This value will not be <code>null</code> but it may be
     * empty.
     */
    public Enumeration<Entity> getSelectedOutOfGameEntities(
            EntitySelector selector) {
        Enumeration<Entity> retVal;

        // If no selector was supplied, return all entities.
        if (null == selector) {
            retVal = vOutOfGame.elements();
        }

        // Otherwise, return an anonymous Enumeration
        // that selects entities in this game.
        else {
            final EntitySelector entry = selector;
            retVal = new Enumeration<>() {
                private EntitySelector entitySelector = entry;
                private Entity current = null;
                private Enumeration<Entity> iter = vOutOfGame.elements();

                // Do any more entities meet the selection criteria?
                @Override
                public boolean hasMoreElements() {
                    // See if we have a pre-approved entity.
                    if (null == current) {

                        // Find the first acceptable entity
                        while ((null == current) && iter.hasMoreElements()) {
                            current = iter.nextElement();
                            if (!entitySelector.accept(current)) {
                                current = null;
                            }
                        }
                    }
                    return (null != current);
                }

                // Get the next entity that meets the selection criteria.
                @Override
                public Entity nextElement() {
                    // Pre-approve an entity.
                    if (!hasMoreElements()) {
                        return null;
                    }

                    // Use the pre-approved entity, and null out our reference.
                    Entity next = current;
                    current = null;
                    return next;
                }
            };

        } // End use-selector

        // Return the selected entities.
        return retVal;

    }

    /**
     * Count all out-of-game<code>Entity</code>s that pass the given selection
     * criteria.
     *
     * @param selector the <code>EntitySelector</code> that implements test that an
     *                 entity must pass to be included. This value may be
     *                 <code>null</code> (in which case the count of all out-of-game
     *                 entities will be returned).
     * @return the <code>int</code> count of all entities that the selector
     * accepts. This value will not be <code>null</code> but it may be
     * empty.
     */
    public int getSelectedOutOfGameEntityCount(EntitySelector selector) {
        int retVal = 0;

        // If no selector was supplied, return the count of all game entities.
        if (null == selector) {
            retVal = vOutOfGame.size();
        }

        // Otherwise, count the entities that meet the selection criteria.
        else {
            Enumeration<Entity> iter = vOutOfGame.elements();
            while (iter.hasMoreElements()) {
                if (selector.accept(iter.nextElement())) {
                    retVal++;
                }
            }

        } // End use-selector

        // Return the number of selected entities.
        return retVal;
    }

    /**
     * Returns true if the player has any valid units this turn that are not
     * infantry, not protomechs, or not either of those. This method is
     * utitilized by the "A players Infantry moves after that players other
     * units", and "A players Protomechs move after that players other units"
     * options.
     */
    public boolean checkForValidNonInfantryAndOrProtomechs(int playerId) {
        Iterator<Entity> iter = getPlayerEntities(getPlayer(playerId), false)
                .iterator();
        while (iter.hasNext()) {
            Entity entity = iter.next();
            boolean excluded = false;
            if ((entity instanceof Infantry)
                && getOptions().booleanOption(OptionsConstants.INIT_INF_MOVE_LATER)) {
                excluded = true;
            } else if ((entity instanceof Protomech)
                       && getOptions().booleanOption(OptionsConstants.INIT_PROTOS_MOVE_LATER)) {
                excluded = true;
            }

            if (!excluded && getTurn().isValidEntity(entity, this)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get Entities that have have a iNarc Nemesis pod attached and are situated between two Coords
     *
     * @param attacker The attacking <code>Entity</code>.
     * @param target The <code>Coords</code> of the original target.
     * @return an <code>Enumeration</code> of entities that have nemesis pods attached, are
     * located between attacker and target, and are friendly with the attacker.
     */
    public Enumeration<Entity> getNemesisTargets(Entity attacker, Coords target) {
        final Coords attackerPos = attacker.getPosition();
        final ArrayList<Coords> in = Coords.intervening(attackerPos, target);
        Vector<Entity> nemesisTargets = new Vector<>();
        for (Coords c : in) {
            for (Entity entity : getEntitiesVector(c)) {
                if (entity.isINarcedWith(INarcPod.NEMESIS)
                    && !entity.isEnemyOf(attacker)) {
                    nemesisTargets.addElement(entity);
                }
            }
        }
        return nemesisTargets.elements();
    }

    /**
     * Adds the specified game listener to receive board events from this board.
     *
     * @param listener the game listener.
     */
    public void addGameListener(GameListener listener) {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        gameListeners.addElement(listener);
    }

    /**
     * Removes the specified game listener.
     *
     * @param listener the game listener.
     */
    public void removeGameListener(GameListener listener) {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        gameListeners.removeElement(listener);
    }

    /**
     * Returns all the GameListeners.
     *
     * @return
     */
    public List<GameListener> getGameListeners() {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        return Collections.unmodifiableList(gameListeners);
    }

    /**
     * purges all Game Listener objects.
     */
    public void purgeGameListeners() {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        gameListeners.clear();
    }

    /**
     * Processes game events occurring on this connection by dispatching them to
     * any registered GameListener objects.
     *
     * @param event the game event.
     */
    public void processGameEvent(GameEvent event) {
        // Since gameListeners is transient, it could be null
        if (gameListeners == null) {
            gameListeners = new Vector<>();
        }
        for (Enumeration<GameListener> e = gameListeners.elements(); e.hasMoreElements(); ) {
            event.fireEvent(e.nextElement());
        }
    }

    /**
     * @return this turn's TAG information
     */
    public Vector<TagInfo> getTagInfo() {
        return tagInfoForTurn;
    }

    /**
     * add the results of one TAG attack
     */
    public void addTagInfo(TagInfo info) {
        tagInfoForTurn.addElement(info);
    }

    /**
     * Resets TAG information
     */
    public void resetTagInfo() {
        tagInfoForTurn.removeAllElements();
    }

    /**
     * @return the list of flares
     */
    public Vector<Flare> getFlares() {
        return flares;
    }

    /**
     * Set the list of flares
     */
    public void setFlares(Vector<Flare> flares) {
        this.flares = flares;
        processGameEvent(new GameBoardChangeEvent(this));
    }

    /**
     * Add a new flare
     */
    public void addFlare(Flare flare) {
        flares.addElement(flare);
        processGameEvent(new GameBoardChangeEvent(this));
    }

    /**
     * Get a set of Coords illuminated by searchlights.
     * 
     * Note: coords could be illuminated by other sources as well, it's likely
     * that IlluminationLevel::isPositionIlluminated is desired unless the searchlighted hex
     * set is being sent to the client or server.
     */
    public HashSet<Coords> getIlluminatedPositions() {
        return illuminatedPositions;
    }

    /**
     * Clear the set of searchlight illuminated hexes.
     */
    public void clearIlluminatedPositions() {
        if (illuminatedPositions == null) {
            return;
        }
        illuminatedPositions.clear();
    }

    /**
     * Setter for the list of Coords illuminated by search lights.
     */
    public void setIlluminatedPositions(final @Nullable HashSet<Coords> ip) throws RuntimeException {
        if (ip == null) {
            var ex = new RuntimeException("Illuminated Positions is null.");
            LogManager.getLogger().error("", ex);
            throw ex;
        }
        illuminatedPositions = ip;
        processGameEvent(new GameBoardChangeEvent(this));
    }

    /**
     * Add a new hex to the collection of Coords illuminated by searchlights.
     *
     * @return True if a new hex was added, else false if the set already
     * contained the input hex.
     */
    public boolean addIlluminatedPosition(Coords c) {
        boolean rv = illuminatedPositions.add(c);
        processGameEvent(new GameBoardChangeEvent(this));
        return rv;
    }

    /**
     * Age the flare list and remove any which have burnt out Artillery flares
     * drift with wind. (called at end of turn)
     */
    public Vector<Report> ageFlares() {
        Vector<Report> reports = new Vector<>();
        Report r;
        for (int i = flares.size() - 1; i >= 0; i--) {
            Flare flare = flares.elementAt(i);
            r = new Report(5235);
            r.add(flare.position.getBoardNum());
            r.newlines = 0;
            reports.addElement(r);
            if ((flare.flags & Flare.F_IGNITED) != 0) {
                flare.turnsToBurn--;
                if ((flare.flags & Flare.F_DRIFTING) != 0) {
                    int dir = planetaryConditions.getWindDirection();
                    int str = planetaryConditions.getWindStrength();

                    // strength 1 and 2: drift 1 hex
                    // strength 3: drift 2 hexes
                    // strength 4: drift 3 hexes
                    // for each above strength 4 (storm), drift one more
                    if (str > 0) {
                        flare.position = flare.position.translated(dir);
                        if (str > 2) {
                            flare.position = flare.position.translated(dir);
                        }
                        if (str > 3) {
                            flare.position = flare.position.translated(dir);
                        }
                        if (str > 4) {
                            flare.position = flare.position.translated(dir);
                        }
                        if (str > 5) {
                            flare.position = flare.position.translated(dir);
                        }
                        r = new Report(5236);
                        r.add(flare.position.getBoardNum());
                        r.newlines = 0;
                        reports.addElement(r);
                    }
                }
            } else {
                r = new Report(5237);
                r.newlines = 0;
                reports.addElement(r);
                flare.flags |= Flare.F_IGNITED;
            }
            if (flare.turnsToBurn <= 0) {
                r = new Report(5238);
                reports.addElement(r);
                flares.removeElementAt(i);
            } else {
                r = new Report(5239);
                r.add(flare.turnsToBurn);
                reports.addElement(r);
                flares.setElementAt(flare, i);
            }
        }
        processGameEvent(new GameBoardChangeEvent(this));
        return reports;
    }

    public boolean gameTimerIsExpired() {
        return ((getOptions().booleanOption(OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT)) && (getRoundCount() == getOptions()
                .intOption(OptionsConstants.VICTORY_GAME_TURN_LIMIT)));
    }

    /**
     * Uses VictoryFactory to generate a new VictoryCondition checker provided that the
     * VictoryContext is saved properly. Calling this method at any time is ok and should not affect
     * anything unless the VictoryCondition Config Options have changed.
     */
    public void createVictoryConditions() {
        victory = new Victory(getOptions());
    }

    @Deprecated
    public Victory getVictory() {
        return victory;
    }

    public VictoryResult getVictoryResult() {
        return victory.checkForVictory(this, getVictoryContext());
    }

    // a shortcut function for determining whether vectored movement is
    // applicable
    public boolean useVectorMove() {
        return getOptions().booleanOption(OptionsConstants.ADVAERORULES_ADVANCED_MOVEMENT)
               && board.inSpace();
    }

    /**
     * Adds a pending Control roll to the list for this phase.
     */
    public void addControlRoll(PilotingRollData control) {
        controlRolls.addElement(control);
    }

    /**
     * @return an Enumeration of pending Control rolls.
     */
    public Enumeration<PilotingRollData> getControlRolls() {
        return controlRolls.elements();
    }

    /**
     * Resets the Control Roll list for a given entity.
     */
    public void resetControlRolls(Entity entity) {
        PilotingRollData roll;
        Vector<Integer> rollsToRemove = new Vector<>();
        int i = 0;

        // first, find all the rolls belonging to the target entity
        for (i = 0; i < controlRolls.size(); i++) {
            roll = controlRolls.elementAt(i);
            if (roll.getEntityId() == entity.getId()) {
                rollsToRemove.addElement(i);
            }
        }

        // now, clear them out
        for (i = rollsToRemove.size() - 1; i > -1; i--) {
            controlRolls.removeElementAt(rollsToRemove.elementAt(i));
        }
    }

    /**
     * Resets the Control Roll list.
     */
    public void resetControlRolls() {
        controlRolls.removeAllElements();
    }

    /**
     * A set of checks for aero units to make sure that the movement order is
     * maintained
     */
    public boolean checkForValidSpaceStations(int playerId) {
        Iterator<Entity> iter = getPlayerEntities(getPlayer(playerId), false)
                .iterator();
        while (iter.hasNext()) {
            Entity entity = iter.next();
            if ((entity instanceof SpaceStation)
                && getTurn().isValidEntity(entity, this)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkForValidJumpships(int playerId) {
        Iterator<Entity> iter = getPlayerEntities(getPlayer(playerId), false)
                .iterator();
        while (iter.hasNext()) {
            Entity entity = iter.next();
            if ((entity instanceof Jumpship) && !(entity instanceof Warship)
                && getTurn().isValidEntity(entity, this)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkForValidWarships(int playerId) {
        Iterator<Entity> iter = getPlayerEntities(getPlayer(playerId), false)
                .iterator();
        while (iter.hasNext()) {
            Entity entity = iter.next();
            if ((entity instanceof Warship)
                && getTurn().isValidEntity(entity, this)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkForValidDropships(int playerId) {
        Iterator<Entity> iter = getPlayerEntities(getPlayer(playerId), false)
                .iterator();
        while (iter.hasNext()) {
            Entity entity = iter.next();
            if ((entity instanceof Dropship)
                && getTurn().isValidEntity(entity, this)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkForValidSmallCraft(int playerId) {
        return getPlayerEntities(getPlayer(playerId), false).stream().anyMatch(e ->
                (e instanceof SmallCraft) && getTurn().isValidEntity(e, this));
    }

    public PlanetaryConditions getPlanetaryConditions() {
        return planetaryConditions;
    }

    public void setPlanetaryConditions(final @Nullable PlanetaryConditions conditions) {
        if (conditions == null) {
            LogManager.getLogger().error("Can't set the planetary conditions to null!");
        } else {
            planetaryConditions.alterConditions(conditions);
            processGameEvent(new GameSettingsChangeEvent(this));
        }
    }

    public void addSmokeCloud(SmokeCloud cloud) {
        smokeCloudList.add(cloud);
    }

    public List<SmokeCloud> getSmokeCloudList() {
        return smokeCloudList;
    }
    
    public void removeSmokeClouds(List<SmokeCloud> cloudsToRemove) {
        for (SmokeCloud cloud : cloudsToRemove) {
            smokeCloudList.remove(cloud);
        }
    }

    /**
     * Updates the map that maps a position to the list of Entity's in that
     * position.
     *
     * @param e
     */
    public synchronized void updateEntityPositionLookup(Entity e,
            HashSet<Coords> oldPositions) {
        HashSet<Coords> newPositions = e.getOccupiedCoords();
        // Check to see that the position has actually changed
        if (newPositions.equals(oldPositions)) {
            return;
        }

        // Remove the old cached location(s)
        if (oldPositions != null) {
            for (Coords pos : oldPositions) {
                HashSet<Integer> posEntities = entityPosLookup.get(pos);
                if (posEntities != null) {
                    posEntities.remove(e.getId());
                }
            }
        }

        // Add Entity for each position
        for (Coords pos : newPositions) {
            HashSet<Integer> posEntities = entityPosLookup.get(pos);
            if (posEntities == null) {
                posEntities = new HashSet<>();
                posEntities.add(e.getId());
                entityPosLookup.put(pos, posEntities);
            } else {
                posEntities.add(e.getId());
            }
        }
    }

    private void removeEntityPositionLookup(Entity e) {
        // Remove Entity from cache
        for (Coords pos : e.getOccupiedCoords()) {
            HashSet<Integer> posEntities = entityPosLookup.get(pos);
            if (posEntities != null) {
                posEntities.remove(e.getId());
            }
        }
    }

    private void resetEntityPositionLookup() {
        entityPosLookup.clear();
        for (Entity e : entities) {
            updateEntityPositionLookup(e, null);
        }
    }

    private int countEntitiesInCache(List<Integer> entitiesInCache) {
        int count = 0;
        for (Coords c : entityPosLookup.keySet()) {
            count += entityPosLookup.get(c).size();
            entitiesInCache.addAll(entityPosLookup.get(c));
        }
        return count;
    }
    
    /**
     * A check to ensure that the position cache is properly updated. This
     * is only used for debugging purposes, and will cause a number of things
     * to slow down.
     */
    @SuppressWarnings(value = "unused")
    private void checkPositionCacheConsistency() {
        // Sanity check on the position cache
        //  This could be removed once we are confident the cache is working
        List<Integer> entitiesInCache = new ArrayList<>();
        List<Integer> entitiesInVector = new ArrayList<>();
        int entitiesInCacheCount = countEntitiesInCache(entitiesInCache);
        int entityVectorSize = 0;
        for (Entity e : entities) {
            if (e.getPosition() != null) {
                entityVectorSize++;
                entitiesInVector.add(e.getId());
            }
        }
        Collections.sort(entitiesInCache);
        Collections.sort(entitiesInVector);
        if ((entitiesInCacheCount != entityVectorSize) && !getPhase().isDeployment()
                && !getPhase().isExchange() && !getPhase().isLounge()
                && !getPhase().isInitiativeReport() && !getPhase().isInitiative()) {
            LogManager.getLogger().warn("Entities vector has " + entities.size()
                    + " but pos lookup cache has " + entitiesInCache.size() + "entities!");
            List<Integer> missingIds = new ArrayList<>();
            for (Integer id : entitiesInVector) {
                if (!entitiesInCache.contains(id)) {
                    missingIds.add(id);
                }
            }
            LogManager.getLogger().info("Missing ids: " + missingIds);
        }
        for (Entity e : entities) {
            HashSet<Coords> positions = e.getOccupiedCoords();
            for (Coords c : positions) {
                HashSet<Integer> ents = entityPosLookup.get(c);
                if ((ents != null) && !ents.contains(e.getId())) {
                    LogManager.getLogger().warn("Entity " + e.getId() + " is in "
                            + e.getPosition() + " however the position cache "
                            + "does not have it in that position!");
                }
            }
        }
        for (Coords c : entityPosLookup.keySet()) {
            for (Integer eId : entityPosLookup.get(c)) {
                Entity e = getEntity(eId);
                if (e == null) {
                    continue;
                }
                HashSet<Coords> positions = e.getOccupiedCoords();
                if (!positions.contains(c)) {
                    LogManager.getLogger().warn("Entity Position Cache thinks Entity " + eId
                            + "is in " + c + " but the Entity thinks it's in " + e.getPosition());
                }
            }
        }
    }

    /**
     * @return a string representation of this game's UUID.
     */
    public String getUUIDString() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return uuid.toString();

    }

    public synchronized Forces getForces() {
        return forces;
    }

    public Stream<Entity> getEntitiesStream() {
        return getEntitiesVector().stream();
    }

    /**
     * Overwrites the current forces object with the provided object.
     * Called from server messages when loading a game.
     */
    public synchronized void setForces(Forces fs) {
        forces = fs;
        forces.setGame(this);
    }
    
    public Map<String, BehaviorSettings> getBotSettings() {
        return botSettings;
    }
    
    public void setBotSettings(Map<String, BehaviorSettings> botSettings) {
        this.botSettings = botSettings;
    }

    /**
     * Cancels a victory
     */
    public void cancelVictory() {
        setForceVictory(false);
        setVictoryPlayerId(Player.PLAYER_NONE);
        setVictoryTeam(Player.TEAM_NONE);
    }
}
