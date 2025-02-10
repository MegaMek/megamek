/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import megamek.client.bot.BotLogger;
import megamek.client.bot.princess.BotGeometry.ConvexBoardArea;
import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.client.bot.princess.BotGeometry.HexLine;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.codeUtilities.MathUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.util.HazardousLiquidPoolUtil;
import megamek.logging.MMLogger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Stream;

/**
 * A very "basic" path ranker
 */
public class BasicPathRanker extends PathRanker {
    private final static MMLogger logger = MMLogger.create(BasicPathRanker.class);

    // this is a value used to indicate how much we value the unit being at its
    // destination
    private final int ARRIVED_AT_DESTINATION_FACTOR = 250;

    // this is a value used to indicate how much we dis-value the unit being
    // destroyed as a result of
    // what it's doing
    private final int UNIT_DESTRUCTION_FACTOR = 1000;

    protected final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance());
    protected final NumberFormat LOG_PERCENT = NumberFormat.getPercentInstance();

    private PathEnumerator pathEnumerator;

    // the best damage enemies could expect were I not here. Used to determine
    // whether they will target me.
    protected final Map<Integer, Double> bestDamageByEnemies;

    protected int blackIce = -1;

    public BasicPathRanker(Princess owningPrincess) {
        super(owningPrincess);
        bestDamageByEnemies = new TreeMap<>();
        logger.debug("Using {} behavior.", getOwner().getBehaviorSettings().getDescription());
    }

    FireControl getFireControl(Entity entity) {
        return getOwner().getFireControl(entity);
    }

    void setPathEnumerator(PathEnumerator pathEnumerator) {
        this.pathEnumerator = pathEnumerator;
    }

    PathEnumerator getPathEnumerator() {
        return pathEnumerator;
    }

    Map<Integer, Double> getBestDamageByEnemies() {
        return bestDamageByEnemies;
    }

    Coords getClosestCoordsTo(int unitId, Coords location) {
        ConvexBoardArea box = pathEnumerator.getUnitMovableAreas().get(unitId);
        if (box == null) {
            return null;
        }
        return box.getClosestCoordsTo(location);
    }

    boolean isInMyLoS(Entity unit, HexLine leftBounds, HexLine rightBounds) {
        return (leftBounds.judgeArea(pathEnumerator.getUnitMovableAreas().get(unit.getId())) > 0)
                && (rightBounds.judgeArea(pathEnumerator.getUnitMovableAreas().get(unit.getId())) < 0);
    }

    double getMaxDamageAtRange(Entity shooter, int range, boolean useExtremeRange,
            boolean useLOSRange) {
        return FireControl.getMaxDamageAtRange(shooter, range, useExtremeRange, useLOSRange);
    }

    boolean canFlankAndKick(Entity enemy, Coords behind, Coords leftFlank, Coords rightFlank, int myFacing) {
        Set<CoordFacingCombo> enemyFacingSet = pathEnumerator.getUnitPotentialLocations().get(enemy.getId());

        if (enemyFacingSet == null) {
            logger.warn("no facing set for {}", enemy.getDisplayName());
            return false;
        }

        return enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(behind, myFacing))
                || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(behind, (myFacing + 1) % 6))
                || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(behind, (myFacing + 5) % 6))
                || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(leftFlank, myFacing))
                || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(leftFlank, (myFacing + 4) % 6))
                || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(leftFlank, (myFacing + 5) % 6))
                || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(rightFlank, myFacing))
                || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(rightFlank, (myFacing + 1) % 6))
                || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(rightFlank, (myFacing + 2) % 6));
    }

    /**
     * Guesses a number of things about an enemy that has not yet moved
     * TODO estimated damage is sloppy. Improve for missile attacks, gun skill, and
     * range
     */
    public EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path, boolean useExtremeRange,
            boolean useLOSRange) {
        // some preliminary calculations
        final double damageDiscount = 0.25;
        EntityEvaluationResponse returnResponse = new EntityEvaluationResponse();

        // Airborne AeroSpace on ground maps always move after other units, and would
        // require an entirely different evaluation
        // TODO (low priority) implement a way to see if I can dodge aero units
        if (enemy.isAirborneAeroOnGroundMap()) {
            return returnResponse;
        }

        Coords finalCoords = path.getFinalCoords();
        int myFacing = path.getFinalFacing();
        Coords behind = finalCoords.translated((myFacing + 3) % 6);
        Coords leftFlank = finalCoords.translated((myFacing + 2) % 6);
        Coords rightFlank = finalCoords.translated((myFacing + 4) % 6);
        Coords closest = getClosestCoordsTo(enemy.getId(), finalCoords);

        if (closest == null) {
            return returnResponse;
        }

        int range = closest.distance(finalCoords);

        // I would prefer if the enemy must end its move in my line of fire if so, I can
        // guess that I may do some damage to it (cover notwithstanding). At the very
        // least, I can force the enemy to take cover on its move.
        HexLine leftBounds;
        HexLine rightBounds;

        if (path.getEntity().canChangeSecondaryFacing()) {
            leftBounds = new HexLine(behind, (myFacing + 2) % 6);
            rightBounds = new HexLine(behind, (myFacing + 4) % 6);
        } else {
            leftBounds = new HexLine(behind, (myFacing + 1) % 6);
            rightBounds = new HexLine(behind, (myFacing + 5) % 6);
        }

        if (isInMyLoS(enemy, leftBounds, rightBounds)) {
            returnResponse.addToMyEstimatedDamage(
                    getMaxDamageAtRange(
                            path.getEntity(),
                            range,
                            useExtremeRange,
                            useLOSRange) * damageDiscount);
        }

        // in general if an enemy can end its position in range, it can hit me
        returnResponse.addToEstimatedEnemyDamage(
                getMaxDamageAtRange(
                        enemy,
                        range,
                        useExtremeRange,
                        useLOSRange)
                        * damageDiscount);

        // It is especially embarrassing if the enemy can move behind or flank me and
        // then kick me
        if (canFlankAndKick(enemy, behind, leftFlank, rightFlank, myFacing)) {
            returnResponse.addToEstimatedEnemyDamage(
                    Math.ceil(enemy.getWeight() / 5.0) *
                            damageDiscount);
        }

        return returnResponse;
    }

    @Override
    protected List<TargetRoll> getPSRList(MovePath path) {
        return super.getPSRList(path);
    }

    @Override
    public double getMovePathSuccessProbability(MovePath movePath) {
        return super.getMovePathSuccessProbability(movePath);
    }

    private double calculateFallMod(double successProbability) {
        double pilotingFailure = (1 - successProbability);
        double fallShame = getOwner().getBehaviorSettings().getFallShameValue();
        double fallMod = pilotingFailure * (pilotingFailure == 1 ? -UNIT_DESTRUCTION_FACTOR : fallShame);
        logger.trace("fall mod [-{} = {} * {}]", fallMod, pilotingFailure, fallShame);
        return fallMod;
    }

    double calculateDamagePotential(Entity enemy, EntityState shooterState, MovePath path, EntityState targetState,
            int distance, Game game) {

        // If they don't have the range, they can't do damage.
        int maxRange = getOwner().getMaxWeaponRange(enemy, path.getEntity().isAirborne());
        if (distance > maxRange) {
            return 0;
        }

        // If they don't have LoS, they can't do damage.
        final LosEffects losEffects = LosEffects.calculateLOS(game, enemy, path.getEntity(), shooterState.getPosition(),
                targetState.getPosition(), false);

        if (!losEffects.canSee()) {
            return 0;
        }

        Targetable actualTarget = path.getEntity();

        // if the target is infantry protected by a building, we have to fire at the
        // building instead.
        if (losEffects.infantryProtected()) {
            actualTarget = new BuildingTarget(targetState.getPosition(), game.getBoard(), false);
            targetState = new EntityState(actualTarget);
        }

        int maxHeat = (enemy.getHeatCapacity() - enemy.heat) + (enemy.isAero() ? 0 : 5);
        FiringPlanCalculationParameters guess = new FiringPlanCalculationParameters.Builder()
                .buildGuess(enemy,
                        shooterState,
                        actualTarget,
                        targetState,
                        maxHeat,
                        null);
        return getFireControl(path.getEntity()).determineBestFiringPlan(guess).getUtility();
    }

    double calculateKickDamagePotential(Entity enemy, MovePath path, Game game) {
        if (!(enemy instanceof Mek)) {
            return 0.0;
        }

        // if they can kick me, and probably hit, they probably will.
        PhysicalInfo theirKick = new PhysicalInfo(enemy, null,
                path.getEntity(), new EntityState(path),
                PhysicalAttackType.RIGHT_KICK, game, getOwner(), true);

        if (theirKick.getProbabilityToHit() <= 0.5) {
            return 0.0;
        }

        return theirKick.getExpectedDamageOnHit() * theirKick.getProbabilityToHit();
    }

    double calculateMyDamagePotential(MovePath path, Entity enemy, int distance, Game game) {
        Entity me = path.getEntity();

        // If I don't have range, I can't do damage.
        // exception: I might, if I'm an aero on a ground map attacking a ground unit
        // because aero unit ranges are a "special case"
        boolean aeroAttackingGroundUnitOnGroundMap = me.isAirborne() && !enemy.isAero() && game.getBoard().onGround();

        int maxRange = getOwner().getMaxWeaponRange(me, enemy.isAirborne());
        if (distance > maxRange && !aeroAttackingGroundUnitOnGroundMap) {
            return 0;
        }

        // If I don't have LoS, I can't do damage. ToDo: Account for indirect fire.
        LosEffects losEffects = LosEffects.calculateLOS(game, me, enemy, path.getFinalCoords(),
                enemy.getPosition(), false);
        if (!losEffects.canSee()) {
            return 0;
        }

        // If I am an infantry unit that cannot both move and fire, and I am
        // moving, I can't do damage.
        boolean isZeroMpInfantry = me instanceof Infantry && (me.getWalkMP() == 0);
        if (isZeroMpInfantry && path.getMpUsed() > 0) {
            return 0;
        }

        FiringPlan myFiringPlan;
        // we're only going to do air to ground attack plans if we're an airborne aero
        // attacking a ground unit
        if (aeroAttackingGroundUnitOnGroundMap) {
            myFiringPlan = getFireControl(path.getEntity()).guessFullAirToGroundPlan(me, enemy,
                    new EntityState(enemy), path, game, false);
        } else {
            FiringPlanCalculationParameters guess = new FiringPlanCalculationParameters.Builder()
                    .buildGuess(path.getEntity(),
                            new EntityState(path),
                            enemy,
                            null,
                            getFireControl(me).calcHeatTolerance(me, me.isAero()),
                            null);
            myFiringPlan = getFireControl(me).determineBestFiringPlan(guess);
        }
        return myFiringPlan.getUtility();
    }

    double calculateMyKickDamagePotential(MovePath path, Entity enemy, Game game) {
        if (!(path.getEntity() instanceof Mek)) {
            return 0.0;
        }

        PhysicalInfo myKick = new PhysicalInfo(path.getEntity(),
                new EntityState(path), enemy, null,
                PhysicalAttackType.RIGHT_KICK, game, getOwner(), true);

        if (myKick.getProbabilityToHit() <= 0.5) {
            return 0;
        }

        return myKick.getExpectedDamageOnHit() * myKick.getProbabilityToHit();
    }

    public EntityEvaluationResponse evaluateMovedEnemy(Entity enemy, MovePath path, Game game) {
        EntityEvaluationResponse returnResponse = new EntityEvaluationResponse();

        int distance = enemy.getPosition().distance(path.getFinalCoords());

        // How much damage can they do to me?
        double theirDamagePotential = calculateDamagePotential(enemy,
                new EntityState(enemy), path, new EntityState(path), distance, game);

        // if they can kick me, and probably hit, they probably will.
        if (distance <= 1) {
            theirDamagePotential += calculateKickDamagePotential(enemy, path, game);
        }

        returnResponse.setEstimatedEnemyDamage(theirDamagePotential);

        // How much damage can I do to them?
        returnResponse.setMyEstimatedDamage(calculateMyDamagePotential(path, enemy, distance, game));

        // How much physical damage can I do to them?
        if (distance <= 1) {
            returnResponse.setMyEstimatedPhysicalDamage(calculateMyKickDamagePotential(path, enemy, game));
        }

        return returnResponse;
    }

    // The further I am from a target, the lower this path ranks (weighted by
    // Hyper Aggression.
    protected double calculateAggressionMod(Entity movingUnit, MovePath path, Game game) {

        double distToEnemy = distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game);

        if ((distToEnemy == 0) && !(movingUnit instanceof Infantry)) {
            distToEnemy = 2;
        }

        double aggression = getOwner().getBehaviorSettings().getHyperAggressionValue();
        double aggressionMod = distToEnemy * aggression;
        logger.trace("aggression mod [ -{} = {} * {}]", aggressionMod, distToEnemy, aggression);
        return aggressionMod;
    }

    // Lower this path ranking if I am moving away from my friends (weighted by Herd Mentality).
    protected double calculateHerdingMod(Coords friendsCoords, MovePath path) {
        if (friendsCoords == null) {
            logger.trace(" herdingMod [-0 no friends]");
            return 0;
        }

        double finalDistance = friendsCoords.distance(path.getFinalCoords());
        double herding = getOwner().getBehaviorSettings().getHerdMentalityValue();
        double herdingMod = finalDistance * herding;

        logger.trace("herding mod [-{} = {} * {}]", herdingMod, finalDistance, herding);
        return herdingMod;
    }

    protected double calculateFacingMod(Entity movingUnit, Game game, final MovePath path, @Nullable Coords closestPosition) {
        // limit to the 5 closest enemies
        int facingDiff = getFacingDiff(movingUnit, game, closestPosition, path);
        double facingMod = Math.max(0.0, 50 * (facingDiff - 1));
        logger.trace("facing mod [-{} = max(0, 50 * ({}) - 1)]", facingMod, facingDiff);
        return facingMod;
    }

    private double calculateWaypointMod(Entity movingUnit, MovePath path, StringBuilder formula) {
        var waypointOpt = getOwner().getUnitBehaviorTracker().getWaypointForEntity(movingUnit);

        double waypointMod = 0.0;
        if (waypointOpt.isPresent() && path.getFinalCoords() != null && path.getStartCoords() != null) {
            var wayPoint = waypointOpt.get();
            var finalCoords = path.getFinalCoords();
            var startingCoords = path.getStartCoords();
            int finalDistanceToWayPoint = finalCoords.distance(wayPoint);
            int startingDistanceToWaypoint = startingCoords.distance(wayPoint);

            int distanceReduced = startingDistanceToWaypoint - finalDistanceToWayPoint;
            // Multiply the distance reduced by that factor
            waypointMod = distanceReduced * ARRIVED_AT_DESTINATION_FACTOR;

            // Now log it in a consistent way:
            formula.append(" + waypointMod [")
                .append(LOG_DECIMAL.format(waypointMod))
                .append(" = (")
                .append(startingDistanceToWaypoint).append(" - ")
                .append(finalDistanceToWayPoint)
                .append(") * ")
                .append(LOG_DECIMAL.format(ARRIVED_AT_DESTINATION_FACTOR))
                .append("] ");
        } else {
            formula.append(" + waypointMod [").append(LOG_DECIMAL.format(waypointMod)).append("]");
        }
        return waypointMod;
    }

    private int getFacingDiff(Entity movingUnit, Game game, @Nullable Coords closestPosition, final MovePath path) {
        Coords toFace = closestPosition == null ? game.getBoard().getCenter() : closestPosition;
        int desiredFacing = (toFace.direction(movingUnit.getPosition()) + 3) % 6;
        int currentFacing = path.getFinalFacing();
        int facingDiff;
        // -1 is bias towards facing left, 1 is bias towards facing right
        int biasTowardsFacing = getBiasTowardsFacing(movingUnit);
        desiredFacing -= biasTowardsFacing;
        if (desiredFacing < 0) {
            desiredFacing += 6;
        }
        if (currentFacing == desiredFacing) {
            facingDiff = 0;
        } else if ((currentFacing == ((desiredFacing + 1) % 6))
                || (currentFacing == ((desiredFacing + 5) % 6))) {
            facingDiff = 1;
        } else if ((currentFacing == ((desiredFacing + 2) % 6))
                || (currentFacing == ((desiredFacing + 4) % 6))) {
            facingDiff = 2;
        } else {
            facingDiff = 3;
        }
        return facingDiff;
    }

    private static int getBiasTowardsFacing(Entity movingUnit) {
        int biasTowardsFacing = 0;
        if (movingUnit.isMek()) {
            // if we're a mek, we want to face the enemy towards the side that has more armor left
            Mek mek = (Mek) movingUnit;
            int leftArmor = mek.getArmor(Mek.LOC_LARM) + mek.getArmor(Mek.LOC_LLEG) + mek.getArmor(Mek.LOC_LT) + mek.getArmor(Mek.LOC_LLEG);
            int rightArmor = mek.getArmor(Mek.LOC_RARM) + mek.getArmor(Mek.LOC_RLEG) + mek.getArmor(Mek.LOC_RT) + mek.getArmor(Mek.LOC_RLEG);
            if (leftArmor > rightArmor) {
                biasTowardsFacing = -1;
            } else if (rightArmor > leftArmor) {
                biasTowardsFacing = 1;
            }
        }
        return biasTowardsFacing;
    }

    /**
     * If intentionally attempting to reach some board edge, favor paths that take
     * me closer to it.
     */
    protected double calculateSelfPreservationMod(Entity movingUnit, MovePath path, Game game) {
        BehaviorType behaviorType = getOwner().getUnitBehaviorTracker().getBehaviorType(movingUnit, getOwner());

        if (behaviorType == BehaviorType.ForcedWithdrawal || behaviorType == BehaviorType.MoveToDestination) {
            int newDistanceToHome = distanceToHomeEdge(path.getFinalCoords(), getOwner().getHomeEdge(movingUnit), game);
            double selfPreservation = getOwner().getBehaviorSettings().getSelfPreservationValue();
            double selfPreservationMod;

            // normally, we favor being closer to the edge we're trying to get to
            if (newDistanceToHome > 0) {
                selfPreservationMod = newDistanceToHome * selfPreservation;
                // if this path gets us to the edge, we value it considerably more than we do
                // paths that don't get us there
            } else {
                selfPreservationMod = -ARRIVED_AT_DESTINATION_FACTOR;
            }

            logger.trace("self preservation mod [-{} = {} * {}]", selfPreservationMod, newDistanceToHome, selfPreservation);
            return selfPreservationMod;
        }
        logger.trace("self preservation mod [0] - not moving nor forced to withdraw");
        return 0.0;
    }

    /**
     * Tells me whether this path will result in me flying to a location from which
     * there is absolutely no way to remain on the board the following turn.
     *
     * Not applicable for ground units, so the default behavior is to return 0.
     */
    protected double calculateOffBoardMod(MovePath path) {
        return 0.0;
    }


    protected void checkBlackIce(Game game) {
        blackIce = ((game.getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE)
            && game.getPlanetaryConditions().getTemperature() <= PlanetaryConditions.BLACK_ICE_TEMP)
            || game.getPlanetaryConditions().getWeather().isIceStorm()) ? 1 : 0;
    }


    /**
     * A path ranking
     */
    @Override
    protected RankedPath rankPath(MovePath path, Game game, int maxRange, double fallTolerance, List<Entity> enemies,
            Coords friendsCoords) {
        Entity movingUnit = path.getEntity();

        if (blackIce == -1) {
            checkBlackIce(game);
        }

        Map<String, Double> scores = new HashMap<>();
        // Copy the path to avoid inadvertent changes.
        MovePath pathCopy = path.clone();

        // Worry about failed piloting rolls (weighted by Fall Shame).
        double successProbability = getMovePathSuccessProbability(pathCopy);
        double fallMod = calculateFallMod(successProbability);
        scores.put("fallMod", fallMod);
        scores.put("successProbability", successProbability);
        scores.put("maxRange", (double) maxRange);
        scores.put("fallTolerance", fallTolerance);
        scores.put("blackIce", (double) blackIce);
        scores.put("enemies", (double) enemies.size());
        scores.put("friendsCoords_x", friendsCoords == null ? -1.0 : friendsCoords.getX());
        scores.put("friendsCoords_y", friendsCoords == null ? -1.0 : friendsCoords.getY());
        scores.put("entityId", (double) movingUnit.getId());
        scores.put("entityBehaviorState", (double) getOwner().getUnitBehaviorTracker().getBehaviorType(movingUnit, getOwner()).ordinal());
        // Worry about how badly we can damage ourselves on this path!
        double expectedDamageTaken = calculateMovePathPSRDamage(movingUnit, pathCopy);
        expectedDamageTaken += checkPathForHazards(pathCopy, movingUnit, game);
        expectedDamageTaken += MinefieldUtil.checkPathForMinefieldHazards(pathCopy);
        scores.put("damageExpectedPath", expectedDamageTaken);
        // look at all of my enemies
        FiringPhysicalDamage damageEstimate = new FiringPhysicalDamage();

        boolean extremeRange = isExtremeRange(game);
        boolean losRange = isLosRange(game);
        for (Entity enemy : enemies) {
            // Skip ejected pilots.
            if (enemy instanceof MekWarrior) {
                continue;
            }

            // Skip units not actually on the board.
            if (enemy.isOffBoard() || (enemy.getPosition() == null)
                    || !game.getBoard().contains(enemy.getPosition())) {
                 continue;
            }

            // Skip broken enemies
            if (getOwner().getHonorUtil().isEnemyBroken(enemy.getId(), enemy.getOwnerId(),
                    getOwner().getForcedWithdrawal())) {
                continue;
            }

            EntityEvaluationResponse eval;

            if (evaluateAsMoved(enemy)) {
                // For units that have already moved
                eval = evaluateMovedEnemy(enemy, pathCopy, game);
            } else {
                // For units that have not moved this round
                eval = evaluateUnmovedEnemy(enemy, path, extremeRange, losRange);
            }

            // if we're not ignoring the enemy, we consider damage that we may do to them;
            // however, just because we're ignoring them doesn't mean they won't shoot at
            // us.
            if (!getOwner().getBehaviorSettings().getIgnoredUnitTargets().contains(enemy.getId())) {
                if (damageEstimate.firingDamage < eval.getMyEstimatedDamage()) {
                    damageEstimate.firingDamage = eval.getMyEstimatedDamage();
                }
                if (damageEstimate.physicalDamage < eval.getMyEstimatedPhysicalDamage()) {
                    damageEstimate.physicalDamage = eval.getMyEstimatedPhysicalDamage();
                }
            }

            expectedDamageTaken += eval.getEstimatedEnemyDamage();
        }

        // if we're not in the air, we may get hit by friendly artillery
        if (!path.getEntity().isAirborne() && !path.getEntity().isAirborneVTOLorWIGE()) {
            double friendlyArtilleryDamage = 0;
            Map<Coords, Double> artyDamage = getOwner().getPathRankerState().getIncomingFriendlyArtilleryDamage();

            if (!artyDamage.containsKey(path.getFinalCoords())) {
                friendlyArtilleryDamage = ArtilleryTargetingControl
                        .evaluateIncomingArtilleryDamage(path.getFinalCoords(), getOwner());
                artyDamage.put(path.getFinalCoords(), friendlyArtilleryDamage);
            } else {
                friendlyArtilleryDamage = artyDamage.get(path.getFinalCoords());
            }

            expectedDamageTaken += friendlyArtilleryDamage;
        }

        damageEstimate = calcDamageToStrategicTargets(pathCopy, game, getOwner().getFireControlState(), damageEstimate);

        // If I cannot kick because I am a clan unit and "No physical attacks for the
        // clans"
        // is enabled, set maximum physical damage for this path to zero.
        if (game.getOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL)
                && path.getEntity().getCrew().isClanPilot()) {
            damageEstimate.physicalDamage = 0;
        }
        scores.put("damageExpectedTotal", expectedDamageTaken);
        scores.put("myAttackFiring", damageEstimate.firingDamage);
        scores.put("myAttackPhysical", damageEstimate.physicalDamage);
        // I can kick a different target than I shoot, so add physical to
        // total damage after I've looked at all enemies

        double braveryMod = getBraveryMod(successProbability, damageEstimate, expectedDamageTaken);
        scores.put("braveryValue", getOwner().getBehaviorSettings().getBraveryValue());
        scores.put("braveryIndex", (double) getOwner().getBehaviorSettings().getBraveryIndex());
        scores.put("braveryMod", braveryMod);
        var isNotAirborne = !path.getEntity().isAirborneAeroOnGroundMap();
        // the only critters not subject to aggression and herding mods are
        // airborne aeros on ground maps, as they move incredibly fast
        // The further I am from a target, the lower this path ranks
        // (weighted by Aggression slider).
        double aggressionMod = isNotAirborne ?
            calculateAggressionMod(movingUnit, pathCopy, game) : 0;
        double distToEnemy = distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game);
        scores.put("closestEnemyDistance", distToEnemy);
        scores.put("aggressionValue", getOwner().getBehaviorSettings().getHyperAggressionValue());
        scores.put("aggressionIndex", (double) getOwner().getBehaviorSettings().getHyperAggressionIndex());
        scores.put("aggressionMod", aggressionMod);

        // The further I am from my teammates, the lower this path
        // ranks (weighted by Herd Mentality).
        double herdingMod = isNotAirborne ? calculateHerdingMod(friendsCoords, pathCopy) : 0;

        // Movement is good, it gives defense and extends a player power in the game.
        if (movingUnit.getPosition() != null && friendsCoords != null) {
            scores.put("friendsDistance", (double) friendsCoords.distance(movingUnit.getPosition()));
        }
        scores.put("herdingValue", getOwner().getBehaviorSettings().getHerdMentalityValue());
        scores.put("herdingIndex", (double) getOwner().getBehaviorSettings().getHerdMentalityIndex());
        scores.put("herdingMod", herdingMod);

        var movementModFormula = new StringBuilder(64);
        // Movement is good, it gives defense and extends a player power in the game.
        double movementMod = calculateMovementMod(pathCopy, game, enemies, movementModFormula);
        scores.put("enemyHotSpotCount", (double) getOwner().getEnemyHotSpots().size());
        scores.put("herdingValue", getOwner().getBehaviorSettings().getSelfPreservationValue());
        scores.put("herdingIndex", (double) getOwner().getBehaviorSettings().getSelfPreservationIndex());
        scores.put("movementMod", movementMod);
        // Try to face the enemy.
        Coords medianEnemyPosition = getEnemiesMedianCoordinate(enemies, movingUnit);

        double facingMod = calculateFacingMod(movingUnit, game, pathCopy, medianEnemyPosition);
        scores.put("facing", (double) movingUnit.getFacing());
        scores.put("finalFacing", (double) pathCopy.getFinalFacing());
        scores.put("facingMod", facingMod);

        var formula = new StringBuilder(256);
        var crowdingToleranceFormula = new StringBuilder(64);
        double crowdingTolerance = calculateCrowdingTolerance(pathCopy, enemies, maxRange, crowdingToleranceFormula);
        // If I need to flee the board, I want to get closer to my home edge.
        double selfPreservationMod= calculateSelfPreservationMod(movingUnit, pathCopy, game);
        double offBoardMod = calculateOffBoardMod(pathCopy);
        // if we're an aircraft, we want to de-value paths that will force us off the
        // board
        // on the subsequent turn.
        double utility = -fallMod;
        utility += braveryMod;
        utility -= aggressionMod;
        utility -= herdingMod;
        utility += movementMod;
        utility -= crowdingTolerance;
        utility -= facingMod;
        utility -= selfPreservationMod;
        utility -= utility * offBoardMod;

        formula.append("Calculation: {fall mod [").append(LOG_DECIMAL.format(fallMod)).append(" = ")
            .append(LOG_DECIMAL.format(1 - successProbability))
            .append(" * ")
            .append(LOG_DECIMAL.format(getOwner().getBehaviorSettings().getFallShameValue()))
            .append("] + braveryMod [")
            .append(LOG_DECIMAL.format(braveryMod))
            .append(" = ")
            .append(LOG_PERCENT.format(successProbability))
            .append(" * ((")
            .append(LOG_DECIMAL.format(damageEstimate.getMaximumDamageEstimate()))
            .append(" * ")
            .append(LOG_DECIMAL.format(getOwner().getBehaviorSettings().getBraveryValue()))
            .append(") - ")
            .append(LOG_DECIMAL.format(expectedDamageTaken))
            .append(")] - aggressionMod [")
            .append(LOG_DECIMAL.format(aggressionMod))
            .append(" = ")
            .append(LOG_DECIMAL.format(distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game)))
            .append(" * ")
            .append(LOG_DECIMAL.format(getOwner().getBehaviorSettings().getHyperAggressionValue()))
            .append("] - herdingMod [");
        if (friendsCoords != null) {
            formula.append(LOG_DECIMAL.format(herdingMod))
                .append(" = ")
                .append(LOG_DECIMAL.format(friendsCoords.distance(path.getFinalCoords())))
                .append(" * ")
                .append(LOG_DECIMAL.format(getOwner().getBehaviorSettings().getHerdMentalityValue()));
        } else {
            formula.append("0 no friends");
        }
        formula.append("]");
        if (movementMod != 0.0) {
            formula.append(" + ").append(movementModFormula);
        }
        if (crowdingTolerance != 0.0) {
            formula.append(" - ").append(crowdingToleranceFormula);
        }

        formula.append(" - facingMod [")
            .append(LOG_DECIMAL.format(facingMod))
            .append(" = max(0, 50 * {")
            .append(getFacingDiff(movingUnit, game, medianEnemyPosition, pathCopy))
            .append(" - 1})]");

        logger.trace("{}", formula);

        RankedPath rankedPath = new RankedPath(utility, pathCopy, formula.toString());
        rankedPath.setExpectedDamage(damageEstimate.getMaximumDamageEstimate());
        rankedPath.getScores().putAll(scores);
        return rankedPath;
    }

    protected boolean isLosRange(Game game) {
        return game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE);
    }

    protected boolean isExtremeRange(Game game) {
        return game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE);
    }

    private static Coords getEnemiesMedianCoordinate(List<Entity> enemies, Entity movingUnit) {
        List<Entity> closestEnemies = enemies.stream().filter(e -> e.getPosition() != null).sorted((e1, e2) -> {
            boolean hasMoved1 = e1.isDone();
            boolean hasMoved2 = e2.isDone();
            double bonusDistance1 = hasMoved1 ? 0 : e1.getRunMP();
            double bonusDistance2 = hasMoved2 ? 0 : e2.getRunMP();
            double dist1 = e1.getPosition().distance(movingUnit.getPosition()) - bonusDistance1;
            double dist2 = e2.getPosition().distance(movingUnit.getPosition()) - bonusDistance2;
            return Double.compare(dist1, dist2);
        }).limit(5).toList();

        Coords medianPosition = Coords.median(closestEnemies.stream().map(Entity::getPosition).toList());
        return medianPosition;
    }

    protected double getBraveryMod(double successProbability, FiringPhysicalDamage damageEstimate, double expectedDamageTaken) {
        double maximumDamageDone = damageEstimate.getMaximumDamageEstimate();
        // My bravery modifier is based on my chance of getting to the
        // firing position (successProbability), how much damage I can do
        // (weighted by bravery), less the damage I might take.
        double braveryValue = getOwner().getBehaviorSettings().getBraveryValue();
        double braveryMod = (successProbability * maximumDamageDone * braveryValue) - expectedDamageTaken;
        logger.trace("bravery mod [{} = {} * (({} * {}) - {})]", braveryMod, successProbability, maximumDamageDone,
                braveryValue, expectedDamageTaken);
        return braveryMod;
    }

    // Only forces unit to move if there are no units around
    protected double calculateMovementMod(MovePath pathCopy, Game game, List<Entity> enemies, StringBuilder formula) {
        var favorHigherTMM = getOwner().getBehaviorSettings().getFavorHigherTMM();
        boolean noEnemiesInSight = enemies.isEmpty() && getOwner().getEnemyHotSpots().isEmpty();
        boolean disabledFavorHigherTMM = favorHigherTMM == 0;
        if (noEnemiesInSight || !disabledFavorHigherTMM) {
            var tmm = Compute.getTargetMovementModifier(pathCopy.getHexesMoved(), pathCopy.isJumping(), pathCopy.isAirborne(), game);
            double selfPreservation = getOwner().getBehaviorSettings().getSelfPreservationValue();
            var tmmValue = tmm.getValue();
            var movementFactor = tmmValue * (selfPreservation + favorHigherTMM);
            formula.append("movementMod [").append(movementFactor).append(" = ").append(tmmValue).append(" * (")
                .append(selfPreservation).append(" + ").append(favorHigherTMM).append(")]");
            logger.trace("movement mod [{} = {} * ({} + {})]", movementFactor, tmmValue, selfPreservation, favorHigherTMM);
            return movementFactor;
        }
        return 0.0;
    }

    protected double calculateCrowdingTolerance(MovePath movePath, List<Entity> enemies, double maxRange, StringBuilder formula) {
        var self = movePath.getEntity();
        formula.append(" crowdingTolerance ");
        if (!(self instanceof Mek) && !(self instanceof Tank)) {
            formula.append("[0 not a Mek or Tank]}");
            return 0.0;
        }

        var antiCrowding = getOwner().getBehaviorSettings().getAntiCrowding();
        if (antiCrowding == 0) {
            formula.append("[0 antiCrowding is disabled]}");
            return 0;
        }

        var antiCrowdingFactor = (10.0 / (11 - antiCrowding));
        final double herdingDistance = Math.ceil(antiCrowding * 1.3);
        final double closingDistance = Math.ceil(Math.max(3.0, maxRange * 0.6));

        var crowdingFriends = getOwner().getFriendEntities().stream()
            .filter(e -> e instanceof Mek || e instanceof Tank)
            .filter(Entity::isDeployed)
            .map(Entity::getPosition)
            .filter(Objects::nonNull)
            .filter(c -> c.distance(movePath.getFinalCoords()) <= herdingDistance)
            .count();

        var crowdingEnemies = enemies.stream()
            .filter(e -> e instanceof Mek || e instanceof Tank)
            .filter(Entity::isDeployed)
            .map(Entity::getPosition)
            .filter(Objects::nonNull)
            .filter(c -> c.distance(movePath.getFinalCoords()) <= closingDistance)
            .count();

        double friendsCrowdingTolerance = antiCrowdingFactor * crowdingFriends;
        double enemiesCrowdingTolerance = antiCrowdingFactor * crowdingEnemies;
        formula.append("[").append(friendsCrowdingTolerance + enemiesCrowdingTolerance).append(" = (")
            .append(antiCrowdingFactor).append(" * ").append(crowdingFriends).append(" friends) + (")
            .append(antiCrowdingFactor).append(" * ").append(crowdingEnemies).append(" enemies)]");
        return friendsCrowdingTolerance + enemiesCrowdingTolerance;
    }

    /**
     * Worker function that determines if a given enemy entity should be evaluated
     * as if it has moved.
     */
    public boolean evaluateAsMoved(Entity enemy) {
        // Aerospace units on ground maps can go pretty much anywhere they want, so it's
        // somewhat pointless to try to predict their movement.
        return !enemy.isSelectableThisTurn() || enemy.isImmobile() || enemy.isAirborneAeroOnGroundMap();
    }

    /**
     * Calculate who all other units would shoot at if I weren't around
     */
    @Override
    public void initUnitTurn(Entity unit, Game game) {
        bestDamageByEnemies.clear();
        List<Entity> enemies = getOwner().getEnemyEntities();
        List<Entity> friends = getOwner().getFriendEntities();
        for (Entity e : enemies) {
            double max_damage = 0;
            for (Entity f : friends) {
                if (f == unit) {
                    // Docstring says ignore self, so ignore self.
                    max_damage = 0;
                } else {
                    FiringPlanCalculationParameters guess = new FiringPlanCalculationParameters.Builder()
                            .buildGuess(e,
                                    null,
                                    f,
                                    null,
                                    (e.getHeatCapacity() - e.getHeat()) + 5,
                                    null);
                    double damage = getFireControl(f).determineBestFiringPlan(guess).getExpectedDamage();
                    if (damage > max_damage) {
                        max_damage = damage;
                    }
                }
            }
            bestDamageByEnemies.put(e.getId(), max_damage);
        }
    }

    public FiringPhysicalDamage calcDamageToStrategicTargets(MovePath path, Game game,
            FireControlState fireControlState, FiringPhysicalDamage damageStructure) {

        for (int i = 0; i < fireControlState.getAdditionalTargets().size(); i++) {
            Targetable target = fireControlState.getAdditionalTargets().get(i);

            if (target.isOffBoard() || (target.getPosition() == null)
                    || !game.getBoard().contains(target.getPosition())) {
                continue; // Skip targets not actually on the board.
            }

            FiringPlanCalculationParameters guess = new FiringPlanCalculationParameters.Builder()
                    .buildGuess(path.getEntity(),
                            new EntityState(path),
                            target,
                            null,
                            Entity.DOES_NOT_TRACK_HEAT,
                            null);
            FiringPlan myFiringPlan = getFireControl(path.getEntity()).determineBestFiringPlan(guess);

            double myDamagePotential = myFiringPlan.getUtility();
            if (myDamagePotential > damageStructure.firingDamage) {
                damageStructure.firingDamage = myDamagePotential;
            }

            if (path.getEntity() instanceof Mek) {
                PhysicalInfo myKick = new PhysicalInfo(
                        path.getEntity(), new EntityState(path), target,
                        null,
                        PhysicalAttackType.RIGHT_KICK, game, getOwner(),
                        true);
                double expectedKickDamage = myKick.getExpectedDamageOnHit() *
                        myKick.getProbabilityToHit();
                if (expectedKickDamage > damageStructure.physicalDamage) {
                    damageStructure.physicalDamage = expectedKickDamage;
                }
            }
        }
        return damageStructure;
    }

    /**
     * Gives the distance to the closest enemy unit, or -1 if none exist.
     * The reason being that the closest enemy unit may be 0 away.
     *
     * @param me       {@link Entity} who has enemies
     * @param position {@link Coords} from which the closest enemy is found
     * @param game     The current {@link Game}
     */
    @Override
    public double distanceToClosestEnemy(Entity me, Coords position, Game game) {
        Targetable closest = findClosestEnemy(me, position, game);
        if (closest == null) {
            return -1;
        }
        return closest.getPosition().distance(position);
    }

    /**
     * Gives the distance to the closest edge
     */
    int distanceToClosestEdge(Coords position, Game game) {
        int width = game.getBoard().getWidth();
        int height = game.getBoard().getHeight();
        int minimum = position.getX();
        if ((width - position.getX()) < minimum) {
            minimum = position.getX();
        }
        if (position.getY() < minimum) {
            minimum = position.getY();
        }
        if ((height - position.getY()) < minimum) {
            minimum = height - position.getY();
        }
        return minimum;
    }

    public double checkPathForHazards(MovePath path, Entity movingUnit, Game game) {
        logger.trace("Checking Path ({}) for hazards.", path);

        // If we're flying or swimming, we don't care about ground hazards.
        if (EntityMovementType.MOVE_FLYING.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_OVER_THRUST.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_SAFE_THRUST.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_VTOL_WALK.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_VTOL_RUN.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_VTOL_SPRINT.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_SUBMARINE_WALK.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_SUBMARINE_RUN.equals(path.getLastStepMovementType())) {

            logger.trace("Move Type ({}) ignores ground hazards.", path.getLastStepMovementType());
            return 0;
        }

        // If we're jumping, we only care about where we land.
        if (path.isJumping()) {
            logger.trace("Jumping, only checking landing hex.");
            Coords endCoords = path.getFinalCoords();
            Hex endHex = game.getBoard().getHex(endCoords);
            return checkHexForHazards(endHex, movingUnit, true,
                    path.getLastStep(), true,
                    path, game.getBoard());
        }

        double totalHazard = 0;
        Coords previousCoords = null;
        MoveStep lastStep = path.getLastStep();
        for (MoveStep step : path.getStepVector()) {
            Coords coords = step.getPosition();
            if ((coords == null) || coords.equals(previousCoords)) {
                continue;
            }
            Hex hex = game.getBoard().getHex(coords);
            totalHazard += checkHexForHazards(hex, movingUnit,
                    lastStep.equals(step), step,
                    false, path,
                    game.getBoard());
            previousCoords = coords;
        }
        logger.trace("Total Hazard = {}", totalHazard);
        return totalHazard;
    }

    private double checkHexForHazards(Hex hex, Entity movingUnit, boolean endHex, MoveStep step,
            boolean jumpLanding, MovePath movePath, Board board) {
        logger.trace("Checking Hex ({}) for hazards.", hex.getCoords());
        Set<Integer> hazards = getHazardTerrainIds(hex);
        // No hazards were found, so nothing to worry about.
        if (hazards.isEmpty()) {
            logger.trace("No hazards found.");
            return 0;
        }

        // Calculate hazard value by terrain type.
        double hazardValue = 0;
        for (int hazard : hazards) {
            switch (hazard) {
                case Terrains.FIRE:
                    hazardValue += calcFireHazard(movingUnit, endHex);
                    break;
                case Terrains.MAGMA:
                    hazardValue += calcMagmaHazard(hex, endHex, movingUnit, jumpLanding, step);
                    break;
                case Terrains.BLACK_ICE:
                case Terrains.ICE:
                    hazardValue += calcIceHazard(movingUnit, hex, step, movePath, jumpLanding);
                    break;
                case Terrains.WATER:
                    if (!hazards.contains(Terrains.ICE)) {
                        hazardValue += calcWaterHazard(movingUnit, hex, step, movePath);
                    }
                    break;
                case Terrains.BUILDING:
                    hazardValue += calcBuildingHazard(step, movingUnit, jumpLanding, board);
                    break;
                case Terrains.BRIDGE:
                    hazardValue += calcBridgeHazard(movingUnit, hex, step, jumpLanding, board);
                    break;
                case Terrains.SNOW:
                    hazardValue += calcSnowHazard(hex, endHex, movingUnit);
                    break;
                case Terrains.RUBBLE:
                    hazardValue += calcRubbleHazard(hex, endHex, movingUnit, jumpLanding);
                    break;
                case Terrains.SWAMP:
                    hazardValue += calcSwampHazard(hex, endHex, movingUnit, jumpLanding);
                    break;
                case Terrains.MUD:
                    hazardValue += calcMudHazard(endHex, movingUnit);
                    break;
                case Terrains.TUNDRA:
                    hazardValue += calcTundraHazard(endHex, jumpLanding, movingUnit);
                    break;
                case Terrains.PAVEMENT:
                    // 1 in 3 chance to hit Black Ice on any given Pavement hex
                    hazardValue += calcIceHazard(movingUnit, hex, step, movePath, jumpLanding) / 3.0;
                    break;
                case Terrains.HAZARDOUS_LIQUID:
                    hazardValue += calcHazardousLiquidHazard(hex, endHex, movingUnit, step);
                    break;
                case Terrains.ULTRA_SUBLEVEL:
                    hazardValue += calcUltraSublevelHazard(endHex, movingUnit);
            }
        }

        logger.trace("Total Hazard = {}", hazardValue);

        return hazardValue;
    }

    private Set<Integer> getHazardTerrainIds(Hex hex) {
        var hazards = new HashSet<Integer>(hex.getTerrainTypesSet());
        // Black Ice can appear if the conditions are favorable
        if (blackIce > 0) {
            hazards.retainAll(Terrains.HAZARDS_WITH_BLACK_ICE);
        } else {
            hazards.retainAll(Terrains.HAZARDS);
        }

        return hazards;
    }

    // Building collapse and basements are handled in PathRanker.validatePaths.
    private double calcBuildingHazard(MoveStep step, Entity movingUnit, boolean jumpLanding, Board board) {
        logger.trace("Checking Building ({}) for hazards.", step.getPosition());
        // Protos, BA and Infantry move through buildings freely.
        if (movingUnit.isProtoMek() || movingUnit.isInfantry() || movingUnit.isConventionalInfantry() || movingUnit.isBattleArmor()) {
            logger.trace("Safe for infantry and protos (0).");
            return 0;
        }

        // Jumping onto a building is handled in PathRanker validatePaths.
        if (jumpLanding) {
            return 0;
        }

        // Get the odds of failing the piloting roll while moving through the building.
        double odds = (1.0 - (Compute.oddsAbove(movingUnit.getCrew()
                .getPiloting()) / 100));
        logger.trace("Chance to fail piloting roll: {}", odds);
        // Hazard is based on potential damage taken.
        double dmg = board.getBuildingAt(step.getPosition())
                .getCurrentCF(step.getPosition()) / 10D;
        logger.trace("Potential building damage: {}", dmg);
        double hazard = dmg * odds;
        logger.trace("Total Hazard = {}", hazard);
        return hazard;
    }

    private double calcBridgeHazard(Entity movingUnit, Hex hex, MoveStep step, boolean jumpLanding, Board board) {
        logger.trace("Checking Bridge ({}) for hazards.", hex.getCoords());
        // if we are going to BWONGGG into a bridge from below, then it's treated as a
        // building.
        // Otherwise, bridge collapse checks have already been handled in validatePaths
        int bridgeElevation = hex.terrainLevel(Terrains.BRIDGE_ELEV);
        if ((bridgeElevation > step.getElevation()) &&
                (bridgeElevation <= (step.getElevation() + movingUnit.getHeight()))) {
            return calcBuildingHazard(step, movingUnit, jumpLanding, board);
        }

        return 0;
    }

    private double calcIceHazard(Entity movingUnit, Hex hex, MoveStep step, MovePath movePath, boolean jumpLanding) {
        logger.trace("Checking Ice ({}) for hazards.", hex.getCoords());
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
                EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above ice (0).");
            return 0;
        }

        // Infantry don't break ice.
        if (EntityMovementMode.INF_LEG == movingUnit.getMovementMode() ||
                EntityMovementMode.INF_MOTORIZED == movingUnit.getMovementMode() ||
                EntityMovementMode.INF_JUMP == movingUnit.getMovementMode() ||
                EntityMovementMode.INF_UMU == movingUnit.getMovementMode()) {
            logger.trace("Infantry on Ice (0).");
            return 0;
        }

        // Categorize chance to skid / fall
        double hazard = 0.0;
        if (!movePath.isCareful()) {
            // Most falling and skidding damage is weight-based...
            double arbitraryHazard = movingUnit.getWeight();
            hazard += Math.round(arbitraryHazard *
                    (1 - (Compute.oddsAbove(movingUnit.getCrew().getPiloting()) / 100.0)));
            if (movingUnit.isReckless()) {
                // Double the hazard for Reckless
                hazard *= 2;
            }
        }

        // If there is no water under the ice, don't worry about breaking
        // through.
        if (hex.depth() < 1) {;
            logger.trace("No water under ice (0).");
            return hazard;
        }

        // Hazard is based on chance to break through to the water underneath.
        double breakthroughMod = jumpLanding ? 0.5 : 0.1667;
        logger.trace("Chance to break through ice: {}", breakthroughMod);
        hazard += calcWaterHazard(movingUnit, hex, step, movePath) *
                breakthroughMod;
        logger.trace("Total Hazard = {}", hazard);
        // Changed this to UNIT_DESTRUCTION_FACTOR because she suicided too often.
        // No reason to be on the ice at all except as an absolute last resort.
        return UNIT_DESTRUCTION_FACTOR;
    }

    private double calcWaterHazard(Entity movingUnit, Hex hex, MoveStep step, MovePath movePath) {
        logger.trace("Checking Water ({}) for hazards.", hex.getCoords());
        // Puddles don't count.
        if (hex.depth() == 0) {
            logger.trace("Puddles don't count (0).");
            return 0;
        }

        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
                EntityMovementMode.WIGE == movingUnit.getMovementMode() ||
                EntityMovementMode.NAVAL == movingUnit.getMovementMode()) {
            logger.trace("Hovering or swimming above water (0).");
            return 0;
        }

        // Amphibious units are safe (kind of the point).
        if (movingUnit.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ||
                movingUnit.hasWorkingMisc(MiscType.F_AMPHIBIOUS)) {
            logger.trace("Amphibious unit (0).");
            return 0;
        }

        // Submarine units should be fine; Orca-riding Infantry goes here.
        if (EntityMovementMode.SUBMARINE == movingUnit.getMovementMode()) {
            logger.trace("Submarine units are safe (0).");
            return 0;
        }

        // if we are crossing a bridge, then we'll be fine. Trust me.
        // 1. Determine bridge elevation
        // 2. If unit elevation is equal to bridge elevation, skip.
        if (hex.containsTerrain(Terrains.BRIDGE_ELEV)) {
            int bridgeElevation = hex.terrainLevel(Terrains.BRIDGE_ELEV);
            if (bridgeElevation == step.getElevation()) {
                logger.trace("Bridge elevation matches unit elevation (0).");
                return 0;
            }
        }

        // Most other units are automatically destroyed. UMU-equipped units _may_ not
        // drown immediately,
        // but all other hazards (e.g. breaches, crush depth) still apply.
        if (!(movingUnit instanceof Mek || movingUnit instanceof ProtoMek ||
                movingUnit instanceof BattleArmor || movingUnit.hasUMU())) {
            logger.trace("Unit will drown ({}).", UNIT_DESTRUCTION_FACTOR);
            return UNIT_DESTRUCTION_FACTOR;
        }

        MoveStep lastStep = movePath.getLastStep();
        // Unsealed unit will drown.
        if (movingUnit instanceof Mek
                && ((Mek) movingUnit).isIndustrial()
                && !movingUnit.hasEnvironmentalSealing()
                && (movingUnit.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)
                && hex.depth() >= 1
                && step.equals(lastStep)) {
            double destructionFactor = hex.depth() >= 2 ? UNIT_DESTRUCTION_FACTOR : UNIT_DESTRUCTION_FACTOR * 0.5d;
            logger.trace("Industrial Meks drown too ({}).", destructionFactor);
            return destructionFactor;
        }

        // TODO: implement crush depth calcs (TO:AR pg. 40)

        // Find the submerged locations.
        Set<Integer> submergedLocations = new HashSet<>();
        for (int loc = 0; loc < movingUnit.locations(); loc++) {
            if (Mek.LOC_CLEG == loc && !(movingUnit instanceof TripodMek)) {
                continue;
            }

            if ((hex.depth() >= 2) || step.isProne() ||
                    !(movingUnit instanceof Mek)) {
                submergedLocations.add(loc);
                continue;
            }

            if (Mek.LOC_RLEG == loc || Mek.LOC_LLEG == loc ||
                    Mek.LOC_CLEG == loc) {
                submergedLocations.add(loc);
                continue;
            }

            if ((movingUnit instanceof QuadMek) && (Mek.LOC_RARM == loc ||
                    Mek.LOC_LARM == loc)) {
                submergedLocations.add(loc);
            }
        }
        logger.trace("Submerged locations: {}", submergedLocations);

        int hazardValue = 0;
        for (int loc : submergedLocations) {
            // Only locations without armor can breach in movement phase.
            if (movingUnit.getArmor(loc) > 0) {
                logger.trace("Location {} is not breached (0).", loc);
                continue;
            }

            // Meks or ProtoMeks having a head or torso breach is deadly.
            // For other units, any breach is deadly.
            // noinspection ConstantConditions
            if ((Mek.LOC_HEAD == loc) ||
                (Mek.LOC_CT == loc) ||
                (ProtoMek.LOC_HEAD == loc) ||
                (ProtoMek.LOC_TORSO == loc) ||
                (!movingUnit.isMek() && !movingUnit.isProtoMek())
            ) {
                logger.trace("Location {} breached and critical (1000).", loc);
                return UNIT_DESTRUCTION_FACTOR;
            }

            // Add 50 points per potential breach location.
            logger.trace("Location {} breached (50).", loc);
            hazardValue += 50;
        }

        return hazardValue;
    }

    private double calcFireHazard(Entity movingUnit, boolean endHex) {
        logger.trace("Calculating fire hazard.");
        double hazardValue = 0;

        // Fireproof BA ignores fire.
        if ((movingUnit instanceof BattleArmor) &&
                ((BattleArmor) movingUnit).isFireResistant()) {
            logger.trace("Fireproof BA ignores fire.");
            return 0;
        }

        // Tanks risk critical hits.
        if (movingUnit instanceof Tank) {
            logger.trace("Tank risks critical hit (25).");
            return 25;
        }

        // ProtoMeks risk location destruction.
        if (movingUnit instanceof ProtoMek) {
            logger.trace("ProtoMek risks location destruction (50).");
            return 50;
        }

        // Infantry and BA risk total destruction.
        if (movingUnit instanceof Infantry) {
            logger.trace("Infantry risks total destruction (1000).");
            return UNIT_DESTRUCTION_FACTOR;
        }

        // If this unit tracks heat, add the heat gain to the hazard value.
        if (movingUnit.getHeatCapacity() != Entity.DOES_NOT_TRACK_HEAT) {
            hazardValue += endHex ? 5 : 2;
            logger.trace("Heat gain ({}).", hazardValue);
        }

        return hazardValue;
    }

    private double calcMagmaHazard(Hex hex, boolean endHex, Entity movingUnit, boolean jumpLanding, MoveStep step) {
        logger.trace("Calculating magma hazard.");
        // Hovers / WiGE are normally unaffected.
        if ((EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
                EntityMovementMode.WIGE == movingUnit.getMovementMode()) && !endHex) {
            logger.trace("Hovering above magma (0).");
            return 0;
        }

        double hazardValue = 0;
        int magmaLevel = hex.terrainLevel(Terrains.MAGMA);

        // Liquid magma.
        if (magmaLevel == 2) {
            return calcLavaHazard(endHex, jumpLanding, movingUnit, step);
        } else {
            double breakThroughMod = jumpLanding ? 0.5 : 0.1667;
            logger.trace("Chance to break through crust = {}", breakThroughMod);
            // Factor in the chance to break through.
            double lavalHazard = Math.round(calcLavaHazard(endHex, jumpLanding, movingUnit, step) * breakThroughMod);
            logger.trace("Lava hazard = {}", lavalHazard);
            hazardValue += lavalHazard;

            // Factor in heat.
            if (movingUnit.getHeatCapacity() != Entity.DOES_NOT_TRACK_HEAT) {
                double heatMod = (endHex ? 5 : 2) * (1 - breakThroughMod);
                hazardValue += heatMod;
                logger.trace("Heat gain = {}", heatMod);
            }
        }

        return hazardValue;
    }

    private double calcLavaHazard(boolean endHex, boolean jumpLanding, Entity movingUnit, MoveStep step) {
        logger.trace("Calculating lava hazard.");
        int unitDamageLevel = movingUnit.getDamageLevel();
        double dmg;

        // Hovers/VTOLs are unaffected _unless_ they end on the hex and are in danger of
        // losing mobility.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode()
                || EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            if (!endHex) {
                logger.trace("Hovering/VTOL while traversing lava (0).");
                return 0;
            } else {
                // Estimate chance of being disabled or immobilized over open lava; this is
                // fatal!
                // Calc expected damage as ((current damage level [0 ~ 4]) / 4) *
                // UNIT_DESTRUCTION_FACTOR
                dmg = (unitDamageLevel / 4.0) * UNIT_DESTRUCTION_FACTOR;
                logger.trace("Ending hover/VTOL movement over lava ({}).", dmg);
                return dmg;
            }
        }

        // Non-Mek units auto-destroyed.
        if (!(movingUnit instanceof Mek)) {
            logger.trace("Non-Mek instant destruction (1000).");
            return UNIT_DESTRUCTION_FACTOR;
        }

        double hazardValue = 0;
        double psrFactor = 1.0;

        // Adjust hazard by chance of getting stuck
        if (endHex && jumpLanding) {
            // Chance of getting stuck in magma is the chance of failing one PSR.
            // Factor applied to damage should also include the expected number of turns
            // _not_ escaping.
            // Former is: %chance _not_ passing PSR
            // Latter is: N = log(desired failure to escape chance, e.g. 10%)/log(%chance
            // Fail PSR)
            logger.trace("Jumping onto lava hex, may get bogged down.");
            int pilotSkill = movingUnit.getCrew().getPiloting();
            int psrMod = +4;
            double oddsPSR = Compute.oddsAbove(pilotSkill + psrMod) / 100;
            double oddsBogged = (1.0 - oddsPSR);
            double expectedTurns = Math.log10(0.10) / Math.log10(oddsBogged);
            logger.trace("Chance to bog down = {}, expected turns = {}", oddsBogged, expectedTurns);
            psrFactor = 1.0 + oddsBogged + (expectedTurns);
        }

        // Factor in heat.
        double heat = endHex ? 10.0 : 5.0;
        hazardValue += heat;
        logger.trace("Heat gain = {}", heat);
        // Factor in potential to suffer fatal damage.
        // Dependent on expected average damage / exposed remaining armor *
        // UNIT_DESTRUCTION_FACTOR
        int exposedArmor;
        if (step.isProne()) {
            dmg = 7 * movingUnit.locations();
            exposedArmor = movingUnit.getTotalArmor();
            logger.trace("Prone Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else if (movingUnit instanceof BipedMek) {
            dmg = 14;
            exposedArmor = Stream.of(Mek.LOC_LLEG, Mek.LOC_RLEG).mapToInt(movingUnit::getArmor).sum();
            logger.trace("Biped Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else if (movingUnit instanceof TripodMek) {
            exposedArmor = Stream.of(Mek.LOC_LLEG, Mek.LOC_RLEG, Mek.LOC_CLEG)
                    .mapToInt(movingUnit::getArmor).sum();
            dmg = 21;
            logger.trace("Tripod Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else {
            exposedArmor = Stream.of(Mek.LOC_LLEG, Mek.LOC_RLEG, Mek.LOC_LARM, Mek.LOC_RARM)
                    .mapToInt(movingUnit::getArmor).sum();
            dmg = 28;
            logger.trace("Quad Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        }
        hazardValue += (UNIT_DESTRUCTION_FACTOR * (dmg / Math.max(exposedArmor, 1)));

        // Multiply total hazard value by the chance of getting stuck for 1 or more
        // additional turns
        logger.trace("Total hazard = {}", hazardValue * psrFactor);
        return Math.round(hazardValue * psrFactor);
    }

    private double calcHazardousLiquidHazard(Hex hex, boolean endHex, Entity movingUnit, MoveStep step) {
        logger.trace("Calculating hazardous liquid hazard.");
        int unitDamageLevel = movingUnit.getDamageLevel();
        double dmg;

        // Hovers/VTOLs are unaffected _unless_ they end on the hex and are in danger of
        // losing mobility.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode()
            || EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            if (!endHex) {
                logger.trace("Hovering/VTOL while traversing hazardous liquids (0).");
                return 0;
            } else {
                // Estimate chance of being disabled or immobilized over open lava; this is
                // fatal!
                // Calc expected damage as ((current damage level [0 ~ 4]) / 4) *
                // UNIT_DESTRUCTION_FACTOR
                dmg = (unitDamageLevel / 4.0) * UNIT_DESTRUCTION_FACTOR;
                logger.trace("Ending hover/VTOL movement over lava ({}).", dmg);
                return dmg;
            }
        }

        dmg = (HazardousLiquidPoolUtil.AVERAGE_DAMAGE_HAZARDOUS_LIQUID_POOL * HazardousLiquidPoolUtil.getHazardousLiquidPoolDamageMultiplierForUnsealed(movingUnit))
            / (HazardousLiquidPoolUtil.getHazardousLiquidPoolDamageDivisorForInfantry(movingUnit));

        // After all that math let's make sure we do at least 1 damage
        // (.6 repeating when normalized for the HLP doing no damage 1/3 of the time)
        dmg = Math.max(dmg, 2.0/3.0);

        // Factor in potential to suffer fatal damage.
        // Dependent on expected average damage / exposed remaining armor *
        // UNIT_DESTRUCTION_FACTOR
        int exposedArmor;
        double hazardValue = 0;
        if (step.isProne() || (hex.containsTerrain(Terrains.WATER) && hex.terrainLevel(Terrains.WATER) > 1)) {
            exposedArmor = movingUnit.getTotalArmor();
            logger.trace("Fully Submerged damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else if (movingUnit instanceof BipedMek) {
            exposedArmor = Stream.of(Mek.LOC_LLEG, Mek.LOC_RLEG).mapToInt(movingUnit::getArmor).sum();
            logger.trace("Biped Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else if (movingUnit instanceof TripodMek) {
            exposedArmor = Stream.of(Mek.LOC_LLEG, Mek.LOC_RLEG, Mek.LOC_CLEG)
                .mapToInt(movingUnit::getArmor).sum();
            logger.trace("Tripod Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else if (movingUnit instanceof QuadMek){
            exposedArmor = Stream.of(Mek.LOC_LLEG, Mek.LOC_RLEG, Mek.LOC_LARM, Mek.LOC_RARM)
                .mapToInt(movingUnit::getArmor).sum();
            logger.trace("Quad Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else {
            exposedArmor = movingUnit.getTotalArmor();
            logger.trace("Fully Submerged non-mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        }
        hazardValue += (UNIT_DESTRUCTION_FACTOR * (dmg / Math.max(exposedArmor, 1)));

        logger.trace("Total hazard = {}", hazardValue);
        return Math.round(hazardValue);
    }

    private double calcUltraSublevelHazard(boolean endHex, Entity movingUnit) {
        logger.trace("Calculating ultra sublevel hazard.");
        int unitDamageLevel = movingUnit.getDamageLevel();
        double dmg;

        // Hovers/VTOLs are unaffected _unless_ they end on the hex and are in danger of
        // losing mobility.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode()
            || EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            if (!endHex) {
                logger.trace("Hovering/VTOL while traversing ultra sublevel (0).");
                return 0;
            } else if (movingUnit.getElevation() > 0) {  //elevation of 0 is on the ground (not airborne), which would be destroyed
                // Estimate chance of being disabled or immobilized over ultra sublevel; this is
                // fatal!
                // Calc expected damage as ((current damage level [0 ~ 4]) / 4) *
                // UNIT_DESTRUCTION_FACTOR
                dmg = (unitDamageLevel / 4.0) * UNIT_DESTRUCTION_FACTOR;
                logger.trace("Ending hover/VTOL movement over ultra sublevel ({}).", dmg);
                return dmg;
            }
        }
        logger.trace(String.format("Ground unit instant destruction from ultra sublevel (%s).", UNIT_DESTRUCTION_FACTOR));
        return UNIT_DESTRUCTION_FACTOR;
    }

    private double calcBogDownFactor(String name, boolean endHex, boolean jumpLanding, int pilotSkill, int modifier) {
        return calcBogDownFactor(name, endHex, jumpLanding, pilotSkill, modifier, true);
    }

    /**
     * Calculate a PSR-related factor for increasing the hazard of terrain where
     * bogging down is
     * possible
     *
     * @param name        Name of terrain type, for logging.
     * @param endHex      If this is checking the final hex of a movement path or
     *                    not.
     * @param jumpLanding Whether unit will be jumping into the end hex or not.
     * @param pilotSkill  base pilot/driver/etc. skill used for the PSR checks to
     *                    escape bogging down.
     * @param modifier    Modifier, based on unit type and terrain type
     * @param bogPossible whether the unit can actually get bogged own in this
     *                    terrain type, or just calculating
     * @return double Factor to multiply by terrain hazards.
     */
    private double calcBogDownFactor(String name, boolean endHex, boolean jumpLanding, int pilotSkill,
            int modifier, boolean bogPossible) {
        double factor;
        int effectiveSkill = pilotSkill + modifier;
        double oddsPSR = Math.max((Compute.oddsAbove(effectiveSkill) / 100.0), 0.0);
        double oddsBogged = 0.0;

        // Adjust hazard by chance of getting stuck
        if (endHex && jumpLanding) {
            // Chance of getting stuck in swamp/mud is the chance of failing one PSR, or
            // 100% if jumping.
            oddsBogged = 1.0;
            logger.trace("Jumping onto {} hex, would get bogged down.", name);
        } else if (!jumpLanding) {
            oddsBogged = 1.0 - oddsPSR;
            logger.trace("Entering onto {} hex, chance to bog down = {}", name, oddsBogged);
        }
        // (Reuse PSR odds to avoid infinite trapped time on turns when jumping into
        // terrain causes 100% bog-down)
        double expectedTurns = ((1 - oddsPSR) < 1.0) ? Math.log10(0.10) / Math.log10(1 - oddsPSR)
                : UNIT_DESTRUCTION_FACTOR;

        if (bogPossible) {
            logger.trace("Chance to bog down = {}, expected turns = {}", oddsBogged, expectedTurns);
        }
        factor = 1.0 + oddsBogged + (expectedTurns);
        logger.trace("Effective Piloting Skill: {}, Expected turns before escape: {}, Factor = {}",
            effectiveSkill, expectedTurns, factor);
        return factor;
    }

    private double calcSnowHazard(Hex hex, boolean endHex, Entity movingUnit) {
        logger.trace("Checking Snow ({}) for hazards.", hex.getCoords());
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode()
                || EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above snow (0).");
            return 0;
        }

        if (hex.terrainLevel(Terrains.SNOW) > 1) {
            // PSR checks _to bog down_ and _escape bogged down_ are at (mod - 1); all
            // others are at a +1 mod
            int psrMod = 0;
            // Infantry use 4+ check instead of Pilot / Driving skill
            int pilotSkill = (movingUnit.isInfantry()) ? 4 : movingUnit.getCrew().getPiloting();
            double hazard;

            // Base hazard is arbitrarily set to 10
            hazard = 10 * calcBogDownFactor(
                    "Deep Snow", endHex, false, pilotSkill, psrMod);

            logger.trace("Deep snow hazard = {}", hazard);
            return Math.round(hazard);
        }

        // Thin snow poses no hazard; MP malus accounted for elsewhere.
        return 0;
    }

    private double calcSwampHazard(Hex hex, boolean endHex, Entity movingUnit, boolean jumpLanding) {
        logger.trace("Checking Swamp ({}) for hazards.", hex.getCoords());
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode()
                || EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above swamp (0).");
            return 0;
        }

        // Base hazard is the chance of becoming Quicksand and destroying this unit
        // If currently Swamp, could become Quicksand if 12 on 2d6 is rolled.
        // If already Quicksand...
        boolean quicksand = hex.terrainLevel(Terrains.SWAMP) > 1;
        String type = (quicksand) ? "Quicksand" : "Swamp";
        double quicksandChance = (quicksand) ? 1.0 : 1 / 36.0;
        // Height + 1 turns to fully sink and be destroyed
        double hazard = quicksandChance * UNIT_DESTRUCTION_FACTOR / (1 + movingUnit.getHeight());
        logger.trace("Base hazard value: {}", hazard);
        // Mod is to difficulty, not to PSR roll results
        // Quicksand makes PSRs an additional +3! Otherwise +1 for Meks, +2 for all
        // other types
        int psrMod = (quicksand) ? +3 : (movingUnit instanceof Mek) ? +1 : +2;

        // Infantry use 4+ check instead of Pilot / Driving skill
        int pilotSkill = (movingUnit.isInfantry()) ? 4 : movingUnit.getCrew().getPiloting();

        double factor = calcBogDownFactor(type, endHex, jumpLanding, pilotSkill, psrMod);
        logger.trace("Factor applied to hazard value: {}", factor);
        // The danger is increased if pilot skill is low, as the chance of succumbing or
        // getting
        // permanently stuck increases!
        hazard = hazard * factor;

        return Math.round(hazard);
    }

    private double calcMudHazard(boolean endHex, Entity movingUnit) {
        logger.trace("Checking Mud for hazards.");
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode()
                || EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above Mud (0).");
            return 0;
        }

        // PSR checks _to bog down_ and _escape bogged down_ are at (mod - 1); all
        // others are at +1 mod
        int psrMod = 0;
        // Infantry use 4+ check instead of Pilot / Driving skill
        int pilotSkill = (movingUnit.isInfantry()) ? 4 : movingUnit.getCrew().getPiloting();
        double hazard;

        if (movingUnit.isMek()) {
            // The only hazard is the +1 to PSRs, which are difficult to quantify
            // Even jumping Meks cannot bog down in mud.
            hazard = calcBogDownFactor("Mud", endHex, false, pilotSkill, psrMod, false);
        } else {
            // Mud is more dangerous for units that can actually bog down
            // Base hazard is arbitrarily set to 10
            hazard = 10 * calcBogDownFactor("Mud", endHex, false, pilotSkill, psrMod);
        }
        logger.trace("Mud hazard = {}", hazard);
        return Math.round(hazard);
    }

    private double calcTundraHazard(boolean endHex, boolean jumpLanding, Entity movingUnit) {
        logger.trace("Checking Tundra for hazards.");
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode()
                || EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above Tundra (0).");
            return 0;
        }

        // PSR checks _to bog down_ and _escape bogged down_ are at (mod - 1); all
        // others are at +1 mod
        int psrMod = 0;
        // Infantry use 4+ check instead of Pilot / Driving skill
        int pilotSkill = (movingUnit.isInfantry()) ? 4 : movingUnit.getCrew().getPiloting();
        double hazard;

        // Base hazard is arbitrarily set to 10
        hazard = 10 * calcBogDownFactor("Tundra", endHex, jumpLanding, pilotSkill, psrMod);
        logger.trace("Tundra hazard = {}", hazard);
        return Math.round(hazard);
    }

    private double calcRubbleHazard(Hex hex, boolean endHex, Entity movingUnit, boolean jumpLanding) {
        logger.trace("Checking Rubble ({}) for hazards.", hex.getCoords());
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode()
                || EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above Rubble (0).");
            return 0;
        }

        double hazard = 0;

        boolean caresAboutRubble = ((!jumpLanding || endHex)
                && (hex.terrainLevel(Terrains.RUBBLE) > 0)
                && (hex.terrainLevel(Terrains.PAVEMENT) == Terrain.LEVEL_NONE)
                && movingUnit.canFall());

        if (caresAboutRubble) {
            // PSR checks are at +0 for Rubble levels up to 6, Ultra, which is +1
            int psrMod = (hex.terrainLevel(Terrains.RUBBLE) < 6) ? 0 : 1;
            if (movingUnit.hasAbility(OptionsConstants.PILOT_TM_MOUNTAINEER)) {
                psrMod -= 1;
            }
            int pilotSkill = movingUnit.getCrew().getPiloting();

            // The only hazard is the +1 to PSRs, which are difficult to quantify
            hazard = calcBogDownFactor("Rubble", endHex, jumpLanding, pilotSkill, psrMod, false);
        }
        logger.trace("Total Hazard = {}", hazard);
        return Math.round(hazard);
    }
}
