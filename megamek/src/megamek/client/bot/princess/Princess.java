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

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import megamek.client.bot.BotClient;
import megamek.client.bot.ChatProcessor;
import megamek.client.bot.PhysicalOption;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Coords;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.Minefield;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.PilotingRollData;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.WeaponType;
import megamek.common.actions.EntityAction;
import megamek.common.containers.PlayerIDandList;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.logging.LogLevel;
import megamek.common.logging.Logger;
import megamek.common.net.Packet;
import megamek.common.util.StringUtil;
import megamek.common.weapons.AmmoWeapon;

public class Princess extends BotClient {

    private static final Logger logger = new Logger();

    private final IHonorUtil honorUtil = new HonorUtil();

    private boolean initialized = false;

    //private PathSearcher pathSearcher;
    private BasicPathRanker pathRanker;
    private FireControl fireControl;
    private BehaviorSettings behaviorSettings;
    private double moveEvaluationTimeEstimate = 0;
    private Precognition precognition;
    private Thread precogThread;
    /**
     * Mapping to hold the damage allocated to each targetable, stored by ID.
     * Used to allocate damage more intelligently and avoid overkill.
     */
    private ConcurrentHashMap<Integer, Double> damageMap = new ConcurrentHashMap<>(); 
    private final Set<Coords> strategicBuildingTargets = new HashSet<>();
    private boolean fallBack = false;
    protected ChatProcessor chatProcessor = new ChatProcessor();
    private boolean fleeBoard = false;
    private IMoralUtil moralUtil = new MoralUtil(logger);
    private final Set<Integer> attackedWhileFleeing =
            Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    private final Set<Integer> myFleeingEntities = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

    public Princess(String name, String host, int port, LogLevel verbosity) {
        super(name, host, port);
        logger.setVerbosity(verbosity);
        setBehaviorSettings(BehaviorSettingsFactory.getInstance(logger).DEFAULT_BEHAVIOR);
        
        // Start-up precog now, so that it can instantiate its game instance,
        // and it will stay up-to date.
        precognition = new Precognition(this);
        precogThread = new Thread(precognition, "Princess-precognition ("
                + getName() + ")");
        precogThread.start();
    }

    public void setVerbosity(LogLevel level) {
        logger.setVerbosity(level);
    }

    public LogLevel getVerbosity() {
        return logger.getVerbosity();
    }

    public BasicPathRanker getPathRanker() {
        return pathRanker;
    }

    public boolean getFallBack() {
        return fallBack;
    }

    public boolean getFleeBoard() {
        return fleeBoard;
    }

    public boolean getForcedWithdrawal() {
        return getBehaviorSettings().isForcedWithdrawal();
    }

    public void setFleeBoard(boolean fleeBoard, String reason) {
        log(getClass(), "setFleeBoard(boolean, String)", LogLevel.DEBUG, "Setting Flee Board " + fleeBoard +
                                                                         " because: " + reason);

        this.fleeBoard = fleeBoard;
    }

    protected Precognition getPrecognition() {
        return precognition;
    }

    public void setFallBack(boolean fallBack, String reason) {
        log(getClass(), "setFallBack(boolean, String)", LogLevel.DEBUG, "Setting Fall Back " + fallBack +
                                                                        " because: " + reason);
        this.fallBack = fallBack;
    }

    public void setBehaviorSettings(BehaviorSettings behaviorSettings) {
        log(getClass(), "setBehaviorSettings(BehaviorSettings)", LogLevel.INFO, "New behavior settings for " +
                                                                                getName() + "\n" +
                                                                                behaviorSettings.toLog());
        try {
            this.behaviorSettings = behaviorSettings.getCopy();
        } catch (PrincessException e) {
            log(getClass(), "setBehaviorSettings(BehaviorSettings)", e);
            return;
        }
        getStrategicBuildingTargets().clear();
        setFallBack(behaviorSettings.shouldGoHome(), "Fall Back Configuration.");
        setFleeBoard(behaviorSettings.shouldAutoFlee(), "Flee Board Configuration.");
        if (getFallBack()) {
            return;
        }

        for (String targetCoords : behaviorSettings.getStrategicBuildingTargets()) {
            if (!StringUtil.isPositiveInteger(targetCoords) || (targetCoords.length() != 4)) {
                continue;
            }
            String x = targetCoords.substring(0, 2);
            String y = targetCoords.replaceFirst(x, "");
            // Need to subtract 1, since we are given a Hex number string, which is Coords X+1Y+1 
            Coords coords = new Coords(Integer.parseInt(x) - 1, Integer.parseInt(y) - 1);
            getStrategicBuildingTargets().add(coords);
        }
    }

    public FireControl getFireControl() {
        return fireControl;
    }

    public ConcurrentHashMap<Integer,Double> getDamageMap() {
        return damageMap;
    }

