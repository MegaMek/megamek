/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import megamek.client.bot.BotClient;
import megamek.client.bot.ChatProcessor;
import megamek.client.bot.PhysicalCalculator;
import megamek.client.bot.PhysicalOption;
import megamek.client.bot.princess.FireControl.FireControlType;
import megamek.client.bot.princess.PathRanker.PathRankerType;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.client.bot.princess.coverage.Builder;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.panels.phaseDisplay.TowLinkWarning;
import megamek.codeUtilities.MathUtility;
import megamek.codeUtilities.StringUtility;
import megamek.common.BulldozerMovePath;
import megamek.common.BulldozerMovePath.MPCostComparator;
import megamek.common.CalledShot;
import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.LosEffects;
import megamek.common.MPCalculationSetting;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.DisengageAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FindClubAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.bays.Bay;
import megamek.common.board.AllowedDeploymentHelper;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.board.DeploymentElevationType;
import megamek.common.board.ElevationOption;
import megamek.common.compute.Compute;
import megamek.common.containers.PlayerIDAndList;
import megamek.common.enums.AimingMode;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.Transporter;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.event.GameCFREvent;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.common.game.IGame;
import megamek.common.game.InitiativeRoll;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.pathfinder.PathDecorator;
import megamek.common.pathfinder.ShortestPathFinder;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.*;
import megamek.common.util.BoardUtilities;
import megamek.common.util.StringUtil;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.attacks.StopSwarmAttack;
import megamek.logging.MMLogger;
import org.apache.logging.log4j.Level;

public class Princess extends BotClient {
    private static final MMLogger LOGGER = MMLogger.create(Princess.class);
    private static final char PLUS = '+';
    private static final char MINUS = '-';

    private static final int MAX_OVERHEAT_AMS = 14;

    /**
     * Highest target number to consider when not aiming at the head on an immobile Mek
     */
    private static final int SHUTDOWN_MAX_TARGET_NUMBER = 12;

    /**
     * Default maximum location armor for checking called shots
     */
    private static final int CALLED_SHOT_DEFAULT_MAX_ARMOR = 10;

    /**
     * Combined armor and structure value where a location is at risk of destruction
     */
    private static final int LOCATION_DESTRUCTION_THREAT = 5;

    /**
     * Difference in to-hit number between a general shot at an immobile Mek, and aiming for the head
     */
    private static final int IMMOBILE_HEAD_SHOT_MODIFIER = 7;

    /**
     * To-hit modifier for aimed shots against active targets
     */
    private static final int AIMED_SHOT_MODIFIER = 3;

    /**
     * To-hit modifier for called shots
     */
    private static final int CALLED_SHOT_MODIFIER = 3;

    /**
     * Minimum damage to be considered as a 'big gun' for prioritizing aimed shot locations
     */
    private static final int BIG_GUN_MIN_DAMAGE = 10;

    /**
     * Range to check damage when determining if a weapon is a 'big gun'
     */
    private static final int BIG_GUN_TYPICAL_RANGE = 5;

    /**
     * Minimum walking speed to consider for calling a shot low
     */
    private static final int CALLED_SHOT_MIN_MOVE = 6;

    /**
     * Minimum jump distance to consider for calling a shot low
     */
    private static final int CALLED_SHOT_MIN_JUMP = 5;

    /**
     * Distance to the waypoint to consider for considering the waypoint reached
     */
    public static final int DISTANCE_TO_WAYPOINT = 3;


    private final IHonorUtil honorUtil = new HonorUtil();

    private boolean initialized = false;

    // path rankers and fire controls, organized by their explicitly given types to avoid confusion
    private HashMap<PathRankerType, IPathRanker> pathRankers;
    private HashMap<FireControlType, FireControl> fireControls;
    private UnitBehavior unitBehaviorTracker;
    private FireControlState fireControlState;
    private PathRankerState pathRankerState;
    private ArtilleryTargetingControl atc;

    private List<HeatMap> enemyHeatMaps;
    private HeatMap friendlyHeatMap;

    private Integer spinUpThreshold = null;

    private BehaviorSettings behaviorSettings;
    private double moveEvaluationTimeEstimate = 0;
    private final Precognition precognition;
    private final Thread precognitionThread;
    /**
     * Mapping to hold the damage allocated to each targetable, stored by ID. Used to allocate damage more intelligently
     * and avoid overkill.
     */
    private final ConcurrentHashMap<Integer, Double> damageMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> teamTagTargetsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Map.Entry<Integer, Coords>, List<WeaponMounted>> incomingGuidablesMap = new ConcurrentHashMap<>();
    private final Set<Coords> strategicBuildingTargets = new HashSet<>();
    private boolean fallBack = false;
    private final ChatProcessor chatProcessor = new ChatProcessor();
    private boolean fleeBoard = false;
    private final MoraleUtil moraleUtil = new MoraleUtil();
    private final Set<Integer> attackedWhileFleeing = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> crippledUnits = new HashSet<>();
    private final ArtilleryCommandAndControl artilleryCommandAndControl = new ArtilleryCommandAndControl();
    // Track entities that fired an AMS manually this round
    private List<Integer> manualAMSIds;

    // Master switch to enable/disable use of enhanced targeting system (aimed/called shots)
    private boolean enableEnhancedTargeting;

    // Limits types of units Princess will target and attack with enhanced targeting
    private List<Integer> enhancedTargetingTargetTypes;
    private List<Integer> enhancedTargetingAttackerTypes;
    private SwarmContext swarmContext;
    // Controls whether Princess will use called shots on immobile targets
    private boolean useCalledShotsOnImmobileTarget;

    // Controls whether Princess will use enhanced targeting on targets that have partial cover
    private boolean allowCoverEnhancedTargeting;
    private EnemyTracker enemyTracker;
    private CoverageValidator coverageValidator;
    private SwarmCenterManager swarmCenterManager;

    /**
     * Returns a new Princess Bot with the given behavior and name, configured for the given host and port. The new
     * Princess Bot outputs its settings to its own LOGGER.
     */
    public static Princess createPrincess(String name, String host, int port, BehaviorSettings behavior) {
        Princess result = new Princess(name, host, port);
        result.startPrecognition();
        result.setBehaviorSettings(behavior);
        LOGGER.debug(result.getBehaviorSettings().toLog());
        return result;
    }

    /**
     * Constructor - initializes a new instance of the Princess bot.
     *
     * @param name The display name.
     * @param host The host address to which to connect.
     * @param port The port on the host where to connect.
     */
    public Princess(final String name, final String host, final int port) {
        super(name, host, port);
        setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);

        fireControlState = new FireControlState();
        pathRankerState = new PathRankerState();

        // Set up enhanced targeting
        resetEnhancedTargeting(true);

