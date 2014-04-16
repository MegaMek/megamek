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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import megamek.client.bot.BotClient;
import megamek.client.bot.ChatProcessor;
import megamek.client.bot.PhysicalOption;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.Minefield;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.PilotingRollData;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.actions.EntityAction;
import megamek.common.containers.PlayerIDandList;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.logging.LogLevel;
import megamek.common.logging.Logger;
import megamek.common.util.StringUtil;

public class Princess extends BotClient {

    public static final String CMD_FLEE = "fl";
    public static final String CMD_VERBOSE = "ve";
    public static final String CMD_BEHAVIOR = "be";
    public static final String CMD_CAUTION = "ca";
    public static final String CMD_AVOID = "av";
    public static final String CMD_AGGRESSION = "ag";
    public static final String CMD_HERDING = "he";
    public static final String CMD_BRAVERY = "br";
    public static final String CMD_TARGET = "ta";
    public static final String CMD_PRIORITY = "pr";

    private static final Logger logger = new Logger();

    private boolean initialized = false;

    //private PathSearcher pathSearcher;
    private BasicPathRanker pathRanker;
    private FireControl fireControl;
    private BehaviorSettings behaviorSettings;
    private double moveEvaluationTimeEstimate = 0;
    private Precognition precognition;
    private final Set<Coords> strategicBuildingTargets = new HashSet<>();
    private final Set<Integer> priorityUnitTargets = new HashSet<>();
    private boolean flee = false;
    protected ChatProcessor chatProcessor = new ChatProcessor();
    private boolean mustFlee = false;

