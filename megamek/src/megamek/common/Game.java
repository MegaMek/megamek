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
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.event.*;
import megamek.common.options.GameOptions;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.planetaryconditions.Wind;
import megamek.common.planetaryconditions.WindDirection;
import megamek.common.weapons.AttackHandler;
import megamek.logging.MMLogger;
import megamek.server.SmokeCloud;
import megamek.server.props.OrbitalBombardment;
import megamek.server.victory.VictoryHelper;
import megamek.server.victory.VictoryResult;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.stream.Collectors.toList;

/**
 * The game class is the root of all data about the game in progress. Both the
 * Client and the Server should have one of these objects, and it is their job
 * to
 * keep it synched.
 */
public final class Game extends AbstractGame implements Serializable, PlanetaryConditionsUsing {
    private static final MMLogger logger = MMLogger.create(Game.class);

    private static final long serialVersionUID = 8376320092671792532L;

    /**
     * A UUID to identify this game instance.
     */
    public UUID uuid = UUID.randomUUID();

    /**
     * Stores the version of MM, so that it can be serialized in saved games.
     */
    public final Version version = MMConstants.VERSION;

    private IGameOptions options = new GameOptions();

    private MapSettings mapSettings = MapSettings.getInstance();

    /**
     * Track entities removed from the game (probably by death)
     */
    private Vector<Entity> vOutOfGame = new Vector<>();

    private final Map<Coords, HashSet<Integer>> entityPosLookup = new HashMap<>();

    /**
     * how's the weather?
     */
    private PlanetaryConditions planetaryConditions = new PlanetaryConditions();

    /**
     * The current turn list
     */
    private final Vector<GameTurn> turnVector = new Vector<>();

    /**
     * The present phase
     */
    private GamePhase phase = GamePhase.UNKNOWN;

    /**
     * The past phase
     */
    private GamePhase lastPhase = GamePhase.UNKNOWN;

    // phase state
    private Vector<AttackAction> pendingCharges = new Vector<>();
    private Vector<AttackAction> pendingRams = new Vector<>();
    private Vector<AttackAction> pendingTeleMissileAttacks = new Vector<>();
    private Vector<PilotingRollData> pilotRolls = new Vector<>();
    private Vector<PilotingRollData> extremeGravityRolls = new Vector<>();
    private Vector<PilotingRollData> controlRolls = new Vector<>();
    private Vector<Team> initiativeRerollRequests = new Vector<>();

    private final GameReports gameReports = new GameReports();

    private boolean forceVictory = false;
    private boolean endImmediately = false;
    private boolean ignorePlayerDefeatVotes = false;
    private int victoryPlayerId = Player.PLAYER_NONE;
    private int victoryTeam = Player.TEAM_NONE;

    private Hashtable<Coords, Vector<Minefield>> minefields = new Hashtable<>();
    private Vector<Minefield> vibrabombs = new Vector<>();
    private Vector<AttackHandler> attacks = new Vector<>();
    private Vector<ArtilleryAttackAction> offboardArtilleryAttacks = new Vector<>();
    private Vector<OrbitalBombardment> orbitalBombardmentAttacks = new Vector<OrbitalBombardment>();
    private int lastEntityId;

    private Vector<TagInfo> tagInfoForTurn = new Vector<>();
    private Vector<Flare> flares = new Vector<>();
    private HashSet<Coords> illuminatedPositions = new HashSet<>();

    private HashMap<String, Object> victoryContext = null;

    // internal integer value for an external game id link
    private int externalGameId = 0;

    // victory condition related stuff
    private VictoryHelper victoryHelper = null;

    // smoke clouds
    private List<SmokeCloud> smokeCloudList = new CopyOnWriteArrayList<>();

    /**
     * Stores princess behaviors for game factions. It does not indicate that a
     * faction is currently
     * played by a bot, only that the most recent bot connected as that faction used
     * these settings.
     * Used to add the settings to savegames and allow restoring bots to their
     * previous settings.
     */
    private Map<String, BehaviorSettings> botSettings = new HashMap<>();

