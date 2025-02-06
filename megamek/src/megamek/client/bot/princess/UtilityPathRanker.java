/*
 * Copyright (c) 2018-2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.codeUtilities.MathUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.logging.MMLogger;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static megamek.client.bot.princess.EnemyTracker.hitChance;

public class UtilityPathRanker extends BasicPathRanker {
    private final static MMLogger logger = MMLogger.create(UtilityPathRanker.class);

    private static final double COVERAGE_RATIO = 0.6;


    public UtilityPathRanker(Princess owningPrincess) {
        super(owningPrincess);
    }


    @Override
    public @Nullable Coords calculateAlliesCenter(int myId, @Nullable List<Entity> friends, Game game) {
        return getOwner().getSwarmContext().getCurrentCenter();
    }
    /**
     * Returns the best path of a list of ranked paths.
     *
     * @param ps The list of ranked paths to process
     * @return "Best" out of those paths
     */
    @Override
    public @Nullable RankedPath getBestPath(TreeSet<RankedPath> ps) {
        if (ps.isEmpty()) {
            return null;
        }
        RankedPath bestRankedPath = ps.first();
        return bestRankedPath;
    }

    /**
     * A path ranking
     */
    @Override
    protected RankedPath rankPath(MovePath path, Game game, int maxRange, double fallTolerance, List<Entity> enemies,
                                  Coords friendsCoords) {
        Entity movingUnit = path.getEntity();
        checkBlackIcePresence(game);
        MovePath pathCopy = path.clone();

        // Worry about failed piloting rolls (weighted by Fall Shame).
        double successProbability = getMovePathSuccessProbability(pathCopy);


        // Worry about how badly we can damage ourselves on this path!
        double expectedDamageTaken = calculateMovePathPSRDamage(movingUnit, pathCopy);
        expectedDamageTaken += checkPathForHazards(pathCopy, movingUnit, game);
        expectedDamageTaken += MinefieldUtil.checkPathForMinefieldHazards(pathCopy);

        // look at all of my enemies
        FiringPhysicalDamage damageEstimate = new FiringPhysicalDamage();

        boolean extremeRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE);
        boolean losRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE);
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

