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

import java.util.*;

public class UtilityPathRanker extends BasicPathRanker {
    private final static MMLogger logger = MMLogger.create(BasicPathRanker.class);

    public UtilityPathRanker(Princess owningPrincess) {
        super(owningPrincess);
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
        getOwner().getSwarmContext().recordPlannedPosition(bestRankedPath.getPath().getFinalCoords());
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

        // I can kick a different target than I shoot, so add physical to
        // total damage after I've looked at all enemies

        double braveryMod = getBraveryMod(successProbability, damageEstimate, expectedDamageTaken);

        var isNotAirborne = !path.getEntity().isAirborneAeroOnGroundMap();
        // the only critters not subject to aggression and herding mods are
        // airborne aeros on ground maps, as they move incredibly fast
        // The further I am from a target, the lower this path ranks
        // (weighted by Aggression slider).
        double aggressionMod = isNotAirborne ?
            calculateAggressionMod(movingUnit, pathCopy, maxRange, game) : 1.0;
        // The further I am from my teammates, the lower this path
        // ranks (weighted by Herd Mentality).
        var formula = new StringBuilder(512);
        formula.append("Calculation: {");

        double fallMod = calculateFallMod(successProbability);

        double herdingMod = isNotAirborne ? calculateHerdingMod(friendsCoords, pathCopy, formula) : 1.0;

        double crowdingTolerance = calculateCrowdingTolerance(pathCopy, enemies, maxRange, formula);

        // Movement is good, it gives defense and extends a player power in the game.
        double movementMod = calculateMovementMod(pathCopy, game, enemies, formula);

        // Try to face the enemy.
        double facingMod = calculateFacingMod(movingUnit, game, pathCopy, formula);

        // If I need to flee the board, I want to get closer to my home edge.
        double selfPreservationMod = calculateSelfPreservationMod(movingUnit, pathCopy, game);
        // if we're an aircraft, we want to de-value paths that will force us off the
        // board
        // on the subsequent turn.
        // Include in utility calculation:
        double strategicMod = calculateStrategicGoalMod(pathCopy);
        double formationMod = calculateFormationModifier(path);
        double threatResponseMod = calculateThreatResponse(path, friendsCoords);
        var fallBackMod = executeThreatResponse(path, movingUnit, (Entity) findClosestEnemy(movingUnit, path.getFinalCoords(), game));

        double utility = fallMod;
        utility *= braveryMod;
        utility *= formationMod;
        utility *= threatResponseMod;
        utility *= fallBackMod;
        utility *= aggressionMod;
        utility *= herdingMod;
        utility *= movementMod;
        utility *= crowdingTolerance;
        utility *= facingMod;
        utility *= selfPreservationMod;
        utility *= strategicMod;
        double modificationFactor = 1.0 - (1.0 / 12.0);
        double makeUpValue = (1 - utility) * modificationFactor;
        double finalScore = utility + (makeUpValue * utility);

        utility = clamp01(finalScore);

        RankedPath rankedPath = new RankedPath(utility, pathCopy,
            "utility [" + utility + "= fallMod(" + fallMod + ") * braveryMod(" + braveryMod+ ") * formationMod("+ formationMod +
                ") * threatResponseMod(" + threatResponseMod + ") * fallBackMod("+ fallBackMod +") * aggressionMod(" + aggressionMod +
                ") * herdingMod(" + herdingMod +") * movementMod("+ movementMod +") * crowdingTolerance("+crowdingTolerance+
                ") * facingMod("+ facingMod +") * selfPreservationMod("+selfPreservationMod+") * strategicMod("+strategicMod+")]"
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

    private double calculateStrategicGoalMod(MovePath path) {
        if (UnitBehavior.BehaviorType.Engaged.equals(getOwner().getUnitBehaviorTracker().getBehaviorType(path.getEntity(), getOwner()))) {
            return 1.0;
        }

        double maxGoalUtility = 0.0;
        double halfQuadrantDiagonal = getOwner().getSwarmContext().getQuadrantDiagonal() / 2.0;
        for (Coords goal : getOwner().getSwarmContext().getStrategicGoalsOnCoordsQuadrant(path.getFinalCoords())) {
            double distance = path.getFinalCoords().distance(goal);
            double utility = (halfQuadrantDiagonal / (distance + 1.0)); // Higher utility closer to the goal
            maxGoalUtility = Math.max(maxGoalUtility, utility);
        }
        return clamp01(maxGoalUtility);
    }

    @Override
    protected double getBraveryMod(double successProbability, FiringPhysicalDamage damageEstimate, double expectedDamageTaken) {
        double maximumDamageDone = damageEstimate.getMaximumDamageEstimate();
        double braveryFactor = getOwner().getBehaviorSettings().getBraveryIndex() / 10.0;
        return clamp01(1.1 - braveryFactor + (successProbability * (maximumDamageDone / Math.max(1.0, expectedDamageTaken))) * braveryFactor);
    }

    @Override
    protected double calculateMovementMod(MovePath pathCopy, Game game, List<Entity> enemies, StringBuilder formula) {
        var tmmFactor = getOwner().getBehaviorSettings().getFavorHigherTMM() / 10.0;
        var tmm = Compute.getTargetMovementModifier(pathCopy.getHexesMoved(), pathCopy.isJumping(), pathCopy.isAirborne(), game);
        var tmmValue = MathUtility.clamp(tmm.getValue() / 8.0, 0.0, 1.0);
        return clamp01((1.1 - tmmFactor) + (tmmValue / 8.0) * tmmFactor);
    }

    protected double calculateHerdingMod(Coords friendsCoords, MovePath path, StringBuilder formula) {
        if (friendsCoords == null) {
            formula.append(" herdingMod [1.0 no friends]");
            logger.trace(" herdingMod [1.0 no friends]");
            return 1.0;
        }
        double herdingFactor = getOwner().getBehaviorSettings().getHerdMentalityIndex() / 10.0;
        double herdingDistance = 5 + (herdingFactor * 10);
        double finalDistance = getOwner().getFriendEntities().stream()
            .map(friend -> friend.getPosition().distance(path.getFinalCoords()))
            .filter(distance -> distance <= herdingDistance)
            .mapToDouble(distance -> distance)
            .average().orElse(0.0);

        double startingDistance = getOwner().getFriendEntities().stream()
            .map(friend -> friend.getPosition().distance(path.getEntity().getPosition()))
            .filter(distance -> distance <= herdingDistance)
            .mapToDouble(distance -> distance)
            .average().orElse(0.0);

        double desiredDistance = 3 + (1 - herdingFactor) * 3; // Desired distance between 3 and 6
        double deltaDistance = finalDistance - startingDistance;

        double distanceFactor = 1.0 - Math.min(Math.max((deltaDistance - desiredDistance) / desiredDistance, 0.0), 1.0);

        Coords finalPos = path.getFinalCoords();

        double densityMod = clamp01(1 - getOwner().getSwarmContext().getPositionDensity(finalPos, 3) / 4.0);
        double herdingMod = (1.1 - herdingFactor) + (herdingFactor * distanceFactor) * densityMod;

        formula.append(" * DensityMod(").append(densityMod).append(")");


        formula.append(" herdingMod [").append(LOG_DECIMAL.format(herdingMod)).append(" = (1.0 - ")
            .append(LOG_DECIMAL.format(herdingFactor)).append(") + (")
            .append(LOG_DECIMAL.format(herdingFactor)).append(" * ")
            .append(LOG_DECIMAL.format(distanceFactor)).append(")]");
        logger.trace("herding mod [{} = (1.0 - {}) + ({} * {})]", herdingMod, herdingFactor, herdingFactor, distanceFactor);
        return clamp01(herdingMod);
    }

    protected double calculateCrowdingTolerance(MovePath movePath, List<Entity> enemies, double maxRange, StringBuilder formula) {
        var self = movePath.getEntity();

        if (!(self instanceof Mek) && !(self instanceof Tank)) {

            return 1.0;
        }
        int antiCrowdingIndex = getOwner().getBehaviorSettings().getAntiCrowding();
        double factor = getOwner().getBehaviorSettings().getAntiCrowding() / 10.0;

        final double herdingDistance = Math.ceil(factor * 13) + 3;
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

        double crowdFactor = (crowdingFriends + crowdingEnemies) / (13.0 - antiCrowdingIndex);
        return clamp01((1.1 - factor) + (crowdFactor * factor));
    }

    protected double calculateFallMod(double successProbability) {
        double fallShameFactor = getOwner().getBehaviorSettings().getFallShameIndex() / 10.0;
        return clamp01((1 - fallShameFactor) + successProbability * fallShameFactor);
    }

    protected double calculateFacingMod(Entity movingUnit, Game game, final MovePath path, StringBuilder formula) {
        int facingDiff = getFacingDiff(movingUnit, game, path);
        double facingMod = (3 - facingDiff)/ 3.0;
        formula.append(" - facingMod [").append(facingMod).append(" = (3 - ").append(facingDiff).append(")/ 3.0]");
        logger.trace("facing mod [{} = (3 - {})/ 3.0]", facingMod, facingDiff);
        return clamp01(facingMod);
    }


    private static final double COVERAGE_RATIO = 0.6;

    public double calculateFormationModifier(MovePath path) {
        Coords newPosition = path.getFinalCoords();
        var allies = getOwner().getFriendEntities();

        // 1. Cover fire range check
        boolean inCoverRange = allies.stream()
            .anyMatch(a -> a.getPosition().distance(newPosition) <=
                a.getMaxWeaponRange() * COVERAGE_RATIO);

        // 2. Border avoidance
        double borderDistance = calculateBorderDistance(newPosition);

        // 3. Flanking positions
        double flankingBonus = calculateFlankingModifier(path);

        return (inCoverRange ? 1.2 : 0.8) *
            (1.0 + borderDistance * 0.1) *
            (1.0 + flankingBonus);
    }

    private double calculateFlankingModifier(MovePath path) {
        double maxBonus = 0.0;

        for (Entity enemy : getOwner().getEnemyTracker().getPriorityTargets(path.getFinalCoords())) {
            Coords enemyPos = enemy.getPosition();
            Coords idealFlank = calculateIdealFlankPosition(enemyPos, path.getFinalCoords());

            double distanceToIdeal = path.getFinalCoords().distance(idealFlank);
            double angleBonus = calculateAngleBonus(enemyPos, path.getFinalCoords());

            maxBonus = Math.max(maxBonus, (1.0 / (distanceToIdeal + 1)) * angleBonus);
        }

        return 1.0 + (maxBonus * 0.5);
    }

    private double calculateAngleBonus(Coords enemyPos, Coords finalCoords) {
        int angle = enemyPos.direction(finalCoords);
        return switch (angle) {
            case 0, 3 -> 1.0;
            case 1, 2, 4, 5 -> 0.5;
            default -> 0.0;
        };
    }


    private double calculateThreatResponse(MovePath path, Coords allyCenter) {
        var swarmCenter = allyCenter == null ? path.getFinalCoords() : allyCenter;
        List<Entity> priorityTargets = getOwner().getEnemyTracker().getPriorityTargets(swarmCenter);
        double threatModifier = 1.0;
        var self = path.getEntity();
        for (Entity threat : priorityTargets) {
            double distanceToThreat = path.getFinalCoords().distance(threat.getPosition());
            threatModifier *= switch (self.getRole()) {
                case MISSILE_BOAT, SNIPER, SCOUT -> 1.0 + (distanceToThreat * 0.05);
                case JUGGERNAUT, BRAWLER -> 1.0 + (1.0 / (distanceToThreat + 1));
                default -> 1.0;
            };
        }

        return threatModifier;
    }


    private double calculateBorderDistance(Coords position) {
        // Calculate distance to nearest map edge
        var mapHeight = getOwner().getBoard().getHeight();
        var mapWidth = getOwner().getBoard().getWidth();
        return Math.min(
            position.getX(),
            Math.min(
                position.getY(),
                Math.min(
                    mapWidth - position.getX(),
                    mapHeight - position.getY()
                )
            )
        );
    }

    // todo account for damaged locations and face those away from enemy.
    private int getFacingDiff(Entity movingUnit, Game game, final MovePath path) {
        Targetable closest = findClosestEnemy(movingUnit, movingUnit.getPosition(), game, false);
        Coords toFace = closest == null ? game.getBoard().getCenter() : closest.getPosition();
        int desiredFacing = (toFace.direction(movingUnit.getPosition()) + 3) % 6;
        int currentFacing = path.getFinalFacing();
        int facingDiff;

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


    private double executeThreatResponse(MovePath movePath, Entity unit, Entity threat) {
        if (shouldFallBack(movePath, unit, threat)) {
            // Find fallback position that maintains coverage
//            MovePath fallbackPath = findCoveredFallback(unit);

            // Update swarm context
//            getOwner().getSwarmContext().markFallbackPosition(fallbackPath.getFinalCoords());

            // Execute path
//            return performPathPostProcessing(fallbackPath);
            var selfPreservationFactor = getOwner().getBehaviorSettings().getSelfPreservationIndex() / 10.0;
            return 1.4 - selfPreservationFactor;
        }
        return 1.0;
    }

    private boolean shouldFallBack(MovePath movePath, Entity unit, Entity threat) {
        return !getOwner().getCoverageValidator().isPositionCovered(unit) && !getOwner().getCoverageValidator().validateUnitCoverage(unit, movePath.getFinalCoords());
    }

//    private MovePath findCoveredFallback(Entity unit) {
//        return getPossiblePaths(unit).stream()
//            .filter(path -> formationManager.isPositionCovered(path.getFinalCoords()))
//            .max(Comparator.comparingDouble(path ->
//                path.getFinalCoords().distance(getNearestThreat().getPosition())
//            ))
//            .orElseGet(() -> new MovePath(game, unit));
//    }

    private Coords calculateIdealFlankPosition(Coords enemyPos, Coords currentPos) {
        // Calculate position 90 degrees from swarm center
        int direction = currentPos.direction(enemyPos);
        return enemyPos.translated((direction + 4) % 6, 5); // Hex grid translation
    }

    static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }


}