    public double getDamageAlreadyAssigned(Targetable target) {
        Integer targetId = new Integer(target.getTargetId());
        if(damageMap.containsKey(targetId)) {
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

    public void addStrategicBuildingTarget(Coords coords) {
        if (coords == null) {
            throw new NullPointerException("Coords is null.");
        }
        if (!getGame().getBoard().contains(coords)) {
            log(getClass(), "addStrategicBuildingTarget(Coords)", LogLevel.WARNING,
                "Board does not contain " + coords.toFriendlyString());
            return;
        }
        getStrategicBuildingTargets().add(coords);
    }

    public Set<Integer> getPriorityUnitTargets() {
        return getBehaviorSettings().getPriorityUnitTargets();
    }

    @Override
    protected Vector<Coords> calculateArtyAutoHitHexes() {
        final String METHOD_NAME = "calculateArtyAutoHitHexes()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // currently returns no artillery hit spots
            // make an empty list
            PlayerIDandList<Coords> artyAutoHitHexes = new PlayerIDandList<>();
            // attach my player id to it
            artyAutoHitHexes.setPlayerID(getLocalPlayer().getId());
            return artyAutoHitHexes;
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected void calculateDeployment() {
        final String METHOD_NAME = "calculateDeployment()";
        methodBegin(getClass(), METHOD_NAME);

        try {

            // get the first unit
            int entityNum = game.getFirstDeployableEntityNum();
            if (logger.getVerbosity().getLevel() > LogLevel.WARNING.getLevel()) {
                sendChat("deploying unit " + getEntity(entityNum).getChassis());
            }

            // on the list to be deployed get a set of all the
            List<Coords> startingCoords = getStartingCoordsArray();
            if (startingCoords.size() == 0) {
                log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "No valid locations to deploy " + getEntity(entityNum).getDisplayName());
            }

            // get the coordinates I can deploy on
            Coords deployCoords = getCoordsAround(getEntity(entityNum), startingCoords);
            if (deployCoords == null) {
                log(getClass(),
                    METHOD_NAME,
                    LogLevel.ERROR,
                    "getCoordsAround gave no location for "
                    + getEntity(entityNum).getChassis());
                return;
            }

            // first coordinate that it is legal to put this unit on now find some sort of reasonable facing. If there
            // are deployed enemies, face them
            int decentFacing = -1;
            for (Entity e : getEnemyEntities()) {
                if (e.isDeployed() && (!e.isOffBoard())) {
                    decentFacing = deployCoords.direction(e.getPosition());
                    break;
                }
            }

            // if I haven't found a decent facing, then at least face towards the center of the board
            if (decentFacing == -1) {
                Coords center = new Coords(game.getBoard().getWidth() / 2, game
                                                                                   .getBoard().getHeight() / 2);
                decentFacing = deployCoords.direction(center);
            }

            Entity deployEntity = game.getEntity(entityNum);
            IHex deployHex = game.getBoard().getHex(deployCoords);

            // Entity.elevatoinOccupied performs a null check on IHex
            int deployElevation = deployEntity.elevationOccupied(deployHex);

            // Compensate for hex elevation where != 0...
            deployElevation -= deployHex.getLevel();
            deploy(entityNum, deployCoords, decentFacing, deployElevation);
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected void calculateFiringTurn() {
        final String METHOD_NAME = "calculateFiringTurn()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // get the first entity that can act this turn make sure weapons are loaded
            Entity shooter = game.getFirstEntity(getMyTurn());

            // If my unit is forced to withdraw, don't fire unless I've been fired on.
            if (getForcedWithdrawal() && shooter.isCrippled()) {
                StringBuilder msg = new StringBuilder(shooter.getDisplayName()).append(" is crippled and withdrawing.");
                try {
                    if (attackedWhileFleeing.contains(shooter.getId())) {
                        msg.append("\n\tBut I was fired on, so I will return fire.");
                    } else {
                        msg.append("\n\tI will not fire so long as I'm not fired on.");
                        sendAttackData(shooter.getId(), new Vector<EntityAction>(0));
                        return;
                    }
                } finally {
                    log(getClass(), METHOD_NAME, LogLevel.INFO, msg);
                }
            }

            // Set up ammo conservation.
            Map<Mounted, Double> ammoConservation = calcAmmoConservation(shooter);

            // entity that can act this turn make sure weapons are loaded
            FiringPlan plan = fireControl.getBestFiringPlan(shooter, getHonorUtil(), game, ammoConservation);
            if (plan != null) {
                fireControl.loadAmmo(shooter, plan);
                plan.sortPlan();

                log(getClass(), METHOD_NAME, LogLevel.INFO, shooter.getDisplayName() + " - Best Firing Plan: " +
                                                            plan.getDebugDescription(LogLevel.DEBUG == getVerbosity()));

                // Add expected damage from the chosen FiringPlan to the damageMap for the target enemy.
                Integer targetId = new Integer(plan.getTarget().getTargetId());
                Double newDamage = damageMap.get(targetId)+plan.getExpectedDamage();
                damageMap.replace(targetId,newDamage);

                // tell the game I want to fire
                sendAttackData(shooter.getId(), plan.getEntityActionVector());

            } else {
                log(getClass(), METHOD_NAME, LogLevel.INFO, "No best firing plan for " + shooter.getDisplayName());
                sendAttackData(shooter.getId(), new Vector<EntityAction>(0));
            }
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    Map<Mounted, Double> calcAmmoConservation(Entity shooter) {
        final String METHOD_NAME = "calcAmmoConservation(Entity)";
        final double aggroFactor = (10 - getBehaviorSettings().getHyperAggressionIndex()) * 2;
        StringBuilder msg = new StringBuilder("\nCalculating ammo conservation for ").append(shooter.getDisplayName());
        msg.append("\nAggression Factor = ").append(aggroFactor);

        try {
            Map<AmmoType, Integer> ammoCounts = new HashMap<>();
            msg.append("\nPooling Ammo:");
            for (Mounted ammo : shooter.getAmmo()) {
                AmmoType ammoType = (AmmoType) ammo.getType();
                msg.append("\n\t").append(ammoType.toString());
                if (ammoCounts.containsKey(ammoType)) {
                    ammoCounts.put(ammoType, ammoCounts.get(ammoType) + ammo.getUsableShotsLeft());
                    msg.append(" + ").append(ammo.getUsableShotsLeft()).append(" = ").append(ammoCounts.get(ammoType));
                    continue;
                }
                ammoCounts.put(ammoType, ammo.getUsableShotsLeft());
                msg.append(" + ").append(ammo.getUsableShotsLeft()).append(" = ").append(ammoCounts.get(ammoType));
            }

            Map<Mounted, Double> ammoConservation = new HashMap<>();
            msg.append("\nCalculating conservation for each weapon");
            for (Mounted weapon : shooter.getWeaponList()) {
                WeaponType weaponType = (WeaponType) weapon.getType();
                msg.append("\n\t").append(weaponType.toString());
                if (!(weaponType instanceof AmmoWeapon)) {
                    ammoConservation.put(weapon, 0.0);
                    msg.append(" doesn't use ammo.");
                    continue;
                }

                int ammoCount = 0;
                for (AmmoType ammoType : ammoCounts.keySet()) {
                    if (!AmmoType.isAmmoValid(ammoType, weaponType)) {
                        continue;
                    }
                    ammoCount += ammoCounts.get(ammoType);
                }
                msg.append(" has ").append(ammoCount).append(" shots left");
                double toHitThreshold = Math.max(0.0, 1 - (ammoCount / aggroFactor));
                msg.append("; To Hit Threshold = ").append(new DecimalFormat("0.000").format(toHitThreshold));
                ammoConservation.put(weapon, toHitThreshold);
            }

            return ammoConservation;
        } finally {
            log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg);
        }
    }

    @Override
    protected Vector<Minefield> calculateMinefieldDeployment() {
        final String METHOD_NAME = "calculateMinefieldDeployment()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // currently returns no minefields
            // make an empty vector
            return new Vector<>();
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Calculates the move index for the given unit.
     * In general, faster units and units closer to the enemy should move before others.
     * Additional modifiers for being prone, stealthed, unit type and so on are also factored in.
     *
     * @param entity The unit to be indexed.
     * @return The movement index of this unit.  May be positive or negative.  Higher index values should move first.
     */
    protected double calculateMoveIndex(Entity entity, StringBuilder msg) {
        msg.append("\n\tCalculating move index for ").append(entity.getDisplayName());
        StringBuilder modifiers = new StringBuilder();
        NumberFormat numberFormat = DecimalFormat.getInstance();
        double total = 0;
        try {
            // Find out how fast this unit can move.
            int fastestMove = entity.getRunMP(true, false, false);
            if (entity.getJumpMP(true) > fastestMove) {
                fastestMove = entity.getJumpMP(true);
            }
            msg.append("\n\t\tFastest Move = ").append(fastestMove);

            // Get the distance to the nearest enemy.
            double distance = getPathRanker().distanceToClosestEnemy(entity, entity.getPosition(), game);
            msg.append("\n\t\tDistance to Nearest Enemy: ").append(numberFormat.format(distance));

            // Get the ratio of distance to speed.
            // Faster units that are closer to the enemy should move later.
            if (fastestMove == 0) {
                total = distance * 2; // This unit should have already moved due to the isImmobilized check.
            } else {
                total = distance / fastestMove;
            }
            msg.append("\n\t\tDistance to Move Ratio (dist / move): ").append(numberFormat.format(total));

            // Prone enemies move sooner.
            if (entity.isProne()) {
                total *= 1.1;
                modifiers.append("\tx1.1 (Is Prone)");
            }

            // If all else is equal, Infantry before Battle Armor before Tanks before Mechs.
            if (entity instanceof BattleArmor) {
                total *= 2;
                modifiers.append("\tx2.0 (is BA)");
            } else if (entity instanceof Infantry) {
                total *= 3;
                modifiers.append("\tx3.0 (is Inf)");
            } else if (entity instanceof Tank) {
                total *= 1.5;
                modifiers.append("\tx1.5 (is Tank)");
            }

            // Fleeing entities should move before those not fleeing.
            if (isFallingBack(entity)) {
                total *= 2;
                modifiers.append("\tx2.0 (is Fleeing)");
            }

            // Move commanders after other units.
            if (entity.isCommander()) {
                total /= 2;
                modifiers.append("\tx0.5 (is Commander)");
            }

            // Move civilian units before military.
            if (!entity.isMilitary()) {
                total *= 5;
                modifiers.append("\tx5.0 (is Civilian)");
            }

            // Move stealthy units later.
            if (entity.isStealthActive() || entity.isStealthOn() || entity.isVoidSigActive() || entity.isVoidSigOn()) {
                total /= 3;
                modifiers.append("\tx1/3 (is Stealthed)");
            }

            return total;
        } finally {
            msg.append("\n\t\tModifiers:").append(modifiers);
            msg.append("\n\t\tTotal = ").append(numberFormat.format(total));
        }
    }

    /**
     * Loops through the list of entities controlled by this Princess instance and decides which should be moved first.
     * Immobile units and ejected mechwarriors/crews will be moved first.  After that, each unit is given an index
     * via the {@link #calculateMoveIndex(Entity, StringBuilder)} method.  The highest index value is moved first.
     *
     * @return The entity that should be moved next.
     */
    protected Entity getEntityToMove() {

        // first move useless units: immobile units, ejected mechwarrior, etc
        Entity movingEntity = null;
        List<Entity> myEntities = getEntitiesOwned();
        double highestIndex = -10000.0;
        StringBuilder msg = new StringBuilder("Deciding who to move next.");
        for (Entity entity : myEntities) {
            msg.append("\n\tUnit ").append(entity.getDisplayName());
            if (entity.isOffBoard() || (entity.getPosition() == null)
                || !entity.isSelectableThisTurn()
                || !getGame().getTurn().isValidEntity(entity, getGame())) {
                msg.append("cannot be moved.");
                continue;
            }

            // Move immobile units & ejected mechwarriors immediately.
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

            // If I only have 1 unit, no need to calculate an index.
            if (myEntities.size() == 1) {
                msg.append("is my only unit.");
                movingEntity = entity;
                break;
            }

            // We will move the entity with the highest index.
            double moveIndex = calculateMoveIndex(entity, msg);
            msg.append("\n\thas index ").append(moveIndex).append(" vs ").append(highestIndex);
            if (moveIndex >= highestIndex) {
                highestIndex = moveIndex;
                movingEntity = entity;
            }
        }

        LogLevel level = (movingEntity == null ? LogLevel.WARNING : LogLevel.DEBUG);
        log(getClass(), "getEntityToMove()", level, msg.toString());

        return movingEntity;
    }

    @Override
    protected MovePath calculateMoveTurn() {
        final String METHOD_NAME = "calculateMoveTurn()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            return continueMovementFor(getEntityToMove());
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected PhysicalOption calculatePhysicalTurn() {
        final String METHOD_NAME = "calculatePhysicalTurn()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // get the first entity that can act this turn
            Entity attacker = game.getFirstEntity(getMyTurn());

            // If my unit is forced to withdraw, don't attack unless I've been attacked.
            if (getForcedWithdrawal() && attacker.isCrippled()) {
                StringBuilder msg = new StringBuilder(attacker.getDisplayName()).append(" is crippled and withdrawing.");
                if (attackedWhileFleeing.contains(attacker.getId())) {
                    msg.append("\n\tBut I was fired on, so I will hit back.");
                } else {
                    msg.append("\n\tI will not attack so long as I'm not fired on.");
                    return null;
                }
                log(getClass(), METHOD_NAME, LogLevel.INFO, msg);
            }

            PhysicalInfo best_attack = null;
            int firstEntityId = attacker.getId();
            int nextEntityId = firstEntityId;

            // this is an array of all my enemies
            List<Entity> enemies = getEnemyEntities();

            do {
                Entity hitter = game.getEntity(nextEntityId);
                nextEntityId = game.getNextEntityNum(hitter.getId());

                if (hitter.getPosition() == null) {
                    continue;
                }

                log(getClass(), METHOD_NAME, LogLevel.DEBUG,
                    "Calculating physical attacks for " + hitter.getDisplayName());

                // cycle through potential enemies
                for (Entity e : enemies) {
                    if (e.getPosition() == null) {
                        continue; // Skip enemies not on the board.
                    }
                    if (hitter.getPosition().distance(e.getPosition()) > 1) {
                        continue;
                    }
                    if (getHonorUtil().isEnemyBroken(e.getTargetId(), e.getOwnerId(), getForcedWithdrawal())) {
                        continue;
                    }

                    PhysicalInfo right_punch = new PhysicalInfo(hitter, e, PhysicalAttackType.RIGHT_PUNCH, game, this,
                                                                false);
                    fireControl.calculateUtility(right_punch);
                    if (right_punch.getUtility() > 0) {
                        if ((best_attack == null) || (right_punch.getUtility() > best_attack.getUtility())) {
                            best_attack = right_punch;
                        }
                    }
                    PhysicalInfo left_punch = new PhysicalInfo(
                            hitter, e, PhysicalAttackType.LEFT_PUNCH, game, this, false);
                    fireControl.calculateUtility(left_punch);
                    if (left_punch.getUtility() > 0) {
                        if ((best_attack == null)
                            || (left_punch.getUtility() > best_attack.getUtility())) {
                            best_attack = left_punch;
                        }
                    }
                    PhysicalInfo right_kick = new PhysicalInfo(
                            hitter, e, PhysicalAttackType.RIGHT_KICK, game, this, false);
                    fireControl.calculateUtility(right_kick);
                    if (right_kick.getUtility() > 0) {
                        if ((best_attack == null)
                            || (right_kick.getUtility() > best_attack.getUtility())) {
                            best_attack = right_kick;
                        }
                    }
                    PhysicalInfo left_kick = new PhysicalInfo(
                            hitter, e, PhysicalAttackType.LEFT_KICK, game, this, false);
                    fireControl.calculateUtility(left_kick);
                    if (left_kick.getUtility() > 0) {
                        if ((best_attack == null)
                            || (left_kick.getUtility() > best_attack.getUtility())) {
                            best_attack = left_kick;
                        }
                    }

                }
                if (best_attack != null) {
                    log(getClass(), METHOD_NAME, LogLevel.INFO, "Best Physical Attack is " +
                                                                best_attack.getDebugDescription());
                } else {
                    log(getClass(), METHOD_NAME, LogLevel.INFO, "No useful physical attack to be made");
                }
                if (best_attack != null) {
                    return best_attack.getAsPhysicalOption();
                }
            } while (nextEntityId != firstEntityId);

            // no one can hit anything anymore, so give up
            return null;
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    protected boolean wantsToFallBack(Entity entity) {
        return (entity.isCrippled() && getForcedWithdrawal()) || getFallBack();
    }

    protected IMoralUtil getMoralUtil() {
        return moralUtil;
    }

    protected boolean isFallingBack(Entity entity) {
        return getMyFleeingEntities().contains(entity.getId());
    }

    protected boolean mustFleeBoard(Entity entity) {
        if (!isFallingBack(entity)) {
            return false;
        }
        if (!entity.canFlee()) {
            return false;
        }
        if (getPathRanker().distanceToHomeEdge(entity.getPosition(), getHomeEdge(), getGame()) > 0) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (!getFleeBoard() && !(entity.isCrippled() && getForcedWithdrawal())) {
            return false;
        }
        return true;
    }

    protected boolean isImmobilized(Entity mover) {
        final String METHOD_NAME = "isImmobilized(Entity, MovePath)";
        if (mover.isImmobile() && !mover.isShutDown()) {
            log(getClass(), METHOD_NAME, LogLevel.INFO, "Is truly immobile.");
            return true;
        }
        if (mover.getRunMP() < 1) {
            log(getClass(), METHOD_NAME, LogLevel.INFO, "Has 0 movement.");
            return true;
        }
        if (!(mover instanceof Mech)) {
            return false;
        }

        Mech mech = (Mech) mover;
        if (!mech.isProne() && !mech.isStuck() && !mech.isStalled()) {
            return false;
        }

        MovePath movePath = new MovePath(getGame(), mover);

        // For a normal fall-shame setting (index 5), our threshold should be a 10+ piloting roll.
        int threshold;
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
                log(getClass(), METHOD_NAME, LogLevel.INFO, "Cannot stand up.");
                return true;
            }

            MovePath.MoveStepType type = (getBooleanOption("tacops_careful_stand") ?
                                          MovePath.MoveStepType.CAREFUL_STAND :
                                          MovePath.MoveStepType.GET_UP);
            MoveStep getUp = new MoveStep(movePath, type);

            // If our odds to get up are equal to or worse than the threshold, consider ourselves immobile.
            PilotingRollData target = mech.checkGetUp(getUp,
                    movePath.getLastStepMovementType());
            log(getClass(), METHOD_NAME, LogLevel.INFO,
                "Need to roll " + target.getValue() + " to stand and our tolerance is " + threshold);
            return (target.getValue() >= threshold);
        }

        // How likely are we to get unstuck.
        MovePath.MoveStepType type = MovePath.MoveStepType.FORWARDS;
        MoveStep walk = new MoveStep(movePath, type);
        IHex hex = getHex(mech.getPosition());
        PilotingRollData target = mech.checkBogDown(walk,
                movePath.getLastStepMovementType(), hex,
                mech.getPriorPosition(), mech.getPosition(), hex.getLevel(),
                false);
        log(getClass(), METHOD_NAME, LogLevel.INFO,
            "Need to roll " + target.getValue() + " to get unstuck and our tolerance is " + threshold);
        return (target.getValue() >= threshold);
    }

    protected boolean getBooleanOption(String name) {
        return getGame().getOptions().booleanOption(name);
    }

    protected IHex getHex(Coords coords) {
        return getBoard().getHex(coords);
    }

    protected ArrayList<RankedPath> rankPaths(List<MovePath> paths, int maxRange, double fallTollerance,
                                              int startingHomeDistance,
                                              List<Entity> enemies, List<Entity> friends) {
        return getPathRanker().rankPaths(paths, getGame(), maxRange, fallTollerance, startingHomeDistance,
                                         enemies, friends);
    }

    @Override
    protected MovePath continueMovementFor(Entity entity) {
        final String METHOD_NAME = "continueMovementFor(Entity)";
        methodBegin(getClass(), METHOD_NAME);

        if (entity == null) {
            throw new NullPointerException("Entity is null.");
        }

        try {
            // figure out who moved last, and who's move lists need to be updated

            // moves this entity during movement phase
            log(getClass(), METHOD_NAME, "Moving " + entity.getDisplayName() + " (ID " + entity.getId() + ")");
            getPrecognition().insureUpToDate();

            if (isFallingBack(entity)) {
                String msg = entity.getDisplayName();
                if (getFallBack()) {
                    msg += " is falling back.";
                } else if (entity.isCrippled()) {
                    msg += " is crippled and withdrawing.";
                }
                log(getClass(), METHOD_NAME, msg);
                sendChat(msg);

                // If this entity is falling back, able to flee the board, on its home edge, and must flee, do so.
                if (mustFleeBoard(entity)) {
                    MovePath mp = new MovePath(game, entity);
                    mp.addStep(MovePath.MoveStepType.FLEE);
                    return mp;
                }

                // If we want to flee, but cannot, eject the crew.
                if (isImmobilized(entity) && entity.isEjectionPossible()) {
                    msg = entity.getDisplayName() + " is immobile.  Abandoning unit.";
                    log(getClass(), METHOD_NAME, LogLevel.INFO, msg);
                    sendChat(msg);
                    MovePath mp = new MovePath(game, entity);
                    mp.addStep(MovePath.MoveStepType.EJECT);
                    return mp;
                }
            }

            List<MovePath> paths = getPrecognition().getPathEnumerator().getUnitPaths().get(entity.getId());

            if (paths == null) {
                log(getClass(), METHOD_NAME, LogLevel.WARNING,
                    "No valid paths found.");
                return new MovePath(game, entity);
            }

            double thisTimeEstimate = (paths.size() * moveEvaluationTimeEstimate) / 1e3;
            if (logger.getVerbosity().getLevel() > LogLevel.WARNING.getLevel()) {
                String timeestimate = "unknown.";
                if (thisTimeEstimate != 0) {
                    timeestimate = Integer.toString((int) thisTimeEstimate)
                                   + " seconds";
                }
                String message = "Moving " + entity.getChassis() + ". "
                                 + Long.toString(paths.size())
                                 + " paths to consider.  Estimated time to completion: "
                                 + timeestimate;
                sendChat(message);
            }

            long startTime = System.currentTimeMillis();
            getPathRanker().initUnitTurn(entity, getGame());
            double fallTolerance = getBehaviorSettings().getFallShameIndex() / 10d;
            int startingHomeDistance = getPathRanker().distanceToHomeEdge(entity.getPosition(),
                                                                          getBehaviorSettings().getHomeEdge(),
                                                                          getGame());
            List<RankedPath> rankedpaths = rankPaths(paths, entity.getMaxWeaponRange(), fallTolerance,
                                                     startingHomeDistance, getEnemyEntities(),
                                                     getFriendEntities());
            long stop_time = System.currentTimeMillis();

            // update path evaluation time estimate
            double updatedEstimate = ((double) (stop_time - startTime)) / ((double) paths.size());
            if (moveEvaluationTimeEstimate == 0) {
                moveEvaluationTimeEstimate = updatedEstimate;
            }
            moveEvaluationTimeEstimate = 0.5 * (updatedEstimate + moveEvaluationTimeEstimate);
            if (rankedpaths.size() == 0) {
                return new MovePath(game, entity);
            }
            log(getClass(), METHOD_NAME, "Path ranking took " + Long.toString(stop_time - startTime) + " millis");
            RankedPath bestpath = getPathRanker().getBestPath(rankedpaths);
            log(getClass(), METHOD_NAME, LogLevel.INFO, "Best Path: " + bestpath.path.toString() + "  Rank: "
                                                        + bestpath.rank);
            return bestpath.path;
        } finally {
            precognition.unPause();
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected void initFiring() {
        final String METHOD_NAME = "initFiring()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            initialize();

            // ----Debugging: print out any errors made in guessing to hit
            // values-----
            List<Entity> ents = game.getEntitiesVector();
            for (Entity ent : ents) {
                String errors = fireControl.checkAllGuesses(ent, game);
                if (errors != null) {
                    log(getClass(), METHOD_NAME, LogLevel.WARNING, errors);
                }
            }
            // -----------------------------------------------------------------------

            // Pick up on any infantry/BA in buildings post-movement and shoot
            // their buildings, similar to the turret check
            // pre-movement(infantry can move so we only set target buildings
            // after they do).
            Enumeration<Building> buildings = game.getBoard().getBuildings();
            while (buildings.hasMoreElements()) {
                Building bldg = buildings.nextElement();
                Enumeration<Coords> bldgCoords = bldg.getCoords();
                while (bldgCoords.hasMoreElements()) {
                    Coords coords = bldgCoords.nextElement();
                    for (Entity entity : game.getEntitiesVector(coords)) {
                        BuildingTarget bt = new BuildingTarget(coords,
                                game.getBoard(), false);
                        // Want to target buildings with hostile infantry/BA
                        // inside them, since there's no other way to attack
                        // them.
                        if (isEnemyInfantry(entity, coords)
                                && Compute.isInBuilding(game, entity)) {
                            fireControl.getAdditionalTargets().add(bt);
                            sendChat("Building in Hex "
                                    + coords.toFriendlyString()
                                    + " designated target due to"
                                    + " infantry inside building.");
                        }
                    }
                }
            }

            //Next, collect the ID's of each potential target and store them in the damageMap for allocating damage during firing.

            //Reset the map generated during the movement phase- The available targets may have changed during that time(ejections, enemies fleeing, etc).
            damageMap.clear();
            //Now add an ID for each possible target.
            List<Targetable> potentialTargets = fireControl.getAllTargetableEnemyEntities(getLocalPlayer(), getGame());
            for (Targetable target : potentialTargets) {
                damageMap.put(new Integer(target.getTargetId()), new Double(0));
            }

        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    private void checkForDishonoredEnemies() {
        final String METHOD_NAME = "checkForDishonoredEnemies()";

        StringBuilder msg = new StringBuilder("Checking for dishonored enemies.");

        try {
            // If the Forced Withdrawal rule is not turned on, then it's a fight to the death anyway.
            if (!getForcedWithdrawal()) {
                msg.append("\n\tForced withdrawal turned off.");
                return;
            }

            for (Entity mine : getEntitiesOwned()) {

                // Who just attacked me?
                Collection<Integer> attackedBy = mine.getAttackedByThisTurn();
                if (attackedBy.isEmpty()) {
                    continue;
                }

                // Is my unit trying to withdraw?
                boolean fleeing = getMyFleeingEntities().contains(mine.getId());

                for (int id : attackedBy) {
                    Entity entity = getGame().getEntity(id);
                    if (entity == null) {
                        continue;
                    }

                    if (getHonorUtil().isEnemyBroken(entity.getTargetId(), entity.getOwnerId(),
                                                     getForcedWithdrawal()) || !entity.isMilitary()) {
                        // If he'd just continued running, I would have let him go, but the bastard shot at me!
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
            log(getClass(), METHOD_NAME, LogLevel.INFO, msg);
        }
    }

    private void checkForBrokenEnemies() {

        // If the Forced Withdrawal rule is not turned on, then it's a fight to the death anyway.
        if (!getForcedWithdrawal()) {
            return;
        }

        for (Entity entity : getEnemyEntities()) {
            getHonorUtil().checkEnemyBroken(entity, getForcedWithdrawal());
        }
    }

    @Override
    protected void initMovement() {
        final String METHOD_NAME = "initMovement()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            initialize();
            checkMoral();

            // reset strategic targets
            fireControl.setAdditionalTargets(new ArrayList<Targetable>());
            for (Coords strategicTarget : getStrategicBuildingTargets()) {
                if (game.getBoard().getBuildingAt(strategicTarget) == null) {
                    sendChat("No building to target in Hex " + strategicTarget.toFriendlyString() + ", ignoring.");
                } else {
                    fireControl.getAdditionalTargets().add(new BuildingTarget(strategicTarget, game.getBoard(), false));
                    sendChat("Building in Hex " + strategicTarget.toFriendlyString() + " designated strategic target.");
                }
            }

            // Pick up on any turrets and shoot their buildings as well.
            Enumeration<Building> buildings = game.getBoard().getBuildings();
            while (buildings.hasMoreElements()) {
                Building bldg = buildings.nextElement();
                Enumeration<Coords> bldgCoords = bldg.getCoords();
                while (bldgCoords.hasMoreElements()) {
                    Coords coords = bldgCoords.nextElement();
                    for (Entity entity : game.getEntitiesVector(coords, true)) {
                        BuildingTarget bt = new BuildingTarget(coords, game.getBoard(), false);
                        if (isEnemyGunEmplacement(entity, coords)) {
                            fireControl.getAdditionalTargets().add(bt);
                            sendChat("Building in Hex " + coords.toFriendlyString()
                                     + " designated target due to Gun Emplacement.");
                        }
                    }
                }
            }

            //Next, collect the ID's of each potential target and store them in the damageMap for allocating damage during movement.
            //Right now, this doesn't get filled because I can't find where FiringPlans for potential move paths are calculated(pretty sure they are, though). This needs to be fixed at some point.

            //Reset last round's damageMap
            damageMap.clear();
            //Now add an ID for each possible target.
            List<Targetable> potentialTargets = fireControl.getAllTargetableEnemyEntities(getLocalPlayer(), getGame());
            for (Targetable target : potentialTargets) {
                damageMap.put(new Integer(target.getTargetId()), new Double(0));
            }

        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    public IGame getGame() {
        return game;
    }

    @Override
    public void initialize() {
        final String METHOD_NAME = "initialize()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            if (initialized) {
                return; // no need to initialize twice
            }
            PathSearcher pathSearcher = new PathSearcher(this);
            pathRanker = new BasicPathRanker(this);
            pathSearcher.ranker = pathRanker;
            fireControl = new FireControl(this);
            pathRanker.setFireControl(fireControl);
            pathRanker.setPathEnumerator(precognition.getPathEnumerator());

            // Pick up any turrets and add their buildings to the strategic targets list.
            Enumeration<Building> buildings = getGame().getBoard().getBuildings();
            while (buildings.hasMoreElements()) {
                Building bldg = buildings.nextElement();
                Enumeration<Coords> bldgCoords = bldg.getCoords();
                while (bldgCoords.hasMoreElements()) {
                    Coords coords = bldgCoords.nextElement();
                    for (Entity entity : game.getEntitiesVector(coords, true)) {
                        if (isEnemyGunEmplacement(entity, coords)) {
                            getStrategicBuildingTargets().add(coords);
                            sendChat("Building in Hex " + coords.toFriendlyString() +
                                     " designated target due to Gun Emplacement.");
                        }
                    }
                }
            }

            initialized = true;
            BotGeometry.debugSelfTest(this);
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    private boolean isEnemyGunEmplacement(Entity entity, Coords coords) {
        return entity instanceof GunEmplacement
               && entity.getOwner().isEnemyOf(getLocalPlayer())
               && !getStrategicBuildingTargets().contains(coords)
               && (entity.getCrew() != null) && !entity.getCrew().isDead();
    }

    private boolean isEnemyInfantry(Entity entity, Coords coords) {
        return (entity instanceof Infantry
                || entity instanceof BattleArmor)
                && entity.getOwner().isEnemyOf(getLocalPlayer())
                && !getStrategicBuildingTargets().contains(coords);
    }

    @Override
    public synchronized void die() {
        super.die();
        if (precognition != null) {
            precognition.signalDone();
            precogThread.interrupt();
        }
    }

    @Override
    protected void processChat(GamePlayerChatEvent ge) {
        chatProcessor.processChat(ge, this);
    }

    public void log(Class<?> callingClass, String methodName, LogLevel level,
                    String msg) {
        logger.log(callingClass, methodName, level, msg);
    }

    public void log(Class<?> callingClass, String methodName, LogLevel level, StringBuilder msg) {
        if (msg == null) {
            return;
        }
        log(callingClass, methodName, level, msg.toString());
    }

    public void log(Class<?> callingClass, String methodName, String msg) {
        log(callingClass, methodName, LogLevel.DEBUG, msg);
    }

    public void log(Class<?> callingClass, String methodName, LogLevel level,
                    Throwable t) {
        logger.log(callingClass, methodName, level, t);
    }

    public void log(Class<?> callingClass, String methodName, Throwable t) {
        log(callingClass, methodName, LogLevel.ERROR, t);
    }

    public void methodBegin(Class<?> callingClass, String methodName) {
        log(callingClass, methodName, LogLevel.DEBUG, "method begin");
    }

    public void methodEnd(Class<?> callingClass, String methodName) {
        log(callingClass, methodName, LogLevel.DEBUG, "method end");
    }

    public HomeEdge getHomeEdge() {
        return getBehaviorSettings().getHomeEdge();
    }

    public int calculateAdjustment(String ticks) {
        int adjustment = 0;
        if (StringUtil.isNullOrEmpty(ticks)) {
            return 0;
        }
        for (char tick : ticks.toCharArray()) {
            if ('+' == tick) {
                adjustment++;
            } else if ('-' == tick) {
                adjustment--;
            } else {
                log(getClass(), "calculateAdjustment", LogLevel.WARNING, "Invalid tick: '" + tick + "'.");
            }
        }
        return adjustment;
    }

    @Override
    protected void checkMoral() {
        moralUtil.checkMoral(behaviorSettings.isForcedWithdrawal(), behaviorSettings.getBraveryIndex(),
                             behaviorSettings.getSelfPreservationIndex(), getLocalPlayer(), game);
    }

    public IHonorUtil getHonorUtil() {
        return honorUtil;
    }

    @Override
    public void endOfTurnProcessing() {
        logger.methodBegin(getClass(), "endOfTurnProcessing()");
        checkForDishonoredEnemies();
        updateMyFleeingEntities();
        checkForBrokenEnemies();
        logger.methodEnd(getClass(), "endOfTurnProcessing()");
    }

    Set<Integer> getMyFleeingEntities() {
        return myFleeingEntities;
    }

    private void updateMyFleeingEntities() {
        final String METHOD_NAME = "updateMyFleeingEntities()";

        StringBuilder msg = new StringBuilder("Updating my list of falling back units.");

        try {
            // If the Forced Withdrawal rule is not turned on, then it's a fight to the death anyway.
            if (!getForcedWithdrawal()) {
                msg.append("\n\tForced withdrawal turned off.");
                return;
            }

            for (Entity mine : getEntitiesOwned()) {
                if (myFleeingEntities.contains(mine.getId())) {
                    continue;
                }
                if (wantsToFallBack(mine)) {
                    msg.append("\n\tAdding ").append(mine.getDisplayName());
                    myFleeingEntities.add(mine.getId());
                }
            }
        } finally {
            log(getClass(), METHOD_NAME, LogLevel.INFO, msg);
        }
    }
    
    protected void handlePacket(Packet c) {
        final String METHOD_NAME = "handlePacket()";
        StringBuilder msg = new StringBuilder("Received packet, cmd: "
                + c.getCommand());
        try {
            super.handlePacket(c);
            getPrecognition().handlePacket(c);
        }
        finally {
            log(getClass(), METHOD_NAME, LogLevel.TRACE, msg);
        }
    }
    
    /**
     * sends a load game file to the server
     */
    public void sendLoadGame(File f) {
        precognition.resetGame();
        super.sendLoadGame(f);
    }
    
    protected void disconnected() {
        if (precognition != null) {
            precognition.signalDone();
            precogThread.interrupt();
        }
        super.disconnected();
    }

    public int getHighestEnemyInitiativeId() {
        int highestEnemyInitiativeBonus = -1;
        int highestEnemyInitiativeId = -1;
        for (Entity entity : getEnemyEntities()) {
            int initBonus = entity.getHQIniBonus() + entity.getMDIniBonus() + entity.getQuirkIniBonus();
            if (initBonus > highestEnemyInitiativeBonus) {
                highestEnemyInitiativeBonus = initBonus;
                highestEnemyInitiativeId = entity.getId();
            }
        }
        return highestEnemyInitiativeId;
    }
}
