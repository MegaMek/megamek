/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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

import megamek.client.bot.BotClient;
import megamek.client.bot.ChatProcessor;
import megamek.client.bot.PhysicalCalculator;
import megamek.client.bot.PhysicalOption;
import megamek.client.bot.princess.FireControl.FireControlType;
import megamek.client.bot.princess.FiringPlanCalculationParameters.Builder;
import megamek.client.bot.princess.PathRanker.PathRankerType;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.client.ui.SharedUtility;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.BulldozerMovePath.MPCostComparator;
import megamek.common.MovePath.MoveStepType;
import megamek.common.actions.*;
import megamek.common.annotations.Nullable;
import megamek.common.containers.PlayerIDandList;
import megamek.common.event.GameCFREvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.pathfinder.PathDecorator;
import megamek.common.util.BoardUtilities;
import megamek.common.util.StringUtil;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.StopSwarmAttack;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Princess extends BotClient {
    private static final char PLUS = '+';
    private static final char MINUS = '-';

    private final IHonorUtil honorUtil = new HonorUtil();

    private boolean initialized = false;
   
    // path rankers and fire controls, organized by their explicitly given types to avoid confusion
    private HashMap<PathRankerType, IPathRanker> pathRankers;
    private HashMap<FireControlType, FireControl> fireControls;
    private UnitBehavior unitBehaviorTracker;
    private FireControlState fireControlState;
    private PathRankerState pathRankerState;
    private ArtilleryTargetingControl atc;
    
    private Integer spinupThreshold = null;
    
    private BehaviorSettings behaviorSettings;
    private double moveEvaluationTimeEstimate = 0;
    private final Precognition precognition;
    private final Thread precogThread;
    /**
     * Mapping to hold the damage allocated to each targetable, stored by ID.
     * Used to allocate damage more intelligently and avoid overkill.
     */
    private final ConcurrentHashMap<Integer, Double> damageMap = new ConcurrentHashMap<>(); 
    private final Set<Coords> strategicBuildingTargets = new HashSet<>();
    private boolean fallBack = false;
    private final ChatProcessor chatProcessor = new ChatProcessor();
    private boolean fleeBoard = false;
    private final MoraleUtil moraleUtil = new MoraleUtil();
    private final Set<Integer> attackedWhileFleeing = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> crippledUnits = new HashSet<>();

    /** 
     * Returns a new Princess Bot with the given behavior and name, configured for the given
     * host and port. The new Princess Bot outputs its settings to its own logger. 
     */
    public static Princess createPrincess(String name, String host, int port, BehaviorSettings behavior) {
        Princess result = new Princess(name, host, port);
        result.setBehaviorSettings(behavior);
        LogManager.getLogger().debug(result.getBehaviorSettings().toLog());
        return result;
    }

    /**
     * Constructor - initializes a new instance of the Princess bot.
     * @param name The display name.
     * @param host The host address to which to connect.
     * @param port The port on the host where to connect.
     */
    public Princess(final String name, final String host, final int port) {
        super(name, host, port);
        setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        
        fireControlState = new FireControlState();
        pathRankerState = new PathRankerState();
        
        // Start-up precog now, so that it can instantiate its game instance,
        // and it will stay up-to date.
        precognition = new Precognition(this);
        precogThread = new Thread(precognition, "Princess-precognition (" + getName() + ")");
        precogThread.start();
    }

    /**
     * Lazy-loading accessor for the artillery targeting control.
     * @return
     */
    public ArtilleryTargetingControl getArtilleryTargetingControl() {
        if (atc == null) {
            atc = new ArtilleryTargetingControl();
        }
        
        return atc;
    }

    /**
     * Gets the appropriate path ranker instance given an entity
     * Uses the entity's EType flags to figure out which one to return
     * Returns BasicPathRanker by default.
     * @param entity The entity whose ETYPE to check
     * @return Path ranker instance
     */
    IPathRanker getPathRanker(Entity entity) {
        if (entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
            return pathRankers.get(PathRankerType.Infantry);
        } else if (entity.isAero() && game.useVectorMove()) {
            return pathRankers.get(PathRankerType.NewtonianAerospace);
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
     * Picks a tag target based on the data contained within the given GameCFREvent
     * Expects the event to have some tag targets and tag target types.
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
            double currentDamage = getArtilleryTargetingControl().calculateDamageValue(10, targetHex, arbitraryEntity, game, this);
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
        LogManager.getLogger().debug("Setting Flee Board " + fleeBoard + " because: " + reason);

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
     * Retrieves maximum weapon range for the given entity.
     * Cached version of entity.getMaxWeaponRange() 
     * @param entity Entity we're checking
     * @param airborneTarget Whether the potential target is in the air, only relevant for
     *          aircraft shooting at other aircraft on ground maps.
     * @return
     */
    public int getMaxWeaponRange(Entity entity, boolean airborneTarget) {
        return getFireControlState().getWeaponRanges(airborneTarget).
            computeIfAbsent(entity.getId(), ent -> entity.getMaxWeaponRange(airborneTarget));
    }

    public void setFallBack(final boolean fallBack, final String reason) {
        LogManager.getLogger().debug("Setting Fall Back " + fallBack + " because: " + reason);
        this.fallBack = fallBack;
    }

    public void setBehaviorSettings(final BehaviorSettings behaviorSettings) {
        LogManager.getLogger().info("New behavior settings for " + getName() +
            "\n" + behaviorSettings.toLog());
        try {
            this.behaviorSettings = behaviorSettings.getCopy();
        } catch (final PrincessException e) {
            LogManager.getLogger().error("", e);
            return;
        }
        getStrategicBuildingTargets().clear();
        setFallBack(behaviorSettings.shouldGoHome(), "Fall Back Configuration.");
        setFleeBoard(behaviorSettings.shouldAutoFlee(), "Flee Board Configuration.");
        if (getFallBack()) {
            return;
        }

        for (final String targetCoords : behaviorSettings.getStrategicBuildingTargets()) {
            if (!StringUtil.isPositiveInteger(targetCoords) ||
                (4 != targetCoords.length())) {
                continue;
            }
            final String x = targetCoords.substring(0, 2);
            final String y = targetCoords.replaceFirst(x, "");
            // Need to subtract 1, since we are given a Hex number string, 
            // which is Coords X + 1Y + 1
            final Coords coords = new Coords(Integer.parseInt(x) - 1,
                    Integer.parseInt(y) - 1);
            getStrategicBuildingTargets().add(coords);
        }
        
        spinupThreshold = null;
    }

    /**
     * Get the appropriate instance of a FireControl object for the given entity.
     * @param entity The entity in question.
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

    public void addStrategicBuildingTarget(final Coords coords) {
        if (null == coords) {
            throw new NullPointerException("Coords is null.");
        }
        if (!getGame().getBoard().contains(coords)) {
            LogManager.getLogger().warn("Board does not contain " + coords.toFriendlyString());
            return;
        }
        getStrategicBuildingTargets().add(coords);
    }

    public Set<Integer> getPriorityUnitTargets() {
        return getBehaviorSettings().getPriorityUnitTargets();
    }
    
    public Targetable getAppropriateTarget(Coords strategicTarget) {
        if (null == game.getBoard().getBuildingAt(strategicTarget)) {
            return new HexTarget(strategicTarget, Targetable.TYPE_HEX_CLEAR);
        } else {
            return new BuildingTarget(strategicTarget, game.getBoard(), false);
        }
    }

    @Override
    protected Vector<Coords> calculateArtyAutoHitHexes() {
        try {
            // currently returns no artillery hit spots
            // make an empty list
            final PlayerIDandList<Coords> artyAutoHitHexes = new PlayerIDandList<>();
            // attach my player id to it
            artyAutoHitHexes.setPlayerID(getLocalPlayer().getId());
            return artyAutoHitHexes;
        } catch (Exception ignored) {
            return new PlayerIDandList<>();
        }
    }
    
    @Override
    protected void initTargeting() {
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
            LogManager.getLogger().info("Declining to deploy crippled unit: "
                    + getEntity(entityNum).getChassis() + ". Removing unit.");
            sendDeleteEntity(entityNum);
            return;
        }
        
        // get a list of all coordinates to which we can deploy
        final List<Coords> startingCoords = getStartingCoordsArray(game.getEntity(entityNum));
        if (startingCoords.isEmpty()) {
            LogManager.getLogger().error("No valid locations to deploy " + getEntity(entityNum).getDisplayName());
        }

        // get the coordinates I can deploy on
        final Coords deployCoords = getFirstValidCoords(getEntity(entityNum), startingCoords);
        if (null == deployCoords) {
            // if I cannot deploy anywhere, then I get rid of the entity instead so that we may go about our business                
            LogManager.getLogger().error("getCoordsAround gave no location for "
                    + getEntity(entityNum).getChassis() + ". Removing unit.");
            
            sendDeleteEntity(entityNum);
            return;
        }

        // first coordinate that it is legal to put this unit on now find some sort of reasonable
        // facing. If there are deployed enemies, face them
        
        // specifically, face the last deployed enemy.
        int decentFacing = -1;
        for (final Entity e : getEnemyEntities()) {
            if (e.isDeployed() && !e.isOffBoard()) {
                decentFacing = deployCoords.direction(e.getPosition());
                break;
            }
        }

        // if I haven't found a decent facing, then at least face towards 
        // the center of the board
        if (-1 == decentFacing) {
            final Coords center = new Coords(game.getBoard().getWidth() / 2,
                                       game.getBoard().getHeight() / 2);
            decentFacing = deployCoords.direction(center);
        }

        final Entity deployEntity = game.getEntity(entityNum);
        final Hex deployHex = game.getBoard().getHex(deployCoords);

        int deployElevation = getDeployElevation(deployEntity, deployHex);

        // Compensate for hex elevation where != 0...
        deployElevation -= deployHex.getLevel();
        deploy(entityNum, deployCoords, decentFacing, deployElevation);
    }
    
    /**
     * Calculate the deployment elevation for the given entity.
     * Gun Emplacements should deploy on the rooftop of the building for maximum visibility.
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
    protected @Nullable Coords getFirstValidCoords(final Entity deployedUnit,
                                         final List<Coords> possibleDeployCoords) {
        if (Entity.ETYPE_GUN_EMPLACEMENT == (deployedUnit.getEntityType() & Entity.ETYPE_GUN_EMPLACEMENT)) {
            final List<Coords> validCoords = calculateTurretDeploymentLocations(
                    (GunEmplacement) deployedUnit, possibleDeployCoords);
            if (!validCoords.isEmpty()) {
                return validCoords.get(0);
            }
            
            return null;
        } else if (getGame().useVectorMove()) {
            return calculateAdvancedAerospaceDeploymentCoords(deployedUnit, possibleDeployCoords);
        } else {
            return super.getFirstValidCoords(deployedUnit, possibleDeployCoords);
        }
    }
    
    /**
     * Function that calculates deployment coordinates 
     * @param deployedUnit The unit being considered for deployment
     * @param possibleDeployCoords The coordinates being considered for deployment
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
     * @param deployedUnit The unit to check
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
            final Building building = game.getBoard().getBuildingAt(coords);
            final Hex hex = game.getBoard().getHex(coords);

            if (null != building) {
                final int buildingHeight = hex.terrainLevel(Terrains.BLDG_ELEV);
                
                // check stacking violation at the roof level
                final Entity violation = Compute.stackingViolation(game, deployedUnit, coords, buildingHeight, coords,
                        null, deployedUnit.climbMode());
                // Ignore coords that could cause a stacking violation
                if (null == violation) {
                    turretDeploymentLocations.add(coords);
                }
            }
        }

        turretDeploymentLocations.sort((arg0, arg1) -> calculateTurretDeploymentValue(arg1) - calculateTurretDeploymentValue(arg0));
        return turretDeploymentLocations;
    }
    
    /**
     * Helper function that calculates the "utility" of placing a turret at the given coords
     * @param coords The location of the building being considered.
     * @return An "arbitrary" utility number
     */
    private int calculateTurretDeploymentValue(final Coords coords) {
        // algorithm: a building is valued by the following formula:
        //      (CF + height * 2) / # turrets placed on the roof
        //      This way, we will generally favor unpopulated higher CF buildings, 
        //      but have some wiggle room in case of a really tall high CF building
        final Building building = game.getBoard().getBuildingAt(coords);
        final Hex hex = game.getBoard().getHex(coords);
        final int turretCount = 1 + game.getGunEmplacements(coords).size();

        return (building.getCurrentCF(coords) + hex.terrainLevel(Terrains.BLDG_ELEV) * 2) / turretCount;
    }
    
    @Override
    protected void calculateFiringTurn() {
        try {
            // get the first entity that can act this turn make sure weapons 
            // are loaded
            final Entity shooter = getEntityToFire(fireControlState);

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
                final StringBuilder msg = new StringBuilder(shooter.getDisplayName())
                        .append(" is crippled and withdrawing.");
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
                    LogManager.getLogger().info(msg.toString());
                }
            }

            if (shooter.isHidden()) {
                skipFiring = true;
                LogManager.getLogger().info("Hidden unit skips firing.");
            }

            // calculating a firing plan is somewhat expensive, so 
            // we skip this step if we have already decided not to fire due to being hidden or under "peaceful forced withdrawal"
            if (!skipFiring) {
                // Set up ammo conservation.
                final Map<Mounted, Double> ammoConservation = calcAmmoConservation(shooter);

                // entity that can act this turn make sure weapons are loaded
                final FiringPlan plan = getFireControl(shooter).getBestFiringPlan(shooter,
                        getHonorUtil(), game, ammoConservation);
                if ((null != plan) && (plan.getExpectedDamage() > 0)) {
                    getFireControl(shooter).loadAmmo(shooter, plan);
                    plan.sortPlan();

                    LogManager.getLogger().info(shooter.getDisplayName() + " - Best Firing Plan: " +
                            plan.getDebugDescription(LogManager.getLogger().getLevel().isLessSpecificThan(Level.DEBUG)));

                    // Add expected damage from the chosen FiringPlan to the 
                    // damageMap for the target enemy.
                    // while we're looping through all the shots anyway, send any firing mode changes
                    for (WeaponFireInfo shot : plan) {
                        Integer targetId = shot.getTarget().getId();
                        double existingTargetDamage = damageMap.getOrDefault(targetId, 0.0);
                        double newDamage = existingTargetDamage + shot.getExpectedDamage();
                        damageMap.put(targetId, newDamage);

                        if (shot.getUpdatedFiringMode() != null) {
                            super.sendModeChange(shooter.getId(), shooter.getEquipmentNum(shot.getWeapon()), shot.getUpdatedFiringMode());
                        }
                    }

                    // tell the game I want to fire
                    Vector<EntityAction> actions = new Vector<>();

                    // if using search light, it needs to go before the other actions so we can light up what we're shooting at
                    SearchlightAttackAction searchLightAction = getFireControl(shooter).getSearchLightAction(shooter, plan);
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
                    LogManager.getLogger().info("No best firing plan for " + shooter.getDisplayName());
                }
            }

            // if I have decided to skip firing or don't have a firing plan, so let's consider some
            // alternative uses of my turn
            Vector<EntityAction> miscPlan = null;

            if (shooter.getSwarmTargetId() != Entity.NONE) {
                // If we are skipping firing while swarming, it is because we are fleeing...
                // so let's stop swarming if we are doing so
                final Mounted stopSwarmWeapon = shooter.getIndividualWeaponList().stream()
                        .filter(weapon -> weapon.getType() instanceof StopSwarmAttack)
                        .findFirst()
                        .orElse(null);
                if (stopSwarmWeapon == null) {
                    LogManager.getLogger().error("Failed to find a Stop Swarm Weapon while Swarming a unit, which should not be possible.");
                } else {
                    miscPlan = new Vector<>();
                    miscPlan.add(new WeaponAttackAction(shooter.getId(), shooter.getSwarmTargetId(),
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
        } catch (Exception ignored) {

        }
    }

    /**
     * Calculates the targeting/ offboard turn
     * This includes firing TAG and non-direct-fire artillery
     */
    @Override
    protected void calculateTargetingOffBoardTurn() {
        Entity entityToFire = getGame().getFirstEntity(getMyTurn());

        // if we're crippled, off-board and can do so, disengage
        if (entityToFire.isOffBoard() && entityToFire.canFlee() && entityToFire.isCrippled(true)) {
            Vector<EntityAction> disengageVector = new Vector<>();
            disengageVector.add(new DisengageAction(entityToFire.getId()));
            sendAttackData(entityToFire.getId(), disengageVector);
            sendDone(true);
            return;
        }

        FiringPlan firingPlan = getArtilleryTargetingControl().calculateIndirectArtilleryPlan(entityToFire, getGame(), this);

        sendAttackData(entityToFire.getId(), firingPlan.getEntityActionVector());
        sendDone(true);
    }

    private Map<Mounted, Double> calcAmmoConservation(final Entity shooter) {
        final double aggroFactor = (10 - getBehaviorSettings().getHyperAggressionIndex()) * 2;
        final StringBuilder msg = new StringBuilder("\nCalculating ammo conservation for ")
                .append(shooter.getDisplayName());
        msg.append("\nAggression Factor = ").append(aggroFactor);

        try {
            final Map<AmmoType, Integer> ammoCounts = new HashMap<>();
            msg.append("\nPooling Ammo:");
            for (final Mounted ammo : shooter.getAmmo()) {
                final AmmoType ammoType = (AmmoType) ammo.getType();
                msg.append("\n\t").append(ammoType);
                if (ammoCounts.containsKey(ammoType)) {
                    ammoCounts.put(ammoType, ammoCounts.get(ammoType) + ammo.getUsableShotsLeft());
                    msg.append(" + ").append(ammo.getUsableShotsLeft())
                       .append(" = ").append(ammoCounts.get(ammoType));
                    continue;
                }
                ammoCounts.put(ammoType, ammo.getUsableShotsLeft());
                msg.append(" + ").append(ammo.getUsableShotsLeft())
                   .append(" = ").append(ammoCounts.get(ammoType));
            }

            final Map<Mounted, Double> ammoConservation = new HashMap<>();
            msg.append("\nCalculating conservation for each weapon");
            for (final Mounted weapon : shooter.getWeaponList()) {
                final WeaponType weaponType = (WeaponType) weapon.getType();
                msg.append("\n\t").append(weaponType);
                if (!(weaponType instanceof AmmoWeapon)) {
                    ammoConservation.put(weapon, 0.0);
                    msg.append(" doesn't use ammo.");
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
                final double toHitThreshold =
                        Math.max(0.0, 1 - (ammoCount / aggroFactor));
                msg.append("; To Hit Threshold = ").append(new DecimalFormat("0.000").format(toHitThreshold));
                ammoConservation.put(weapon, toHitThreshold);
            }

            return ammoConservation;
        } finally {
            LogManager.getLogger().debug(msg.toString());
        }
    }

    /**
     * Worker method that calculates a point blank shot action vector given a firing entity ID and a target ID.
     * 
     * @param firingEntityID the ID of the entity taking the point blank shot
     * @param targetID the ID of the entity being shot at potentially
     */
    @Override
    protected Vector<EntityAction> calculatePointBlankShot(int firingEntityID, int targetID) {
        Entity shooter = getGame().getEntity(firingEntityID);
        Targetable target = getGame().getEntity(targetID);
        if ((shooter == null) || (target == null)) {
            return new Vector<>();
        }

        final FiringPlanCalculationParameters fccp = new Builder().buildExact(shooter, target, calcAmmoConservation(shooter));
        FiringPlan plan = getFireControl(shooter).determineBestFiringPlan(fccp); 
        getFireControl(shooter).loadAmmo(shooter, plan);
        plan.sortPlan();

        return plan.getEntityActionVector();
    }
    
    @Override
    protected Vector<Minefield> calculateMinefieldDeployment() {
        try {
            // currently returns no minefields
            // make an empty vector
            return new Vector<>();
        } catch (Exception ignored) {
            return new Vector<>();
        }
    }

    /**
     * Calculates the move index for the given unit.
     * In general, faster units and units closer to the enemy should move before others.
     * Additional modifiers for being prone, stealthed, unit type and so on are also factored in.
     *
     * @param entity The unit to be indexed.
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
            if (entity.getJumpMP() > fastestMove) {
                fastestMove = entity.getJumpMP();
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

            // If all else is equal, Infantry before Battle Armor before Tanks before Mechs.
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
            if (entity.isStealthActive() || entity.isStealthOn() ||
                entity.isVoidSigActive() || entity.isVoidSigOn()) {
                total *= PRIORITY_STEALTH;
                modifiers.append("\tx1/3 (is Stealthed)");
            }

            return total;
        } finally {
            msg.append("\n\t\tModifiers:").append(modifiers);
            msg.append("\n\t\tTotal = ").append(numberFormat.format(total));
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
     * Sorts firing entities to ensure that entities that can do indirect fire go after
     * entities that cannot, so that IDF units go after spotting units.
     */
    private void initFiringEntities(FireControlState fireControlState) {
        List<Entity> myEntities = game.getPlayerEntities(this.getLocalPlayer(), true);
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
     * Loops through the list of entities controlled by this Princess instance 
     * and decides which should be moved first.
     * Immobile units and ejected MechWarriors / crews will be moved first.
     * After that, each unit is given an index// This unit should have already 
     * moved due to the isImmobilized check. via the 
     * {@link #calculateMoveIndex(Entity, StringBuilder)} method.  The highest 
     * index value is moved first.
     *
     * @return The entity that should be moved next.
     */
    Entity getEntityToMove() {

        // first move useless units: immobile units, ejected MechWarrior, etc
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
            
            if (!getGame().getPhase().isSimultaneous(getGame()) && (entity.isOffBoard()
                    || (null == entity.getPosition())
                    || entity.isUnloadedThisTurn()
                    || !getGame().getTurn().isValidEntity(entity, getGame()))) {
                msg.append("cannot be moved.");
                continue;
            }

            // Move immobile units & ejected MechWarriors immediately.
            if (isImmobilized(entity) && !(entity instanceof Infantry)) {
                msg.append("is immobile.");
                movingEntity = entity;
                break;
            }
            
            if (entity instanceof MechWarrior) {
                msg.append("is ejected crew.");
                movingEntity = entity;
                break;
            }
            
            // can't do anything with out-of-control aeros, so use them as init sinks
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
            msg.append("\n\thas index ").append(moveIndex).append(" vs ")
               .append(highestIndex);
            if (moveIndex >= highestIndex) {
                highestIndex = moveIndex;
                movingEntity = entity;
            }
        }

        if (movingEntity == null) {
            LogManager.getLogger().warn(msg.toString());
        } else {
            LogManager.getLogger().debug(msg.toString());
        }

        return movingEntity;
    }

    @Override
    protected @Nullable MovePath calculateMoveTurn() {
        try {
            return continueMovementFor(getEntityToMove());
        } catch (Exception ignored) {
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
                final StringBuilder msg = new StringBuilder(attacker.getDisplayName())
                        .append(" is crippled and withdrawing.");
                if (attackedWhileFleeing.contains(attacker.getId())) {
                    msg.append("\n\tBut I was fired on, so I will hit back.");
                } else {
                    msg.append("\n\tI will not attack so long as I'm not fired on.");
                    return null;
                }
                LogManager.getLogger().info(msg.toString());
            }

            // the original bot's physical options seem superior
            return PhysicalCalculator.getBestPhysical(attacker, game);
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
     * @param entity The entity to check.
     * @return Whether or not the entity is falling back.
     */
    boolean isFallingBack(final Entity entity) {
        return (getBehaviorSettings().shouldAutoFlee() ||
                (getBehaviorSettings().isForcedWithdrawal() && entity.isCrippled(true)));
    }

    /**
     * Logic to determine if this entity is in a state where it can shoot due to being attacked while fleeing.
     * @param entity Entity to check.
     * @return Whether or not this entity can shoot while falling back.
     */
    boolean canShootWhileFallingBack(Entity entity) {
        return attackedWhileFleeing.contains(entity.getId());
    }
    
    boolean mustFleeBoard(final Entity entity) {
        if (!isFallingBack(entity)) {
            return false;
        } else if (!entity.canFlee()) {
            return false;
        } else if (0 < getPathRanker(entity).distanceToHomeEdge(entity.getPosition(),
                getHomeEdge(entity), getGame())) {
            return false;
        } else if (!getFleeBoard() && !(entity.isCrippled() && getForcedWithdrawal())) {
            return false;
        } else {
            return true;
        }
    }

    boolean isImmobilized(final Entity mover) {
        if (mover.isImmobile() && !mover.isShutDown()) {
            LogManager.getLogger().info("Is truly immobile.");
            return true;
        } else if (1 > mover.getRunMP()) {
            LogManager.getLogger().info("Has 0 movement.");
            return true;
        } else if (!(mover instanceof Mech)) {
            return false;
        }

        final Mech mech = (Mech) mover;
        if (!mech.isProne() && !mech.isStuck() && !mech.isStalled()) {
            return false;
        }

        final MovePath movePath = new MovePath(getGame(), mover);

        // For a normal fall-shame setting (index 5), our threshold should be 
        // a 10+ piloting roll.
        final int threshold;
        switch (getBehaviorSettings().getFallShameIndex()) {
            case 10:
                threshold = 7;
                break;
            case 9:
                threshold = 8;
                break;
            case 8:
            case 7:
                threshold = 9;
                break;
            case 6:
            case 5:
                threshold = 10;
                break;
            case 4:
                threshold = 11;
                break;
            case 3:
                threshold = 12;
                break;
            default:
                threshold = 13; // Actually impossible.
        }

        // If we're prone, see if we have a chance of getting up.
        if (mech.isProne()) {
            if (mech.cannotStandUpFromHullDown()) {
                LogManager.getLogger().info("Cannot stand up.");
                return true;
            }

            final MoveStepType type = getBooleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_CAREFUL_STAND)
                    ? MoveStepType.CAREFUL_STAND : MoveStepType.GET_UP;
            final MoveStep getUp = new MoveStep(movePath, type);

            // If our odds to get up are equal to or worse than the threshold, 
            // consider ourselves immobile.
            final PilotingRollData target = mech.checkGetUp(getUp, movePath.getLastStepMovementType());
            LogManager.getLogger().info("Need to roll " + target.getValue() +
                " to stand and our tolerance is " + threshold);
            return (target.getValue() >= threshold);
        }

        // How likely are we to get unstuck.
        final MoveStepType type = MoveStepType.FORWARDS;
        final MoveStep walk = new MoveStep(movePath, type);
        final Hex hex = getHex(mech.getPosition());
        final PilotingRollData target = mech.checkBogDown(walk, movePath.getLastStepMovementType(),
                hex, mech.getPriorPosition(), mech.getPosition(), hex.getLevel(), false);
        LogManager.getLogger().info("Need to roll " + target.getValue() + " to get unstuck and our tolerance is " + threshold);
        return (target.getValue() >= threshold);
    }

    boolean getBooleanOption(final String name) {
        return getGame().getOptions().booleanOption(name);
    }

    protected Hex getHex(final Coords coords) {
        return getBoard().getHex(coords);
    }

    @Override
    protected MovePath continueMovementFor(final Entity entity) {
        Objects.requireNonNull(entity, "Entity is null.");

        try {
            // figure out who moved last, and whose move lists need to be updated

            // moves this entity during movement phase
            LogManager.getLogger().debug("Moving " + entity.getDisplayName() + " (ID " + entity.getId() + ")");
            getPrecognition().ensureUpToDate();

            if (isFallingBack(entity)) {
                String msg = entity.getDisplayName();
                if (getFallBack()) {
                    msg += " is falling back.";
                } else if (entity.isCrippled()) {
                    msg += " is crippled and withdrawing.";
                }
                LogManager.getLogger().debug(msg);
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
                    LogManager.getLogger().info(msg);
                    sendChat(msg, Level.ERROR);
                    final MovePath mp = new MovePath(game, entity);
                    mp.addStep(MoveStepType.EJECT);
                    return mp;
                }
            }
            
            final List<MovePath> paths = getMovePathsAndSetNecessaryTargets(entity, false);

            if (null == paths) {
                LogManager.getLogger().warn("No valid paths found.");
                return performPathPostProcessing(new MovePath(game, entity), 0);
            }

            final double thisTimeEstimate = (paths.size() * moveEvaluationTimeEstimate) / 1e3;
            if (LogManager.getLogger().getLevel().isLessSpecificThan(Level.INFO)) {
                String timeestimate = "unknown.";
                if (0 != thisTimeEstimate) {
                    timeestimate = (int) thisTimeEstimate + " seconds";
                }
                final String message = "Moving " + entity.getChassis() + ". "
                        + Long.toString(paths.size())
                        + " paths to consider.  Estimated time to completion: "
                        + timeestimate;
                sendChat(message);
            }

            final long startTime = System.currentTimeMillis();
            getPathRanker(entity).initUnitTurn(entity, getGame());
            // fall tolerance range between 0.50 and 1.0
            final double fallTolerance = getBehaviorSettings().getFallShameIndex() / 20d + 0.50d;
                       
            final List<RankedPath> rankedpaths = getPathRanker(entity).rankPaths(paths,
                    getGame(), getMaxWeaponRange(entity), fallTolerance, getEnemyEntities(),
                    getFriendEntities());

            final long stop_time = System.currentTimeMillis();

            // update path evaluation time estimate
            final double updatedEstimate =
                    ((double) (stop_time - startTime)) / ((double) paths.size());
            if (0 == moveEvaluationTimeEstimate) {
                moveEvaluationTimeEstimate = updatedEstimate;
            }
            
            moveEvaluationTimeEstimate = 0.5 * (updatedEstimate + moveEvaluationTimeEstimate);
            
            if (rankedpaths.isEmpty()) {
                return performPathPostProcessing(new MovePath(game, entity), 0);
            }

            LogManager.getLogger().debug("Path ranking took " + (stop_time - startTime) + " millis");

            final RankedPath bestpath = getPathRanker(entity).getBestPath(rankedpaths);
            LogManager.getLogger().info("Best Path: " + bestpath.getPath() + "  Rank: " + bestpath.getRank());

            return performPathPostProcessing(bestpath);
        } finally {
            precognition.unPause();
        }
    }

    @Override
    protected void initFiring() {
        try {
            initialize();

            // ----Debugging: print out any errors made in guessing to hit
            // values-----
            final List<Entity> ents = game.getEntitiesVector();
            for (final Entity ent : ents) {
                final String errors = getFireControl(ent).checkAllGuesses(ent, game);
                if (!StringUtility.isNullOrBlank(errors)) {
                    LogManager.getLogger().warn(errors);
                }
            }
            // -----------------------------------------------------------------------

            // Pick up on any infantry/BA in buildings post-movement and shoot
            // their buildings, similar to the turret check
            // pre-movement(infantry can move so we only set target buildings
            // after they do).
            final Enumeration<Building> buildings = game.getBoard().getBuildings();
            while (buildings.hasMoreElements()) {
                final Building bldg = buildings.nextElement();
                final Enumeration<Coords> bldgCoords = bldg.getCoords();
                while (bldgCoords.hasMoreElements()) {
                    final Coords coords = bldgCoords.nextElement();
                    for (final Entity entity : game.getEntitiesVector(coords)) {
                        final BuildingTarget bt = new BuildingTarget(coords, game.getBoard(), false);
                        // Want to target buildings with hostile infantry / BA inside them, since
                        // there's no other way to attack them.
                        if (isEnemyInfantry(entity, coords) && Compute.isInBuilding(game, entity)
                                && !entity.isHidden()) {
                            fireControlState.getAdditionalTargets().add(bt);
                            sendChat("Building in Hex " + coords.toFriendlyString()
                                    + " designated target due to infantry inside building.", Level.INFO);
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
                    getGame(), getFireControlState());
            for (final Targetable target : potentialTargets) {
                damageMap.put(target.getId(), 0d);
            }

            getFireControlState().clearTransientData();
        } catch (Exception ignored) {

        }
    }
    
    /**
     * Function with side effects. Retrieves the move path collection we want
     * the entity to consider. Sometimes it's the standard "circle", sometimes it's pruned long-range movement paths
     */
    public List<MovePath> getMovePathsAndSetNecessaryTargets(Entity mover, boolean forceMoveToContact) {
        // if the mover can't move, then there's nothing for us to do here, let's cut out.
        if (mover.isImmobile()) {
            return Collections.emptyList();
        }
        
        BehaviorType behavior = forceMoveToContact ? BehaviorType.MoveToContact : unitBehaviorTracker.getBehaviorType(mover, this);
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
                return getPrecognition().getPathEnumerator()
                        .getUnitPaths()
                        .get(mover.getId());
            case MoveToDestination:
            case MoveToContact:
            case ForcedWithdrawal:
            default: {
                List<BulldozerMovePath> bulldozerPaths = getPrecognition().getPathEnumerator().getLongRangePaths().get(mover.getId());

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
                    levelingTarget = getAppropriateTarget(bulldozerPaths.get(0).getCoordsToLevel().get(0));
                    getFireControlState().getAdditionalTargets().add(levelingTarget);
                    sendChat("Hex " + levelingTarget.getPosition().toFriendlyString() + " impedes route to destination, targeting for clearing.", Level.INFO);
                }
                
                // if any of the long range paths, pruned, are within LOS of leveling coordinates, then we're actually
                // just going to go back to the standard unit paths
                List<MovePath> prunedPaths = new ArrayList<>();
                for (BulldozerMovePath movePath : bulldozerPaths) {
                    BulldozerMovePath prunedPath = movePath.clone();
                    prunedPath.clipToPossible();

                    if (levelingTarget != null) {
                        LosEffects los = LosEffects.calculateLOS(game, mover, levelingTarget, prunedPath.getFinalCoords(), levelingTarget.getPosition(), false);

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

                    if (getHonorUtil().isEnemyBroken(entity.getId(), entity.getOwnerId(),
                            getForcedWithdrawal()) || !entity.isMilitary()) {
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
            LogManager.getLogger().info(msg.toString());
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
     * Update the various state trackers for a specific entity.
     * Useful to call when receiving an entity update packet
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

            // reset strategic targets
            fireControlState.setAdditionalTargets(new ArrayList<>());
            for (final Coords strategicTarget : getStrategicBuildingTargets()) {
                if (null == game.getBoard().getBuildingAt(strategicTarget)) {
                    fireControlState.getAdditionalTargets().add(
                            getAppropriateTarget(strategicTarget));
                    sendChat("No building to target in Hex " +
                             strategicTarget.toFriendlyString() +
                             ", targeting for clearing.", Level.INFO);
                } else {
                    fireControlState.getAdditionalTargets().add(
                            getAppropriateTarget(strategicTarget));
                    sendChat("Building in Hex " +
                             strategicTarget.toFriendlyString() +
                             " designated strategic target.", Level.INFO);
                }
            }

            // Pick up on any turrets and shoot their buildings as well.
            final Enumeration<Building> buildings = game.getBoard().getBuildings();
            while (buildings.hasMoreElements()) {
                final Building bldg = buildings.nextElement();
                final Enumeration<Coords> bldgCoords = bldg.getCoords();
                while (bldgCoords.hasMoreElements()) {
                    final Coords coords = bldgCoords.nextElement();
                    for (final Entity entity : game.getEntitiesVector(coords, true)) {
                        final Targetable bt = getAppropriateTarget(coords);
                        
                        if (isEnemyGunEmplacement(entity, coords)) {
                            fireControlState.getAdditionalTargets().add(bt);
                            sendChat("Building in Hex " + coords.toFriendlyString()
                                    + " designated target due to Gun Emplacement.", Level.INFO);
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
            final List<Targetable> potentialTargets = FireControl.getAllTargetableEnemyEntities(
                    getLocalPlayer(), getGame(), fireControlState);
            for (final Targetable target : potentialTargets) {
                damageMap.put(target.getId(), 0d);
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public Game getGame() {
        return game;
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

            // Pick up any turrets and add their buildings to the strategic targets list.
            final Enumeration<Building> buildings = getGame().getBoard().getBuildings();
            while (buildings.hasMoreElements()) {
                final Building bldg = buildings.nextElement();
                final Enumeration<Coords> bldgCoords = bldg.getCoords();
                while (bldgCoords.hasMoreElements()) {
                    final Coords coords = bldgCoords.nextElement();
                    for (final Entity entity : game.getEntitiesVector(coords, true)) {
                        if (isEnemyGunEmplacement(entity, coords)) {
                            getStrategicBuildingTargets().add(coords);
                            sendChat("Building in Hex " + coords.toFriendlyString()
                                    + " designated target due to Gun Emplacement.", Level.INFO);
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
     * Initialize the possible path rankers.
     * Has a dependency on the fire controls being initialized.
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
    }
    
    /**
     * Load the list of units considered crippled at the time the bot was loaded or the beginning of the turn,
     * whichever is the more recent.
     */
    public void refreshCrippledUnits() {
        // if we're not following 'forced withdrawal' rules, there's no need for this
        if (!getForcedWithdrawal()) {
            return;
        }
        
        // this approach is a little bit inefficient, but the running time is only O(n) where n is the number
        // of princess owned units, so it shouldn't be a big deal. 
        crippledUnits.clear();
        
        for (Entity e : this.getEntitiesOwned()) {
            if (e.isCrippled(true)) {
                crippledUnits.add(e.getId());
            }
        }
    }
    
    private boolean isEnemyGunEmplacement(final Entity entity,
                                          final Coords coords) {
        return entity.hasETypeFlag(Entity.ETYPE_GUN_EMPLACEMENT)
               && !getBehaviorSettings().getIgnoredUnitTargets().contains(entity.getId())
               && entity.getOwner().isEnemyOf(getLocalPlayer())
               && !getStrategicBuildingTargets().contains(coords)
               && !entity.isCrippled();
    }

    private boolean isEnemyInfantry(final Entity entity,
                                    final Coords coords) {
        return entity.hasETypeFlag(Entity.ETYPE_INFANTRY) && !entity.hasETypeFlag(Entity.ETYPE_MECHWARRIOR)
                && !getBehaviorSettings().getIgnoredUnitTargets().contains(entity.getId())
                && entity.getOwner().isEnemyOf(getLocalPlayer())
                && !getStrategicBuildingTargets().contains(coords)
                && !entity.isCrippled();
    }

    @Override
    public synchronized void die() {
        super.die();
        if (null != precognition) {
            precognition.signalDone();
            precogThread.interrupt();
        }
    }

    @Override
    protected void processChat(final GamePlayerChatEvent ge) {
        chatProcessor.processChat(ge, this);
    }
    
    /**
     * Given an entity and the current behavior settings, get the "home" edge to which the entity should attempt to retreat
     * Guaranteed to return a cardinal edge or NONE.
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

    public int calculateAdjustment(final String ticks) {
        int adjustment = 0;
        if (StringUtility.isNullOrBlank(ticks)) {
            return 0;
        }
        for (final char tick : ticks.toCharArray()) {
            if (PLUS == tick) {
                adjustment++;
            } else if (MINUS == tick) {
                adjustment--;
            } else {
                LogManager.getLogger().warn("Invalid tick: '" + tick + "'.");
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

    IHonorUtil getHonorUtil() {
        return honorUtil;
    }
    
    /**
     * Lazy-loaded calculation of the "to-hit target number" threshold for
     * spinning up a rapid fire autocannon.
     */
    public int getSpinupThreshold() {
        if (spinupThreshold == null) {
        // we start spinning up the cannon at 11+ TN at highest aggression levels
        // dropping it down to 6+ TN at the lower aggression levels
            spinupThreshold = Math.min(11, Math.max(getBehaviorSettings().getHyperAggressionIndex() + 2, 6));
        }
        
        return spinupThreshold;
    }
    
    public void resetSpinupThreshold() {
        spinupThreshold = null;
    }

    @Override
    public void endOfTurnProcessing() {
        checkForDishonoredEnemies();
        checkForBrokenEnemies();
        // refreshCrippledUnits should happen after checkForDishonoredEnemies, since checkForDishoneredEnemies
        // wants to examine the units that were considered crippled at the *beginning* of the turn and were attacked.
        refreshCrippledUnits();
    }

    @Override
    protected void handlePacket(final Packet c) {
        final StringBuilder msg = new StringBuilder("Received packet, cmd: " + c.getCommand());
        try {
            super.handlePacket(c);
            getPrecognition().handlePacket(c);
        }
        finally {
            LogManager.getLogger().trace(msg.toString());
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
            precogThread.interrupt();
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
     * Helper function to perform some modifications to a given path.
     * Intended to happen after we pick the best path. 
     * @param path The ranked path to process
     * @return Altered move path
     */
    private MovePath performPathPostProcessing(final RankedPath path) {
        return performPathPostProcessing(path.getPath(), path.getExpectedDamage());
    }
    
    /**
     * Helper function to perform some modifications to a given path.
     * @param path The move path to process
     * @param expectedDamage The damage expected to be done by the unit as a result of the path
     * @return Altered move path
     */
    private MovePath performPathPostProcessing(MovePath path, double expectedDamage) {
        MovePath retval = path;
        evadeIfNotFiring(retval, expectedDamage >= 0);
        turnOnSearchLight(retval, expectedDamage >= 0);
        unloadTransportedInfantry(retval);
        launchFighters(retval);
        unjamRAC(retval);
        
        // if we are using vector movement, there's a whole bunch of post-processing that happens to
        // aircraft flight paths when a player does it, so we apply it here.
        if (path.getEntity().isAero() || (path.getEntity() instanceof EjectedCrew && path.getEntity().isSpaceborne())) {
            retval = SharedUtility.moveAero(retval, null);
        }
        
        return retval;
    }
    
    /**
     * Helper function that appends an unjam RAC command to the end of a qualifying path.
     * @param path The path to process.
     */
    private void unjamRAC(MovePath path) { 
        if (path.getEntity().canUnjamRAC() &&
                (path.getMpUsed() <= path.getEntity().getWalkMP()) &&
                !path.isJumping()) {
            path.addStep(MoveStepType.UNJAM_RAC);
        }
    }
    
    /**
     * Helper function that insinuates an "evade" step for aircraft that will not be shooting.
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
     * @param path Path being considered
     * @param possibleToInflictDamage Whether we expect to be shooting at something.
     */
    private void turnOnSearchLight(MovePath path, boolean possibleToInflictDamage) {
        Entity pathEntity = path.getEntity();
        if (possibleToInflictDamage &&
                pathEntity.hasSearchlight() && 
                !pathEntity.isUsingSearchlight() &&
                (path.getGame().getPlanetaryConditions().getLight() >= PlanetaryConditions.L_FULL_MOON)) {
            path.addStep(MoveStepType.SEARCHLIGHT);
        }
    }
    
    /**
     * Helper function that adds an "unload" step for units that are transporting infantry
     * if the conditions for unloading are favorable.
     * 
     * Infantry unloading logic is different from, for example, hot-dropping mechs or launching aerospace fighters,
     * so we handle it separately.
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
        Targetable closestEnemy = getPathRanker(movingEntity).findClosestEnemy(movingEntity, pathEndpoint, getGame(), false);

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
                boolean unloadFatal = loadedEntity.isBoardProhibited(getGame().getBoard().getType())
                        || loadedEntity.isLocationProhibited(pathEndpoint)
                        || loadedEntity.isLocationDeadly(pathEndpoint);
                
                // Unloading a unit may sometimes cause a stacking violation, take that into account when planning
                boolean unloadIllegal = Compute.stackingViolation(getGame(), loadedEntity, pathEndpoint, movingEntity,
                        loadedEntity.climbMode()) != null;
                
                // this is a primitive condition that checks whether we're within "engagement range" of an enemy
                // where "engagement range" is defined as the maximum range of our weapons plus our walking movement
                boolean inEngagementRange = loadedEntity.getWalkMP() + getMaxWeaponRange(loadedEntity) >= distanceToClosestEnemy;
                
                if (!unloadFatal && !unloadIllegal && inEngagementRange) {
                    path.addStep(MoveStepType.UNLOAD, loadedEntity, pathEndpoint);
                    return; // we can only unload one infantry unit per hex per turn, so once we've unloaded, we're done. 
                }
            }
        }
    }
    
    /**
     * Helper function that adds an "launch" step for units that are transporting 
     * launchable units in some kind of bay.
     */
    private void launchFighters(MovePath path) {
        // if my objective is to cross the board, even though it's tempting, I won't be leaving the aerospace
        // behind. They're not that good at screening against high speed pursuit anyway.
        if (getBehaviorSettings().shouldAutoFlee()) {
            return;
        }
        
        Entity movingEntity = path.getEntity();
        Coords pathEndpoint = path.getFinalCoords();
        Targetable closestEnemy = getPathRanker(movingEntity).findClosestEnemy(movingEntity, pathEndpoint, getGame(), false);

        // if there are no enemies on the board, then we're not launching anything.
        if ((null == closestEnemy) || (closestEnemy.getTargetType() != Targetable.TYPE_ENTITY)) {
            return;
        }
        
        TreeMap<Integer, Vector<Integer>> unitsToLaunch = new TreeMap<>();
        boolean executeLaunch = false;
        
        // loop through all fighter (or smallcraft) bays in the current entity
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

    public void sendChat(final String message, final Level logLevel) {
        if (LogManager.getLogger().getLevel().isLessSpecificThan(logLevel)) {
            super.sendChat(message);
        }
    }

    /**
     * Override for the 'receive entity update' handler
     * Updates internal state in addition to base client functionality
     */
    @Override    
    public void receiveEntityUpdate(final Packet packet) {
        super.receiveEntityUpdate(packet);
        updateEntityState((Entity) packet.getObject(1));
    }
}