//        double braveryMod = getBraveryMod(successProbability, damageEstimate, expectedDamageTaken);

        var isNotAirborne = !path.getEntity().isAirborneAeroOnGroundMap();
        // the only critters not subject to aggression and herding mods are
        // airborne aeros on ground maps, as they move incredibly fast
        // The further I am from a target, the lower this path ranks
        // (weighted by Aggression slider).
        double aggressionMod = isNotAirborne ?
            calculateAggressionMod(movingUnit, pathCopy, maxRange, game) : 1.0;
        // The further I am from my teammates, the lower this path
        // ranks (weighted by Herd Mentality).

        double fallMod = calculateFallMod(successProbability);

        // Movement is good, it gives defense and extends a player power in the game.
        double movementMod = calculateMovementMod(pathCopy, game);

        // Try to face the enemy.
        double facingMod = calculateFacingMod(pathCopy);

        // If I need to flee the board, I want to get closer to my home edge.
        double selfPreservationMod = calculateSelfPreservationMod(movingUnit, pathCopy, game);
        // if we're an aircraft, we want to de-value paths that will force us off the
        // board
        // on the subsequent turn.
        // Include in utility calculation:
        double strategicMod = calculateStrategicGoalMod(pathCopy);
        double formationMod = calculateFormationModifier(path);
        double exposurePenalty = calculateExposurePenalty(movingUnit, pathCopy, enemies);

        double fallBack = shouldFallBack(pathCopy, movingUnit, enemies.get(0)) ? 0.5 : 1.0;
        double utility = 1.0;
        utility *= fallMod;
        utility *= formationMod;
        utility *= aggressionMod;
        utility *= movementMod;
        utility *= facingMod;
        utility *= selfPreservationMod;
        utility *= strategicMod;
        utility *= exposurePenalty;
        utility *= fallBack;

        double modificationFactor = 1.0 - (1.0 / 13.0);
        double makeUpValue = (1 - utility) * modificationFactor;
        double finalScore = utility + (makeUpValue * utility);

        utility = clamp01(finalScore);

        RankedPath rankedPath = new RankedPath(utility, pathCopy,
            "utility [" + utility + "= fallMod(" + fallMod + ") * formationMod("+ formationMod +
                ") * movementMod("+ movementMod + ") * aggressionMod(" + aggressionMod + ") * fallBack(" + fallBack +
                ") * facingMod("+ facingMod +") * selfPreservationMod("+selfPreservationMod+") * strategicMod("+strategicMod+
                ") * exposurePenalty("+exposurePenalty+")]"
        );

        logger.info(rankedPath.getReason());
        rankedPath.setExpectedDamage(damageEstimate.getMaximumDamageEstimate());
        return rankedPath;
    }

    private void checkBlackIcePresence(Game game) {
        if (blackIce == -1) {
            blackIce = ((game.getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE)
                && game.getPlanetaryConditions().getTemperature() <= PlanetaryConditions.BLACK_ICE_TEMP)
                || game.getPlanetaryConditions().getWeather().isIceStorm()) ? 1 : 0;
        }
    }

    /**
     * When playing Double Blind, we want to move towards strategic goals.
     * @param path The path to evaluate
     * @return score from 1 to 0
     */
    private double calculateStrategicGoalMod(MovePath path) {
        if (!getOwner().getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            return 1.0;
        }

        // Existing strategic goal calculation
        double maxGoalUtility = 0.0;
        for (Coords goal : getOwner().getSwarmContext().getStrategicGoalsOnCoordsQuadrant(path.getFinalCoords())) {
            double distance = path.getFinalCoords().distance(goal);
            double utility = (10.0 / (distance + 1.0));
            maxGoalUtility = Math.max(maxGoalUtility, utility);
        }
        return clamp01(maxGoalUtility);
    }

    /**
     * Calculates bravery modifier of the unit
     * @param successProbability The probability of success of the move
     * @param damageEstimate The estimated damage that the unit can do
     * @param expectedDamageTaken The expected damage that the unit will take
     * @return The bravery modifier score
     */
    @Override
    protected double getBraveryMod(double successProbability, FiringPhysicalDamage damageEstimate, double expectedDamageTaken) {
        double maximumDamageDone = damageEstimate.getMaximumDamageEstimate();
        double braveryFactor = getOwner().getBehaviorSettings().getBraveryIndex() / 10.0;
        return clamp01(1.1 - braveryFactor + (successProbability * (maximumDamageDone / Math.max(1.0, expectedDamageTaken))) * braveryFactor);
    }

    /**
     * Calculates the TMM score of the unit
     * @param pathCopy The path to evaluate
     * @param game The game
     * @return The TMM score
     */
    protected double calculateMovementMod(MovePath pathCopy, Game game) {
        var tmmFactor = getOwner().getBehaviorSettings().getFavorHigherTMM() / 10.0;
        var tmm = Compute.getTargetMovementModifier(pathCopy.getHexesMoved(), pathCopy.isJumping(), pathCopy.isAirborne(), game);
        var tmmValue = MathUtility.clamp(tmm.getValue() / 8.0, 0.0, 1.0);
        return tmmValue * tmmFactor;
    }

    protected double calculateFallMod(double successProbability) {
        double fallShameFactor = getOwner().getBehaviorSettings().getFallShameIndex() / 10.0;
        return clamp01((1 - fallShameFactor) + successProbability * fallShameFactor);
    }

    private int getFacingDiff(MovePath path) {
        List<Entity> threats = getOwner().getEnemyTracker().getPriorityTargets(path.getFinalCoords(), 5);
        if (threats.isEmpty()) {
            return 1;
        }
        Coords position = Coords.average(threats.stream().map(Entity::getPosition).toList());
        // Calculate optimal facing direction
        int bestFacing = calculateOptimalFacing(path, position);

        int currentFacing = path.getFinalFacing();
        int facingDiff = Math.abs(currentFacing - bestFacing);
        facingDiff = Math.min(facingDiff, 6 - facingDiff); // Account for hex directions

        // Heavy penalty for exposing rear
        if (isRearExposed(path.getFinalCoords(), currentFacing, threats)) {
            facingDiff = 6;
        }

        return facingDiff;
    }

    private boolean isRearExposed(Coords position, int facing, List<Entity> threats) {
        int rearArc = (facing + 3) % 6;
        return threats.stream()
                .anyMatch(e -> position.direction(e.getPosition()) == rearArc);
    }

    private int calculateOptimalFacing(MovePath movePath, Coords position) {
        return movePath.getFinalCoords().direction(position);
    }

    protected double calculateFacingMod(final MovePath path) {
        int facingDiff = getFacingDiff(path);
        var facingMod = 1 / Math.pow(10, facingDiff);

        logger.trace("facing mod [{} = 1 / (10 ^ {})", facingMod, facingDiff);
        return clamp01(facingMod);
    }

    private double calculateFormationModifier(MovePath path) {
        SwarmContext.SwarmCluster cluster = getOwner().getSwarmContext().getClusterFor(path.getEntity());

        double lineMod = calculateLineFormationMod(path, cluster);
        double coverageMod = calculateCoverageModifier(path);
        double spacingMod = calculateOptimalSpacingMod(path, cluster);

        return clamp01(lineMod * coverageMod * spacingMod);
    }

    private double calculateCoverageModifier(MovePath path) {
        long coveringAllies = getOwner().getFriendEntities().stream()
                .filter(a -> a.getPosition().distance(path.getFinalCoords()) <=
                        a.getMaxWeaponRange() * COVERAGE_RATIO)
                .count();

        return 0.8 + (coveringAllies * 0.1);
    }

    private double calculateOptimalSpacingMod(MovePath path, SwarmContext.SwarmCluster cluster) {
        double avgDistance = cluster.members.stream()
                .filter(m -> m != path.getEntity())
                .mapToDouble(m -> m.getPosition().distance(path.getFinalCoords()))
                .average()
                .orElse(0);

        // Ideal spacing between 3-5 hexes
        if (avgDistance < 3) return 0.8;
        if (avgDistance > 5) return 0.9;
        return 1.0;
    }

    protected double calculateAggressionMod(Entity movingUnit, MovePath path, double maxRange, Game game) {

        var distToEnemy = distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game);

        if (distToEnemy == 0) {
            distToEnemy = 2;
        }

        maxRange = Math.max(1, maxRange);

        double weight = getOwner().getBehaviorSettings().getHyperAggressionIndex() / 10.0;
        double aggression = clamp01(maxRange / distToEnemy);
        return clamp01(1.1 - weight + aggression * weight);
    }

    @Override
    protected double calculateSelfPreservationMod(Entity movingUnit, MovePath path, Game game) {
        UnitBehavior.BehaviorType behaviorType = getOwner().getUnitBehaviorTracker().getBehaviorType(movingUnit, getOwner());
        double weight = getOwner().getBehaviorSettings().getSelfPreservationIndex() / 10.0;
        if (behaviorType == UnitBehavior.BehaviorType.ForcedWithdrawal || behaviorType == UnitBehavior.BehaviorType.MoveToDestination) {
            int newDistanceToHome = distanceToHomeEdge(path.getFinalCoords(), getOwner().getHomeEdge(movingUnit), game);
            int currentDistanceToHome = distanceToHomeEdge(path.getEntity().getPosition(), getOwner().getHomeEdge(movingUnit), game);

            double deltaDistance = currentDistanceToHome - newDistanceToHome;
            double selfPreservationMod;

            // normally, we favor being closer to the edge we're trying to get to
            if (deltaDistance > 0 && currentDistanceToHome > 0) {
                selfPreservationMod = 1.0 - newDistanceToHome / (double) currentDistanceToHome;
            } else if (deltaDistance < 0){
                selfPreservationMod = 1.0 - currentDistanceToHome / (double) newDistanceToHome;
            } else {
                selfPreservationMod = 1.0;
            }

            return clamp01(1.1 - weight + selfPreservationMod * weight);
        }
        logger.trace("self preservation mod [1] - not moving nor forced to withdraw");
        return clamp01(1.0);
    }

    private double calculateExposurePenalty(Entity unit, MovePath movePath, List<Entity> enemies) {
        if (unit.getRole() == UnitRole.AMBUSHER || unit.getRole() == UnitRole.SCOUT || unit.getRole() == UnitRole.MISSILE_BOAT || unit.getRole() == UnitRole.SNIPER) { // SCOUT/FLANKER
            long threateningEnemies = enemies.stream()
                    .filter(Entity::isDone) // Only consider enemies that have moved
                    .filter(e -> hitChance(getOwner().getGame(), e, unit) > 0.33)
                    .count();
            long somewhatThreateningEnemies = enemies.stream()
                    .filter(e -> !e.isDone()) // Only consider enemies that have moved
                    .filter(e -> hitChance(getOwner().getGame(), e, unit) > 0.5)
                    .count();
            if (getOwner().getCoverageValidator().validateUnitCoverage(unit, movePath.getFinalCoords())) {
                double exposureScore = 1 + (threateningEnemies * 0.3 + somewhatThreateningEnemies * 0.15);
                return 1.0 / exposureScore;
            } else {
                double exposureScore = 1 + (threateningEnemies * 0.5 + somewhatThreateningEnemies * 0.25);
                return 1.0 / exposureScore;
            }
        }
        return 1.0;
    }

    private boolean shouldFallBack(MovePath movePath, Entity unit, Entity threat) {
        return getOwner().getCoverageValidator().isPositionExposed(unit)
                && !getOwner().getCoverageValidator().validateUnitCoverage(unit, movePath.getFinalCoords())
                && getOwner().getCoverageValidator().isPositionExposed(threat);
    }

    private Coords calculatePrimaryThreatPosition(SwarmContext.SwarmCluster cluster) {
        List<Entity> threats = getOwner().getEnemyTracker().getPriorityTargets(cluster.centroid, 5);
        var positions = threats.stream()
                .map(Entity::getPosition)
                .toList();
        return Coords.average(positions);
    }

    static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }

    private double calculateLineFormationMod(MovePath path, SwarmContext.SwarmCluster cluster) {
        // 1. Get combat parameters
        Entity unit = path.getEntity();
        Coords currentPos = unit.getPosition();
        Coords newPos = path.getFinalCoords();
        double maxRange = unit.getMaxWeaponRange();
        double optimalRange = maxRange * 0.6;

        // 2. Calculate threat parameters
        Coords primaryThreat = calculatePrimaryThreatPosition(cluster);
        int threatDirection = cluster.centroid.direction(primaryThreat);

        // 3. Determine ideal firing line position
        Coords idealPosition = calculateLinePosition(
                cluster.centroid,
                threatDirection,
                cluster.members.indexOf(unit),
                optimalRange
        );

        // 4. Calculate position quality components
        double rangeQuality = calculateRangeQuality(newPos, primaryThreat, optimalRange, maxRange);
        double formationQuality = calculateFormationQuality(newPos, idealPosition);
        double forwardBias = calculateForwardBias(currentPos, newPos, primaryThreat);
        double borderPenalty = calculateBorderPenalty(newPos);

        // 5. Combine modifiers with weights
        double positionQuality = (rangeQuality * 0.5) +
                (formationQuality * 0.3) +
                (forwardBias * 0.2) -
                borderPenalty;

        return clamp01(positionQuality);
    }

    private Coords calculateLinePosition(Coords centroid, int threatDirection, int unitIndex, double optimalRange) {
        // 1. Calculate base position in threat direction
        Coords basePosition = centroid.translated(threatDirection, (int) Math.round(optimalRange));

        // 2. Calculate lateral offset (staggered line formation)
        int lateralDirection = (unitIndex % 2 == 0) ?
                (threatDirection + 2) % 6 : // Right flank
                (threatDirection + 4) % 6;  // Left flank

        // 3. Apply lateral offset based on unit index
        int lateralDistance = unitIndex + 2;
        return basePosition.translated(lateralDirection, lateralDistance);
    }

    private double calculateRangeQuality(Coords newPos, Coords threatPos, double optimalRange, double maxRange) {
        double distance = newPos.distance(threatPos);

        // Quadratic penalty outside optimal range
        if (distance > optimalRange) {
            double overRange = distance - optimalRange;
            return Math.max(0, 1 - Math.pow(overRange / (maxRange - optimalRange), 2));
        }
        // Bonus for being in optimal range
        return 1 + (1 - (distance / optimalRange));
    }

    private double calculateFormationQuality(Coords newPos, Coords idealPos) {
        double distance = newPos.distance(idealPos);
        return 1.0 / (1.0 + distance * 0.5); // Gentle falloff
    }

    private double calculateForwardBias(Coords currentPos, Coords newPos, Coords threatPos) {
        double currentDistance = currentPos.distance(threatPos);
        double newDistance = newPos.distance(threatPos);

        // Reward moving closer to threat, penalize retreating
        return clamp01(1.5 - (newDistance / currentDistance));
    }

    private double calculateBorderPenalty(Coords position) {
        int boardWidth = getOwner().getBoard().getWidth();
        int boardHeight = getOwner().getBoard().getHeight();
        int borderDistance = Math.min(
                position.getX(),
                Math.min(
                        position.getY(),
                        Math.min(
                                boardWidth - position.getX(),
                                boardHeight - position.getY()
                        )
                )
        );
        return borderDistance < 5 ? (5 - borderDistance) * 0.2 : 0;
    }
}