        // Start-up precognition now, so that it can instantiate its game instance,
        // and it will stay up-to date.
        precognition = new Precognition(this);
        precognitionThread = new Thread(precognition, "Princess-precognition (" + getName() + ")");
    }

    /**
     * Helper method to start the pre-cognition. This is extracted from the initializer to allow for sub-classing of
     * Princess and remove the possibility that it'll start before a sub-class finishes initializing.
     */
    public void startPrecognition() {
        precognitionThread.start();
    }

    /**
     * Lazy-loading accessor for the artillery targeting control.
     *
     * @return {@link ArtilleryTargetingControl}
     */
    public ArtilleryTargetingControl getArtilleryTargetingControl() {
        if (atc == null) {
            atc = new ArtilleryTargetingControl();
        }

        return atc;
    }

    /**
     * Gets the appropriate path ranker instance given an entity Uses the entity's EType flags to figure out which one
     * to return Returns BasicPathRanker by default.
     *
     * @param entity The entity whose ETYPE to check
     *
     * @return Path ranker instance
     */
    IPathRanker getPathRanker(Entity entity) {
        if (entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
            return pathRankers.get(PathRankerType.Infantry);
        } else if (entity.isAero() && game.useVectorMove()) {
            return pathRankers.get(PathRankerType.NewtonianAerospace);
        } else if (behaviorSettings.isExperimental()) {
            return pathRankers.get(PathRankerType.Utility);
        }

        return pathRankers.get(PathRankerType.Basic);
    }

    IPathRanker getPathRanker(PathRankerType pathRankerType) {
        return pathRankers.get(pathRankerType);
    }

    public boolean getFallBack() {
        return fallBack;
    }

    boolean getFleeBoard() {
        return fleeBoard;
    }

    /**
     * Picks a tag target based on the data contained within the given GameCFREvent Expects the event to have some tag
     * targets and tag target types.
     */
    @Override
    protected int pickTagTarget(GameCFREvent evt) {
        List<Integer> TAGTargets = evt.getTAGTargets();
        List<Integer> TAGTargetTypes = evt.getTAGTargetTypes();
        Map<Coords, Integer> tagTargetHexes = new HashMap<>(); // maps coordinates to target index

        // Algorithm:
        // get a list of the hexes being tagged
        // figure out how much damage a hit to each of the tagged hexes will do (relatively)
        // pick the one which will result in the best damage

        // get list of targetable hexes
        for (int tagIndex = 0; tagIndex < TAGTargets.size(); tagIndex++) {
            int nType = TAGTargetTypes.get(tagIndex);
            Targetable tgt = getGame().getTarget(nType, TAGTargets.get(tagIndex));
            if (tgt != null && !tagTargetHexes.containsKey(tgt.getPosition())) {
                tagTargetHexes.put(tgt.getPosition(), tagIndex);
            }
        }

        Entity arbitraryEntity = getArbitraryEntity();
        if (arbitraryEntity == null) {
            return 0;
        }

        double maxDamage = -Double.MAX_VALUE;
        Coords maxDamageHex = null;

        // invoke ArtilleryTargetingControl.calculateDamageValue
        for (Coords targetHex : tagTargetHexes.keySet()) {
            // a note on parameters:
            // we don't care about exact damage value since we're just comparing them relative to one another
            //  note: technically we should,
            // we don't care about specific firing entity, we just want one on our side
            //      since we only use it to determine friendliness
            double currentDamage = getArtilleryTargetingControl().calculateDamageValue(10,
                  targetHex,
                  arbitraryEntity,
                  game,
                  this);
            if (currentDamage > maxDamage) {
                maxDamage = currentDamage;
                maxDamageHex = targetHex;
            }
        }

        if (maxDamageHex != null) {
            return tagTargetHexes.get(maxDamageHex);
        } else {
            return 0;
        }
    }

    boolean getForcedWithdrawal() {
        return getBehaviorSettings().isForcedWithdrawal();
    }

    private void setFleeBoard(final boolean fleeBoard, final String reason) {
        LOGGER.debug("Setting Flee Board {} because: {}", fleeBoard, reason);

        this.fleeBoard = fleeBoard;
    }

    public FireControlState getFireControlState() {
        return fireControlState;
    }

    public PathRankerState getPathRankerState() {
        return pathRankerState;
    }

    Precognition getPrecognition() {
        return precognition;
    }

    public int getMaxWeaponRange(Entity entity) {
        return getMaxWeaponRange(entity, false);
    }

    /**
     * @param entity         Entity we're checking
     * @param airborneTarget Whether the potential target is in the air, only relevant for aircraft shooting at other
     *                       aircraft on ground maps.
     *
     * @return maximum weapon range for the given entity. Cached version of entity.getMaxWeaponRange()
     */
    public int getMaxWeaponRange(Entity entity, boolean airborneTarget) {
        return getFireControlState().getWeaponRanges(airborneTarget)
              .computeIfAbsent(entity.getId(), ent -> entity.getMaxWeaponRange(airborneTarget));
    }

    public void setFallBack(final boolean fallBack, final String reason) {
        LOGGER.debug("Setting Fall Back {} because: {}", fallBack, reason);
        this.fallBack = fallBack;
    }

    public void setBehaviorSettings(final BehaviorSettings behaviorSettings) {
        LOGGER.info("New behavior settings for {}\n{}", getName(), behaviorSettings.toLog());
        try {
            this.behaviorSettings = behaviorSettings.getCopy();
        } catch (final PrincessException e) {
            LOGGER.error("", e);
            return;
        }
        getStrategicBuildingTargets().clear();
        setFallBack(behaviorSettings.shouldGoHome(), "Fall Back Configuration.");
        setFleeBoard(behaviorSettings.shouldAutoFlee(), "Flee Board Configuration.");
        if (behaviorSettings.iAmAPirate() && honorUtil instanceof HonorUtil honorUtilCast) {
            honorUtilCast.setIAmAPirate(behaviorSettings.iAmAPirate());
        }
        // Fallback do not care for targets
        if (!getFallBack()) {
            for (final String targetCoords : behaviorSettings.getStrategicBuildingTargets()) {
                if (!StringUtil.isPositiveInteger(targetCoords) || (4 != targetCoords.length())) {
                    continue;
                }
                final String x = targetCoords.substring(0, 2);
                final String y = targetCoords.replaceFirst(x, "");
                // Need to subtract 1, since we are given a Hex number string,
                // which is Coords X + 1Y + 1
                final Coords coords = new Coords(MathUtility.parseInt(x) - 1, MathUtility.parseInt(y) - 1);
                getStrategicBuildingTargets().add(coords);
            }
        }
        spinUpThreshold = null;
        if (initialized) {
            // path rankers need to be reinitialized since the behavior settings changed, this will
            // propagate any changes the BasicPathRanker needs.
            initializePathRankers();
        }
        sendPrincessSettings();
    }

    /**
     * Get the appropriate instance of a FireControl object for the given entity.
     *
     * @param entity The entity in question.
     *
     * @return Instance of FireControl
     */
    FireControl getFireControl(Entity entity) {
        if (entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
            return fireControls.get(FireControlType.Infantry);
            // some entities can shoot at multiple targets without undergoing too much penalty
            // so let's get them doing that.
        } else if (entity.getCrew().getCrewType().getMaxPrimaryTargets() > 1 ||
              entity.hasQuirk(OptionsConstants.QUIRK_POS_MULTI_TRAC) ||
              entity.hasAbility(OptionsConstants.GUNNERY_MULTI_TASKER) ||
              entity.getCrew().getCrewType().getMaxPrimaryTargets() < 0) {
            return fireControls.get(FireControlType.MultiTarget);
        }

        return fireControls.get(FireControlType.Basic);
    }

    FireControl getFireControl(FireControlType fireControlType) {
        return fireControls.get(fireControlType);
    }

    public UnitBehavior getUnitBehaviorTracker() {
        if (unitBehaviorTracker == null) {
            unitBehaviorTracker = new UnitBehavior();
        }
        return unitBehaviorTracker;
    }

    double getDamageAlreadyAssigned(final Targetable target) {
        final Integer targetId = target.getId();
        if (damageMap.containsKey(targetId)) {
            return damageMap.get(targetId);
        }
        return 0.0; // If we have no entry, return zero
    }

    public BehaviorSettings getBehaviorSettings() {
        return behaviorSettings;
    }

    public Set<Coords> getStrategicBuildingTargets() {
        return strategicBuildingTargets;
    }

    public boolean hasStrategicBuildingTargets(final Coords coords) {
        return getStrategicBuildingTargets().contains(coords);
    }

    public void addStrategicBuildingTarget(final Coords coords) {
        if (null == coords) {
            throw new NullPointerException("Coords is null.");
        }
        if (!getGame().getBoard().contains(coords)) {
            LOGGER.warn("Board does not contain {}", coords.toFriendlyString());
            return;
        }
        getStrategicBuildingTargets().add(coords);
    }

    public void removeStrategicBuildingTarget(final Coords coords) {
        if (null == coords) {
            throw new NullPointerException("Coords is null.");
        }
        if (!getGame().getBoard().contains(coords)) {
            LOGGER.warn("Board does not contain {}", coords.toFriendlyString());
            return;
        }
        if (!hasStrategicBuildingTargets(coords)) {
            LOGGER.warn("Strategic Building Targets does not contain {}", coords.toFriendlyString());
            return;
        }

        getStrategicBuildingTargets().remove(coords);

    }

    public Set<Integer> getPriorityUnitTargets() {
        return getBehaviorSettings().getPriorityUnitTargets();
    }

    public Targetable getAppropriateTarget(Coords strategicTarget) {
        return getAppropriateTarget(strategicTarget, IGame.DEFAULT_BOARD_ID);
    }

    public Targetable getAppropriateTarget(Coords strategicTarget, int boardId) {
        if (null == game.getBoard(boardId).getBuildingAt(strategicTarget)) {
            return new HexTarget(strategicTarget, boardId, Targetable.TYPE_HEX_CLEAR);
        } else {
            return new BuildingTarget(strategicTarget, game.getBoard(boardId), false);
        }
    }

    @Override
    protected Vector<BoardLocation> calculateArtyAutoHitHexes() {
        try {
            // currently returns no artillery hit spots
            // make an empty list
            final PlayerIDAndList<BoardLocation> artyAutoHitHexes = new PlayerIDAndList<>();
            // attach my player id to it
            artyAutoHitHexes.setPlayerID(getLocalPlayer().getId());
            return artyAutoHitHexes;
        } catch (Exception ignored) {
            return new PlayerIDAndList<>();
        }
    }

    @Override
    protected void initTargeting() {
        // Reset incoming guided weapon lists for each friendly unit.
        // Reset planned TAG expected utility map
        incomingGuidablesMap.clear();
        teamTagTargetsMap.clear();
        getArtilleryTargetingControl().initializeForTargetingPhase();
    }

    @Override
    protected void calculateDeployment() {
        // get the first unit
        final int entityNum = game.getFirstDeployableEntityNum(game.getTurnForPlayer(localPlayerNumber));
        sendChat("deploying unit " + getEntity(entityNum).getChassis(), Level.INFO);

        // if we are using forced withdrawal, and the entity being considered is crippled
        // we will opt to not re-deploy the entity
        if (getForcedWithdrawal() && getEntity(entityNum).isCrippled()) {
            LOGGER.info("Declining to deploy crippled unit: {}. Removing unit.", getEntity(entityNum).getChassis());
            sendDeleteEntity(entityNum);
            return;
        }

        // get a list of all coordinates to which we can deploy
        final List<Coords> startingCoords = getStartingCoordsArray(game.getEntity(entityNum));
        if (startingCoords.isEmpty()) {
            LOGGER.error("No valid locations to deploy {}", getEntity(entityNum).getDisplayName());
        }

        // get the coordinates I can deploy on
        final Coords deployCoords = getFirstValidCoords(getEntity(entityNum), startingCoords);
        if (null == deployCoords) {
            // if I cannot deploy anywhere, then I get rid of the entity instead so that we may go about our business
            LOGGER.error("getCoordsAround gave no location for {}. Removing unit.", getEntity(entityNum).getChassis());

            sendDeleteEntity(entityNum);
            return;
        }

        final Entity deployEntity = getEntity(entityNum);

        // For now, just use whatever board the unit is set to be on, usually board 0 by default
        Board board = game.getBoard(deployEntity);

        // first coordinate that it is legal to put this unit on now find some sort of reasonable
        // facing. If there are deployed enemies, face them

        // specifically, face the last deployed enemy.
        int decentFacing = -1;
        for (final Entity enemy : getEnemyEntities()) {
            if (enemy.isDeployed() && !enemy.isOffBoard() && game.onTheSameBoard(deployEntity, enemy)) {
                decentFacing = deployCoords.direction(enemy.getPosition());
                break;
            }
        }

        // if I haven't found a decent facing, then at least face towards
        // the center of the board
        if (-1 == decentFacing) {
            final Coords center = new Coords(board.getWidth() / 2, board.getHeight() / 2);
            decentFacing = deployCoords.direction(center);
        }

        final Hex deployHex = board.getHex(deployCoords);
        int deployElevation = deployEntity.getElevation();

        if (deployEntity.isAero()) {
            if (board.isGround()) {
                // keep the altitude set in the lobby, possibly starting grounded
                deployElevation = deployEntity.getAltitude();
            } else if (board.isLowAltitude()) {
                // try to keep the altitude set in the lobby, but stay above the terrain
                var deploymentHelper = new AllowedDeploymentHelper(deployEntity,
                      deployCoords,
                      board,
                      deployHex,
                      game);
                List<ElevationOption> allowedDeployment = deploymentHelper.findAllowedElevations(DeploymentElevationType.ALTITUDE);
                if (allowedDeployment.isEmpty()) {
                    // that's bad, cannot deploy at all
                    LOGGER.error("Cannot find viable altitude to deploy to");
                    sendDeleteEntity(entityNum);
                    return;
                } else {
                    deployElevation = Math.max(deployEntity.getAltitude(),
                          Collections.min(allowedDeployment).elevation());
                }
            }
        } else {
            deployElevation = getDeployElevation(deployEntity, deployHex);
            // Compensate for hex elevation where != 0...
            deployElevation -= deployHex.getLevel();
        }
        deploy(entityNum, deployCoords, board.getBoardId(), decentFacing, deployElevation, new Vector<>(), false);
    }

    /**
     * Calculate the deployment elevation for the given entity. Gun Emplacements should deploy on the rooftop of the
     * building for maximum visibility.
     */
    private int getDeployElevation(Entity deployEntity, Hex deployHex) {
        // Entity.elevationOccupied performs a null check on Hex
        if (deployEntity instanceof GunEmplacement) {
            return deployEntity.elevationOccupied(deployHex) + deployHex.terrainLevel(Terrains.BLDG_ELEV);
        } else {
            return deployEntity.elevationOccupied(deployHex);
        }
    }

    /**
     * Returns the first set of valid deployment coordinates for the given unit out of the given deployment coordinates.
     * It's possible to return null, which indicates that there are no valid hexes for the given unit to deploy into.
     */
    @Override
    protected @Nullable Coords getFirstValidCoords(final Entity deployedUnit, final List<Coords> possibleDeployCoords) {
        if (Entity.ETYPE_GUN_EMPLACEMENT == (deployedUnit.getEntityType() & Entity.ETYPE_GUN_EMPLACEMENT)) {
            final List<Coords> validCoords = calculateTurretDeploymentLocations((GunEmplacement) deployedUnit,
                  possibleDeployCoords);
            if (!validCoords.isEmpty()) {
                return validCoords.get(0);
            }

            return null;
        } else if (getGame().useVectorMove()) {
            return calculateAdvancedAerospaceDeploymentCoords(deployedUnit, possibleDeployCoords);
        } else {
            return rankDeploymentCoords(deployedUnit, possibleDeployCoords);
        }
    }

    /**
     * Function that calculates deployment coordinates
     *
     * @param deployedUnit         The unit being considered for deployment
     * @param possibleDeployCoords The coordinates being considered for deployment
     *
     * @return The first valid deployment coordinates.
     */
    private Coords calculateAdvancedAerospaceDeploymentCoords(final Entity deployedUnit,
          final List<Coords> possibleDeployCoords) {
        for (Coords coords : possibleDeployCoords) {
            if (!NewtonianAerospacePathRanker.willFlyOffBoard(deployedUnit, coords)) {
                return coords;
            }
        }

        // if we can't find any good deployment coordinates, deploy anyway to the first available one
        // and maybe eventually we'll slow down enough that we can deploy without immediately flying off
        if (!possibleDeployCoords.isEmpty()) {
            return possibleDeployCoords.get(0);
        }

        return null;
    }

    /**
     * Helper function that calculates the possible locations where a given gun emplacement can be deployed
     *
     * @param deployedUnit         The unit to check
     * @param possibleDeployCoords The list of possible deployment coordinates
     */
    private List<Coords> calculateTurretDeploymentLocations(final GunEmplacement deployedUnit,
          final List<Coords> possibleDeployCoords) {
        // algorithm:
        // get all hexes in deployment zone with buildings
        // for each building, if deploying on the roof does not cause a stacking violation, add it to the list
        // sort the list in decreasing order based on CF then height
        final List<Coords> turretDeploymentLocations = new Vector<>();

        for (final Coords coords : possibleDeployCoords) {
            final IBuilding building = game.getBoard(deployedUnit).getBuildingAt(coords);
            final Hex hex = game.getBoard(deployedUnit).getHex(coords);

            if (null != building) {
                final int buildingHeight = hex.terrainLevel(Terrains.BLDG_ELEV);

                // check stacking violation at the roof level
                final Entity violation = Compute.stackingViolation(game,
                      deployedUnit,
                      coords,
                      buildingHeight,
                      coords,
                      null,
                      deployedUnit.climbMode(),
                      true);
                // Ignore coords that could cause a stacking violation
                if (null == violation) {
                    turretDeploymentLocations.add(coords);
                }
            }
        }

        turretDeploymentLocations.sort((arg0, arg1) -> calculateTurretDeploymentValue(arg1) -
              calculateTurretDeploymentValue(arg0));
        return turretDeploymentLocations;
    }

    /**
     * Helper function that calculates the "utility" of placing a turret at the given coords
     *
     * @param coords The location of the building being considered.
     *
     * @return An "arbitrary" utility number
     */
    private int calculateTurretDeploymentValue(final Coords coords) {
        // algorithm: a building is valued by the following formula:
        //      (CF + height * 2) / # turrets placed on the roof
        //      This way, we will generally favor unpopulated higher CF buildings,
        //      but have some wiggle room in case of a really tall high CF building
        final IBuilding building = game.getBoard().getBuildingAt(coords);
        final Hex hex = game.getBoard().getHex(coords);
        final int turretCount = 1 + game.getGunEmplacements(coords).size();

        return (building.getCurrentCF(coords) + hex.terrainLevel(Terrains.BLDG_ELEV) * 2) / turretCount;
    }

    protected double rankKernelAroundCoords(MovePath start, Entity deployedUnit, int radius, BasicPathRanker ranker) {
        // Logging is extremely slow, only use when debugging.
        StringBuilder sb = null;
        if (LOGGER.isDebugEnabled()) {
            sb = new StringBuilder();
            sb.append("Ranking kernel around hex ").append(start.getFinalCoords().toString());
        }

        double rank;
        // allAtDistance uses a concept of radius that is 1 smaller.
        ArrayList<Coords> kernel = start.getFinalCoords().allAtDistance(radius + 1);

        // Get all paths from the start point to the outer hexes that use "radius" MP
        // Worse starting hexes have fewer, and shorter, paths
        ShortestPathFinder pf = ShortestPathFinder.newInstanceOfOneToAll(radius, MoveStepType.FORWARDS, game);
        pf.run(start);

        // Lower rank is better; 0.0 is minimum at this point.
        rank = Math.max(kernel.size() - pf.getAllComputedPaths().size(), 0.0);
        for (MovePath mp : pf.getAllComputedPaths().values()) {
            rank -= mp.getHexesMoved();
            rank += ranker.checkPathForHazards(mp, deployedUnit, game);
        }

        if (sb != null) {
            sb.append("\n\tAll computed ")
                  .append(radius)
                  .append("-length paths: ")
                  .append(pf.getAllComputedPaths().size());
            sb.append("\n\tFinal rank (including hazards): ").append(rank);
            LOGGER.debug(sb.toString());
        }

        return rank;
    }

    /**
     * Rank possible deployment coordinates by hazard, path freedom, concealment
     * <p>
     * <ol>
     *     <li>Randomly select N coords from list</li>
     *     <li>
     *         For selected coords:
     *         <ol>
     *             <li>Check if hex is invalid</li>
     *             <li>Create a MovePath containing the starting coordinate</li>
     *             <li>Get the hazard value</li>
     *             <li>Save Coords to HashMap with hazard as key</li>
     *         </ol>
     *     </li>
     * </ol>
     */
    protected Coords rankDeploymentCoords(Entity deployedUnit, List<Coords> possibleDeployCoords) {
        StringBuilder sb = null;
        if (LOGGER.isDebugEnabled()) {
            sb = new StringBuilder();
            sb.append("Ranking deployment hexes...");
        }

        if ((deployedUnit.getTowing() != Entity.NONE && game.getEntity(deployedUnit.getTowing()) != null) ||
              (deployedUnit.getTowedBy() != Entity.NONE) && game.getEntity(deployedUnit.getTowedBy()) != null) {
            List<Coords> filteredCoords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(game,
                  deployedUnit,
                  game.getBoard(deployedUnit));
            if (!filteredCoords.isEmpty()) {
                possibleDeployCoords = possibleDeployCoords.stream().filter(filteredCoords::contains).toList();
            } else {
                LOGGER.error("No valid locations to tractor/trailer deploy {}", deployedUnit.getDisplayName());
            }
        }


        // Sample LIMIT number of valid starting hexes, check accessibility and hazards within RADIUS
        int LIMIT = 20;
        int RADIUS = 3;

        // Shallow copy of refs list
        ArrayList<Coords> localCopy = new ArrayList<>(possibleDeployCoords);

        // Hacky, but really, "DEPLOY" should be a path step...
        MovePath mp = new MovePath(game, deployedUnit);
        mp.addStep(MoveStepType.NONE);
        MoveStep deployStep = mp.getLastStep();
        IPathRanker ranker = getPathRanker(deployedUnit);
        HashMap<Double, ArrayList<Coords>> rankedCoords = new HashMap<>();

        // Units that deploy airborne don't need to worry about all this
        if (!(deployedUnit.isAero() ||
              ((deployedUnit.getMovementMode().isVTOL() || deployedUnit.getMovementMode().isWiGE()) &&
                    deployedUnit.getElevation() > 0))) {
            double hazard;
            int longest = 0;
            int size;
            for (Coords dest : localCopy) {
                deployStep.setPosition(dest);
                if (null != super.getFirstValidCoords(deployedUnit, List.of(dest))) {
                    hazard = -((BasicPathRanker) ranker).checkPathForHazards(mp, deployedUnit, game);
                    if (deployedUnit instanceof BuildingEntity && getBoard() != null && getBoard().getHex(dest) != null) {
                        // If there's anything in the hex, let's increase the hazard so we don't prefer it
                        hazard -= getBoard().getHex(dest).getTerrainTypesSet().size();
                    }
                    if (!rankedCoords.containsKey(hazard)) {
                        rankedCoords.put(hazard, new ArrayList<>());
                    }
                    rankedCoords.get(hazard).add(dest);
                    size = rankedCoords.get(hazard).size();
                    longest = Math.max(size, longest);

                    if (sb != null) {
                        sb.append("\n\tFound valid coordinates (")
                              .append(dest)
                              .append(") with initial hazard of: ")
                              .append(hazard);
                    }
                }

                // Only get some subset
                if (longest > LIMIT || rankedCoords.size() > LIMIT) {
                    break;
                }
            }
            if (!rankedCoords.isEmpty()) {
                double bestRank = rankedCoords.keySet().stream().mapToDouble(d -> d).max().getAsDouble();
                Coords bestCandidate = null;
                double scoreToBeat = -Double.MAX_VALUE;
                double current;
                ArrayList<Coords> candidates = rankedCoords.get(bestRank);
                for (Coords c : candidates) {
                    mp.clear();
                    mp.addStep((deployedUnit.getAnyTypeMaxJumpMP() == 0) ? MoveStepType.NONE : MoveStepType.START_JUMP);
                    mp.getLastStep().setPosition(c);
                    current = bestRank - rankKernelAroundCoords(mp, deployedUnit, RADIUS, (BasicPathRanker) ranker);
                    if (current > scoreToBeat) {
                        scoreToBeat = current;
                        bestCandidate = c;
                    }
                }
                if (bestCandidate != null) {
                    if (sb != null) {
                        sb.append("\n\tFound best candidate (")
                              .append(bestCandidate)
                              .append(") out of ")
                              .append(candidates.size())
                              .append(" with a score of ")
                              .append(scoreToBeat);
                        LOGGER.debug(sb.toString());
                    }
                    return bestCandidate;
                }
            }
        } else {
            if (sb != null) {
                sb.append("\n\tAerospace / flying ground units don't worry about ground level hazards;");
            }
        }

        if (sb != null) {
            sb.append("\n\tFalling back to default getFirstValidCoords method!");
            LOGGER.debug(sb.toString());
        }
        // Fall back on old method
        Coords bestCandidate = super.getFirstValidCoords(deployedUnit, possibleDeployCoords);
        if (bestCandidate == null) {
            LOGGER.error("Returning no deployment position; THIS IS BAD!");
        }
        return bestCandidate;
    }

    @Override
    protected void calculateFiringTurn() {
        final Entity shooter;
        try {
            // get the first entity that can act this turn make sure weapons
            // are loaded
            shooter = getEntityToFire(fireControlState);
        } catch (Exception e) {
            // If we fail to get the shooter, literally nothing can be done.
            LOGGER.error(e.getMessage(), e);
            return;
        }

        try {
            // Forego firing if
            // a) hidden,
            // b) under "peaceful" forced withdrawal,
            // c) majority firepower is jammed
            // d) best firing plan comes up as crap (no expected damage/null)
            //
            // If foregoing firing, unjam highest-damage weapons first, then turret
            boolean skipFiring = false;

            // If my unit is forced to withdraw, don't fire unless I've been fired on.
            if (getForcedWithdrawal() && shooter.isCrippled()) {
                final StringBuilder msg = new StringBuilder(shooter.getDisplayName()).append(
                      " is crippled and withdrawing.");
                try {
                    if (shooter.getSwarmTargetId() != Entity.NONE) {
                        msg.append("\n\tBut will need to stop swarming before fleeing.");
                        skipFiring = true;
                    } else if (attackedWhileFleeing.contains(shooter.getId())) {
                        msg.append("\n\tBut I was fired on, so I will return fire.");
                    } else {
                        msg.append("\n\tI will not fire so long as I'm not fired on.");
                        skipFiring = true;
                    }
                } finally {
                    LOGGER.info(msg.toString());
                }
            }

            if (shooter.isHidden()) {
                skipFiring = true;
                LOGGER.info("Hidden unit skips firing.");
            }

            // calculating a firing plan is somewhat expensive, so
            // we skip this step if we have already decided not to fire due to being hidden or under "peaceful forced withdrawal"
            if (!skipFiring) {
                // Set up ammo conservation.
                final Map<WeaponMounted, Double> ammoConservation = calcAmmoConservation(shooter);

                // entity that can act this turn make sure weapons are loaded
                final FiringPlan plan = getFireControl(shooter).getBestFiringPlan(shooter,
                      getHonorUtil(),
                      game,
                      ammoConservation);
                if ((null != plan) && (plan.getExpectedDamage() > 0)) {
                    getFireControl(shooter).loadAmmo(shooter, plan);
                    plan.sortPlan();

                    // Log info and debug at different levels
                    LOGGER.info("{} - Best Firing Plan: {}", shooter.getDisplayName(), plan.getDebugDescription(false));
                    LOGGER.debug("{} - Detailed Best Firing Plan: {}",
                          shooter.getDisplayName(),
                          plan.getDebugDescription(true));

                    // Consider making an aimed shot if the target is shut down or the attacker has
                    // a targeting computer. Alternatively, consider using the called shots optional
                    // rule to adjust the hit table to something more favorable.

                    boolean isCalledShot = false;
                    int locationDestruction = Integer.MAX_VALUE;
                    int aimLocation = Mek.LOC_NONE;
                    int calledShotDirection = CalledShot.CALLED_NONE;

                    WeaponFireInfo primaryFire = plan.get(0);
                    int targetID;
                    if (primaryFire != null) {
                        targetID = primaryFire.getTarget().getId();
                    } else {
                        targetID = Entity.NONE;
                    }

                    // TODO: gate this block on a game option or client option
                    if (targetID > Entity.NONE &&
                          primaryFire.getTarget() != null &&
                          plan.stream().allMatch(curFire -> primaryFire.getTarget().getId() == targetID) &&
                          checkForEnhancedTargeting(shooter,
                                primaryFire.getTarget(),
                                primaryFire.getToHit().getCover())) {

                        Entity aimTarget = (Mek) primaryFire.getTarget();
                        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CALLED_SHOTS) &&
                              (!aimTarget.isImmobile() || useCalledShotsOnImmobileTarget)) {
                            isCalledShot = true;
                        }

                        // Check for an aimed shot
                        if (aimTarget.isImmobile() || shooter.hasTargComp()) {
                            boolean rearShot = primaryFire.getToHit().getSideTable() == ToHitData.SIDE_REAR;

                            // Get the Mek location to aim at. Infantry and BA will go for the head
                            // if the odds are good.
                            aimLocation = getAimedShotLocation(primaryFire.getTarget(),
                                  plan,
                                  rearShot,
                                  shooter.isInfantry());

                            // When aiming at a location, don't bother checking for called shots
                            if (aimLocation != Mek.LOC_NONE) {
                                isCalledShot = false;
                                // TODO: this should be adjusted to better handle multiple target types
                                locationDestruction = aimTarget.getArmor(aimLocation, rearShot) +
                                      aimTarget.getInternal(aimLocation);
                            }

                        }

                        if (isCalledShot) {

                            calledShotDirection = getCalledShotDirection(primaryFire.getTarget(),
                                  primaryFire.getToHit().getSideTable(),
                                  plan);

                        }

                    }

                    // Add expected damage from the chosen FiringPlan to the
                    // damageMap for the target enemy.
                    // while we're looping through all the shots anyway, send any firing mode changes
                    for (WeaponFireInfo shot : plan) {
                        Integer targetId = shot.getTarget().getId();
                        double existingTargetDamage = damageMap.getOrDefault(targetId, 0.0);
                        double newDamage = existingTargetDamage + shot.getExpectedDamage();
                        damageMap.put(targetId, newDamage);

                        // Track manual AMS use each round
                        if (shot.getWeapon().getType().hasFlag(Weapon.F_AMS)) {
                            if (shot.getWeapon().curMode().equals(Weapon.MODE_AMS_MANUAL)) {
                                flagManualAMSUse(shooter.getId());
                            }
                        }

                        // Set attacks as aimed or called, as required
                        if (aimLocation != Mek.LOC_NONE || calledShotDirection != CalledShot.CALLED_NONE) {
                            setAttackAsAimedOrCalled(shot, aimLocation, calledShotDirection, locationDestruction);
                        }
                        if (shot.getUpdatedFiringMode() != null) {
                            super.sendModeChange(shooter.getId(),
                                  shooter.getEquipmentNum(shot.getWeapon()),
                                  shot.getUpdatedFiringMode());
                        }
                    }

                    // tell the game I want to fire
                    Vector<EntityAction> actions = new Vector<>();

                    // if using search light, it needs to go before the other actions so we can light up what we're shooting at
                    SearchlightAttackAction searchLightAction = getFireControl(shooter).getSearchLightAction(shooter,
                          plan);
                    if (searchLightAction != null) {
                        actions.add(searchLightAction);
                    }

                    actions.addAll(plan.getEntityActionVector());

                    EntityAction spotAction = getFireControl(shooter).getSpotAction(plan, shooter, fireControlState);
                    if (spotAction != null) {
                        actions.add(spotAction);
                    }

                    sendAttackData(shooter.getId(), actions);
                    return;
                } else {
                    LOGGER.info("No best firing plan for {}", shooter.getDisplayName());
                }
            }

            // if I have decided to skip firing or don't have a firing plan, so let's consider some
            // alternative uses of my turn
            Vector<EntityAction> miscPlan = null;

            if (shooter.getSwarmTargetId() != Entity.NONE) {
                // If we are skipping firing while swarming, it is because we are fleeing...
                // so let's stop swarming if we are doing so
                final Mounted<?> stopSwarmWeapon = shooter.getIndividualWeaponList()
                      .stream()
                      .filter(weapon -> weapon.getType() instanceof StopSwarmAttack)
                      .findFirst()
                      .orElse(null);
                if (stopSwarmWeapon == null) {
                    LOGGER.error(
                          "Failed to find a Stop Swarm Weapon while Swarming a unit, which should not be possible.");
                } else {
                    miscPlan = new Vector<>();
                    miscPlan.add(new WeaponAttackAction(shooter.getId(),
                          shooter.getSwarmTargetId(),
                          shooter.getEquipmentNum(stopSwarmWeapon)));
                }
            }

            if (miscPlan == null) {
                // If we don't have any plans, let's consider unjamming our weaponry
                miscPlan = getFireControl(shooter).getUnjamWeaponPlan(shooter);
            }

            // if we didn't produce an "unjam weapon" plan, consider spotting and lighting
            // things up with a searchlight
            if (miscPlan.isEmpty()) {
                EntityAction spotAction = getFireControl(shooter).getSpotAction(null, shooter, fireControlState);
                if (spotAction != null) {
                    miscPlan.add(spotAction);
                }

                SearchlightAttackAction searchLightAction = getFireControl(shooter).getSearchLightAction(shooter, null);
                if (searchLightAction != null) {
                    miscPlan.add(searchLightAction);
                }
            }

            // if we have absolutely nothing else to do, see if we can find a club
            if (miscPlan.isEmpty()) {
                FindClubAction findClubAction = getFireControl(shooter).getFindClubAction(shooter);
                if (findClubAction != null) {
                    miscPlan.add(findClubAction);
                }
            }

            sendAttackData(shooter.getId(), miscPlan);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            // Don't lock up, just skip this entity.
            Vector<EntityAction> fallback = new Vector<>();
            sendAttackData(shooter.getId(), fallback);
        }
    }

    /**
     * Calculates the targeting/ off board turn This includes firing TAG and non-direct-fire artillery
     */
    @Override
    protected void calculateTargetingOffBoardTurn() {
        Entity entityToFire = getGame().getFirstEntity(getMyTurn());

        // if we're crippled, off-board and can do so, disengage
        if (entityToFire.isOffBoard() &&
              entityToFire.canFlee(entityToFire.getPosition()) &&
              entityToFire.isCrippled(true)) {
            Vector<EntityAction> disengageVector = new Vector<>();
            disengageVector.add(new DisengageAction(entityToFire.getId()));
            sendAttackData(entityToFire.getId(), disengageVector);
            sendDone(true);
            return;
        }

        FiringPlan firingPlan = getArtilleryTargetingControl().calculateIndirectArtilleryPlan(entityToFire,
              getGame(),
              this);

        if (!firingPlan.getEntityActionVector().isEmpty()) {
            sendAttackData(entityToFire.getId(), firingPlan.getEntityActionVector());
        } else {
            if (fireControls == null) {
                initializeFireControls();
            }
            sendAttackData(entityToFire.getId(), getFireControl(entityToFire).getUnjamWeaponPlan(entityToFire));
        }
        sendDone(true);
    }

    protected Map<WeaponMounted, Double> calcAmmoConservation(final Entity shooter) {
        final double aggroFactor = getBehaviorSettings().getHyperAggressionIndex();
        final StringBuilder msg = new StringBuilder("\nCalculating ammo conservation for ").append(shooter.getDisplayName());
        msg.append("\nAggression Factor = ").append(aggroFactor);

        try {
            final Map<AmmoType, Integer> ammoCounts = new HashMap<>();
            msg.append("\nPooling Ammo:");
            for (final Mounted<?> ammo : shooter.getAmmo()) {
                final AmmoType ammoType = (AmmoType) ammo.getType();
                msg.append("\n\t").append(ammoType);
                if (ammoCounts.containsKey(ammoType)) {
                    ammoCounts.put(ammoType, ammoCounts.get(ammoType) + ammo.getUsableShotsLeft());
                    msg.append(" + ").append(ammo.getUsableShotsLeft()).append(" = ").append(ammoCounts.get(ammoType));
                    continue;
                }
                ammoCounts.put(ammoType, ammo.getUsableShotsLeft());
                msg.append(" + ").append(ammo.getUsableShotsLeft()).append(" = ").append(ammoCounts.get(ammoType));
            }

            final Map<WeaponMounted, Double> ammoConservation = new HashMap<>();
            msg.append("\nCalculating conservation for each weapon");
            for (final WeaponMounted weapon : shooter.getWeaponList()) {
                final WeaponType weaponType = weapon.getType();
                msg.append("\n\t").append(weapon);
                if (!(weaponType instanceof AmmoWeapon)) {
                    // Just require a 12 or lower TN
                    ammoConservation.put(weapon, 0.01);
                    msg.append(" doesn't use ammo.");
                    continue;
                } else if (weaponType.hasFlag(WeaponType.F_ONE_SHOT)) {
                    // Shoot OS weapons on a 10 / 9 / 8 for Aggro 10 / 5 / 0
                    ammoConservation.put(weapon, (35 - 2.0 * aggroFactor) / 100.0);
                    msg.append(" One Shot weapon.");
                    continue;
                }

                int ammoCount = 0;
                for (final AmmoType ammoType : ammoCounts.keySet()) {
                    if (!AmmoType.isAmmoValid(ammoType, weaponType)) {
                        continue;
                    }
                    ammoCount += ammoCounts.get(ammoType);
                }
                msg.append(" has ").append(ammoCount).append(" shots left");
                // Desired behavior, with 7 / 3 / 1 rounds left:
                // At min aggro (0 of 10), fire on TN 10, 9, 7
                // At normal aggro (5 of 10), fire on 12, 11, 10
                // At max aggro (10 of 10), fire on 12, 12, 10
                final double toHitThreshold = Math.max(0.01,
                      (0.6 / ((8 * aggroFactor) + 4) +
                            4.0 / (4 * (ammoCount * ammoCount) * (aggroFactor + 2) + (4 / (aggroFactor + 1)))));
                msg.append("; To Hit Threshold = ").append(new DecimalFormat("0.000").format(toHitThreshold));
                ammoConservation.put(weapon, toHitThreshold);
            }

            return ammoConservation;
        } finally {
            LOGGER.debug(msg.toString());
        }
    }

    /**
     * Worker method that calculates a point blank shot action vector given a firing entity ID and a target ID.
     *
     * @param firingEntityID the ID of the entity taking the point blank shot
     * @param targetID       the ID of the entity being shot at potentially
     */
    @Override
    protected Vector<EntityAction> calculatePointBlankShot(int firingEntityID, int targetID) {
        Entity shooter = getGame().getEntity(firingEntityID);
        Targetable target = getGame().getEntity(targetID);
        if ((shooter == null) || (target == null)) {
            return new Vector<>();
        }

        final FiringPlanCalculationParameters firingPlanCalculationParameters = new Builder().buildExact(shooter,
              target,
              calcAmmoConservation(shooter));
        FiringPlan plan = getFireControl(shooter).determineBestFiringPlan(firingPlanCalculationParameters);
        getFireControl(shooter).loadAmmo(shooter, plan);
        plan.sortPlan();

        return plan.getEntityActionVector();
    }

    /**
     * Calculates the move index for the given unit. In general, faster units and units closer to the enemy should move
     * before others. Additional modifiers for being prone, stealth-ed, unit type and so on are also factored in.
     *
     * @param entity The unit to be indexed.
     *
     * @return The movement index of this unit. May be positive or negative. Higher index values should move first.
     */
    double calculateMoveIndex(final Entity entity, final StringBuilder msg) {
        final double PRIORITY_PRONE = 1.1;
        final double PRIORITY_TANK = 1.5;
        final double PRIORITY_BA = 2;
        final double PRIORITY_INF = 3;
        final double PRIORITY_FALLBACK = 2;
        final double PRIORITY_COMMANDER = 0.5;
        final double PRIORITY_CIVILIAN = 5;
        final double PRIORITY_STEALTH = 1.0 / 3;

        msg.append("\n\tCalculating move index for ").append(entity.getDisplayName());
        final StringBuilder modifiers = new StringBuilder();
        final NumberFormat numberFormat = DecimalFormat.getInstance();
        double total = 0;
        try {
            // Find out how fast this unit can move.
            int fastestMove = entity.getRunMP(MPCalculationSetting.STANDARD);
            if (entity.getAnyTypeMaxJumpMP() > fastestMove) {
                fastestMove = entity.getAnyTypeMaxJumpMP();
            }
            msg.append("\n\t\tFastest Move = ").append(fastestMove);

            // Get the distance to the nearest enemy.
            final double distance = getPathRanker(entity).distanceToClosestEnemy(entity, entity.getPosition(), game);

            msg.append("\n\t\tDistance to Nearest Enemy: ").append(numberFormat.format(distance));

            // Get the ratio of distance to speed.
            // Faster units that are closer to the enemy should move later.
            if (0 == fastestMove) {
                // This unit should have already moved due to the isImmobilized check.
                total = distance * 2;
            } else {
                total = distance / fastestMove;
            }
            msg.append("\n\t\tDistance to Move Ratio (dist / move): ").append(numberFormat.format(total));

            // Prone enemies move sooner.
            if (entity.isProne()) {
                total *= PRIORITY_PRONE;
                modifiers.append("\tx1.1 (Is Prone)");
            }

            // If all else is equal, Infantry before Battle Armor before Tanks before Meks.
            if (entity instanceof BattleArmor) {
                total *= PRIORITY_BA;
                modifiers.append("\tx2.0 (is BA)");
            } else if (entity instanceof Infantry) {
                total *= PRIORITY_INF;
                modifiers.append("\tx3.0 (is Inf)");
            } else if (entity instanceof Tank) {
                total *= PRIORITY_TANK;
                modifiers.append("\tx1.5 (is Tank)");
            }

            // Fleeing entities should move before those not fleeing.
            if (isFallingBack(entity)) {
                total *= PRIORITY_FALLBACK;
                modifiers.append("\tx2.0 (is Fleeing)");
            }

            // Move commanders after other units.
            if (entity.isCommander()) {
                total *= PRIORITY_COMMANDER;
                modifiers.append("\tx0.5 (is Commander)");
            }

            // Move civilian units before military.
            if (!entity.isMilitary()) {
                total *= PRIORITY_CIVILIAN;
                modifiers.append("\tx5.0 (is Civilian)");
            }

            // Move stealthy units later.
            if (entity.isStealthActive() || entity.isStealthOn() || entity.isVoidSigActive() || entity.isVoidSigOn()) {
                total *= PRIORITY_STEALTH;
                modifiers.append("\tx1/3 (is Stealth-ed)");
            }

            return total;
        } finally {
            msg.append("\n\t\tModifiers:").append(modifiers);
            msg.append("\n\t\tTotal = ").append(numberFormat.format(total));
        }
    }


    // Enhanced targeting controls

    public boolean getEnhancedTargetingControl() {
        return enableEnhancedTargeting;
    }

    public void setEnableEnhancedTargeting(boolean newSetting) {
        enableEnhancedTargeting = newSetting;
    }

    /**
     * Sets all enhanced targeting controls to default values and optionally enables its use
     *
     * @param enable true to immediately enable enhanced targeting features after reset
     */
    public void resetEnhancedTargeting(boolean enable) {

        // Toggle enhanced targeting
        enableEnhancedTargeting = enable;

        // Set default enhanced targeting target and attacker types
        enhancedTargetingTargetTypes = new ArrayList<>(List.of(UnitType.MEK));
        enhancedTargetingAttackerTypes = new ArrayList<>(Arrays.asList(UnitType.MEK,
              UnitType.TANK,
              UnitType.BATTLE_ARMOR,
              UnitType.INFANTRY,
              UnitType.PROTOMEK,
              UnitType.VTOL,
              UnitType.GUN_EMPLACEMENT));

        // Set default as not using called shots against immobile targets
        useCalledShotsOnImmobileTarget = false;

        // Set default as not allowing enhanced targeting if the target has partial cover.
        // This prevents all sorts of issues, such as aiming for locations that are covered.
        allowCoverEnhancedTargeting = false;

    }

    /**
     * Swap out current set of valid enhanced targeting target types for a new set. Automatically removes certain types
     * that will never apply, such as infantry.
     *
     * @param newTargetTypes List of {@link UnitType} constants, may be empty or null to clear
     */
    public void setEnhancedTargetingTargetTypes(List<Integer> newTargetTypes) {
        enhancedTargetingTargetTypes = Objects.requireNonNullElseGet(newTargetTypes, ArrayList::new);
        if (enhancedTargetingTargetTypes.contains(UnitType.INFANTRY)) {
            enhancedTargetingTargetTypes.remove(UnitType.INFANTRY);
        }
        if (enhancedTargetingTargetTypes.contains(UnitType.BATTLE_ARMOR)) {
            enhancedTargetingTargetTypes.remove(UnitType.BATTLE_ARMOR);
        }
    }

    /**
     * Swap out current set of valid enhanced targeting attacker types for a new set
     *
     * @param newAttackerTypes List of {@link UnitType} constants, may be empty or null to clear
     */
    public void setEnhancedTargetingAttackerTypes(List<Integer> newAttackerTypes) {
        enhancedTargetingAttackerTypes = Objects.requireNonNullElseGet(newAttackerTypes, ArrayList::new);
    }

    /**
     * Returns a copy of the list of valid enhanced targeting target types
     *
     * @return list of {@link UnitType} constants, or empty list
     */
    public List<Integer> seeEnhancedTargetingTargetTypes() {
        return new ArrayList<>(enhancedTargetingTargetTypes);
    }

    /**
     * Returns a copy of the list of valid enhanced targeting attacker types
     *
     * @return list of {@link UnitType} constants, or empty list
     */
    public List<Integer> seeEnhancedTargetingAttackerTypes() {
        return new ArrayList<>(enhancedTargetingAttackerTypes);
    }

    /**
     * Checks if the supplied unit type is considered a valid target for enhanced targeting
     *
     * @param testType {@link UnitType} constant
     *
     * @return true, if unit is a valid target for enhanced targeting
     */
    public boolean isValidEnhancedTargetingTarget(int testType) {
        if (enhancedTargetingTargetTypes != null) {
            return enhancedTargetingTargetTypes.contains(testType);
        } else {
            return false;
        }
    }

    /**
     * Checks if the supplied unit type is considered a valid attacker for enhanced targeting
     *
     * @param testType {@link UnitType} constant
     *
     * @return true, if unit is a valid attacker for enhanced targeting
     */
    public boolean isValidEnhancedTargetingAttacker(int testType) {
        if (enhancedTargetingAttackerTypes != null) {
            return enhancedTargetingAttackerTypes.contains(testType);
        } else {
            return false;
        }
    }

    public boolean getAllowCalledShotsOnImmobile() {
        return useCalledShotsOnImmobileTarget;
    }

    public void setAllowCalledShotsOnImmobile(boolean newSetting) {
        useCalledShotsOnImmobileTarget = newSetting;
    }

    public boolean getPartialCoverEnhancedTargeting() {
        return allowCoverEnhancedTargeting;
    }

    /**
     * Controls whether enhanced targeting will be used against targets with partial cover from the shooter. Use with
     * caution as this can result in situations like aiming for a location which is protected by intervening cover.
     *
     * @param newSetting true, to allow aimed/called shots against targets with partial cover
     */
    public void setPartialCoverEnhancedTargeting(boolean newSetting) {
        allowCoverEnhancedTargeting = newSetting;
    }

    /**
     * Determine if a shooter should consider using enhanced targeting - aimed or called shots - against a given target.
     * This includes some basic filtering for unit types and equipment such targeting computers.
     *
     * @param shooter    Entity doing the shooting
     * @param targetable Hex, Building, or Entity being shot at (Enhanced Targeting only works on the last one)
     * @param cover      {@link LosEffects} constant for partial cover, derived from {@code ToHitData.getCover()}
     *
     * @return true, if aimed or called shots should be checked
     */
    protected boolean checkForEnhancedTargeting(Entity shooter, Targetable targetable, int cover) {
        // Only works on entities
        if (!(targetable instanceof Entity target)) {
            return false;
        }

        if (!enableEnhancedTargeting) {
            return false;
        }

        // Partial cover adds all sorts of complications, don't bother unless enabled
        if (cover != LosEffects.COVER_NONE && !allowCoverEnhancedTargeting) {
            return false;
        }

        // Basic unit type filtering for shooter. Ejected crews are considered infantry, so need
        // to be specifically checked.
        if (!isValidEnhancedTargetingAttacker(shooter.getUnitType()) || shooter instanceof EjectedCrew) {
            return false;
        }

        if (!isValidEnhancedTargetingTarget(target.getUnitType())) {
            return false;
        }

        boolean useAimedShot = false;
        boolean useCalledShot = false;

        // Only certain unit types can be the target of aimed shots
        List<Integer> validAimTypes = new ArrayList<>(Arrays.asList(UnitType.MEK,
              UnitType.TANK,
              UnitType.VTOL,
              UnitType.CONV_FIGHTER,
              UnitType.AEROSPACE_FIGHTER));

        if (validAimTypes.contains(target.getUnitType())) {

            // Aimed shots are only possible if the target is immobile or the shooter has a
            // targeting computer
            if (target.isImmobile() || shooter.hasTargComp()) {
                useAimedShot = true;
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CALLED_SHOTS)) {
            // Called shots against immobile targets can be a little too effective, so only use
            // when enabled
            if (!target.isImmobile() || useCalledShotsOnImmobileTarget) {
                useCalledShot = true;
            }
        }

        return useAimedShot || useCalledShot;
    }

    /**
     * Determine which location to aim for on a general target. Returned location constant is relative to the provided
     * target type. Currently only supports aimed shots against Meks.
     *
     * @param target       Entity being shot at
     * @param planOfAttack Proposed attacks against {@code target}
     * @param rearAttack   true if attacking from rear arc
     * @param includeHead  true if the head is a valid location, ignored for non-Mek targets
     *
     * @return location constant to aim for, with the {@code LOC_NONE} constant indicating no suitable location
     */
    protected int getAimedShotLocation(Targetable target, FiringPlan planOfAttack, boolean rearAttack,
          boolean includeHead) {

        int aimLocation = Entity.LOC_NONE;
        if (planOfAttack == null || target == null) {
            return aimLocation;
        }

        // Only check attacks if the target has locations that can be aimed for, which can be
        // aimed, and are against the designated target
        List<Integer> validAimTypes = new ArrayList<>(Arrays.asList(UnitType.MEK,
              UnitType.TANK,
              UnitType.VTOL,
              UnitType.CONV_FIGHTER,
              UnitType.AEROSPACE_FIGHTER));

        if (!validAimTypes.contains(((Entity) target).getUnitType())) {
            return aimLocation;
        }

        List<WeaponFireInfo> workingShots = planOfAttack.stream()
              .filter(curShot -> curShot.getTarget().getId() == target.getId())
              .filter(curShot -> Compute.allowAimedShotWith(curShot.getWeapon(),
                    target.isImmobile() ?
                          AimingMode.IMMOBILE :
                          AimingMode.TARGETING_COMPUTER))
              .collect(Collectors.toList());

        if (workingShots.isEmpty()) {
            return aimLocation;
        }

        // Each type of unit requires its own checking process due to unique locations.
        // TODO: placeholders are used for non-Mek targets. Create appropriate methods for each.
        if (((Entity) target).getUnitType() == UnitType.MEK) {
            aimLocation = calculateAimedShotLocation((Mek) target, workingShots, rearAttack, includeHead);
        }

        return aimLocation;
    }

    /**
     * Determine which direction to make a called shot - left, right, high, or low. Some target types only support
     * calling shots left or right. Currently only supports called shots against Meks.
     *
     * @param target       Entity being shot at
     * @param attackSide   {@link ToHitData} SIDE_ constant, indicating attack direction relative to target
     * @param planOfAttack Proposed attacks against {@code target}
     *
     * @return {@link CalledShot} constant indicating which direction to call, may return
     *       {@code CalledShot.CALLED_NONE}.
     */
    protected int getCalledShotDirection(Targetable target, int attackSide, FiringPlan planOfAttack) {
        int calledShotDirection = CalledShot.CALLED_NONE;
        if (planOfAttack == null || target == null) {
            return calledShotDirection;
        }

        WeaponFireInfo primaryFire = planOfAttack.get(0);
        if (primaryFire == null) {
            return calledShotDirection;
        }

        // Limit the weapons fire to those against the designated target and which have a
        // reasonable to-hit number
        int maximumToHit = calcEnhancedTargetingMaxTN(false);
        List<WeaponFireInfo> workingShots = planOfAttack.stream()
              .filter(curShot -> curShot.getTarget().getId() == target.getId())
              .filter(curShot -> curShot.getToHit().getValue() +
                    CALLED_SHOT_MODIFIER <= maximumToHit)
              .collect(Collectors.toList());

        if (workingShots.isEmpty()) {
            return calledShotDirection;
        }

        // TODO: placeholder for non-Mek targets. Create appropriate methods for each.
        if (target instanceof Mek) {
            calledShotDirection = calculateCalledShotDirection((Mek) target, attackSide, workingShots);
        }

        return calledShotDirection;
    }

    /**
     * Determine which location to aim for on a Mek. Prioritizes torsos and legs, and ignores destroyed locations.
     * Prefers right to left, given that most non-symmetrical Meks are 'right-handed'.
     *
     * @param target      Mek being shot at
     * @param aimedShots  Proposed attacks against {@code target}
     * @param rearAttack  true if attacking from the rear arc
     * @param includeHead true to include the head as a valid location
     *
     * @return {@link Mek} constant for location to shoot, or {@code Mek.LOC_NONE} for none
     */
    protected int calculateAimedShotLocation(Mek target, List<WeaponFireInfo> aimedShots, boolean rearAttack,
          boolean includeHead) {
        int aimLocation = Mek.LOC_NONE;

        if (aimedShots == null || target == null) {
            return aimLocation;
        }

        WeaponFireInfo primaryFire = aimedShots.get(0);
        int lowestArmor = Integer.MAX_VALUE;
        List<Integer> rankedLocations = new ArrayList<>();

        // Aiming for the head can only be done against an immobile Mek, and takes a penalty.
        // Don't aim for the head for anti-Mek attacks except after swarming.
        if (includeHead &&
              target.isImmobile() &&
              !primaryFire.getWeapon().getShortName().equalsIgnoreCase(Infantry.LEG_ATTACK) &&
              !primaryFire.getWeapon().getShortName().equalsIgnoreCase(Infantry.SWARM_MEK) &&
              !primaryFire.getWeapon().getShortName().equalsIgnoreCase(Infantry.STOP_SWARM)) {
            aimLocation = Mek.LOC_HEAD;
            int headShotMaxTN = calcEnhancedTargetingMaxTN(false);
            if (aimedShots.stream()
                  .anyMatch(curFire -> curFire.getToHit().getValue() + IMMOBILE_HEAD_SHOT_MODIFIER >
                        headShotMaxTN)) {
                aimLocation = Mek.LOC_NONE;
            } else {
                return aimLocation;
            }
        }

        // Limit leg attack aimed shots to the legs
        if (!primaryFire.getWeapon().getShortName().equalsIgnoreCase(Infantry.LEG_ATTACK)) {

            // Consider arm locations if they have a 'big' weapon
            for (WeaponMounted curWeapon : target.getWeaponList()
                  .stream()
                  .filter(w -> w.isOperable() && isBigGun(w))
                  .collect(Collectors.toSet())) {

                if (!rankedLocations.contains(Mek.LOC_RIGHT_ARM) &&
                      curWeapon.getLocation() == Mek.LOC_RIGHT_ARM &&
                      target.getInternal(Mek.LOC_RIGHT_ARM) > 0) {
                    rankedLocations.add(Mek.LOC_RIGHT_ARM);
                } else if (!rankedLocations.contains(Mek.LOC_LEFT_ARM) &&
                      curWeapon.getLocation() == Mek.LOC_LEFT_ARM &&
                      target.getInternal(Mek.LOC_LEFT_ARM) > 0) {
                    rankedLocations.add(Mek.LOC_LEFT_ARM);
                }
                if (rankedLocations.contains(Mek.LOC_RIGHT_ARM) && rankedLocations.contains(Mek.LOC_LEFT_ARM)) {
                    break;
                }

            }

            // Most Mek designs will have their main weapon in either the right torso or right arm,
            // so going after the right torso first solves both conditions. Putting the right torso
            // first ensures the left torso and other locations will only supersede it if they have
            // taken more damage and make for a better target.
            if (target.getInternal(Mek.LOC_RIGHT_TORSO) > 0) {
                rankedLocations.add(Mek.LOC_RIGHT_TORSO);
            } else if (target.getInternal(Mek.LOC_LEFT_TORSO) > 0) {
                rankedLocations.add(Mek.LOC_LEFT_TORSO);
            }

            if (!rankedLocations.contains(Mek.LOC_LEFT_TORSO)) {
                if (target.getInternal(Mek.LOC_LEFT_TORSO) > 0) {
                    rankedLocations.add((Mek.LOC_LEFT_TORSO));
                }
            }

            rankedLocations.add(Mek.LOC_CENTER_TORSO);
        }

        // Favor right leg over left due to damage transfer to right torso, except if right leg is
        // completely gone
        if (target.getInternal(Mek.LOC_RIGHT_LEG) > 0) {
            rankedLocations.add(Mek.LOC_RIGHT_LEG);
        } else if (target.getInternal(Mek.LOC_LEFT_LEG) > 0) {
            rankedLocations.add(Mek.LOC_LEFT_LEG);
        }

        if (!rankedLocations.contains(Mek.LOC_LEFT_LEG)) {
            if (target.getInternal(Mek.LOC_LEFT_LEG) > 0) {
                rankedLocations.add(Mek.LOC_LEFT_LEG);
            }
        }

        // Select the most vulnerable location
        int locationDestruction = 0;
        for (int curLocation : rankedLocations) {
            int locationArmor = Math.max(target.hasRearArmor(curLocation) ?
                  target.getArmor(curLocation, rearAttack) :
                  target.getArmor(curLocation), 0);

            if (target.getInternal(curLocation) > 0 &&
                  (lowestArmor > locationArmor ||
                        locationDestruction > locationArmor + target.getInternal(curLocation))) {

                aimLocation = curLocation;
                lowestArmor = locationArmor;
                locationDestruction = lowestArmor + target.getInternal(aimLocation);

            }

            // Doesn't get any better than a torso with no armor
            if (lowestArmor == 0 &&
                  (aimLocation == Mek.LOC_RIGHT_TORSO
                        || aimLocation == Mek.LOC_LEFT_TORSO
                        || aimLocation == Mek.LOC_CENTER_TORSO)) {
                break;
            }
        }
        // Evaluate whether all the weapons at the chosen location will be effective
        if (aimLocation != Mek.LOC_NONE && (!target.isImmobile() || aimLocation == Mek.LOC_HEAD)) {

            int offset = 0;
            if (locationDestruction <= LOCATION_DESTRUCTION_THREAT) {
                offset = 1;
            }

            int penetratorCount = 0;
            double totalDamage = 0;
            int maximumToHit = calcEnhancedTargetingMaxTN(target.isImmobile() && aimLocation != Mek.LOC_HEAD);
            for (WeaponFireInfo curFire : aimedShots) {
                if (curFire.getToHit().getValue() +
                      (aimLocation == Mek.LOC_HEAD ? IMMOBILE_HEAD_SHOT_MODIFIER : AIMED_SHOT_MODIFIER) <=
                      (maximumToHit + offset)) {

                    totalDamage += curFire.getMaxDamage();
                    if (curFire.getMaxDamage() >= lowestArmor) {
                        penetratorCount++;
                    }

                }
            }

            // If none of the weapons have a low enough to-hit number, or if none of the weapons
            // can penetrate the armor individually or cumulatively, don't bother aiming
            if (totalDamage == 0 || (penetratorCount == 0 && 0.4 * totalDamage < lowestArmor)) {
                aimLocation = Mek.LOC_NONE;
            }

        }

        return aimLocation;
    }

    /**
     * Determine which direction to make a called shot against a Mek - left, right, high, or low. Shots into a side arc
     * will be called to become rear shots. Shots to the front or rear will call high or low based on how many locations
     * have minimal armor.
     *
     * @param target      Mek being shot at
     * @param attackSide  {@link ToHitData} SIDE_ constant, indicating attack direction relative to target
     * @param calledShots Proposed attacks against {@code target} parameter
     *
     * @return {@link CalledShot} constant indicating which direction to call, may return
     *       {@code CalledShot.CALLED_NONE}.
     */
    protected int calculateCalledShotDirection(Mek target, int attackSide, List<WeaponFireInfo> calledShots) {
        int calledShotDirection = CalledShot.CALLED_NONE;

        if (calledShots == null || calledShots.isEmpty() || calledShots.get(0) == null) {
            return calledShotDirection;
        }

        WeaponFireInfo primaryFire = calledShots.get(0);

        // If the target is being shot in a side arc, set the call direction to hit the rear arc
        if (attackSide == ToHitData.SIDE_LEFT || attackSide == ToHitData.SIDE_REAR_LEFT) {
            calledShotDirection = CalledShot.CALLED_RIGHT;
        } else if (attackSide == ToHitData.SIDE_RIGHT || attackSide == ToHitData.SIDE_REAR_RIGHT) {
            calledShotDirection = CalledShot.CALLED_LEFT;
        }

        if (attackSide == ToHitData.SIDE_FRONT || attackSide == ToHitData.SIDE_REAR) {

            List<Integer> upperLocations = new ArrayList<>(Arrays.asList(Mek.LOC_RIGHT_TORSO,
                  Mek.LOC_LEFT_TORSO,
                  Mek.LOC_CENTER_TORSO));

            // Only consider the arms if they have 'big' weapons
            for (WeaponMounted curWeapon : target.getWeaponList()
                  .stream()
                  .filter(w -> w.isOperable() && isBigGun(w))
                  .collect(Collectors.toSet())) {

                if (!upperLocations.contains(Mek.LOC_RIGHT_ARM) &&
                      curWeapon.getLocation() == Mek.LOC_RIGHT_ARM &&
                      target.getInternal(Mek.LOC_RIGHT_ARM) > 0) {
                    upperLocations.add(Mek.LOC_RIGHT_ARM);
                } else if (!upperLocations.contains(Mek.LOC_LEFT_ARM) &&
                      curWeapon.getLocation() == Mek.LOC_LEFT_ARM &&
                      target.getInternal(Mek.LOC_LEFT_ARM) > 0) {
                    upperLocations.add(Mek.LOC_LEFT_ARM);
                }
                if (upperLocations.contains(Mek.LOC_RIGHT_ARM) && upperLocations.contains(Mek.LOC_LEFT_ARM)) {
                    break;
                }

            }

            // Establish a maximum armor value for calling shots high/low. If most
            // of the locations have more armor than this, it's not a good option.
            // Infantry and battle armor weapons rely on many small hits, so use
            // a default value.
            int armorThreshold;
            Entity shooter = primaryFire.getShooter();
            if (!shooter.isInfantry()) {
                OptionalDouble averageDamage = calledShots.stream().mapToDouble(WeaponFireInfo::getMaxDamage).average();
                if (averageDamage.isPresent()) {
                    armorThreshold = (int) Math.floor(averageDamage.getAsDouble());
                } else {
                    armorThreshold = CALLED_SHOT_DEFAULT_MAX_ARMOR;
                }
            } else {
                armorThreshold = CALLED_SHOT_DEFAULT_MAX_ARMOR;
            }

            double upperTargets = upperLocations.stream()
                  .mapToInt(loc -> loc)
                  .filter(loc -> target.getArmor(loc, attackSide == ToHitData.SIDE_REAR) <=
                        armorThreshold)
                  .count();

            // Only consider shooting low if both legs are intact
            double lowerTargets = 0;
            if (target.getInternal(Mek.LOC_RIGHT_LEG) > 0 && target.getInternal(Mek.LOC_LEFT_LEG) > 0) {
                if (target.getArmor(Mek.LOC_RIGHT_LEG) <= armorThreshold) {
                    lowerTargets++;
                }
                if (target.getArmor(Mek.LOC_LEFT_LEG) <= armorThreshold) {
                    lowerTargets++;
                }
            }

            // If the head armor is weak or there are proportionally more upper targets, call high.
            // If the leg armor is weak and this is a fast and/or jumping Mek, call low.
            if (target.getArmor(Mek.LOC_HEAD) + target.getInternal(Mek.LOC_HEAD) <= LOCATION_DESTRUCTION_THREAT ||
                  (upperTargets / upperLocations.size() > lowerTargets / 2.0)) {
                calledShotDirection = CalledShot.CALLED_HIGH;
            } else if (lowerTargets >= 1 ||
                  target.getWalkMP() >= CALLED_SHOT_MIN_MOVE ||
                  target.getAnyTypeMaxJumpMP() >= CALLED_SHOT_MIN_JUMP) {
                calledShotDirection = CalledShot.CALLED_LOW;
            }

        }

        return calledShotDirection;
    }


    /**
     * Checks if a weapon is considered a 'big gun' worth taking a shot at
     *
     * @param testWeapon weapon to check
     *
     * @return true if weapon damage exceeds {@code BIG_GUN_MIN_DAMAGE} at a typical range value
     */
    private boolean isBigGun(WeaponMounted testWeapon) {
        return testWeapon.getType().getDamage(BIG_GUN_TYPICAL_RANGE) >= BIG_GUN_MIN_DAMAGE;
    }

    /**
     * Figure out the highest practical to-hit number for enhanced aiming (aimed/called shots), using behavior settings
     *
     * @param isAimedImmobile true if making aimed shot at immobile target
     *
     * @return maximum to-hit number for a weapons attack with enhanced aiming
     */
    private int calcEnhancedTargetingMaxTN(boolean isAimedImmobile) {
        if (isAimedImmobile) {
            return SHUTDOWN_MAX_TARGET_NUMBER;
        } else {
            return Math.max(10 - getBehaviorSettings().getSelfPreservationIndex(), 2);
        }
    }


    /**
     * If a shot meets criteria, set it as aimed or called.  {@code aimLocation} and {@code calledShotDirection} are not
     * mutually exclusive - if both are provided, weapons which cannot make an aimed shot will make a called shot
     * instead
     *
     * @param shot                 Single-weapon attack action
     * @param aimLocation          {@link Mek} LOC_ constant with aiming location
     * @param destructionThreshold how much damage to completely destroy the location
     */
    protected void setAttackAsAimedOrCalled(WeaponFireInfo shot, int aimLocation, int calledShotDirection,
          int destructionThreshold) {
        Entity shooter = shot.getShooter();

        int offset = 0;

        // If the target is a Mek and the attack is not artillery or non-damaging anti-Mek
        if (shot.getTarget().getTargetType() == UnitType.MEK &&
              !shot.getWeapon().getType().hasFlag(WeaponType.F_ARTILLERY) &&
              !shot.getWeapon().getShortName().equalsIgnoreCase(Infantry.SWARM_MEK) &&
              !shot.getWeapon().getShortName().equalsIgnoreCase(Infantry.STOP_SWARM)) {

            Mek target = (Mek) shot.getTarget();
            int maximumTN;

            // If set for aimed shots, and the weapon can make aimed shots
            if ((aimLocation != Mek.LOC_NONE) &&
                  Compute.allowAimedShotWith(shot.getWeapon(),
                        target.isImmobile() ? AimingMode.IMMOBILE : AimingMode.TARGETING_COMPUTER)) {

                maximumTN = calcEnhancedTargetingMaxTN(target.isImmobile() && aimLocation != Mek.LOC_HEAD);

                // Increase the maximum target number for attacks that may destroy the location,
                // as well as infantry weapons which may have multiple hits per shot
                if ((!shooter.isInfantry() && shot.getMaxDamage() >= destructionThreshold) ||
                      shot.getWeapon().getType().hasFlag(WeaponType.F_INFANTRY)) {
                    offset = 1;
                }

                // If the target number is considered viable set attack as aimed
                // at the provided location
                if ((shot.getToHit().getValue() + (target.isImmobile() ? 0 : AIMED_SHOT_MODIFIER)) <=
                      (maximumTN + offset)) {
                    shot.getAction()
                          .setAimingMode(target.isImmobile() ? AimingMode.IMMOBILE : AimingMode.TARGETING_COMPUTER);
                    shot.getAction().setAimedLocation(aimLocation);
                }

            } else if (calledShotDirection != CalledShot.CALLED_NONE) {

                maximumTN = calcEnhancedTargetingMaxTN(false);

                // If the weapon uses the cluster table, increase the maximum target number
                if (shot.getWeapon().getType().getDamage() == WeaponType.DAMAGE_BY_CLUSTER_TABLE ||
                      (shot.getAmmo() != null &&
                            shot.getAmmo().getType().getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) ||
                      shot.getWeapon().getType().hasFlag(WeaponType.F_INFANTRY)) {
                    offset = 2;
                }

                // If the target number is considered viable, step through the options until
                // it gets to the desired setting
                if ((shot.getToHit().getValue() + CALLED_SHOT_MODIFIER) <= (maximumTN + offset)) {
                    // TODO: adjust send/receive method to transmit new called shot rather than stepping through
                    for (int i = 0; i < calledShotDirection; i++) {
                        sendCalledShotChange(shooter.getId(), shot.getWeaponAttackAction().getWeaponId());
                    }
                }

            }

        }

    }


    /**
     * Gets an entity eligible to fire from a list contained in the fire control state.
     */
    Entity getEntityToFire(FireControlState fireControlState) {
        if (fireControlState.getOrderedFiringEntities().isEmpty()) {
            initFiringEntities(fireControlState);
        }

        // if, even after initializing entities, we have no valid entities
        // we'll let the game determine
        if (fireControlState.getOrderedFiringEntities().isEmpty()) {
            return game.getFirstEntity(getMyTurn());
        }

        Entity entityToReturn = fireControlState.getOrderedFiringEntities().getFirst();
        fireControlState.getOrderedFiringEntities().removeFirst();
        return entityToReturn;
    }

    /**
     * Sorts firing entities to ensure that entities that can do indirect fire go after entities that cannot, so that
     * IDF units go after spotting units.
     */
    private void initFiringEntities(FireControlState fireControlState) {
        List<Entity> myEntities = game.getPlayerEntities(getLocalPlayer(), true);
        fireControlState.clearOrderedFiringEntities();

        for (Entity entity : myEntities) {
            // if you can't fire, you can't fire.
            if (!getMyTurn().isValidEntity(entity, game)) {
                continue;
            }

            if (getFireControl(entity).entityCanIndirectFireMissile(fireControlState, entity)) {
                fireControlState.getOrderedFiringEntities().addLast(entity);
            } else {
                fireControlState.getOrderedFiringEntities().addFirst(entity);
            }
        }
    }

    /**
     * Loops through the list of entities controlled by this Princess instance and decides which should be moved first.
     * Immobile units and ejected MekWarriors / crews will be moved first. After that, each unit is given an index//
     * This unit should have already moved due to the isImmobilized check. via the
     * {@link #calculateMoveIndex(Entity, StringBuilder)} method.  The highest index value is moved first.
     *
     * @return The entity that should be moved next.
     */
    Entity getEntityToMove() {

        // first move useless units: immobile units, ejected MekWarrior, etc
        Entity movingEntity = null;
        final List<Entity> myEntities = getEntitiesOwned();
        double highestIndex = -Double.MAX_VALUE;
        final StringBuilder msg = new StringBuilder("Deciding who to move next.");
        for (final Entity entity : myEntities) {
            msg.append("\n\tUnit ").append(entity.getDisplayName());

            if (entity.isDone()) {
                msg.append("has already moved this phase");
                continue;
            }

            if (!getGame().getPhase().isSimultaneous(getGame()) &&
                  (entity.isOffBoard() ||
                        (null == entity.getPosition()) ||
                        entity.isUnloadedThisTurn() ||
                        !Objects.requireNonNull(getGame().getTurn()).isValidEntity(entity, getGame()))) {
                msg.append("cannot be moved.");
                continue;
            }

            // Move immobile units & ejected MekWarriors immediately.
            if (isImmobilized(entity) && !(entity instanceof Infantry)) {
                msg.append("is immobile.");
                movingEntity = entity;
                break;
            }

            if (entity instanceof EjectedCrew) {
                msg.append("is ejected crew.");
                movingEntity = entity;
                break;
            }

            // can't do anything with out-of-control aero's, so use them as init sinks
            if (entity.isAero() && ((IAero) entity).isOutControlTotal()) {
                msg.append("is out-of-control aero.");
                movingEntity = entity;
                break;
            }

            // If I only have 1 unit, no need to calculate an index.
            if (1 == myEntities.size()) {
                msg.append("is my only unit.");
                movingEntity = entity;
                break;
            }

            // We will move the entity with the highest index.
            final double moveIndex = calculateMoveIndex(entity, msg);
            msg.append("\n\thas index ").append(moveIndex).append(" vs ").append(highestIndex);
            if (moveIndex >= highestIndex) {
                highestIndex = moveIndex;
                movingEntity = entity;
            }
        }

        if (movingEntity == null) {
            LOGGER.warn(msg.toString());
        } else {
            LOGGER.debug(msg.toString());
        }

        return movingEntity;
    }

    @Override
    protected @Nullable MovePath calculateMoveTurn() {
        try {
            MovePath path = continueMovementFor(getEntityToMove());
            // Update the friendly heat map with movement of ground units
            if (path != null && path.getEntity().isGround()) {
                friendlyHeatMap.updateTrackers(path);
            }
            return path;
        } catch (Exception ignored) {
            LOGGER.error("Error while calculating movement");
            return null;
        }
    }

    @Override
    protected @Nullable PhysicalOption calculatePhysicalTurn() {
        try {
            initialize();
            // get the first entity that can act this turn
            final Entity attacker = game.getFirstEntity(getMyTurn());

            // If my unit is forced to withdraw, don't attack unless I've been
            // attacked.
            if (getForcedWithdrawal() && attacker.isCrippled()) {
                final StringBuilder msg = new StringBuilder(attacker.getDisplayName()).append(
                      " is crippled and withdrawing.");
                if (attackedWhileFleeing.contains(attacker.getId())) {
                    msg.append("\n\tBut I was fired on, so I will hit back.");
                } else {
                    msg.append("\n\tI will not attack so long as I'm not fired on.");
                    return null;
                }
                LOGGER.info(msg.toString());
            }

            // the original bot's physical options seem superior
            return PhysicalCalculator.getBestPhysical(attacker, game, getBehaviorSettings(), getHonorUtil());
        } catch (Exception ignored) {
            return null;
        }
    }

    boolean wantsToFallBack(final Entity entity) {
        return (entity.isCrippled() && getForcedWithdrawal()) || getFallBack();
    }

    MoraleUtil getMoraleUtil() {
        return moraleUtil;
    }

    /**
     * Logic to determine if this entity is "falling back" for any reason
     *
     * @param entity The entity to check.
     *
     * @return Whether or not the entity is falling back.
     */
    boolean isFallingBack(final Entity entity) {
        return (getBehaviorSettings().shouldAutoFlee() ||
              (getBehaviorSettings().isForcedWithdrawal() && entity.isCrippled(true)));
    }

    /**
     * Logic to determine if this entity is in a state where it can shoot due to being attacked while fleeing.
     *
     * @param entity Entity to check.
     *
     * @return Whether or not this entity can shoot while falling back.
     */
    boolean canShootWhileFallingBack(Entity entity) {
        return attackedWhileFleeing.contains(entity.getId());
    }

    boolean mustFleeBoard(final Entity entity) {
        if (!isFallingBack(entity)) {
            return false;
        } else if (!entity.canFlee(entity.getPosition())) {
            return false;
        } else if (0 < getPathRanker(entity).distanceToHomeEdge(entity.getPosition(), entity.getBoardId(),
              getHomeEdge(entity), getGame())) {
            return false;
        } else {return getFleeBoard() || entity.isCrippled() && getForcedWithdrawal();}
    }

    boolean isImmobilized(final Entity mover) {
        if (mover.isImmobile() && !mover.isShutDown()) {
            LOGGER.info("Is truly immobile.");
            return true;
        } else if (1 > mover.getRunMP()) {
            LOGGER.info("Has 0 movement.");
            return true;
        } else if (!(mover instanceof Mek)) {
            return false;
        }

        final Mek mek = (Mek) mover;
        if (!mek.isProne() && !mek.isStuck() && !mek.isStalled()) {
            return false;
        }

        final MovePath movePath = new MovePath(getGame(), mover);

        // For a normal fall-shame setting (index 5), our threshold should be
        // a 10+ piloting roll.
        final int threshold = switch (getBehaviorSettings().getFallShameIndex()) {
            case 10 -> 7;
            case 9 -> 8;
            case 8, 7 -> 9;
            case 6, 5 -> 10;
            case 4 -> 11;
            case 3 -> 12;
            default -> 13; // Actually impossible.
        };

        // If we're prone, see if we have a chance of getting up.
        if (mek.isProne()) {
            if (mek.cannotStandUpFromHullDown()) {
                LOGGER.info("Cannot stand up.");
                return true;
            }

            final MoveStepType type = getBooleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_CAREFUL_STAND) ?
                  MoveStepType.CAREFUL_STAND :
                  MoveStepType.GET_UP;
            final MoveStep getUp = new MoveStep(movePath, type);

            // If our odds to get up are equal to or worse than the threshold,
            // consider ourselves immobile.
            final PilotingRollData target = mek.checkGetUp(getUp, movePath.getLastStepMovementType());
            LOGGER.info("Need to roll {} to stand and our tolerance is {}", target.getValue(), threshold);
            return (target.getValue() >= threshold);
        }

        // How likely are we to get unstuck.
        final MoveStepType type = MoveStepType.FORWARDS;
        final MoveStep walk = new MoveStep(movePath, type);
        final Hex hex = getGame().getHexOf(mek);
        final PilotingRollData target = mek.checkBogDown(walk,
              movePath.getLastStepMovementType(),
              hex,
              mek.getPriorPosition(),
              mek.getPosition(),
              hex.getLevel(),
              false);
        LOGGER.info("Need to roll {} to get unstuck and our tolerance is {}", target.getValue(), threshold);
        return (target.getValue() >= threshold);
    }

    boolean getBooleanOption(final String name) {
        return getGame().getOptions().booleanOption(name);
    }

    @Override
    protected MovePath continueMovementFor(final Entity entity) {
        Objects.requireNonNull(entity, "Entity is null.");

        try {
            // figure out who moved last, and whose move lists need to be updated

            // moves this entity during movement phase
            LOGGER.debug("Moving {} (ID {})", entity.getDisplayName(), entity.getId());
            getPrecognition().ensureUpToDate();

            if (isFallingBack(entity)) {
                String msg = entity.getDisplayName();
                if (getFallBack()) {
                    msg += " is falling back.";
                } else if (entity.isCrippled()) {
                    msg += " is crippled and withdrawing.";
                }
                LOGGER.debug(msg);
                sendChat(msg, Level.ERROR);

                // If this entity is falling back, able to flee the board, on
                // its home edge, and must flee, do so.
                if (mustFleeBoard(entity)) {
                    final MovePath mp = new MovePath(game, entity);
                    mp.addStep(MoveStepType.FLEE);
                    return mp;
                }

                // If we want to flee, but cannot, eject the crew.
                if (isImmobilized(entity) && entity.isEjectionPossible()) {
                    msg = entity.getDisplayName() + " is immobile. Abandoning unit.";
                    LOGGER.info(msg);
                    sendChat(msg, Level.ERROR);
                    final MovePath mp = new MovePath(game, entity);
                    mp.addStep(MoveStepType.EJECT);
                    return mp;
                }
            }

            final List<MovePath> paths = getMovePathsAndSetNecessaryTargets(entity, false);

            if (null == paths) {
                LOGGER.warn("No valid paths found.");
                return performPathPostProcessing(new MovePath(game, entity), 0);
            }

            final double thisTimeEstimate = (paths.size() * moveEvaluationTimeEstimate) / 1e3;
            if (LOGGER.getLevel().isLessSpecificThan(Level.INFO)) {
                final String message = getMessage(entity, thisTimeEstimate, paths);
                sendChat(message);
            }

            final long startTime = java.lang.System.currentTimeMillis();
            getPathRanker(entity).initUnitTurn(entity, getGame());
            // fall tolerance range between 0.50 and 1.0
            final double fallTolerance = getBehaviorSettings().getFallShameIndex() / 20d + 0.50d;

            final TreeSet<RankedPath> rankedPaths = getPathRanker(entity).rankPaths(paths,
                  getGame(),
                  getMaxWeaponRange(entity),
                  fallTolerance,
                  getEnemyEntities(),
                  getBehaviorSettings().isExclusiveHerding() ? getEntitiesOwned() : getFriendEntities());

            final long stop_time = java.lang.System.currentTimeMillis();

            // update path evaluation time estimate
            final double updatedEstimate = ((double) (stop_time - startTime)) / ((double) paths.size());
            if (0 == moveEvaluationTimeEstimate) {
                moveEvaluationTimeEstimate = updatedEstimate;
            }

            moveEvaluationTimeEstimate = 0.5 * (updatedEstimate + moveEvaluationTimeEstimate);

            if (rankedPaths.isEmpty()) {
                return performPathPostProcessing(new MovePath(game, entity), 0);
            }

            LOGGER.debug("Path ranking took {} millis", stop_time - startTime);

            final RankedPath bestPath = getPathRanker(entity).getBestPath(rankedPaths);
            LOGGER.info("Best Path: {}  Rank: {}", bestPath.getPath(), bestPath.getRank());

            return performPathPostProcessing(bestPath);
        } catch (Exception e) {
            LOGGER.error("MP is now null!", e);
            return null;
        } finally {
            precognition.unPause();
        }
    }

    private static String getMessage(Entity entity, double thisTimeEstimate, List<MovePath> paths) {
        String timeEstimate = "unknown.";
        if (0 != thisTimeEstimate) {
            timeEstimate = (int) thisTimeEstimate + " seconds";
        }
        return "Moving " +
              entity.getChassis() +
              ". " +
              Long.toString(paths.size()) +
              " paths to consider.  Estimated time to completion: " +
              timeEstimate;
    }

    @Override
    protected void initFiring() {
        try {
            initialize();

            // ----Debugging: print out any errors made in guessing to hit
            // values-----
            final List<Entity> entities = game.getEntitiesVector();
            for (final Entity entity : entities) {
                final String errors = getFireControl(entity).checkAllGuesses(entity, game);
                if (!StringUtility.isNullOrBlank(errors)) {
                    LOGGER.warn(errors);
                }
            }
            // -----------------------------------------------------------------------

            // Pick up on any infantry/BA in buildings post-movement and shoot
            // their buildings, similar to the turret check
            // pre-movement(infantry can move so we only set target buildings
            // after they do).
            for (Board board : game.getBoards().values()) {
                final Enumeration<IBuilding> buildings = board.getBuildings();
                while (buildings.hasMoreElements()) {
                    final IBuilding bldg = buildings.nextElement();
                    final Enumeration<Coords> bldgCoords = bldg.getCoords();
                    while (bldgCoords.hasMoreElements()) {
                        final Coords coords = bldgCoords.nextElement();
                        for (final Entity entity : game.getEntitiesVector(coords, board.getBoardId())) {
                            final BuildingTarget bt = new BuildingTarget(coords, board, false);
                            // Want to target buildings with hostile infantry / BA inside them, since
                            // there's no other way to attack them.
                            if (isEnemyInfantry(entity, coords) &&
                                  entity.isInBuilding() &&
                                  !entity.isHidden()) {
                                fireControlState.addAdditionalTarget(bt);
                                sendChat("Building in Hex " +
                                      coords.toFriendlyString() +
                                      " designated target due to infantry inside building.", Level.INFO);
                            }
                        }
                    }
                }
            }

            // Next, collect the ID's of each potential target and store them in
            // the damageMap for allocating damage during firing.

            // Reset the map generated during the movement phase- The available
            // targets may have changed during that time(ejections, enemies
            // fleeing, etc).
            damageMap.clear();
            // Now add an ID for each possible target.
            final List<Targetable> potentialTargets = FireControl.getAllTargetableEnemyEntities(getLocalPlayer(),
                  getGame(),
                  getFireControlState());
            for (final Targetable target : potentialTargets) {
                damageMap.put(target.getId(), 0d);
            }

            getFireControlState().clearTransientData();
        } catch (Exception ignored) {

        }
    }

    /**
     * Function with side effects. Retrieves the move path collection we want the entity to consider. Sometimes it's the
     * standard "circle", sometimes it's pruned long-range movement paths
     */
    public List<MovePath> getMovePathsAndSetNecessaryTargets(Entity mover, boolean forceMoveToContact) {
        // if the mover can't move, then there's nothing for us to do here, let's cut out.
        if (mover.isImmobile()) {
            return Collections.emptyList();
        }

        BehaviorType behavior = forceMoveToContact ?
              BehaviorType.MoveToContact :
              unitBehaviorTracker.getBehaviorType(mover, this);
        // during the movement phase, it is technically necessary to clear this data between each unit
        // as the state of the board may have changed due to crashes etc.
        // generating movable clusters is a relatively cheap operation, so it's not a big deal
        getClusterTracker().clearMovableAreas();
        getClusterTracker().updateMovableAreas(mover);

        // basic idea:
        // if we're "in battle", just use the standard set of move paths
        // if we're trying to get somewhere
        //  - sort all long range paths by "mp cost" (actual MP + how long it'll take to do terrain leveling)
        //  - set the first terrain/building as 'strategic target' if the shortest path requires terrain leveling
        //  - if the first strategic target is in LOS at the pruned end of the shortest path,
        //      then we actually return the paths for "engaged" behavior
        //  - if we're unable to get where we're going, use standard set of move paths
        switch (behavior) {
            case Engaged:
                return getPrecognition().getPathEnumerator().getUnitPaths().get(mover.getId());
            case MoveToDestination:
            case MoveToContact:
            case ForcedWithdrawal:
            default: {
                List<BulldozerMovePath> bulldozerPaths = getPrecognition().getPathEnumerator()
                      .getLongRangePaths()
                      .get(mover.getId());

                // for whatever reason (most likely it's wheeled), there are no long-range paths for this unit,
                // so just have it mill around in place as usual. Also set the behavior to "no path to destination"
                // so it doesn't hump the walls due to "self preservation mods"
                if ((bulldozerPaths == null) || bulldozerPaths.isEmpty()) {
                    if (!mover.isAirborne()) {
                        getUnitBehaviorTracker().overrideBehaviorType(mover, BehaviorType.NoPathToDestination);
                    }
                    return getPrecognition().getPathEnumerator().getUnitPaths().get(mover.getId());
                }

                bulldozerPaths.sort(new MPCostComparator());

                // if the quickest route needs some terrain adjustments, let's get working on that
                Targetable levelingTarget = null;

                if (bulldozerPaths.get(0).needsLeveling()) {
                    levelingTarget = getAppropriateTarget(bulldozerPaths.get(0).getCoordsToLevel().get(0),
                          mover.getBoardId());
                    getFireControlState().addAdditionalTarget(levelingTarget);
                    sendChat("Hex " +
                          levelingTarget.getPosition().toFriendlyString() +
                          " impedes route to destination, targeting for clearing.", Level.INFO);
                }

                // if any of the long range paths, pruned, are within LOS of leveling coordinates, then we're actually
                // just going to go back to the standard unit paths
                List<MovePath> prunedPaths = new ArrayList<>();
                for (BulldozerMovePath movePath : bulldozerPaths) {
                    BulldozerMovePath prunedPath = movePath.clone();
                    prunedPath.clipToPossible();

                    if (levelingTarget != null) {
                        LosEffects los = LosEffects.calculateLOS(game,
                              mover,
                              levelingTarget,
                              prunedPath.getFinalCoords(),
                              levelingTarget.getPosition(),
                              mover.getBoardId(),
                              false);

                        // break out of this loop, we can get to the thing we're trying to level this turn, so let's
                        // use normal movement routines to move into optimal position to blow it up
                        // Also set the behavior to "engaged"
                        // so it doesn't hump walls due to "self-preservation mods"
                        if (los.canSee()) {
                            // if we've explicitly forced 'move to contact' behavior, don't flip back to 'engaged'
                            if (!forceMoveToContact) {
                                getUnitBehaviorTracker().overrideBehaviorType(mover, BehaviorType.Engaged);
                            }

                            return getPrecognition().getPathEnumerator().getUnitPaths().get(mover.getId());
                        }
                    }

                    // add the pruned path to the list of paths we'll be returning
                    prunedPaths.add(prunedPath);

                    // also return some paths that go a little slower than max speed
                    // in case the faster path would force an unwanted PSR or MASC check
                    prunedPaths.addAll(PathDecorator.decoratePath(prunedPath));
                    // Return some of the already-computed unit paths as well.
                    prunedPaths.addAll(getPrecognition().getPathEnumerator()
                          .getSimilarUnitPaths(mover.getId(), prunedPath));
                }
                return prunedPaths;
            }
        }
    }

    private void checkForDishonoredEnemies() {
        final StringBuilder msg = new StringBuilder("Checking for dishonored enemies.");

        try {
            // If the Forced Withdrawal rule is not turned on, then it's a
            // fight to the death anyway.
            if (!getForcedWithdrawal()) {
                msg.append("\n\tForced withdrawal turned off.");
                return;
            }

            for (final Entity mine : getEntitiesOwned()) {
                // Who just attacked me?
                final Collection<Integer> attackedBy = mine.getAttackedByThisTurn();
                if (attackedBy.isEmpty()) {
                    continue;
                }

                // Is my unit trying to withdraw as per forced withdrawal rules?
                // shortcut: we already check for forced withdrawal above, so need to do that here
                final boolean fleeing = crippledUnits.contains(mine.getId());

                for (final int id : attackedBy) {
                    final Entity entity = getGame().getEntity(id);
                    if (null == entity) {
                        continue;
                    }

                    if (getHonorUtil().isEnemyBroken(entity.getId(), entity.getOwnerId(), getForcedWithdrawal()) ||
                          !entity.isMilitary()) {
                        // If he'd just continued running, I would have let him
                        // go, but the bastard shot at me!
                        msg.append("\n\t")
                              .append(entity.getDisplayName())
                              .append("dishonored himself by attacking me even though he is ");
                        if (!entity.isMilitary()) {
                            msg.append("a civilian.");
                        } else {
                            msg.append("fleeing.");
                        }
                        getHonorUtil().setEnemyDishonored(entity.getOwnerId());
                        continue;
                    }

                    // He shot me while I was running away!
                    if (fleeing) {
                        msg.append("\n\t")
                              .append(entity.getDisplayName())
                              .append("dishonored himself by attacking a fleeing unit (")
                              .append(mine.getDisplayName())
                              .append(").");
                        getHonorUtil().setEnemyDishonored(entity.getOwnerId());
                        attackedWhileFleeing.add(mine.getId());
                    }
                }
            }
        } finally {
            LOGGER.info(msg.toString());
        }
    }

    private void checkForBrokenEnemies() {
        // If the Forced Withdrawal rule is not turned on, then it's a fight
        // to the death anyway.
        if (!getForcedWithdrawal()) {
            return;
        }

        for (final Entity entity : getEnemyEntities()) {
            getHonorUtil().checkEnemyBroken(entity, getForcedWithdrawal());
        }
    }

    /**
     * Update the various state trackers for a specific entity. Useful to call when receiving an entity update packet
     */
    public void updateEntityState(final @Nullable Entity entity) {
        if ((entity != null) && entity.getOwner().isEnemyOf(getLocalPlayer())) {
            // currently just the honor util, and only update it for hostile units
            getHonorUtil().checkEnemyBroken(entity, getForcedWithdrawal());
        }
    }

    @Override
    protected void initMovement() {
        try {
            initialize();
            checkMorale();
            unitBehaviorTracker.clear();
            swarmContext.assignClusters(getEntitiesOwned());
            enemyTracker.updateThreatAssessment(swarmContext.getCurrentCenter());
            // reset strategic targets
            fireControlState.setAdditionalTargets(new ArrayList<>());
            for (final Coords strategicTarget : getStrategicBuildingTargets()) {
                if (null == game.getBoard().getBuildingAt(strategicTarget)) {
                    fireControlState.addAdditionalTarget(getAppropriateTarget(strategicTarget));
                    sendChat("No building to target in Hex " +
                          strategicTarget.toFriendlyString() +
                          ", targeting for clearing.", Level.INFO);
                } else {
                    fireControlState.addAdditionalTarget(getAppropriateTarget(strategicTarget));
                    sendChat("Building in Hex " + strategicTarget.toFriendlyString() + " designated strategic target.",
                          Level.INFO);
                }
            }

            // Pick up on any turrets and shoot their buildings as well.
            for (Board board : game.getBoards().values()) {
                final Enumeration<IBuilding> buildings = board.getBuildings();
                while (buildings.hasMoreElements()) {
                    final IBuilding bldg = buildings.nextElement();
                    final Enumeration<Coords> bldgCoords = bldg.getCoords();
                    while (bldgCoords.hasMoreElements()) {
                        final Coords coords = bldgCoords.nextElement();
                        for (final Entity entity : game.getEntitiesVector(coords, board.getBoardId(), true)) {
                            final Targetable bt = getAppropriateTarget(coords, board.getBoardId());

                            if (isEnemyGunEmplacement(entity, coords)) {
                                fireControlState.addAdditionalTarget(bt);
                                sendChat("Building in Hex " +
                                      coords.toFriendlyString() +
                                      " designated target due to Gun Emplacement.", Level.INFO);
                            }

                            if (isEnemyBuildingEntity(entity, coords)) {
                                fireControlState.getAdditionalTargets().add(bt);
                                sendChat("Building in Hex " +
                                      coords.toFriendlyString() +
                                      " designated target due to Building Entity.", Level.INFO);
                            }
                        }
                    }
                }
            }

            // Next, collect the ID's of each potential target and store them in
            // the damageMap for allocating damage during movement.
            // Right now, this doesn't get filled because I can't find where
            // FiringPlans for potential move paths are calculated(pretty sure
            // they are, though). This needs to be fixed at some point.

            // Reset last round's damageMap
            damageMap.clear();

            // Now add an ID for each possible target.
            final List<Targetable> potentialTargets = FireControl.getAllTargetableEnemyEntities(getLocalPlayer(),
                  getGame(),
                  fireControlState);
            for (final Targetable target : potentialTargets) {
                damageMap.put(target.getId(), 0d);
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void initialize() {
        try {
            if (initialized) {
                return; // no need to initialize twice
            }

            checkForDishonoredEnemies();
            checkForBrokenEnemies();
            refreshCrippledUnits();
            initializePathRankers();
            fireControlState = new FireControlState();
            pathRankerState = new PathRankerState();
            unitBehaviorTracker = new UnitBehavior();
            boardClusterTracker = new BoardClusterTracker();

            // Set up heat mapping
            initFriendlyHeatMap();
            initEnemyHeatMaps();
            initExperimentalFeatures();

            // Pick up any turrets and add their buildings to the strategic targets list.
            for (Board board : game.getBoards().values()) {
                final Enumeration<IBuilding> buildings = board.getBuildings();
                while (buildings.hasMoreElements()) {
                    final IBuilding bldg = buildings.nextElement();
                    final Enumeration<Coords> bldgCoords = bldg.getCoords();
                    while (bldgCoords.hasMoreElements()) {
                        final Coords coords = bldgCoords.nextElement();
                        for (final Entity entity : game.getEntitiesVector(coords, board.getBoardId(), true)) {
                            if (isEnemyGunEmplacement(entity, coords)) {
                                getStrategicBuildingTargets().add(coords);

                                sendChat("Building in Hex " +
                                      coords.toFriendlyString() +
                                      " designated target due to Gun Emplacement.", Level.INFO);
                            }


                            if (isEnemyBuildingEntity(entity, coords)) {
                                getStrategicBuildingTargets().add(coords);
                                sendChat("Building in Hex " +
                                      coords.toFriendlyString() +
                                      " designated target due to Building Entity.", Level.INFO);
                            }
                        }
                    }
                }
            }

            initialized = true;
            BotGeometry.debugSelfTest(this);
        } catch (Exception ignored) {

        }
    }

    /**
     * Initialize the experimental features.
     */
    private void initExperimentalFeatures() {
        enemyTracker = new EnemyTracker(this);
        coverageValidator = new CoverageValidator(this);
        swarmContext = new SwarmContext();
        swarmCenterManager = new SwarmCenterManager(this);
        int quadrantSize = Math.min(getGame().getBoard().getWidth(), Math.min(getGame().getBoard().getHeight(), 11));
        swarmContext.initializeStrategicGoals(getGame().getBoard(), quadrantSize, quadrantSize);
    }

    /**
     * Initialize the fire controls.
     */
    public void initializeFireControls() {
        fireControls = new HashMap<>();

        FireControl fireControl = new FireControl(this);
        fireControls.put(FireControlType.Basic, fireControl);

        InfantryFireControl infantryFireControl = new InfantryFireControl(this);
        fireControls.put(FireControlType.Infantry, infantryFireControl);

        MultiTargetFireControl multiTargetFireControl = new MultiTargetFireControl(this);
        fireControls.put(FireControlType.MultiTarget, multiTargetFireControl);
    }

    /**
     * Initialize the possible path rankers. Has a dependency on the fire controls being initialized.
     */
    public void initializePathRankers() {
        initializeFireControls();

        pathRankers = new HashMap<>();

        BasicPathRanker basicPathRanker = new BasicPathRanker(this);
        basicPathRanker.setPathEnumerator(precognition.getPathEnumerator());
        pathRankers.put(PathRankerType.Basic, basicPathRanker);

        InfantryPathRanker infantryPathRanker = new InfantryPathRanker(this);
        infantryPathRanker.setPathEnumerator(precognition.getPathEnumerator());
        pathRankers.put(PathRankerType.Infantry, infantryPathRanker);

        NewtonianAerospacePathRanker newtonianAerospacePathRanker = new NewtonianAerospacePathRanker(this);
        newtonianAerospacePathRanker.setPathEnumerator(precognition.getPathEnumerator());
        pathRankers.put(PathRankerType.NewtonianAerospace, newtonianAerospacePathRanker);

        UtilityPathRanker utilityPathRanker = new UtilityPathRanker(this);
        utilityPathRanker.setPathEnumerator(precognition.getPathEnumerator());
        pathRankers.put(PathRankerType.Utility, utilityPathRanker);
    }

    /**
     * Reduce utility of TAGging something if we're already trying.  Update the utility if it's better, otherwise try to
     * dissuade the next attacker.
     */
    public int computeTeamTagUtility(Targetable te, int damage) {
        int key = te.getId();
        if (teamTagTargetsMap.containsKey(key)) {
            if (teamTagTargetsMap.get(key) > damage) {
                return damage / 4;
            }
        } else {
            teamTagTargetsMap.put(key, damage);
        }
        return damage;
    }

    /**
     * Because Aerospace units can either TAG _or_ attack with their weapons, we want every friendly TAG-equipped Aero
     * (and possibly others) to know about all the possible incoming Homing or IF attacks that could take advantage of
     * their TAGs, for later calculations.
     * <p>
     * Steps are: 1. All relevant guidable attacks (within range of the target) are added to a list. 1a. All Homing
     * Weapons landing this turn are added to the list. 1b. All IF-capable, Semi-Guided, and Homing weapons that have
     * not yet fired but _could_ are added as well. 2. Each attacker-target location pair's expected homing/indirect
     * fire damage is cached for later re-use. 3. During the Indirect phase, when TAG attacks are announced, relevant
     * units can use this info to decide whether they want to TAG or reserve their activation for actual attacks. 4. All
     * info is cleared at the start of the next turn.
     */
    public List<WeaponMounted> computeGuidedWeapons(Entity entityToFire, Coords location) {
        // Key cached entries to firing entity _and_ coordinates, as each shooter may have different targets and weapons
        // to support.
        Map.Entry<Integer, Coords> key = Map.entry(entityToFire.getId(), location);
        if (incomingGuidablesMap.containsKey(key)) {
            return incomingGuidablesMap.get(key);
        }

        List<WeaponMounted> friendlyGuidedWeapons = new ArrayList<>();

        // First, friendly incoming homing artillery that will land this turn.  May include entity's own shots from prior
        // turns, but not from this turn.
        for (Enumeration<ArtilleryAttackAction> attacks = game.getArtilleryAttacks(); attacks.hasMoreElements(); ) {
            ArtilleryAttackAction a = attacks.nextElement();
            if (a.getTurnsTilHit() == 0 &&
                  a.getAmmoMunitionType().contains(AmmoType.Munitions.M_HOMING) &&
                  (!a.getEntity(game).isEnemyOf(entityToFire))) {
                // Must be a shot that should land within 8 hexes of the target hex
                if (a.getCoords().distance(location) <= Compute.HOMING_RADIUS) {
                    friendlyGuidedWeapons.add((WeaponMounted) a.getEntity(game).getEquipment(a.getWeaponId()));
                }
            }
        }

        // Next find all friendly Homing-weapon-havers, Indirect-Firers, and LG-Bombers within range.
        for (Entity f : new HashSet<>(getEntitiesOwned())) {
            if (f.equals(entityToFire)) {
                continue; // This entity's weapons should not be considered for this calculation
            }
            Set<WeaponMounted> candidateWeapons = new HashSet<>();
            Coords fLoc = f.getPosition();

            for (WeaponMounted m : f.getTotalWeaponList()) {
                // Ignore weapons outside of viable long range; does not apply to bombs.
                if (fLoc.distance(location) > m.getType().getLongRange() + f.getRunMP() && !m.isGroundBomb()) {
                    continue;
                }
                if (m.getType().hasIndirectFire() && !f.isAero()) {
                    // Only care about ground IF weapons.
                    candidateWeapons.add(m);
                } else if (m.getLinked() != null && m.getLinked().isHomingAmmoInHomingMode()) {
                    candidateWeapons.add(m);
                } else if (m.isGroundBomb()) {
                    // Only care about Laser-Guided bombs here; Homing Arrow IV handled separately.
                    if (((IBomber) f).getBombs()
                          .stream()
                          .anyMatch(b -> (b.getType()).getBombType() == BombTypeEnum.LG)) {
                        candidateWeapons.add(m);
                    }
                }
            }

            // All candidate weapons should be unique instances.
            friendlyGuidedWeapons.addAll(candidateWeapons);
        }
        // Cache result in case needed for later path planning.
        incomingGuidablesMap.put(key, friendlyGuidedWeapons);
        return friendlyGuidedWeapons;
    }

    /**
     * Load the list of units considered crippled at the time the bot was loaded or the beginning of the turn, whichever
     * is the more recent.
     */
    public void refreshCrippledUnits() {
        // if we're not following 'forced withdrawal' rules, there's no need for this
        if (!getForcedWithdrawal()) {
            return;
        }

        // this approach is a little bit inefficient, but the running time is only O(n) where n is the number
        // of princess owned units, so it shouldn't be a big deal.
        crippledUnits.clear();

        for (Entity e : getEntitiesOwned()) {
            if (e.isCrippled(true)) {
                crippledUnits.add(e.getId());
            }
        }
    }

    private boolean isEnemyGunEmplacement(final Entity entity, final Coords coords) {
        return entity.hasETypeFlag(Entity.ETYPE_GUN_EMPLACEMENT) &&
              !getBehaviorSettings().getIgnoredUnitTargets().contains(entity.getId()) &&
              entity.getOwner().isEnemyOf(getLocalPlayer()) &&
              !getStrategicBuildingTargets().contains(coords) &&
              !entity.isCrippled();
    }

    private boolean isEnemyBuildingEntity(final Entity entity, final Coords coords) {
        return entity.hasETypeFlag(Entity.ETYPE_BUILDING_ENTITY) &&
              !getBehaviorSettings().getIgnoredUnitTargets().contains(entity.getId()) &&
              entity.getOwner().isEnemyOf(getLocalPlayer()) &&
              !getStrategicBuildingTargets().contains(coords) &&
              !entity.isCrippled();
    }

    private boolean isEnemyInfantry(final Entity entity, final Coords coords) {
        return entity.hasETypeFlag(Entity.ETYPE_INFANTRY) &&
              !entity.hasETypeFlag(Entity.ETYPE_MEKWARRIOR) &&
              !getBehaviorSettings().getIgnoredUnitTargets().contains(entity.getId()) &&
              entity.getOwner().isEnemyOf(getLocalPlayer()) &&
              !getStrategicBuildingTargets().contains(coords) &&
              !entity.isCrippled();
    }

    @Override
    public synchronized void die() {
        super.die();
        if (null != precognition) {
            precognition.signalDone();
            precognitionThread.interrupt();
        }
    }

    @Override
    protected void processChat(final GamePlayerChatEvent ge) {
        chatProcessor.processChat(ge, this);
    }

    /**
     * Given an entity and the current behavior settings, get the "home" edge to which the entity should attempt to
     * retreat Guaranteed to return a cardinal edge or NONE.
     */
    CardinalEdge getHomeEdge(Entity entity) {
        // if I am crippled and using forced withdrawal rules, my home edge is the "retreat" edge
        if (entity.isCrippled(true) && getBehaviorSettings().isForcedWithdrawal()) {
            if (getBehaviorSettings().getRetreatEdge() == CardinalEdge.NEAREST) {
                return BoardUtilities.getClosestEdge(entity);
            } else {
                return getBehaviorSettings().getRetreatEdge();
            }
        }

        // otherwise, return the destination edge
        if (getBehaviorSettings().getDestinationEdge() == CardinalEdge.NEAREST) {
            return BoardUtilities.getClosestEdge(entity);
        } else {
            return getBehaviorSettings().getDestinationEdge();
        }
    }

    public static int calculateAdjustment(final String ticks) {
        int adjustment = 0;
        if (StringUtility.isNullOrBlank(ticks)) {
            return 0;
        }
        if (StringUtil.isNumeric(ticks)) {
            return Integer.parseInt(ticks);
        }

        for (final char tick : ticks.toCharArray()) {
            if (PLUS == tick) {
                adjustment++;
            } else if (MINUS == tick) {
                adjustment--;
            } else {
                LOGGER.warn("Invalid tick: {}", tick);
            }
        }
        return adjustment;
    }

    @Override
    protected void checkMorale() {
        moraleUtil.checkMorale(behaviorSettings.isForcedWithdrawal(),
              behaviorSettings.getBraveryIndex(),
              behaviorSettings.getSelfPreservationIndex(),
              getLocalPlayer(),
              game);
    }

    public IHonorUtil getHonorUtil() {
        return honorUtil;
    }

    /**
     * Lazy-loaded calculation of the "to-hit target number" threshold, below which rapid fire autocannon will fire
     * multiple shots. More aggressive behavior (left on the Self Preservation slider) start at TN 11 and under, while
     * less aggressive behavior (right on the slider) start at 4 and under.
     */
    public int getSpinUpThreshold() {
        if (spinUpThreshold == null) {
            spinUpThreshold = Math.max(4, Math.min(11, 13 - getBehaviorSettings().getSelfPreservationIndex()));
        }

        return spinUpThreshold;
    }

    public void resetSpinUpThreshold() {
        spinUpThreshold = null;
    }

    @Override
    public void endOfTurnProcessing() {
        checkForDishonoredEnemies();
        checkForBrokenEnemies();
        // refreshCrippledUnits should happen after checkForDishonoredEnemies, since checkForDishonoredEnemies
        // wants to examine the units that were considered crippled at the *beginning* of the turn and were attacked.
        refreshCrippledUnits();
        setAMSModes();
        updateEnemyHeatMaps();
        updateFriendlyHeatMap();
        updateExperimentalFeatures();
    }

    private void updateExperimentalFeatures() {
        if (getBehaviorSettings().isExperimental()) {
            updateSwarmContext();
        }
    }

    private void updateSwarmContext() {
        if (swarmContext == null || enemyTracker == null || coverageValidator == null || swarmCenterManager == null) {
            return;
        }

        swarmContext.setCurrentCenter(swarmCenterManager.calculateCenter());
        enemyTracker.updateThreatAssessment(swarmContext.getCurrentCenter());
        swarmContext.assignClusters(getFriendEntities());
        swarmContext.resetEnemyTargets();

        for (var entity : getEntitiesOwned()) {
            if (entity.isDeployed()) {
                swarmContext.removeAllStrategicGoalsOnCoordsQuadrant(entity.getPosition());
            }
        }
    }

    @Override
    protected void handlePacket(final Packet c) {
        final StringBuilder msg = new StringBuilder("Received packet, cmd: " + c.command());
        try {
            super.handlePacket(c);
            getPrecognition().handlePacket(c);
        } finally {
            LOGGER.trace(msg.toString());
        }
    }

    /**
     * sends a load game file to the server
     */
    @Override
    public void sendLoadGame(final File f) {
        precognition.resetGame();
        super.sendLoadGame(f);
    }

    public void sendPrincessSettings() {
        send(new Packet(PacketCommand.PRINCESS_SETTINGS, behaviorSettings));
    }

    @Override
    protected void disconnected() {
        if (null != precognition) {
            precognition.signalDone();
            precognitionThread.interrupt();
        }
        super.disconnected();
    }

    int getHighestEnemyInitiativeId() {
        int highestEnemyInitiativeBonus = -1;
        int highestEnemyInitiativeId = -1;
        for (final Entity entity : getEnemyEntities()) {
            final int initBonus = entity.getHQIniBonus() + entity.getQuirkIniBonus();
            if (initBonus > highestEnemyInitiativeBonus) {
                highestEnemyInitiativeBonus = initBonus;
                highestEnemyInitiativeId = entity.getId();
            }
        }
        return highestEnemyInitiativeId;
    }

    /**
     * Helper function to perform some modifications to a given path. Intended to happen after we pick the best path.
     *
     * @param path The ranked path to process
     *
     * @return Altered move path
     */
    private MovePath performPathPostProcessing(final RankedPath path) {
        return performPathPostProcessing(path.getPath(), path.getExpectedDamage());
    }

    /**
     * Helper function to perform some modifications to a given path.
     *
     * @param path           The move path to process
     * @param expectedDamage The damage expected to be done by the unit as a result of the path
     *
     * @return Altered move path
     */
    private MovePath performPathPostProcessing(MovePath path, double expectedDamage) {
        MovePath retVal = path;
        evadeIfNotFiring(retVal, expectedDamage >= 0);
        turnOnSearchLight(retVal, expectedDamage >= 0);
        unloadTransportedInfantry(retVal);
        launchFighters(retVal);
        abandonShip(retVal);
        unjamRAC(retVal, expectedDamage >= 5);

        // if we are using vector movement, there's a whole bunch of post-processing that happens to
        // aircraft flight paths when a player does it, so we apply it here.
        if (path.getEntity().isAero() || (path.getEntity() instanceof EjectedCrew && path.getEntity().isSpaceborne())) {
            retVal = SharedUtility.moveAero(retVal, null);
        }

        // allow fleeing at end of movement if is falling back and can flee from position
        if ((isFallingBack(path.getEntity())) &&
              (path.getLastStep() != null) &&
              (getGame().canFleeFrom(path.getEntity(), path.getLastStep().getPosition())) &&
              (path.getMpUsed() < path.getMaxMP())) {
            path.addStep(MoveStepType.FLEE);
        }

        return retVal;
    }

    /**
     * Helper function that appends an unjam RAC command to the end of a qualifying path.
     *
     * @param path The path to process.
     */
    private void unjamRAC(MovePath path, boolean expectFiveOrMore) {
        if (path.getEntity().canUnjamRAC() &&
              (path.getMpUsed() <= path.getEntity().getWalkMP()) &&
              !path.isJumping() &&
              !expectFiveOrMore) {
            path.addStep(MoveStepType.UNJAM_RAC);
        }
    }

    /**
     * Helper function that insinuates an "evade" step for aircraft that will not be shooting.
     *
     * @param path The path to process
     */
    private void evadeIfNotFiring(MovePath path, boolean possibleToInflictDamage) {
        Entity pathEntity = path.getEntity();

        // we cannot evade if we are out of control
        if (pathEntity.isAero() && pathEntity.isAirborne() && ((IAero) pathEntity).isOutControlTotal()) {
            return;
        }

        // if we're an airborne aircraft
        // and we're not going to do any damage anyway
        // and we can do so without causing a PSR
        // then evade
        if (pathEntity.isAirborne() &&
              !possibleToInflictDamage &&
              (path.getMpUsed() <= AeroPathUtil.calculateMaxSafeThrust((IAero) path.getEntity()) - 2)) {
            path.addStep(MoveStepType.EVADE);
        }
    }

    /**
     * Turn on the searchlight if we expect to be shooting at something and it's dark out
     *
     * @param path                    Path being considered
     * @param possibleToInflictDamage Whether we expect to be shooting at something.
     */
    private void turnOnSearchLight(MovePath path, boolean possibleToInflictDamage) {
        Entity pathEntity = path.getEntity();
        if (possibleToInflictDamage &&
              pathEntity.hasSearchlight() &&
              !pathEntity.isUsingSearchlight() &&
              path.getGame().getPlanetaryConditions().getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) {
            path.addStep(MoveStepType.SEARCHLIGHT);
        }
    }

    /**
     * Helper function that adds an "unload" step for units that are transporting infantry if the conditions for
     * unloading are favorable.
     * <p>
     * Infantry unloading logic is different from, for example, hot-dropping Meks or launching aerospace fighters, so we
     * handle it separately.
     *
     * @param path The path to modify
     */
    private void unloadTransportedInfantry(MovePath path) {
        // if my objective is to cross the board, even though it's tempting, I won't be leaving the infantry
        // behind. They're not that good at screening against high speed pursuit anyway.
        if (getBehaviorSettings().shouldAutoFlee()) {
            return;
        }

        final Entity movingEntity = path.getEntity();
        final Coords pathEndpoint = path.getFinalCoords();
        Targetable closestEnemy = getPathRanker(movingEntity).findClosestEnemy(movingEntity,
              pathEndpoint,
              getGame(),
              false);

        // if there are no enemies on the board, then we're not unloading anything.
        // infantry can't clear hexes, so let's not unload them for that purpose
        if ((null == closestEnemy) || (closestEnemy.getTargetType() == Targetable.TYPE_HEX_CLEAR)) {
            return;
        }

        int distanceToClosestEnemy = pathEndpoint.distance(closestEnemy.getPosition());

        // loop through all entities carried by the current entity
        for (Transporter transport : movingEntity.getTransports()) {
            // this operation is intended for entities on the ground
            if (transport instanceof Bay) {
                continue;
            }

            for (Entity loadedEntity : transport.getLoadedUnits()) {
                // there's really no good reason for Princess to disconnect trailers.
                // Let's skip those for now. We don't want to create a bogus 'unload' step for them anyhow.
                if (loadedEntity.isTrailer() && loadedEntity.getTowedBy() != Entity.NONE) {
                    continue;
                }
                // favorable conditions include:
                // - the loaded entity should be able to enter the current terrain
                // - the loaded entity should be within max weapons range + movement range of an enemy
                // - unloading the loaded entity cannot violate stacking limits
                // - only one unit

                // this condition is a simple check that we're not unloading infantry into deep space
                // or into lava or some other such nonsense
                boolean unloadFatal = loadedEntity.isBoardProhibited(getGame().getBoard(path.getFinalBoardId())) ||
                      loadedEntity.isLocationProhibited(pathEndpoint) ||
                      loadedEntity.isLocationDeadly(pathEndpoint);

                // Unloading a unit may sometimes cause a stacking violation, take that into account when planning
                boolean unloadIllegal = Compute.stackingViolation(getGame(),
                      loadedEntity,
                      pathEndpoint,
                      movingEntity,
                      loadedEntity.climbMode(),
                      true) != null;

                // this is a primitive condition that checks whether we're within "engagement range" of an enemy
                // where "engagement range" is defined as the maximum range of our weapons plus our walking movement
                boolean inEngagementRange = loadedEntity.getWalkMP() + getMaxWeaponRange(loadedEntity) >=
                      distanceToClosestEnemy;

                if (!unloadFatal && !unloadIllegal && inEngagementRange) {
                    path.addStep(MoveStepType.UNLOAD, loadedEntity, pathEndpoint);
                    return; // we can only unload one infantry unit per hex per turn, so once we've unloaded, we're done.
                }
            }
        }
    }

    /**
     * Helper function that adds an "launch" step for units that are transporting launch-able units in some kind of
     * bay.
     */
    private void launchFighters(MovePath path) {
        // if my objective is to cross the board, even though it's tempting, I won't be leaving the aerospace
        // behind. They're not that good at screening against high speed pursuit anyway.
        if (getBehaviorSettings().shouldAutoFlee()) {
            return;
        }

        Entity movingEntity = path.getEntity();
        Coords pathEndpoint = path.getFinalCoords();
        int finalBoardId = path.getFinalBoardId();
        Targetable closestEnemy = getPathRanker(movingEntity).findClosestEnemy(movingEntity,
              pathEndpoint,
              getGame(),
              false);

        // Don't launch at high velocity in atmosphere or the fighters will be destroyed!
        if (path.getFinalVelocity() > 2 && !game.getBoard(finalBoardId).isSpace()) {
            return;
        }

        // if there are no enemies on the board, then we're not launching anything.
        if ((null == closestEnemy) || (closestEnemy.getTargetType() != Targetable.TYPE_ENTITY)) {
            return;
        }

        TreeMap<Integer, Vector<Integer>> unitsToLaunch = new TreeMap<>();
        boolean executeLaunch = false;

        // loop through all fighter (or small craft) bays in the current entity
        // grouping launched craft by bay to limit launches to 'safe' rate.
        Vector<Bay> fighterBays = movingEntity.getFighterBays();

        for (int bayIndex = 0; bayIndex < fighterBays.size(); bayIndex++) {
            Bay bay = fighterBays.get(bayIndex);

            for (Entity loadedEntity : bay.getLaunchableUnits()) {
                unitsToLaunch.putIfAbsent(bayIndex, new Vector<>());

                // for now, just launch fighters at the 'safe' rate
                if (unitsToLaunch.get(bayIndex).size() < bay.getSafeLaunchRate()) {
                    unitsToLaunch.get(bayIndex).add(loadedEntity.getId());
                    executeLaunch = true;
                } else {
                    break;
                }
            }
        }

        // only add the step if we're actually launching something
        if (executeLaunch) {
            path.addStep(MoveStepType.LAUNCH, unitsToLaunch);
        }
    }

    protected boolean shouldAbandon(Entity entity) {
        boolean shouldAbandon = false;
        // Nonfunctional entity?  Abandon!
        if ((entity.isPermanentlyImmobilized(true) ||
              entity.isCrippled() ||
              entity.isShutDown() ||
              entity.isDoomed()) && (entity.getAltitude() < 1)) {
            shouldAbandon = true;
        } else if (entity instanceof Tank tank) {
            // Immobile tank?  Abandon!
            // Ground-bound VTOL?  Abandon!
            if (tank.isImmobile()) {
                shouldAbandon = true;
            }
        } else if ((entity instanceof Aero aero && aero.getAltitude() < 1 && !aero.isAirborne())) {
            // Aero and unable to take off?  Abandon!
            if (!aero.canTakeOffHorizontally() && !aero.canTakeOffVertically()) {
                shouldAbandon = true;
            }
            // Aero and no clearance to take off?  You guessed it: straight to Abandon!
            if (aero.canTakeOffHorizontally() && (null != aero.hasRoomForHorizontalTakeOff())) {
                shouldAbandon = true;
            }
            // Effectively immobile Aerospace?  Believe it or not, Abandon!
            if (EntityMovementType.MOVE_NONE == aero.moved && EntityMovementType.MOVE_NONE == aero.movedLastRound) {
                shouldAbandon = true;
            }
        }
        return shouldAbandon;
    }

    protected void abandonShipOneUnit(Entity movingEntity, Vector<Transporter> transporters, MovePath path) {
        // Set dismount locations: this hex / adjacent hexes / outer-ring adjacent hexes
        final Board board = game.getBoard(path.getFinalBoardId());
        List<Coords> dismountLocations = List.of(movingEntity.getPosition());
        if (movingEntity.isDropShip() || movingEntity.isSmallCraft() ||
              (movingEntity.isSupportVehicle() && movingEntity.isSuperHeavy())) {
            int distance = (movingEntity.isDropShip()) ? 2 : 1;
            dismountLocations = movingEntity.getPosition()
                  .allAtDistance(distance)
                  .stream()
                  .filter(board::contains)
                  .toList();
        }

        int dismountIndex;
        int unitsUnloaded;

        for (Transporter transporter : transporters) {
            // Roundabout bookkeeping method required by the fact that we do multiple dismounts by
            // giving the carrier multiple _turns_; we leave and return to this method several times in one round.
            unitsUnloaded = transporter.getNumberUnloadedThisTurn();

            // Infantry can stack
            dismountIndex = unitsUnloaded / ((transporter instanceof InfantryTransporter) ? 2 : 1);

            Vector<Entity> entityList;
            int currentDoors;
            if (transporter instanceof InfantryTransporter infantryTransporter) {
                entityList = infantryTransporter.getLoadedUnits();
                currentDoors = infantryTransporter.getCurrentDoors();
            } else if (transporter instanceof Bay bay) {
                entityList = bay.getLoadedUnits();
                currentDoors = bay.getCurrentDoors();
            } else {
                // Currently unhandled.
                continue;
            }
            for (Entity loadedEntity : entityList) {
                if (dismountIndex >= dismountLocations.size()) {
                    // Exhausted dismount locations
                    break;
                }
                Coords dismountLocation = dismountLocations.get(dismountIndex);

                // for now, just unload transporters at 'safe' rate
                // (1 per transporter door per turn except for infantry which limited by adj. hexes, TW 91)
                int maxDismountsPerBay = (loadedEntity.isInfantry()) ? Integer.MAX_VALUE : currentDoors;
                if (unitsUnloaded < maxDismountsPerBay) {
                    while (dismountIndex < dismountLocations.size()) {
                        // this condition is a simple check that we're not unloading units into fatal conditions
                        boolean unloadFatal = loadedEntity.isBoardProhibited(board) ||
                              loadedEntity.isLocationProhibited(dismountLocation) ||
                              loadedEntity.isLocationDeadly(dismountLocation);

                        // Unloading a unit may sometimes cause a stacking violation, take that into account when planning
                        boolean unloadIllegal = Compute.stackingViolation(getGame(),
                              loadedEntity,
                              dismountLocation,
                              (dismountLocations.size() == 1) ? movingEntity : null,
                              loadedEntity.climbMode(),
                              true) != null;

                        if (unloadIllegal) {
                            // Try the next hex
                            dismountIndex++;
                        } else if (!unloadFatal) {
                            // Princess *has* to track this as we don't see entity updates
                            // to our local game instance until movement is fully done!
                            movingEntity.unload(loadedEntity);
                            loadedEntity.setUnloaded(true);
                            loadedEntity.setTransportId(Entity.NONE);

                            path.addStep(MoveStepType.UNLOAD, loadedEntity, dismountLocation);
                            return;
                        } else {
                            // Still increase the index so the loop finishes!
                            dismountIndex++;
                        }
                    }
                } else {
                    // Hit the max for this bay
                    break;
                }
            }
        }
    }

    /**
     * Helper function that starts unloading a transport if it is crippled, doomed, unable to move, or otherwise no
     * longer functional as transport. We don't care if there are enemies on the board here; get the units out as soon
     * as possible. Returns after one unload b/c MovePathHandler will give us another Turn if we still have more to
     * unload (up to standard limits).
     *
     * @param path MovePath that brought us here.
     */
    private void abandonShip(MovePath path) {
        // Can Unload?
        Entity movingEntity = path.getEntity();

        // If no method of carrying units, skip it.
        Vector<Transporter> transporters = movingEntity.getTransports();
        if (transporters == null || transporters.isEmpty()) {
            return;
        }

        // Should Unload?
        // If this entity is still able to maneuver and fight, don't abandon it yet.
        if (!shouldAbandon(movingEntity)) {
            return;
        }

        // Unload What, Where?
        abandonShipOneUnit(movingEntity, transporters, path);
    }

    /**
     * Sets the mode for AMS on each unit this bot controls. This may be on or off, and possibly manual fire if the game
     * options allow it, with any change taking effect next round. Normal setting is to have the AMS active/automatic.
     * It may be turned off to conserve ammo on relatively undamaged units, and laser AMS may be turned off to help
     * reduce overheating. Manual use is reserved as an emergency anti-infantry measure.
     */
    private void setAMSModes() {

        // Get conventional infantry if manual mode is available
        List<Entity> enemyInfantry = new ArrayList<>();
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_MANUAL_AMS)) {
            for (Entity curEnemy : getEnemyEntities()
                  .stream()
                  .filter(Entity::isDeployed)
                  .collect(Collectors.toSet())) {
                if (curEnemy.getPosition() != null && curEnemy.isVisibleToEnemy()) {

                    if (curEnemy instanceof Infantry && !(curEnemy instanceof EjectedCrew)) {
                        enemyInfantry.add(curEnemy);
                    }

                }
            }
        }

        for (Entity curEntity : getEntitiesOwned()) {
            if (!curEntity.isDeployed() || curEntity.getPosition() == null) {
                continue;
            }

            List<WeaponMounted> activeAMS = curEntity.getWeaponList()
                  .stream()
                  .filter(w -> w.getType().hasFlag(AmmoWeapon.F_AMS) && w.hasModes())
                  .toList();

            if (!activeAMS.isEmpty()) {

                // Set default to on/automatic and test to see if it should be off or manual instead
                EquipmentMode newAMSMode = EquipmentMode.getMode(Weapon.MODE_AMS_ON);

                boolean isOverheating = (curEntity instanceof Mek) && (curEntity.getHeat() >= MAX_OVERHEAT_AMS);

                // If there are enough nearby enemy infantry (only counted if the game option is
                // set), choose manual fire
                if (!enemyInfantry.isEmpty() && !curEntity.isAirborne()) {
                    int infantryRange = enemyInfantry.stream()
                          .mapToInt(e -> Compute.effectiveDistance(game, curEntity, e))
                          .min()
                          .getAsInt();
                    if (infantryRange <= 3) {
                        newAMSMode = EquipmentMode.getMode(Weapon.MODE_AMS_MANUAL);
                    }
                }

                // If AMS was used manually this round, chances are it will be needed next round too
                if (usedManualAMS(curEntity.getId())) {
                    newAMSMode = EquipmentMode.getMode(Weapon.MODE_AMS_MANUAL);
                }

                for (WeaponMounted curAMS : activeAMS) {

                    EquipmentMode curMode = curAMS.curMode();

                    // Turn off laser AMS to help with overheating problems
                    if (curAMS.getType().hasFlag(WeaponType.F_ENERGY)) {
                        if (isOverheating) {
                            newAMSMode = EquipmentMode.getMode(Weapon.MODE_AMS_OFF);
                        }
                    } else {

                        // Determine if ammo needs to be conserved
                        boolean conserveAmmo = curAMS.getLinkedAmmo().getUsableShotsLeft() <=
                              (int) Math.floor(curAMS.getOriginalShots() *
                                    behaviorSettings.getSelfPreservationValue() /
                                    100.0);

                        // Consider turning off AMS to conserve ammo unless it's needed for infantry
                        if (conserveAmmo && !newAMSMode.equals(Weapon.MODE_AMS_MANUAL)) {

                            int ammoTN = 12 - behaviorSettings.getBraveryIndex();

                            // Fighting a missile boat is more likely to require an active AMS
                            int lastTargetID = curEntity.getLastTarget();
                            if (lastTargetID >= 0) {
                                Entity lastTarget = game.getEntity(lastTargetID);
                                if (lastTarget != null && lastTarget.getRole() == UnitRole.MISSILE_BOAT) {
                                    ammoTN += 4;
                                }
                            }

                            // Heavily damaged units are more likely to require an active AMS than
                            // lightly damaged ones
                            switch (curEntity.getDamageLevel()) {
                                case Entity.DMG_NONE:
                                    ammoTN -= 4;
                                    break;
                                case Entity.DMG_LIGHT:
                                    ammoTN -= 2;
                                    break;
                                case Entity.DMG_MODERATE:
                                    ammoTN += 1;
                                    break;
                                case Entity.DMG_HEAVY:
                                    ammoTN += 4;
                                case Entity.DMG_CRIPPLED:
                                    ammoTN += 8;
                                    break;
                                default:
                                    break;
                            }

                            if (ammoTN < 10) {
                                if (Compute.d6(2) >= ammoTN) {
                                    newAMSMode = EquipmentMode.getMode(Weapon.MODE_AMS_OFF);
                                }
                            }

                        }

                    }

                    // Set the mode for the AMS to get the new mode number, and register the change
                    // with the server
                    if (!curMode.equals(newAMSMode)) {
                        int modeNumber = curAMS.setMode(newAMSMode.getName());
                        if (modeNumber != -1) {
                            sendModeChange(curEntity.getId(), curEntity.getEquipmentNum(curAMS), modeNumber);
                        }
                    }

                }

            }

        }

        // Clear the manual AMS tracking list for next round
        clearManualAMSIds();
    }

    /**
     * Flag an entity as having used manual AMS this round
     */
    public void flagManualAMSUse(int id) {
        if (manualAMSIds == null) {
            manualAMSIds = new ArrayList<>();
        }
        if (!manualAMSIds.contains(id)) {
            manualAMSIds.add(id);
        }
    }

    public boolean usedManualAMS(int id) {
        if (manualAMSIds == null) {
            manualAMSIds = new ArrayList<>();
            return false;
        }
        return manualAMSIds.contains(id);
    }

    /**
     * Clear the manual AMS tracking list
     */
    public void clearManualAMSIds() {
        if (manualAMSIds == null) {
            manualAMSIds = new ArrayList<>();
        }
        manualAMSIds.clear();
    }

    /**
     * Get a list of all hot spots (positions of high activity) for opposing units
     */
    public List<Coords> getEnemyHotSpots() {
        List<Coords> accumulatedHotSpots = new ArrayList<>();
        for (HeatMap curMap : enemyHeatMaps) {
            List<Coords> mapHotSpots = curMap.getHotSpots();
            if (mapHotSpots != null) {
                for (Coords curPosition : mapHotSpots) {
                    if (!accumulatedHotSpots.contains(curPosition)) {
                        accumulatedHotSpots.add(curPosition);
                    }
                }
            }
        }

        return accumulatedHotSpots;
    }

    /**
     * Get the best hot spot (positions of high activity) for friendly units
     *
     * @return {@code Coords} with high friendly activity; may return null
     */
    public Coords getFriendlyHotSpot() {
        return friendlyHeatMap.getHotSpot();
    }

    /**
     * Get the nearest top-rated hot spot for friendly units
     */
    public Coords getFriendlyHotSpot(Coords testPosition) {
        return friendlyHeatMap == null ? null : friendlyHeatMap.getHotSpot(testPosition, true);
    }

    /**
     * Set up heat maps to track enemy unit positions over time
     */
    protected void initEnemyHeatMaps() {
        enemyHeatMaps = new ArrayList<>();
        int princessTeamId = getGame().getTeamForPlayer(getLocalPlayer()).getId();
        for (Team curTeam : getGame().getTeams()) {
            if (curTeam.getId() != princessTeamId) {
                HeatMap newMap = new HeatMap(curTeam.getId());
                newMap.setMapTrimThreshold(0.5);
                newMap.setActivityDecay(-200);
                enemyHeatMaps.add(newMap);
            }
        }
    }

    /**
     * Set up heat map to track friendly units over time
     */
    protected void initFriendlyHeatMap() {
        friendlyHeatMap = new HeatMap(getGame().getTeamForPlayer(getLocalPlayer()).getId());
        friendlyHeatMap.setMovementWeightValue(5);
        friendlyHeatMap.setMapTrimThreshold(0.6);
        friendlyHeatMap.setActivityDecay(-200);
        friendlyHeatMap.setIsTrackingFriendlyTeam(true);
    }

    /**
     * Update the heat maps with known enemy unit positions, then apply decay
     */
    protected void updateEnemyHeatMaps() {

        List<Entity> trackedEntities = getGame().inGameTWEntities()
              .stream()
              .filter(HeatMap::validateForTracking)
              .collect(Collectors.toList());

        // Process entities into each heat map, then age it
        for (HeatMap curMap : enemyHeatMaps) {
            if (!trackedEntities.isEmpty()) {
                curMap.updateTrackers(trackedEntities);
            }
            curMap.ageMaps(game);
        }
    }

    /**
     * Update the heat map with allied unit positions (entities controlled by this bot have already been processed as
     * they move), then apply decay
     */
    protected void updateFriendlyHeatMap() {

        List<Entity> trackedEntities = getGame().inGameTWEntities()
              .stream()
              .filter(e -> e.getOwner().getId() != getLocalPlayer().getId() &&
                    HeatMap.validateForTracking(e))
              .collect(Collectors.toList());

        if (!trackedEntities.isEmpty()) {
            friendlyHeatMap.updateTrackers(trackedEntities);
        }
        // Units may have skidded, fallen, etc. and need their actual last position updated
        friendlyHeatMap.refreshLastKnownCache(game);
        friendlyHeatMap.ageMaps(game);
    }


    public void sendChat(final String message, final Level logLevel) {
        if (LOGGER.getLevel().isLessSpecificThan(logLevel)) {
            super.sendChat(message);
        }
    }

    /**
     * Override for the 'receive entity update' handler Updates internal state in addition to base client functionality
     */
    @Override
    public void receiveEntityUpdate(final Packet packet) throws InvalidPacketDataException {
        super.receiveEntityUpdate(packet);
        updateEntityState((Entity) packet.getObject(1));
    }

    public SwarmContext getSwarmContext() {
        return swarmContext;
    }

    public EnemyTracker getEnemyTracker() {
        return enemyTracker;
    }

    public CoverageValidator getCoverageValidator() {
        return coverageValidator;
    }

    public SwarmCenterManager getSwarmCenterManager() {
        return swarmCenterManager;
    }

    @Override
    protected void postMovementProcessing() {
        for (var entity : getEntitiesOwned()) {
            if (entity.getPosition() == null) {
                continue;
            }
            var waypoint = getUnitBehaviorTracker().getWaypointForEntity(entity);
            if (waypoint.isPresent()) {
                var wp = waypoint.get();
                if (wp.distance(entity.getPosition()) <= DISTANCE_TO_WAYPOINT) {
                    LOGGER.debug("{} arrived at waypoint {}", entity.getDisplayName(), wp);
                    getUnitBehaviorTracker().removeHeadWaypoint(entity);
                }
            }
        }
    }

    public ArtilleryCommandAndControl getArtilleryCommandAndControl() {
        return artilleryCommandAndControl;
    }

    /**
     * Determines whether Princess should reroll initiative using the Tactical Genius special ability.
     *
     * <p>The decision is based on:</p>
     *
     * <ul>
     *   <li>Whether Tactical Genius is available to the player</li>
     *   <li>Whether it has already been used this round</li>
     *   <li>The probability that rerolling will improve the net initiative outcome against enemies</li>
     * </ul>
     *
     * <p>Princess will only reroll if currently losing more initiative comparisons than winning, and if the
     * probability of improvement exceeds the configured threshold.</p>
     *
     * @return {@code true} if Tactical Genius should be used to reroll initiative
     */
    @Override
    protected boolean decideToRerollInitiative() {
        Player me = getLocalPlayer();

        if (!game.hasTacticalGenius(me) || rerolledInitiative) {
            return false;
        }

        int myRoll = getLastInitiativeRoll(me);
        List<Integer> enemyRolls = game.getPlayersList().stream()
              .filter(p -> p != me && p.isEnemyOf(me))
              .map(this::getLastInitiativeRoll)
              .sorted()
              .toList();

        int winsNow = countWins(myRoll, enemyRolls);
        int lossesNow = countLosses(myRoll, enemyRolls);

        // We're only interested in rerolling if we're actually losing overall.
        if (winsNow >= lossesNow) {
            return false;
        }

        double pMajority = probabilityOfMajorityWinOnReroll(enemyRolls);

        // Threshold can be tweaked; 0.5 = only reroll if it's more likely than not to help.
        double THRESHOLD = 0.5;
        return pMajority >= THRESHOLD;
    }

    /**
     * Retrieves the most recent initiative roll for the specified player.
     *
     * @param player the player whose initiative roll to retrieve
     *
     * @return the player's last initiative roll value, or {@code 0} if no roll exists
     */
    private int getLastInitiativeRoll(Player player) {
        InitiativeRoll roll = player.getInitiative();
        if (roll == null || roll.size() == 0) {
            return 0;
        }
        return roll.getRoll(roll.size() - 1);
    }

    /**
     * Counts how many enemy initiative rolls Princess' roll beats.
     *
     * @param myRoll     the initiative roll to compare
     * @param enemyRolls the list of enemy initiative rolls to compare against
     *
     * @return the number of enemy rolls that are lower than myRoll
     */
    private int countWins(int myRoll, List<Integer> enemyRolls) {
        int wins = 0;
        for (int roll : enemyRolls) {
            if (myRoll > roll) {
                wins++;
            }
        }
        return wins;
    }

    /**
     * Counts how many enemy initiative rolls beat Princess' roll.
     *
     * @param myRoll     the initiative roll to compare
     * @param enemyRolls the list of enemy initiative rolls to compare against
     *
     * @return the number of enemy rolls that are higher than myRoll
     */
    private int countLosses(int myRoll, List<Integer> enemyRolls) {
        int losses = 0;
        for (int roll : enemyRolls) {
            if (myRoll < roll) {
                losses++;
            }
        }
        return losses;
    }

    /**
     * Calculates the probability that rerolling 2d6 initiative will result in beating more enemy rolls than lose to.
     *
     * <p>Uses the standard 2d6 probability distribution to determine favorable outcomes, where a favorable outcome
     * is defined as winning more initiative comparisons than losing.</p>
     *
     * @param enemyRolls the list of enemy initiative rolls to compare against
     *
     * @return the probability (0.0 to 1.0) that a reroll will improve the net initiative outcome
     */
    private double probabilityOfMajorityWinOnReroll(List<Integer> enemyRolls) {
        // Number of ways to roll each total on 2d6
        int[] ways = { 0, 0, 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1 };

        int favorableOutcomes = 0;
        int totalOutcomes = 36;

        for (int myRoll = 2; myRoll <= 12; myRoll++) {
            int wins = countWins(myRoll, enemyRolls);
            int losses = countLosses(myRoll, enemyRolls);

            if (wins > losses) {
                favorableOutcomes += ways[myRoll];
            }
        }

        return (double) favorableOutcomes / totalOutcomes;
    }
}