    public Princess(String name, String host, int port, LogLevel verbosity) {
        super(name, host, port);
        logger.setVerbosity(verbosity);
        setBehaviorSettings(BehaviorSettingsFactory.getInstance(logger).DEFAULT_BEHAVIOR);
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

    public boolean shouldFlee() {
        return flee;
    }

    public boolean isMustFlee() {
        return mustFlee;
    }

    public void setMustFlee(boolean mustFlee) {
        this.mustFlee = mustFlee;
    }

    protected Precognition getPrecognition() {
        return precognition;
    }

    public void setShouldFlee(boolean shouldFlee, String reason) {
        log(getClass(), "setShouldFlee(boolean, String)", LogLevel.INFO, "Setting Should Flee " + shouldFlee +
                                                                         " because: " + reason);
        flee = shouldFlee;
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
        setShouldFlee(behaviorSettings.shouldAutoFlee(), "Configured to auto flee.");
        if (shouldFlee()) {
            return;
        }

        for (String targetCoords : behaviorSettings.getStrategicBuildingTargets()) {
            if (!StringUtil.isPositiveInteger(targetCoords) || (targetCoords.length() != 4)) {
                continue;
            }
            String x = targetCoords.substring(0, 2);
            String y = targetCoords.replaceFirst(x, "");
            Coords coords = new Coords(Integer.parseInt(x), Integer.parseInt(y));
            getStrategicBuildingTargets().add(coords);
        }

        for (int priorityUnit : behaviorSettings.getPriorityUnitTargets()) {
            if (priorityUnit <= 0) {
                continue;
            }
            getPriorityUnitTargets().add(priorityUnit);
        }
    }

    public FireControl getFireControl() {
        return fireControl;
    }

    public BehaviorSettings getBehaviorSettings() {
        return behaviorSettings;
    }

    public Set<Coords> getStrategicBuildingTargets() {
        return strategicBuildingTargets;
    }

    public Set<Integer> getPriorityUnitTargets() {
        return priorityUnitTargets;
    }

    @Override
    protected Vector<Coords> calculateArtyAutoHitHexes() {
        final String METHOD_NAME = "calculateArtyAutoHitHexes()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // currently returns no artillery hit spots
            // make an empty list
            PlayerIDandList<Coords> artyAutoHitHexes = new PlayerIDandList<Coords>();
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
            Coords[] startingCoords = getStartingCoordsArray();
            if (startingCoords.length == 0) {
                log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "No valid locations to deploy "
                    + getEntity(entityNum).getDisplayName());
            }

            // get the coordinates I can deploy on
            Coords deployCoords = getCoordsAround(getEntity(entityNum), startingCoords);
            if (deployCoords == null) {
                log(getClass(),
                    METHOD_NAME,
                    LogLevel.ERROR,
                    "getCoordsAround gave no location for "
                    + getEntity(entityNum).getChassis());
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
            deployElevation -= deployHex.getElevation();
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
            Entity shooter = game.getFirstEntity(getMyTurn()); // get the first
            // entity that can
            // act this turn
            // make sure weapons are loaded
            FiringPlan plan = fireControl.getBestFiringPlan(shooter, game);
            if (plan != null) {
                fireControl.loadAmmo(shooter, plan);

                log(getClass(), METHOD_NAME, LogLevel.INFO, shooter.getDisplayName() + " - Best Firing Plan: " +
                                                            plan.getDebugDescription(true));

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

    @Override
    protected Vector<Minefield> calculateMinefieldDeployment() {
        final String METHOD_NAME = "calculateMinefieldDeployment()";
        methodBegin(getClass(), METHOD_NAME);

        try {
            // currently returns no minefields
            // make an empty vector
            return new Vector<Minefield>();
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

            // Get the ration of distance to speed.
            // Faster units that are closer to the enemy should move later.
            total = distance / fastestMove;
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
            if (isFleeing(entity)) {
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
            if (entity.isImmobile()) {
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
            msg.append("\n\thas index " + moveIndex + " vs " + highestIndex);
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
            Entity first_entity = game.getFirstEntity(getMyTurn());
            PhysicalInfo best_attack = null;
            int firstEntityId = first_entity.getId();
            int nextEntityId = firstEntityId;

            // this is an array of all my enemies
            List<Entity> enemies = getEnemyEntities();

            do {
                Entity hitter = game.getEntity(nextEntityId);
                nextEntityId = game.getNextEntityNum(hitter.getId());

                if (hitter.getPosition() == null) {
                    continue;
                }

                log(getClass(), METHOD_NAME, LogLevel.INFO,
                    "Calculating physical attacks for " + hitter.getDisplayName());

                // cycle through potential enemies
                for (Entity e : enemies) {
                    if (e.getPosition() == null) {
                        continue; // Skip enemies not on the board.
                    }
                    if (hitter.getPosition().distance(e.getPosition()) > 1) {
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
                    if (right_kick.getUtility() > 0) {
                        if ((best_attack == null)
                            || (right_kick.getUtility() > best_attack.getUtility())) {
                            best_attack = right_kick;
                        }
                    }
                    PhysicalInfo left_kick = new PhysicalInfo(
                            hitter, e, PhysicalAttackType.LEFT_KICK, game, this, false);
                    if (left_kick.getUtility() > 0) {
                        if ((best_attack == null)
                            || (left_kick.getUtility() > best_attack.getUtility())) {
                            best_attack = left_kick;
                        }
                    }

                }
                if (best_attack != null) {
                    log(getClass(), METHOD_NAME, LogLevel.INFO, "Attack is " + best_attack.getDebugDescription());
                } else {
                    log(getClass(), METHOD_NAME, LogLevel.INFO, "No useful attack to be made");
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

    protected boolean wantsToFlee(Entity entity) {
        return shouldFlee()
               || isMustFlee()
               || (entity.isCrippled() && getBehaviorSettings().isForcedWithdrawal());
    }

    protected boolean isFleeing(Entity entity) {
        return !entity.isImmobile() && wantsToFlee(entity);
    }

    protected boolean mustFleeBoard(Entity entity) {
        if (!isFleeing(entity)) {
            return false;
        }
        if (!entity.canFlee()) {
            return false;
        }
        if (getPathRanker().distanceToHomeEdge(entity.getPosition(), getHomeEdge(), getGame()) > 0) {
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
            PilotingRollData target = mech.checkGetUp(getUp);
            log(getClass(), METHOD_NAME, LogLevel.INFO,
                "Need to roll " + target.getValue() + " to stand and our tolerance is " + threshold);
            return (target.getValue() >= threshold);
        }

        // How likely are we to get unstuck.
        MovePath.MoveStepType type = MovePath.MoveStepType.FORWARDS;
        MoveStep walk = new MoveStep(movePath, type);
        IHex hex = getHex(mech.getPosition());
        PilotingRollData target = mech.checkBogDown(walk, hex, mech.getPriorPosition(), mech.getPosition(),
                                                    hex.getElevation(), false);
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

    protected ArrayList<MovePath> getMovePaths(Entity entity) {
        return getPrecognition().getPathEnumerator().unit_paths.get(entity.getId());
    }

    protected ArrayList<RankedPath> rankPaths(ArrayList<MovePath> paths, int maxRange, double fallTollerance,
                                              int startingHomeDistance, int startingDistToNearestEnemy,
                                              List<Entity> enemies, List<Entity> friends) {
        return getPathRanker().rankPaths(paths, getGame(), maxRange, fallTollerance, startingHomeDistance,
                                         startingDistToNearestEnemy, enemies, friends);
    }

    @Override
    protected MovePath continueMovementFor(Entity entity) {
        final String METHOD_NAME = "continueMovementFor(Entity)";
        methodBegin(getClass(), METHOD_NAME);

        if (entity == null) {
            log(getClass(), METHOD_NAME, LogLevel.WARNING, "Entity is NULL.");
        }

        try {
            // figure out who moved last, and who's move lists need to be updated

            // moves this entity during movement phase
            log(getClass(), METHOD_NAME, "Moving " + entity.getDisplayName() + " (ID " + entity.getId() + ")");
            getPrecognition().insureUpToDate();

            if (wantsToFlee(entity)) {
                String msg = entity.getDisplayName();
                if (entity.isCrippled()) {
                    msg += " is crippled and withdrawing.";
                } else if (shouldFlee()) {
                    msg += " is retreating.";
                } else if (isMustFlee()) {
                    msg += " is forced to withdraw.";
                }
                log(getClass(), METHOD_NAME, msg);
                sendChat(msg);

                // If this entity must withdraw, is on its home edge and is able to flee the board, do so.
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

            ArrayList<MovePath> paths = getPrecognition().getPathEnumerator().unit_paths.get(entity.getId());

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
            int distanceToNerestEnemy = (int) getPathRanker().distanceToClosestEnemy(entity, entity.getPosition(),
                                                                                     getGame());
            List<RankedPath> rankedpaths = rankPaths(paths, entity.getMaxWeaponRange(), fallTolerance,
                                                     startingHomeDistance, distanceToNerestEnemy, getEnemyEntities(),
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
            precognition.unpause();
            RankedPath bestpath = PathRanker.getBestPath(rankedpaths);
            log(getClass(), METHOD_NAME, LogLevel.INFO, "Best Path: " + bestpath.path.toString() + "  Rank: "
                                                        + bestpath.rank);
            return bestpath.path;
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected void initFiring() {
        final String METHOD_NAME = "initFiring()";
        methodBegin(getClass(), METHOD_NAME);

        try {

            // ----Debugging: print out any errors made in guessing to hit
            // values-----
            Vector<Entity> ents = game.getEntitiesVector();
            for (Entity ent : ents) {
                String errors = fireControl.checkAllGuesses(ent, game);
                if (errors != null) {
                    log(getClass(), METHOD_NAME, LogLevel.WARNING, errors);
                }
            }
            // -----------------------------------------------------------------------
        } finally {
            methodEnd(getClass(), METHOD_NAME);
        }
    }

    @Override
    protected void initMovement() {
        final String METHOD_NAME = "initMovement()";
        methodBegin(getClass(), METHOD_NAME);

        try {
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
                    for (Enumeration<Entity> i = game.getEntities(coords, true); i.hasMoreElements(); ) {
                        Entity entity = i.nextElement();
                        BuildingTarget bt = new BuildingTarget(coords, game.getBoard(), false);
                        if ((entity instanceof GunEmplacement)
                            && entity.getOwner().isEnemyOf(getLocalPlayer())
                            && (fireControl.getAdditionalTargets().indexOf(bt) == -1)) {
                            fireControl.getAdditionalTargets().add(bt);
                            sendChat("Building in Hex " + coords.toFriendlyString()
                                     + " designated target due to Gun Emplacement.");
                        }
                    }
                }
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
            precognition = new Precognition(this);
            precognition.setGame(getGame());
            pathRanker.setPathEnumerator(precognition.getPathEnumerator());

            Thread precognitionThread = new Thread(precognition, "Princess-precognition");
            precognitionThread.start();

            // Pick up any turrets and add their buildings to the strategic targets list.
            Enumeration<Building> buildings = getGame().getBoard().getBuildings();
            while (buildings.hasMoreElements()) {
                Building bldg = buildings.nextElement();
                Enumeration<Coords> bldgCoords = bldg.getCoords();
                while (bldgCoords.hasMoreElements()) {
                    Coords coords = bldgCoords.nextElement();
                    for (Enumeration<Entity> i = getGame().getEntities(coords, true); i.hasMoreElements(); ) {
                        Entity entity = i.nextElement();
                        if (entity instanceof GunEmplacement
                            && entity.getOwner().isEnemyOf(getLocalPlayer())
                            && !getStrategicBuildingTargets().contains(coords)) {
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

    @Override
    protected void processChat(GamePlayerChatEvent ge) {
        chatProcessor.processChat(ge, this);
    }

    public void log(Class<?> callingClass, String methodName, LogLevel level,
                    String msg) {
        logger.log(callingClass, methodName, level, msg);
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

    public void setHomeEdge(HomeEdge homeEdge) {
        if (homeEdge == null) {
            log(getClass(), "setHomeEdge(BasicPathRanker.HomeEdge)",
                new IllegalArgumentException("Home Edge is required!"));
            return;
        }
        getBehaviorSettings().setHomeEdge(homeEdge);
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
}