    /**
     * Constructor
     */
    public Game() {
        setBoard(0, new Board());
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

    public void setBoard(Board board) {
        receiveBoard(0, board);
    }

    public void setBoardDirect(final Board board) {
        setBoard(0, board);
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
     *         minefields. This will not be <code>null</code>.
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
        getPlayersList().forEach(Player::removeMinefields);
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

    @Override
    public GameOptions getOptions() {
        return (GameOptions) options;
    }

    public void setOptions(final @Nullable GameOptions options) {
        if (options == null) {
            logger.error("Can't set the game options to null!");
        } else {
            this.options = options;
            processGameEvent(new GameSettingsChangeEvent(this));
        }
    }

    /**
     * Set up the teams vector. Each player on a team (Team 1 .. Team X) is
     * placed in the appropriate vector. Any player on 'No Team', is placed in
     * their own object
     */
    @Override
    public void setupTeams() {
        Vector<Team> initTeams = new Vector<>();
        boolean useTeamInit = getOptions().getOption(OptionsConstants.BASE_TEAM_INITIATIVE)
            .booleanValue();

        // Get all NO_TEAM players. If team_initiative is false, all
        // players are on their own teams for initiative purposes.
        for (Player player : getPlayersList()) {
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
                for (Player player : getPlayersList()) {
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
        if (!getPhase().isLounge()) {
            for (Team newTeam : initTeams) {
                for (Team oldTeam : teams) {
                    if (newTeam.equals(oldTeam)) {
                        newTeam.setInitiative(oldTeam.getInitiative());
                    }
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
    public void addPlayer(int id, Player player) {
        player.setGame(this);

        if ((player.isBot()) && (!player.getSingleBlind())) {
            boolean sbb = getOptions().booleanOption(OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS);
            boolean db = getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
            player.setSingleBlind(sbb && db);
        }

        players.put(id, player);
        setupTeams();
        updatePlayer(player);
    }

    @Override
    public boolean isCurrentPhasePlayable() {
        switch (phase) {
            case INITIATIVE:
            case END:
                return false;
            case DEPLOYMENT:
            case TARGETING:
            case PREMOVEMENT:
            case MOVEMENT:
            case PREFIRING:
            case FIRING:
            case PHYSICAL:
            case DEPLOY_MINEFIELDS:
            case SET_ARTILLERY_AUTOHIT_HEXES:
                return hasMoreTurns();
            case OFFBOARD:
                return hasMoreTurns() && isOffboardPlayable();
            default:
                return true;
        }
    }

    /**
     * Skip offboard phase, if there is no homing / semiguided ammo in play
     */
    private boolean isOffboardPlayable() {
        for (final Entity entity : getEntitiesVector()) {
            for (final AmmoMounted mounted : entity.getAmmo()) {
                AmmoType ammoType = mounted.getType();

                // per errata, TAG will spot for LRMs and such
                if ((ammoType.getAmmoType() == AmmoType.T_LRM)
                    || (ammoType.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (ammoType.getAmmoType() == AmmoType.T_MML)
                    || (ammoType.getAmmoType() == AmmoType.T_NLRM)
                    || (ammoType.getAmmoType() == AmmoType.T_MEK_MORTAR)) {
                    return true;
                }

                if (((ammoType.getAmmoType() == AmmoType.T_ARROW_IV)
                    || (ammoType.getAmmoType() == AmmoType.T_LONG_TOM)
                    || (ammoType.getAmmoType() == AmmoType.T_SNIPER)
                    || (ammoType.getAmmoType() == AmmoType.T_THUMPER))
                    && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_HOMING))) {
                    return true;
                }
            }

            if (entity.getBombs().stream().anyMatch(bomb -> !bomb.isDestroyed()
                && (bomb.getUsableShotsLeft() > 0)
                && (bomb.getType().getBombType() == BombType.B_LG))) {
                return true;
            }
        }

        // Go through all current attacks, checking if any use homing ammunition. If so,
        // the phase
        // is playable. This prevents issues from aerospace homing artillery with the
        // aerospace
        // unit having left the field already, for example
        return getAttacksVector().stream()
            .map(AttackHandler::getWaa)
            .filter(Objects::nonNull)
            .anyMatch(waa -> waa.getAmmoMunitionType().contains(AmmoType.Munitions.M_HOMING));
    }

    @Override
    public void setPlayer(int id, Player player) {
        player.setGame(this);
        players.put(id, player);
        setupTeams();
        updatePlayer(player);
    }

    @Override
    public void removePlayer(int id) {
        Player playerToRemove = getPlayer(id);
        players.remove(id);
        setupTeams();
        updatePlayer(playerToRemove);
    }

    private void updatePlayer(Player player) {
        processGameEvent(new GamePlayerChangeEvent(this, player));
    }

    /**
     * Returns the number of entities owned by the player, regardless of their
     * status.
     */
    public int getAllEntitiesOwnedBy(Player player) {
        int count = 0;
        for (Entity entity : inGameTWEntities()) {
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
        for (Entity entity : inGameTWEntities()) {
            if (entity.getOwner().equals(player) && !entity.isDestroyed()
                && !entity.isCarcass()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the number of non-destroyed entities owned by the player, including
     *         entities not yet
     *         deployed. Ignores offboard units and captured Mek pilots.
     */
    public int getLiveDeployedEntitiesOwnedBy(Player player) {
        int count = 0;
        for (Entity entity : inGameTWEntities()) {
            if (entity.getOwner().equals(player) && !entity.isDestroyed()
                && !entity.isCarcass()
                && !entity.isOffBoard() && !entity.isCaptured()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the number of non-destroyed commander entities owned by the player.
     *         Ignores offboard
     *         units and captured Mek pilots.
     */
    public int getLiveCommandersOwnedBy(Player player) {
        int count = 0;
        for (Entity entity : inGameTWEntities()) {
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
     * @return true if the player has a valid unit with the Tactical Genius pilot
     *         special ability.
     */
    public boolean hasTacticalGenius(Player player) {
        for (Entity entity : inGameTWEntities()) {
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

        for (Entity otherEntity : inGameTWEntities()) {
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
    @Override
    public @Nullable GameTurn getTurn() {
        synchronized (turnVector) {
            if ((turnIndex < 0) || (turnIndex >= turnVector.size())) {
                return null;
            }
            return turnVector.elementAt(turnIndex);
        }
    }

    @Override
    public boolean hasMoreTurns() {
        return super.hasMoreTurns();
    }

    /**
     * @return the first GameTurn object for the specified player, or null if the
     *         player has no
     *         turns to play
     */
    public @Nullable GameTurn getTurnForPlayer(int pn) {
        if ((turnIndex >= 0) && (turnIndex < turnVector.size())) {
            for (int i = turnIndex; i < turnVector.size(); i++) {
                GameTurn gt = turnVector.get(i);
                if (gt.isValid(pn, this)) {
                    return gt;
                }
            }
        }
        return null;
    }

    /**
     * Changes to the next turn, returning it.
     */
    public GameTurn changeToNextTurn() {
        synchronized (turnVector) {
            turnIndex++;
            return getTurn();
        }
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
        synchronized (turnVector) {
            if ((index + 1) >= turnVector.size()) {
                turnVector.add(turn);
            } else {
                turnVector.insertElementAt(turn, index + 1);
            }
        }
    }

    /**
     * Swaps the turn at index 1 with the turn at index 2.
     */
    public void swapTurnOrder(int index1, int index2) {
        synchronized (turnVector) {
            GameTurn turn1 = turnVector.get(index1);
            GameTurn turn2 = turnVector.get(index2);
            turnVector.set(index2, turn1);
            turnVector.set(index1, turn2);
        }
    }

    /**
     * Returns an Enumeration of the current turn list
     */
    public Enumeration<GameTurn> getTurns() {
        return turnVector.elements();
    }

    /**
     * Sets the current turn index
     *
     * @param turnIndex    The new turn index.
     * @param prevPlayerId The ID of the player who triggered the turn index change.
     */
    public void setTurnIndex(int turnIndex, int prevPlayerId) {
        // FIXME: occasionally getTurn() returns null. Handle that case
        // intelligently.
        this.turnIndex = turnIndex;
        processGameEvent(new GameTurnChangeEvent(this, getPlayer(getTurn().playerId()), prevPlayerId));
    }

    @Override
    public List<GameTurn> getTurnsList() {
        return Collections.unmodifiableList(turnVector);
    }

    /**
     * Sets the current turn vector
     */
    public void setTurnVector(List<GameTurn> turnVector) {
        synchronized (turnVector) {
            this.turnVector.clear();
            this.turnVector.addAll(turnVector);
        }
    }

    @Override
    public GamePhase getPhase() {
        return phase;
    }

    @Override
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
                clearActions();
                break;
            case INITIATIVE:
                clearActions();
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

    public void processGameEvent(GameEvent event) {
        fireGameEvent(event);
    }

    public GamePhase getLastPhase() {
        return lastPhase;
    }

    public void setLastPhase(GamePhase lastPhase) {
        this.lastPhase = lastPhase;
    }

    /**
     * @return an enumeration of all the entities in the game.
     * @deprecated Use {@link #inGameTWEntities()} instead.
     */
    @Deprecated
    public Iterator<Entity> getEntities() {
        return inGameTWEntities().iterator();
    }

    /**
     * @param current The <code>Entity</code> whose list position you wish to start
     *                from.
     * @return The previous <code>Entity</code> from the master list of entities.
     *         Will wrap around
     *         to the end of the list if necessary, returning null if there are no
     *         entities.
     */
    public @Nullable Entity getPreviousEntityFromList(final @Nullable Entity current) {
        if ((current != null) && inGameTWEntities().contains(current)) {
            int prev = inGameTWEntities().indexOf(current) - 1;
            if (prev < 0) {
                prev = inGameTWEntities().size() - 1; // wrap around to end
            }
            return inGameTWEntities().get(prev);
        }
        return null;
    }

    /**
     * @param current The <code>Entity</code> whose list position you wish to start
     *                from.
     * @return The next <code>Entity</code> from the master list of entities. Will
     *         wrap around to
     *         the beginning of the list if necessary, returning null if there are
     *         no entities.
     */
    public @Nullable Entity getNextEntityFromList(final @Nullable Entity current) {
        if ((current != null) && inGameTWEntities().contains(current)) {
            int next = inGameTWEntities().indexOf(current) + 1;
            if (next >= inGameTWEntities().size()) {
                next = 0; // wrap-around to beginning
            }
            return inGameTWEntities().get(next);
        }
        return null;
    }

    /**
     * @return the actual vector for the entities
     */
    public List<Entity> getEntitiesVector() {
        return Collections.unmodifiableList(inGameTWEntities());
    }

    public synchronized void setEntitiesVector(List<Entity> entities) {
        // checkPositionCacheConsistency();
        // this.entities.clear();
        // this.entities.addAll(entities);
        reindexEntities(entities);
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
     *         out-of-game entity has that ID, returns a <code>null</code>.
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
     *         <code>Entity</code>s that are in the same C3 network as the
     *         passed-in unit. This <code>Vector</code> may be empty, but it
     *         will not be <code>null</code>.
     * @see #getC3SubNetworkMembers(Entity)
     */
    public Vector<Entity> getC3NetworkMembers(Entity entity) {
        Vector<Entity> members = new Vector<>();
        // WOR
        // Does the unit have a C3 computer?
        if ((entity != null) && entity.hasAnyC3System()) {

            // Walk through the entities in the game, and add all
            // members of the C3 network to the output Vector.
            for (Entity unit : inGameTWEntities()) {
                if (entity.equals(unit) || entity.onSameC3NetworkAs(unit)) {
                    members.addElement(unit);
                }
            }
        }

        return members;
    }

    /**
     * Returns a <code>Vector</code> containing the <code>Entity</code>s that
     * are in the C3 subnetwork under the passed-in unit. The output will
     * contain the passed-in unit, if the unit has a C3 computer. If the unit
     * has no C3 computer, the output will be empty (but it will never be
     * <code>null</code>). If the passed-in unit is a company commander or a
     * member of a C3i network, this call is the same as
     * <code>getC3NetworkMembers</code>.
     *
     * @param entity - the <code>Entity</code> whose C3 network sub- members is
     *               required. This value may be <code>null</code>.
     * @return a <code>Vector</code> that will contain all other
     *         <code>Entity</code>s that are in the same C3 network under the
     *         passed-in unit. This <code>Vector</code> may be empty, but it
     *         will not be <code>null</code>.
     * @see #getC3NetworkMembers(Entity)
     */
    public Vector<Entity> getC3SubNetworkMembers(Entity entity) {
        // WOR
        // Handle null, C3i, NC3, and company commander units.
        if ((entity == null) || entity.hasC3i() || entity.hasNavalC3() || entity.hasActiveNovaCEWS()
            || entity.C3MasterIs(entity)) {
            return getC3NetworkMembers(entity);
        }

        Vector<Entity> members = new Vector<>();

        // Does the unit have a C3 computer?
        if (entity.hasC3()) {
            // Walk through the entities in the game, and add all
            // sub-members of the C3 network to the output Vector.
            for (Entity unit : inGameTWEntities()) {
                if (entity.equals(unit) || unit.C3MasterIs(entity)) {
                    members.addElement(unit);
                }
            }
        }

        return members;
    }

    /**
     * Returns a <code>Hashtable</code> that maps the <code>Coords</code> of
     * each unit in this <code>Game</code> to a <code>Vector</code> of
     * <code>Entity</code>s at that positions. Units that have no position (e.g.
     * loaded units) will not be in the map.
     *
     * @return a <code>Hashtable</code> that maps the <code>Coords</code>
     *         positions or each unit in the game to a <code>Vector</code> of
     *         <code>Entity</code>s at that position.
     */
    public Hashtable<Coords, Vector<Entity>> getPositionMap() {
        Hashtable<Coords, Vector<Entity>> positionMap = new Hashtable<>();
        Vector<Entity> atPos;

        // Walk through the entities in this game.
        for (Entity entity : inGameTWEntities()) {
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
     * Returns an enumeration of salvageable entities.
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

        for (Entity entity : inGameTWEntities()) {
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
        return inGameTWEntities().size();
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
                        return new BuildingTarget(BuildingTarget.idToCoords(nID), getBoard(), nType);
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
            logger.error("", e);
            return null;
        }
    }

    /** @return The entity with the given id number, if any. */
    public @Nullable Entity getEntity(final int id) {
        InGameObject possibleEntity = inGameObjects.get(id);
        return (possibleEntity instanceof Entity) ? (Entity) possibleEntity : null;
    }

    /**
     * When it has to exist, the entity HAS to exists. If it doesn't, throw a no such element exception.
     * This is to be used in place of the previous getEntity method when the entity is expected to exist and the following actions
     * will cause a null pointer exception if the entity does not exist.
     * @param id The id number of the entity to get.
     * @return The entity with the given id number or throw a no such element exception.
     */
    public Entity getEntityOrThrow(final int id) {
        return (Entity) getInGameObject(id).orElseThrow();
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
     * Adds a new Entity. The id parameter is ignored and addEntity(Entity) is
     * called instead. This
     * is just to maintain compatibility.
     *
     * @param id     Value that is ignored: the id is pulled from the passed Entity
     * @param entity The Entity to add to the game.
     */
    @Deprecated
    public void addEntity(int id, Entity entity) {
        // Disregard the passed id, addEntity(Entity) pulls the id from the
        // Entity instance.
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
     * @param entity   The Entity to add.
     * @param genEvent A flag that determines whether a GameEntityNewEvent is
     *                 generated.
     */
    public synchronized void addEntity(Entity entity, boolean genEvent) {
        entity.setGame(this);
        if (entity instanceof Mek) {
            ((Mek) entity).setBAGrabBars();
            ((Mek) entity).setProtoMekClampMounts();
        } else if (entity instanceof Tank) {
            ((Tank) entity).setBAGrabBars();
            ((Tank) entity).setTrailerHitches();
        }

        // Add magnetic clamp mounts
        if ((entity instanceof Mek) && !entity.isOmni() && !entity.hasBattleArmorHandles()) {
            entity.addTransporter(new ClampMountMek());
        } else if ((entity instanceof Tank) && !entity.isOmni()
            && !entity.hasBattleArmorHandles()) {
            entity.addTransporter(new ClampMountTank());
        }

        entity.setGameOptions();
        if (entity.getC3UUIDAsString() == null) {
            // We don't want to be resetting a UUID that exists already!
            entity.setC3UUID();
        }
        // Add this Entity, ensuring that its id is unique
        int id = entity.getId();
        if (isIdUsed(id)) {
            id = getNextEntityId();
            entity.setId(id);
        }
        inGameObjects.put(id, entity);
        updateEntityPositionLookup(entity, null);

        if (id > lastEntityId) {
            lastEntityId = id;
        }

        // And... lets get this straight now.
        if (entity instanceof Mek mek) {
            if (getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)) {
                mek.setAutoEject(true);
                mek.setCondEjectAmmo(!entity.hasCase() && !entity.hasCASEII());
                mek.setCondEjectEngine(true);
                mek.setCondEjectCTDest(true);
                mek.setCondEjectHeadshot(true);
            } else {
                mek.setAutoEject(!entity.hasCase() && !entity.hasCASEII());
            }
        }

        if (genEvent) {
            entity.setInitialBV(entity.calculateBattleValue(false, false));
            processGameEvent(new GameEntityNewEvent(this, entity));
        }
    }

    /**
     * @return true if the given ID is in use among active and dead units
     */
    private boolean isIdUsed(int id) {
        return inGameObjects.containsKey(id) || isOutOfGame(id);
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
            inGameObjects.put(id, entity);
            // Get the collection of positions
            HashSet<Coords> oldPositions = oldEntity.getOccupiedCoords();
            // Update position lookup table
            updateEntityPositionLookup(entity, oldPositions);

            // Not sure if this really required
            if (id > lastEntityId) {
                lastEntityId = id;
            }

            processGameEvent(new GameEntityChangeEvent(this, entity, movePath, oldEntity));
        }
    }

    @Override
    public int getNextEntityId() {
        return lastEntityId + 1;
    }

    @Override
    public void replaceUnits(List<InGameObject> units) {
        addEntities(filterToEntity(units));
    }

    @Override
    public List<InGameObject> getGraveyard() {
        return new ArrayList<>(getOutOfGameEntitiesVector());
    }

    /**
     * @return <code>true</code> if an entity with the specified id number exists in
     *         this game.
     */
    public boolean hasEntity(int entityId) {
        Optional<InGameObject> possibleEntity = getInGameObject(entityId);
        return possibleEntity.isPresent() && possibleEntity.get() instanceof Entity;
    }

    /**
     * Remove an entity from the master list. If we can't find that entity,
     * (probably due to double-blind) ignore it.
     */
    public synchronized void removeEntity(int id, int condition) {
        Entity toRemove = getEntity(id);
        if (toRemove == null) {
            return;
        }

        inGameObjects.remove(id);
        removeEntityPositionLookup(toRemove);

        toRemove.setRemovalCondition(condition);

        // do not keep never-joined entities
        if ((vOutOfGame != null)
            && (condition != IEntityRemovalConditions.REMOVE_NEVER_JOINED)) {
            vOutOfGame.addElement(toRemove);
        }

        // We also need to remove it from the list of things to be deployed...
        // we might still be in this list if we never joined the game
        setupDeployment();
        processGameEvent(new GameEntityRemoveEvent(this, toRemove));
    }

    public void removeEntities(List<Integer> ids, int condition) {
        for (int i = 0; i < ids.size(); i++) {
            removeEntity(ids.get(i), condition);
        }
    }

    @Override
    public synchronized void reset() {
        super.reset();
        uuid = UUID.randomUUID();

        entityPosLookup.clear();
        vOutOfGame.removeAllElements();
        turnVector.clear();

        clearActions();
        resetCharges();
        resetRams();
        resetPSRs();
        resetArtilleryAttacks();
        resetAttacks();
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
        for (Player player : getPlayersList()) {
            player.removeArtyAutoHitHexes();
        }
    }

    /**
     * Regenerates the entities by id hashtable by going thru all entities in
     * the Vector
     */
    private void reindexEntities(List<Entity> entities) {
        inGameObjects.clear();
        lastEntityId = 0;

        // Add these entities to the game.
        for (Entity entity : entities) {
            final int id = entity.getId();
            inGameObjects.put(id, entity);

            if (id > lastEntityId) {
                lastEntityId = id;
            }
        }
        // We need to ensure that each entity has the proper Game reference
        // however, the entityIds Hashmap must be fully formed before this
        // is called, since setGame also calls setGame for loaded Entities
        for (Entity entity : inGameTWEntities()) {
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
        for (Entity entity : inGameTWEntities()) {
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
     * @param c             the coordinates to search at
     * @param currentEntity the entity that is firing
     */
    public Entity getFirstEnemyEntity(Coords c, Entity currentEntity) {
        for (Entity entity : inGameTWEntities()) {
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
     * Return an {@link Entity} <code>List</code> at {@link Coords} <code>c</code>,
     * checking if
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
     * @param c      The coordinates to check
     * @param ignore Flag that determines whether the ability to target is ignored
     * @return the {@link Entity} <code>List</code>
     */
    public synchronized List<Entity> getEntitiesVector(Coords c, boolean ignore) {
        // checkPositionCacheConsistency();
        // Make sure the look-up is initialized
        if (entityPosLookup.isEmpty() && !inGameTWEntities().isEmpty()) {
            resetEntityPositionLookup();
        }
        // For sanity check
        GamePhase phase = getPhase();

        Set<Integer> posEntities = entityPosLookup.get(c);
        List<Entity> vector = new ArrayList<>();
        if (posEntities != null) {
            for (Integer eId : posEntities) {
                Entity e = getEntity(eId);

                // if the entity with the given ID doesn't exist, we will update the lookup
                // table
                // and move on
                if (e == null) {
                    posEntities.remove(eId);
                    continue;
                }

                if (e.isTargetable() || ignore) {
                    vector.add(e);

                    // Sanity check: report out-of-place entities if it's not the deployment phase
                    HashSet<Coords> positions = e.getOccupiedCoords();
                    if (!phase.isDeployment() && !positions.contains(c)) {
                        logger.error(e.getDisplayName() + " is not in " + c + "!");
                    }
                }
            }
        }
        return Collections.unmodifiableList(vector);
    }

    /**
     * Convenience function that gets a list of all off-board enemy entities.
     *
     * @param player
     * @return
     */
    public synchronized List<Entity> getAllOffboardEnemyEntities(Player player) {
        List<Entity> vector = new ArrayList<>();
        for (Entity e : inGameTWEntities()) {
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
        if (getBoard().contains(c)) {
            for (Entity entity : getEntitiesVector(c, true)) {
                if (entity.hasETypeFlag(Entity.ETYPE_GUN_EMPLACEMENT)) {
                    vector.addElement((GunEmplacement) entity);
                }
            }
        }

        return vector;
    }

    /**
     * Determine if the given set of coordinates has a gun emplacement on the roof
     * of a building.
     *
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
     * @param coords      The <code>Coords</code> of the hex in which the accidental fall
     *               from above happens
     * @param ignore The entity who is falling, so shouldn't be returned
     * @return The <code>Entity</code> that should be an AFFA target.
     */
    public @Nullable Entity getAffaTarget(Coords coords, Entity ignore) {
        Vector<Entity> vector = new Vector<>();
        if (getBoard().contains(coords)) {
            Hex hex = getBoard().getHex(coords);
            for (Entity entity : getEntitiesVector(coords)) {
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
     * Returns an <code>Iterator</code> of the enemy's active entities at the given
     * coordinates.
     *
     * @param coords        the <code>Coords</code> of the hex being examined.
     * @param currentEntity the <code>Entity</code> whose enemies are needed.
     * @return an <code>Enumeration</code> of <code>Entity</code>s at the given
     *         coordinates who are
     *         enemies of the given unit.
     */
    public Iterator<Entity> getEnemyEntities(final Coords coords, final Entity currentEntity) {
        return getSelectedEntities(entity -> coords.equals(entity.getPosition())
            && entity.isTargetable() && entity.isEnemyOf(currentEntity));
    }

    /**
     * Returns an <code>Enumeration</code> of active enemy entities
     *
     * @param currentEntity the <code>Entity</code> whose enemies are needed.
     * @return an <code>Enumeration</code> of <code>Entity</code>s at the given
     *         coordinates who are
     *         enemies of the given unit.
     */
    public Iterator<Entity> getAllEnemyEntities(final Entity currentEntity) {
        return getSelectedEntities(entity -> entity.isTargetable() && entity.isEnemyOf(currentEntity));
    }

    public Iterator<Entity> getTeamEntities(final Team team) {
        return getSelectedEntities(entity -> team.players().contains(entity.getOwner()));
    }

    /**
     * Returns an <code>Iterator</code> of friendly active entities at the given
     * coordinates.
     *
     * @param coords        the <code>Coords</code> of the hex being examined.
     * @param currentEntity the <code>Entity</code> whose friends are needed.
     * @return an <code>Enumeration</code> of <code>Entity</code>s at the given
     *         coordinates who are
     *         friends of the given unit.
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
     *         <code>false</code> otherwise.
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
     *         <code>false</code> otherwise.
     */
    public boolean isOutOfGame(Entity entity) {
        return isOutOfGame(entity.getId());
    }

    /**
     * @return the first entity that can act in the present turn, or null if none
     *         can.
     */
    public @Nullable Entity getFirstEntity() {
        return getFirstEntity(getTurn());
    }

    /**
     * @param turn the current game turn, which may be null
     * @return the first entity that can act in the specified turn, or null if none
     *         can.
     */
    public @Nullable Entity getFirstEntity(final @Nullable GameTurn turn) {
        return getEntity(getFirstEntityNum(turn));
    }

    /**
     * @return the id of the first entity that can act in the current turn, or -1 if
     *         none can.
     */
    public int getFirstEntityNum() {
        return getFirstEntityNum(getTurn());
    }

    /**
     * @param turn the current game turn, which may be null
     * @return the id of the first entity that can act in the specified turn, or -1
     *         if none can.
     */
    public int getFirstEntityNum(final @Nullable GameTurn turn) {
        if (turn == null) {
            return -1;
        }

        for (Entity entity : inGameTWEntities()) {
            if (turn.isValidEntity(entity, this)) {
                return entity.getId();
            }
        }

        return -1;
    }

    /**
     * @param start the index number to start at (not an Entity Id)
     * @return the next selectable entity that can act this turn, or null if none
     *         can.
     */
    public @Nullable Entity getNextEntity(int start) {
        if (inGameTWEntities().isEmpty()) {
            return null;
        }
        start = start % inGameTWEntities().size();
        int entityId = inGameTWEntities().get(start).getId();
        return getEntity(getNextEntityNum(getTurn(), entityId));
    }

    /**
     * @param turn  the turn to use, which may be null
     * @param start the entity id to start at
     * @return the entity id of the next entity that can move during the specified
     *         turn
     */
    public int getNextEntityNum(final @Nullable GameTurn turn, int start) {
        List<Entity> sortedEntities = inGameTWEntities();
        sortedEntities.sort(Comparator.comparingInt(Entity::getId));
        // If we don't have a turn, return ENTITY_NONE
        if (turn == null) {
            return Entity.NONE;
        }
        boolean hasLooped = false;
        int i = (sortedEntities.indexOf(getEntity(start)) + 1) % sortedEntities.size();
        if (i == -1) {
            // This means we were given an invalid entity ID, punt
            return Entity.NONE;
        }
        int startingIndex = i;
        while (!((hasLooped == true) && (i == startingIndex))) {
            final Entity entity = sortedEntities.get(i);
            if (turn.isValidEntity(entity, this)) {
                return entity.getId();
            }
            i++;
            if (i == sortedEntities.size()) {
                i = 0;
                hasLooped = true;
            }
        }
        // return getFirstEntityNum(turn);
        return Entity.NONE;
    }

    /**
     * @param turn  the turn to use
     * @param start the entity id to start at
     * @return the entity id of the previous entity that can move during the
     *         specified turn
     */
    public int getPrevEntityNum(GameTurn turn, int start) {
        List<Entity> sortedEntities = inGameTWEntities();
        sortedEntities.sort(Comparator.comparingInt(Entity::getId));
        boolean hasLooped = false;
        int i = (sortedEntities.indexOf(getEntity(start)) - 1) % sortedEntities.size();
        if (i == -2) {
            // This means we were given an invalid entity ID, punt
            return -1;
        }
        if (i == -1) {
            // This means we were given an invalid entity ID, punt
            i = sortedEntities.size() - 1;
        }
        int startingIndex = i;
        while (!((hasLooped == true) && (i == startingIndex))) {
            final Entity entity = sortedEntities.get(i);
            if (turn.isValidEntity(entity, this)) {
                return entity.getId();
            }
            i--;
            if (i < 0) {
                i = sortedEntities.size() - 1;
                hasLooped = true;
            }
        }
        // return getFirstEntityNum(turn);
        return -1;
    }

    /**
     * @param turn the current game turn, which may be null
     * @return the number of the first deployable entity that is valid for the
     *         specified turn
     */
    public int getFirstDeployableEntityNum(final @Nullable GameTurn turn) {
        // Repeat the logic from getFirstEntityNum.
        if (turn == null) {
            return -1;
        }
        for (Entity entity : inGameTWEntities()) {
            if (turn.isValidEntity(entity, this) && entity.shouldDeploy(getRoundCount())) {
                return entity.getId();
            }
        }
        return -1;
    }

    /**
     * @return the number of the next deployable entity that is valid for the
     *         specified turn
     */
    public int getNextDeployableEntityNum(GameTurn turn, int start) {
        if (start >= 0) {
            for (int i = start; i < inGameTWEntities().size(); i++) {
                final Entity entity = inGameTWEntities().get(i);
                if (turn.isValidEntity(entity, this) && entity.shouldDeploy(getRoundCount())) {
                    return entity.getId();
                }
            }
        }
        return getFirstDeployableEntityNum(turn);
    }

    /**
     * @param turn the current game turn, which may be null
     * @return the number of the first hidden entity that is valid for the specified
     *         turn
     */
    public int getFirstHiddenEntityNum(final @Nullable GameTurn turn) {
        // Reviewers: Not sure if this is where to add filtering (this is hoe deployment
        // does it)
        // or if the right way is to create a subclass of GameTurn.EntityClassTurn that
        // overrides isValidEntity
        // the latter seems more correct, but I see no other examples of that

        // Repeat the logic from getFirstEntityNum.
        if (turn == null) {
            return -1;
        }
        for (Entity entity : inGameTWEntities()) {
            if ((!entity.isDone()) && turn.isValidEntity(entity, this) && entity.isHidden()) {
                return entity.getId();
            }
        }
        return -1;
    }

    /**
     * @return the number of the next hidden entity that is valid for the specified
     *         turn
     */
    public int getNextHiddenEntityNum(GameTurn turn, int start) {
        if (start >= 0) {
            for (int i = start; i < inGameTWEntities().size(); i++) {
                final Entity entity = inGameTWEntities().get(i);
                if ((!entity.isDone()) && turn.isValidEntity(entity, this) && entity.isHidden()) {
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
        for (Entity entity : inGameTWEntities()) {
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
     * @param hide   - should fighters loaded into squadrons be excluded from this
     *               list?
     * @return a <code>Vector</code> of <code>Entity</code>s.
     */
    public ArrayList<Integer> getPlayerEntityIds(Player player, boolean hide) {
        ArrayList<Integer> output = new ArrayList<>();
        for (Entity entity : inGameTWEntities()) {
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
     * Get the entities for the player.
     *
     * @param player - the <code>Player</code> whose entities are required.
     * @return a <code>Vector</code> of <code>Entity that have retreaded</code>s.
     */
    public ArrayList<Entity> getPlayerRetreatedEntities(Player player) {
        ArrayList<Entity> output = new ArrayList<>();
        for (Entity entity : vOutOfGame) {
            if (player.equals(entity.getOwner()) &&
                ((entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_IN_RETREAT)
                    || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_CAPTURED)
                    || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_PUSHED))) {
                output.add(entity);
            }
        }
        return output;
    }

    /**
     * Determines if the indicated entity is stranded on a transport that can't
     * move.
     * <p>
     * According to
     * <a href=
     * "http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=555466&page=2&view=collapsed&sb=5&o=0&fpart=">Randall
     * Bills</a>,
     * the "minimum move" rule allow stranded units to dismount at the start of the
     * turn.
     *
     * @param entity the <code>Entity</code> that may be stranded
     * @return <code>true</code> if the entity is stranded <code>false</code>
     *         otherwise.
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
     * @return number of infantry <code>playerId</code> has not selected yet this
     *         turn
     */
    public int getInfantryLeft(int playerId) {
        Player player = getPlayer(playerId);
        int remaining = 0;

        for (Entity entity : inGameTWEntities()) {
            if (player.equals(entity.getOwner()) && entity.isSelectableThisTurn()
                && (entity instanceof Infantry)) {
                remaining++;
            }
        }

        return remaining;
    }

    /**
     * @param playerId the player's Id
     * @return number of ProtoMeks <code>playerId</code> has not selected yet this
     *         turn
     */
    public int getProtoMeksLeft(int playerId) {
        Player player = getPlayer(playerId);
        int remaining = 0;

        for (Entity entity : inGameTWEntities()) {
            if (player.equals(entity.getOwner()) && entity.isSelectableThisTurn()
                && (entity instanceof ProtoMek)) {
                remaining++;
            }
        }

        return remaining;
    }

    /**
     * @param playerId the player's Id
     * @return number of vehicles <code>playerId</code> has not selected yet this
     *         turn
     */
    public int getVehiclesLeft(int playerId) {
        Player player = getPlayer(playerId);
        int remaining = 0;

        for (Entity entity : inGameTWEntities()) {
            if (player.equals(entity.getOwner()) && entity.isSelectableThisTurn()
                && (entity instanceof Tank)) {
                remaining++;
            }
        }

        return remaining;
    }

    /**
     * @param playerId the player's Id
     * @return number of 'Meks <code>playerId</code> has not selected yet this turn
     */
    public int getMeksLeft(int playerId) {
        Player player = getPlayer(playerId);
        int remaining = 0;

        for (Entity entity : inGameTWEntities()) {
            if (player.equals(entity.getOwner()) && entity.isSelectableThisTurn()
                && (entity instanceof Mek)) {
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

        synchronized (turnVector) {
            for (int i = turnIndex; i < turnVector.size(); i++) {
                GameTurn turn = turnVector.elementAt(i);
                if (turn.isValidEntity(entity, this)) {
                    turnVector.removeElementAt(i);
                    return turn;
                }
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
        // If the game option "move multiple infantry per mek" is selected,
        // then we might not need to remove a turn at all.
        // A turn only needs to be removed when going from 4 inf (2 turns) to
        // 3 inf (1 turn)
        if (getOptions().booleanOption(OptionsConstants.INIT_INF_MOVE_MULTI)
            && (entity instanceof Infantry) && getPhase().isMovement()) {
            if ((getInfantryLeft(entity.getOwnerId()) % getOptions().intOption(
                OptionsConstants.INIT_INF_PROTO_MOVE_MULTI)) != 1) {
                // exception, if the _next_ turn is an infantry turn, remove that
                // contrived, but may come up e.g. one inf accidentally kills another
                synchronized (turnVector) {
                    if (hasMoreTurns()) {
                        GameTurn nextTurn = turnVector.elementAt(turnIndex + 1);
                        if (nextTurn instanceof EntityClassTurn) {
                            EntityClassTurn ect = (EntityClassTurn) nextTurn;
                            if (ect.isValidClass(EntityClassTurn.CLASS_INFANTRY)
                                && !ect.isValidClass(~EntityClassTurn.CLASS_INFANTRY)) {
                                turnVector.removeElementAt(turnIndex + 1);
                            }
                        }
                    }
                }
                return;
            }
        }
        // Same thing but for ProtoMeks
        if (getOptions().booleanOption(OptionsConstants.INIT_PROTOS_MOVE_MULTI)
            && (entity instanceof ProtoMek) && getPhase().isMovement()) {
            if ((getProtoMeksLeft(entity.getOwnerId()) % getOptions()
                .intOption(OptionsConstants.INIT_INF_PROTO_MOVE_MULTI)) != 1) {
                // exception, if the _next_ turn is an ProtoMek turn, remove that
                // contrived, but may come up e.g. one inf accidentally kills another
                synchronized (turnVector) {
                    if (hasMoreTurns()) {
                        GameTurn nextTurn = turnVector.elementAt(turnIndex + 1);
                        if (nextTurn instanceof EntityClassTurn) {
                            EntityClassTurn ect = (EntityClassTurn) nextTurn;
                            if (ect.isValidClass(EntityClassTurn.CLASS_PROTOMEK)
                                && !ect.isValidClass(~EntityClassTurn.CLASS_PROTOMEK)) {
                                turnVector.removeElementAt(turnIndex + 1);
                            }
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
                synchronized (turnVector) {
                    if (hasMoreTurns()) {
                        GameTurn nextTurn = turnVector.elementAt(turnIndex + 1);
                        if (nextTurn instanceof EntityClassTurn) {
                            EntityClassTurn ect = (EntityClassTurn) nextTurn;
                            if (ect.isValidClass(EntityClassTurn.CLASS_TANK)
                                && !ect.isValidClass(~EntityClassTurn.CLASS_TANK)) {
                                turnVector.removeElementAt(turnIndex + 1);
                            }
                        }
                    }
                }
                return;
            }
        }

        // Same thing but for meks
        if (getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT)
            && (entity instanceof Mek) && getPhase().isMovement()) {
            if ((getMeksLeft(entity.getOwnerId()) % getOptions()
                .intOption(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT_NUMBER)) != 1) {
                // exception, if the _next_ turn is a mek turn, remove that
                // contrived, but may come up e.g. one mek accidentally kills another
                synchronized (turnVector) {
                    if (hasMoreTurns()) {
                        GameTurn nextTurn = turnVector.elementAt(turnIndex + 1);
                        if (nextTurn instanceof EntityClassTurn) {
                            EntityClassTurn ect = (EntityClassTurn) nextTurn;
                            if (ect.isValidClass(EntityClassTurn.CLASS_MEK)
                                && !ect.isValidClass(~EntityClassTurn.CLASS_MEK)) {
                                turnVector.removeElementAt(turnIndex + 1);
                            }
                        }
                    }
                }
                return;
            }
        }

        boolean useInfantryMoveLaterCheck = true;
        // If we have the "infantry move later" or "ProtoMeks move later" optional
        // rules, then we may be removing an infantry unit that would be
        // considered invalid unless we don't consider the extra validity
        // checks.
        if ((getOptions().booleanOption(OptionsConstants.INIT_INF_MOVE_LATER) &&
            (entity instanceof Infantry)) ||
            (getOptions().booleanOption(OptionsConstants.INIT_PROTOS_MOVE_LATER) &&
                (entity instanceof ProtoMek))) {
            useInfantryMoveLaterCheck = false;
        }

        synchronized (turnVector) {
            for (int i = turnVector.size() - 1; i >= turnIndex; i--) {
                GameTurn turn = turnVector.elementAt(i);

                if (turn.isValidEntity(entity, this, useInfantryMoveLaterCheck)) {
                    turnVector.removeElementAt(i);
                    break;
                }
            }
        }
    }

    /**
     * Removes any turns that can only be taken by the specified entity. Useful if
     * the specified
     * Entity is being removed from the game to ensure any turns that only it can
     * take are gone.
     *
     * @param entity the entity to remove turns for
     * @return The number of turns returned
     */
    public int removeSpecificEntityTurnsFor(Entity entity) {
        List<GameTurn> turnsToRemove = new ArrayList<>();

        synchronized (turnVector) {
            for (GameTurn turn : turnVector) {
                if (turn instanceof SpecificEntityTurn) {
                    int turnOwner = ((SpecificEntityTurn) turn).getEntityNum();
                    if (entity.getId() == turnOwner) {
                        turnsToRemove.add(turn);
                    }
                }
            }
        }
        turnVector.removeAll(turnsToRemove);
        return turnsToRemove.size();
    }

    /**
     * Set the new vector of orbital bombardments for this round.
     * @param orbitalBombardments
     */
    public void setOrbitalBombardmentVector(Vector<OrbitalBombardment> orbitalBombardments) {
        orbitalBombardmentAttacks = orbitalBombardments;
        processGameEvent(new GameBoardChangeEvent(this));
    }

    /**
     * Resets the orbital bombardment attacks list.
     */
    public void resetOrbitalBombardmentAttacks() {
        orbitalBombardmentAttacks.removeAllElements();
    }

    /**
     * @return an Enumeration of orbital bombardment attacks.
     */
    public Enumeration<OrbitalBombardment> getOrbitalBombardmentAttacks() {
        return orbitalBombardmentAttacks.elements();
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
        return Collections.enumeration(pendingActions);
    }

    public void addInitiativeRerollRequest(Team t) {
        initiativeRerollRequests.addElement(t);
    }

    public void rollInitAndResolveTies() {
        if (getOptions().booleanOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            Vector<TurnOrdered> vRerolls = new Vector<>();
            for (int i = 0; i < inGameTWEntities().size(); i++) {
                Entity e = inGameTWEntities().get(i);
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
            TurnOrdered.resetInitiativeCompensation(teams,
                getOptions().booleanOption(OptionsConstants.INIT_INITIATIVE_STREAK_COMPENSATION));
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
     *
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
        return getCurrentRound();
    }

    public void setRoundCount(int roundCount) {
        setCurrentRound(roundCount);
    }

    /**
     * Getter for property forceVictory. This tells us that there is an active claim
     * for victory.
     *
     * @return Value of property forceVictory.
     */
    @Override
    public boolean isForceVictory() {
        return forceVictory;
    }


    public boolean isIgnorePlayerDefeatVotes() {
        return ignorePlayerDefeatVotes;
    }

    public boolean isEndImmediately() {
        return endImmediately;
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
     * Setter for property endImmediately.
     * The endImmediately flag is used to signal that the game should check for victory conditions
     * as soon as possible, instead of waiting for the end phase. This does bypass the server option to not
     * end the game immediately, so it should be used with caution.
     *
     * @param endImmediately New value of property endImmediately.
     */
    public void setEndImmediately(boolean endImmediately) {
        this.endImmediately = endImmediately;
    }

    /**
     * Setter for property ignorePlayerDefeatVotes. This flag is used to signal that the game should ignore the need
     * for players voting for the end of the game. This is used to give the gamemaster the ability to end the game
     * without player input.
     *
     * @param ignorePlayerDefeatVotes New value of property ignorePlayerDefeatVotes.
     */
    public void setIgnorePlayerDefeatVotes(boolean ignorePlayerDefeatVotes) {
        this.ignorePlayerDefeatVotes = ignorePlayerDefeatVotes;
    }

    /**
     * Adds the given reports vector to the GameReport collection.
     *
     * @param v the reports vector
     */
    public void addReports(List<Report> v) {
        if (v.isEmpty()) {
            return;
        }
        gameReports.add(getCurrentRound(), v);
    }

    /**
     * @param r Round number
     * @return a vector of reports for the given round.
     */
    public List<Report> getReports(int r) {
        return gameReports.get(r);
    }

    /**
     * @return a vector of all the reports.
     */
    public List<List<Report>> getAllReports() {
        return gameReports.get();
    }

    /**
     * Used to populate previous game reports, e.g. after a client connects to an
     * existing game.
     */
    public void setAllReports(List<List<Report>> v) {
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
     * @return true if the specified player is either the victor, or is on the
     *         winning team. Best
     *         to call during GamePhase.VICTORY.
     */
    public boolean isPlayerVictor(Player player) {
        if (player.getTeam() == Player.TEAM_NONE) {
            return player.getId() == victoryPlayerId;
        }
        return player.getTeam() == victoryTeam;
    }

    /**
     * @return the currently active context-object for VictoryCondition checking.
     *         This should be a
     *         mutable object, and it will be modified by the victory condition
     *         checkers. Whoever saves the
     *         game state when doing saves is also responsible for saving this
     *         state. At the start of the
     *         game this should be initialized to an empty HashMap
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
     *                 <code>null</code> (in which case all entities in the game
     *                 will
     *                 be returned).
     * @return an <code>Enumeration</code> of all entities that the selector
     *         accepts. This value will not be <code>null</code> but it may be
     *         empty.
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
     *         accepts. This value will not be <code>null</code> but it may be
     *         empty.
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
     *                 <code>null</code> (in which case all entities in the game
     *                 will
     *                 be returned).
     * @return an <code>Enumeration</code> of all entities that the selector
     *         accepts. This value will not be <code>null</code> but it may be
     *         empty.
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
     *         accepts. This value will not be <code>null</code> but it may be
     *         empty.
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
     * infantry, not protomeks, or not either of those. This method is
     * utitilized by the "A players Infantry moves after that players other
     * units", and "A players Protomeks move after that players other units"
     * options.
     */
    public boolean checkForValidNonInfantryAndOrProtoMeks(int playerId) {
        Iterator<Entity> iter = getPlayerEntities(getPlayer(playerId), false)
            .iterator();
        while (iter.hasNext()) {
            Entity entity = iter.next();
            boolean excluded = false;
            if ((entity instanceof Infantry)
                && getOptions().booleanOption(OptionsConstants.INIT_INF_MOVE_LATER)) {
                excluded = true;
            } else if ((entity instanceof ProtoMek)
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
     * Get Entities that have have a iNarc Nemesis pod attached and are situated
     * between two Coords
     *
     * @param attacker The attacking <code>Entity</code>.
     * @param target   The <code>Coords</code> of the original target.
     * @return an <code>Enumeration</code> of entities that have nemesis pods
     *         attached, are
     *         located between attacker and target, and are friendly with the
     *         attacker.
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
     * that IlluminationLevel::isPositionIlluminated is desired unless the
     * searchlighted hex
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
            logger.error("", ex);
            throw ex;
        }
        illuminatedPositions = ip;
        processGameEvent(new GameBoardChangeEvent(this));
    }

    /**
     * Add a new hex to the collection of Coords illuminated by searchlights.
     *
     * @return True if a new hex was added, else false if the set already
     *         contained the input hex.
     */
    public boolean addIlluminatedPosition(Coords c) {
        boolean rv = illuminatedPositions.add(c);
        processGameEvent(new GameBoardChangeEvent(this));
        return rv;
    }

    /**
     * Ages all flares, drifts them with the wind and removes any which have burnt
     * out or drifted off the map.
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
            boolean notDriftedOffMap = true;
            if (flare.isIgnited()) {
                flare.turnsToBurn--;
                if (flare.isDrifting()) {
                    Wind wind = planetaryConditions.getWind();
                    if (!planetaryConditions.getWind().isCalm()) {
                        WindDirection dir = planetaryConditions.getWindDirection();
                        flare.position = flare.position.translated(dir.ordinal(),
                            (wind.ordinal() > 1) ? (wind.ordinal() - 1) : wind.ordinal());
                        if (getBoard().contains(flare.position)) {
                            r = new Report(5236);
                            r.add(flare.position.getBoardNum());
                            r.newlines = 0;
                            reports.addElement(r);
                        } else {
                            reports.addElement(new Report(5240));
                            flares.removeElementAt(i);
                            notDriftedOffMap = false;
                        }
                    }
                }
            } else {
                r = new Report(5237);
                r.newlines = 0;
                reports.addElement(r);
                flare.ignite();
            }
            if (notDriftedOffMap) {
                if (flare.turnsToBurn <= 0) {
                    reports.addElement(new Report(5238));
                    flares.removeElementAt(i);
                } else {
                    r = new Report(5239);
                    r.add(flare.turnsToBurn);
                    reports.addElement(r);
                    flares.setElementAt(flare, i);
                }
            }
        }
        processGameEvent(new GameBoardChangeEvent(this));
        return reports;
    }

    public boolean gameTimerIsExpired() {
        return getOptions().booleanOption(OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT)
            && (getRoundCount() == getOptions().intOption(OptionsConstants.VICTORY_GAME_TURN_LIMIT));
    }

    /**
     * Uses VictoryFactory to generate a new VictoryCondition checker provided that
     * the
     * VictoryContext is saved properly. Calling this method at any time is ok and
     * should not affect
     * anything unless the VictoryCondition Config Options have changed.
     */
    public void createVictoryConditions() {
        victoryHelper = new VictoryHelper(this);
    }

    @Deprecated
    public VictoryHelper getVictory() {
        return victoryHelper;
    }

    public VictoryResult getVictoryResult() {
        return victoryHelper.checkForVictory(this, getVictoryContext());
    }

    // a shortcut function for determining whether vectored movement is
    // applicable
    public boolean useVectorMove() {
        return getOptions().booleanOption(OptionsConstants.ADVAERORULES_ADVANCED_MOVEMENT)
            && getBoard().inSpace();
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

    public boolean checkForValidDropShips(int playerId) {
        Iterator<Entity> iter = getPlayerEntities(getPlayer(playerId), false).iterator();
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
        return getPlayerEntities(getPlayer(playerId), false).stream()
            .anyMatch(e -> (e instanceof SmallCraft) && getTurn().isValidEntity(e, this));
    }

    @Override
    public PlanetaryConditions getPlanetaryConditions() {
        return planetaryConditions;
    }

    @Override
    public void setPlanetaryConditions(final @Nullable PlanetaryConditions conditions) {
        if (conditions == null) {
            logger.error("Can't set the planetary conditions to null!");
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

    public void removeEmptySmokeClouds() {
        smokeCloudList.removeIf(SmokeCloud::hasNoHexes);
    }

    public void removeCompletelyDissipatedSmokeClouds() {
        smokeCloudList.removeIf(SmokeCloud::isCompletelyDissipated);
    }

    /**
     * Only needed for Entity's that have secondaryPositions. This method
     * is used to make sure setPosition() doesn't get an inaccurate list
     * of positions for an entity that changed from between
     * using secondaryPositions and not, such as a Dropship taking off.
     * Iterates through all cached coords to get where the provided entity is.
     * Inefficient, and usually unnecessary.
     * @see Dropship#setPosition(Coords)
     * @param entity Entity we want to get the cached old positions of
     * @return cached coords that contain this entity
     */
    public synchronized HashSet<Coords> getEntityPositions(Entity entity) {
        HashSet<Coords> retVal = new HashSet<>();
        if (entityPosLookup.isEmpty()) {
            return retVal;
        }

        for (Coords coords : entityPosLookup.keySet()) {
            if (entityPosLookup.get(coords).contains(entity.getId())) {
                retVal.add(coords);
            }
        }
        return retVal;
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
        for (Entity e : inGameTWEntities()) {
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
        // This could be removed once we are confident the cache is working
        List<Integer> entitiesInCache = new ArrayList<>();
        List<Integer> entitiesInVector = new ArrayList<>();
        int entitiesInCacheCount = countEntitiesInCache(entitiesInCache);
        int entityVectorSize = 0;
        for (Entity e : inGameTWEntities()) {
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
            logger.warn("Entities vector has " + inGameTWEntities().size()
                + " but pos lookup cache has " + entitiesInCache.size() + "entities!");
            List<Integer> missingIds = new ArrayList<>();
            for (Integer id : entitiesInVector) {
                if (!entitiesInCache.contains(id)) {
                    missingIds.add(id);
                }
            }
            logger.info("Missing ids: " + missingIds);
        }
        for (Entity e : inGameTWEntities()) {
            HashSet<Coords> positions = e.getOccupiedCoords();
            for (Coords c : positions) {
                HashSet<Integer> ents = entityPosLookup.get(c);
                if ((ents != null) && !ents.contains(e.getId())) {
                    logger.warn("Entity " + e.getId() + " is in "
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
                    logger.warn("Entity Position Cache thinks Entity " + eId
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

    public Map<String, BehaviorSettings> getBotSettings() {
        return botSettings;
    }

    public void setBotSettings(Map<String, BehaviorSettings> botSettings) {
        this.botSettings = botSettings;
    }

    /**
     * Get a list of all objects on the ground at the given coordinates
     * that can be picked up by the given entity
     */
    public List<ICarryable> getGroundObjects(Coords coords, Entity entity) {
        if (!getGroundObjects().containsKey(coords)) {
            return new ArrayList<>();
        }

        // if the entity doesn't have working actuators etc
        if (!entity.canPickupGroundObject()) {
            return new ArrayList<>();
        }

        double maxTonnage = entity.maxGroundObjectTonnage();
        ArrayList<ICarryable> result = new ArrayList<>();

        for (ICarryable object : getGroundObjects().get(coords)) {
            if (maxTonnage >= object.getTonnage()) {
                result.add(object);
            }
        }

        return result;
    }

    public Map<Coords, List<ICarryable>> getGroundObjects() {
        // this is a temporary guard to preserve savegame compatibility. Remove after
        // this entire override after .50
        if (groundObjects == null) {
            groundObjects = new HashMap<>();
        }

        return groundObjects;
    }

    /**
     * Cancels a victory
     */
    public void cancelVictory() {
        setForceVictory(false);
        setVictoryPlayerId(Player.PLAYER_NONE);
        setVictoryTeam(Player.TEAM_NONE);
    }

    public MapSettings getMapSettings() {
        if (mapSettings == null) {
            mapSettings = MapSettings.getInstance();
        }
        return mapSettings;
    }

    public void setMapSettings(MapSettings mapSettings) {
        this.mapSettings = mapSettings;
    }

    /** @return The TW Units (Entity) currently in the game. */
    public List<Entity> inGameTWEntities() {
        return filterToEntity(inGameObjects.values());
    }

    private List<Entity> filterToEntity(Collection<? extends BTObject> objects) {
        return objects.stream().filter(o -> o instanceof Entity).map(o -> (Entity) o).collect(toList());
    }

    @Override
    public ReportEntry getNewReport(int messageId) {
        return new Report(messageId);
    }

    /**
     * @return The ID of the Player with the given name, if there is such a Player.
     */
    public Optional<Integer> idForPlayername(String playerName) {
        return playerForPlayername(playerName).map(Player::getId);
    }

    /**
     * @return The ID of the Player with the given name, if there is such a Player.
     */
    public Optional<Player> playerForPlayername(String playerName) {
        return getPlayersList().stream().filter(p -> p.getName().equals(playerName)).findFirst();
    }
}
