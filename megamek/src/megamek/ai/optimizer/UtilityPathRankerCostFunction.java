/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.ai.optimizer;

import megamek.client.bot.princess.CardinalEdge;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.ai.dataset.UnitAction;
import megamek.ai.dataset.UnitState;

import java.util.*;
import java.util.stream.Collectors;


/**
 * This class represents a cost function for the utility path ranker. It serves as a kind of "sandbox" for
 * experimenting with new functions, parameters, etc.
 *
 * @param homeEdge The edge of the map where the home base is located.
 * @param swarmContext The context of the swarm.
 * @param board The game board.
 * @author Luana Coppio
 */
public record UtilityPathRankerCostFunction(CardinalEdge homeEdge, CostFunctionSwarmContext swarmContext, Board board) implements CostFunction {

    public UtilityPathRankerCostFunction {
        if (swarmContext == null) {
            swarmContext = new CostFunctionSwarmContext();
        }
        if (board == null) {
            throw new IllegalArgumentException("Board cannot be null");
        }
        initializeBoardTerrainLevels(board);
        swarmContext.initializeStrategicGoals(board, 5, 5);
    }

    private enum Parameter implements ModelParameter {
        AGGRESSION,
        FALL,
        SELF_PRESERVATION,
        BRAVERY,
        MOVEMENT,
        FACING,
        STRATEGIC_GOAL,
        FORMATION,
        LINE_FORMATION,
        EXPOSURE,
        HEALTH,
        SWARM,
        ENEMY_POSITION,
        EXPECTED_DAMAGE,
        COVER,
        ENVIRONMENT,
        CROWDING,
        SCOUTING_BONUS,
        AGGRESSION_BONUS,
        DEFENSIVE_BONUS,
        ANTI_CROWDING,
    }

    @Override
    public int numberOfParameters() {
        return Parameter.values().length;
    }

    @Override
    public double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        if (swarmContext.getClusters().isEmpty()) {
            swarmContext.assignClusters(currentUnitStates.values());
        }
        BehaviorState state = determineBehaviorState(unitAction, currentUnitStates);

        double aggressionMod = calculateAggressionMod(unitAction, currentUnitStates, parameters);
        double fallMod = calculateFallMod(unitAction, parameters);
        double bravery = getBraveryMod(unitAction, currentUnitStates, parameters);
        double movementMod = calculateMovementMod(unitAction, parameters);
        double facingMod = calculateFacingMod(unitAction, currentUnitStates, parameters);
        double selfPreservationMod = calculateSelfPreservationMod(unitAction, currentUnitStates, parameters);
        double strategicMod = calculateStrategicGoalMod(unitAction, parameters);
        double formationMod = calculateFormationModifier(unitAction, currentUnitStates, parameters);
        double exposurePenalty = calculateExposurePenalty(unitAction, currentUnitStates, parameters);
        double healthMod = calculateHealthMod(unitAction, parameters);
        double nearbyUnitsMod = calculateNearbyUnitsMod(unitAction, currentUnitStates, parameters);
        double swarmMod = calculateSwarmCohesionMod(unitAction, currentUnitStates, parameters);
        double enemyPosMod = calculateEnemyPositioningMod(unitAction, currentUnitStates, parameters);
        double damageMod = calculateExpectedDamage(unitAction, currentUnitStates, parameters);
        double advancedCoverMod = calculateAdvancedCoverage(unitAction, currentUnitStates, parameters);
        double environmentMod = calculateImmediateEnvironmentMod(unitAction, parameters);
        double antiCrowding = calculateCrowdingTolerance(unitAction, currentUnitStates, parameters);

        List<Double> factors = List.of(
            aggressionMod,
            fallMod,
            bravery,
            movementMod,
            facingMod,
            strategicMod,
            formationMod,
            exposurePenalty,
            healthMod,
            swarmMod,
            enemyPosMod,
            damageMod,
            advancedCoverMod,
            nearbyUnitsMod,
            selfPreservationMod,
            environmentMod,
            antiCrowding
        );

        double logSum = 0.0;
        int count = 0;
        for (double f : factors) {
            double safeFactor = Math.max(0.01, f);
            logSum += Math.log(safeFactor);
            count++;
        }
        double geometricMean = Math.exp(logSum / count);
        double geometricMeanAdjustedToBehavior = applyBehaviorState(state, geometricMean, unitAction, currentUnitStates, parameters);
        return clamp01(geometricMeanAdjustedToBehavior);
    }

    static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }

    private BehaviorState determineBehaviorState(UnitAction action, Map<Integer, UnitState> states) {
        UnitState unit = states.get(action.id());
        double health = (unit.armorP() + unit.internalP()) / 2;

        if (unit.role() == UnitRole.SCOUT) {
            return BehaviorState.SCOUTING;
        } else if (health > 0.7 && getDamageAtRange(5, unit) > 10) {
            return BehaviorState.AGGRESSIVE;
        } else {
            return BehaviorState.DEFENSIVE;
        }
    }
    private double calculateSwarmCohesionMod(UnitAction action, Map<Integer, UnitState> states,
                                             Parameters params) {
        CostFunctionSwarmContext.SwarmCluster cluster = swarmContext.getClusterFor(states.get(action.id()));
        Coords centroid = cluster.getCentroid();

        double distanceToCentroid = action.finalPosition().distance(centroid);
        double vectorAlignment = calculateVectorAlignment(
            new Coords(action.fromX(), action.fromY()),
            action.finalPosition(),
            centroid
        );

        return clamp01(clamp01(params.get(Parameter.SWARM) * (1 - distanceToCentroid/20) + params.get(Parameter.SWARM) * vectorAlignment) + 0.1);
    }

    private double calculateVectorAlignment(Coords from, Coords to, Coords target) {
        Coords movementVector = to.subtract(from);
        Coords targetVector = target.subtract(from);
        return movementVector.cosineSimilarity(targetVector);
    }

    private double calculateEnemyPositioningMod(UnitAction action, Map<Integer, UnitState> states,
                                                Parameters params) {
        int playerId = states.get(action.id()).playerId();
        List<UnitState> validEnemies = nonAlliedUnits(action, states).stream()
            .filter(e -> e.playerId() != playerId)
            .filter(e -> !e.type().equals("MekWarrior") && !e.type().equals("EjectedCrew"))
            .filter(e -> !e.crippled())
            .toList();

        Coords enemyMedian = calculateEnemyMedian(validEnemies);
        double enemyMedianDistance = action.finalPosition().distance(enemyMedian);

        List<Double> top5EnemyDistances = validEnemies.stream()
            .map(e -> (double)e.position().distance(action.finalPosition()))
            .sorted()
            .limit(5)
            .toList();

        double proximityThreat = top5EnemyDistances.stream()
            .mapToDouble(d -> 1/(d+1))
            .sum();

        return clamp01(params.get(Parameter.ENEMY_POSITION) * enemyMedianDistance/30 + params.get(Parameter.ENEMY_POSITION) * proximityThreat);
    }

    private Coords calculateEnemyMedian(List<UnitState> enemyStates) {
        if (enemyStates.isEmpty()) {
            return new Coords(0, 0);
        }
        double sumX = 0;
        double sumY = 0;
        for (UnitState enemy : enemyStates) {
            Coords pos = enemy.position();
            sumX += pos.getX();
            sumY += pos.getY();
        }
        int avgX = (int) Math.round(sumX / enemyStates.size());
        int avgY = (int) Math.round(sumY / enemyStates.size());
        return new Coords(avgX, avgY);
    }

    private double calculateTerrainCover(Coords position) {
        double coverScore = 0;
        int index = position.getY() * board.getWidth() + position.getX();
        if (woodedTerrain.get(index)) {
            for (Coords adj : position.allAdjacent()) {
                if (insideBoard(adj)) {
                    int adjIndex = adj.getY() * board.getWidth() + adj.getX();
                    if (woodedTerrain.get(adjIndex)) {
                        coverScore += 0.2;
                    }
                }
            }
        } else {
            // Open terrain may be less desirable.
            coverScore -= 0.05;
        }
        // Return an average bonus (or simply the sum—adjust as desired)
        return coverScore;
    }

    private double calculateExpectedDamage(UnitAction action, Map<Integer, UnitState> states,
                                           Parameters params) {
        Coords finalPos = action.finalPosition();
        double damageSum = nonAlliedUnits(action, states).stream()
            .filter(e -> e.position().distance(finalPos) <= e.maxRange())
            .mapToDouble(e -> getDamageAtRange(finalPos.distance(e.position()), e))
            .sum();

        return clamp01(1 - params.get(Parameter.EXPECTED_DAMAGE) * damageSum/100);
    }

    private double calculateAdvancedCoverage(UnitAction action, Map<Integer, UnitState> states,
                                             Parameters params) {
        long coveringAllies = alliedUnits(action, states).stream()
            .filter(a -> a.position().distance(action.finalPosition()) <= a.maxRange() * 0.6)
            .filter(a -> hasLineOfSight(a.position(), action.finalPosition()))
            .count();

        double baseCoverage = 0.8 + (coveringAllies * params.get(Parameter.COVER));
        double densityBonus = calculateCoverDensity(action.finalPosition(), 3);

        return clamp01(baseCoverage + params.get(Parameter.COVER) * densityBonus);
    }

    private double calculateImmediateEnvironmentMod(UnitAction action,
                                                    Parameters params) {
        double coverScore = 0;
        int currentHeight = boardTerrainLevels[action.toY() * board.getWidth() + action.toX()];
        List<Coords> hexesAround = action.finalPosition().allAdjacent();
        for (var targetPosition : hexesAround) {
            if (!insideBoard(targetPosition)) {
                continue;
            }

            coverScore += calculateHeightCover(action.finalPosition(), currentHeight);
            // Water depth analysis
            coverScore += getWaterCoverScore(targetPosition);

            // Surrounding terrain analysis
            coverScore += calculateTerrainCover(action.finalPosition());

            // Building cover
            coverScore += calculateBuildingCovert(targetPosition);
        }
        return 1 + coverScore * params.get(Parameter.ENVIRONMENT);
    }

    private double getWaterCoverScore(Coords targetPosition) {
        if (hasWaterLevel.get(targetPosition.getY() * board.getWidth() + targetPosition.getX())) {
            return 0.3;
        }
        return 0.0;
    }

    private double calculateBuildingCovert(Coords targetPosition) {
        if (buildings.get(targetPosition.getY() * board.getWidth() + targetPosition.getX())) {
            return 0.4;
        }
        return 0.0;
    }

    static int[] boardTerrainLevels;
    static BitSet hasWaterLevel;
    static BitSet woodedTerrain;
    static BitSet buildings;
    static BitSet clearTerrain;
    static BitSet hazardousTerrain;

    private boolean insideBoard(Coords position) {
        int idx = position.getY() * board.getWidth() + position.getX();
        return idx < boardTerrainLevels.length && idx >= 0;
    }

    private void initializeBoardTerrainLevels(Board board) {
        boardTerrainLevels = new int[board.getWidth() * board.getHeight()];
        hazardousTerrain = new BitSet(board.getWidth() * board.getHeight());
        woodedTerrain = new BitSet(board.getWidth() * board.getHeight());
        buildings = new BitSet(board.getWidth() * board.getHeight());
        clearTerrain = new BitSet(board.getWidth() * board.getHeight());
        hasWaterLevel = new BitSet(board.getWidth() * board.getHeight());
        int idx;
        int level;
        int temp;
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                idx = y * board.getWidth() + x;
                Hex hex = board.getHex(x, y);
                if (hex != null) {
                    level = hex.floor();
                    temp = hex.terrainLevel(Terrains.BUILDING);
                    if (temp != Terrain.LEVEL_NONE) {
                        level += temp;
                        buildings.set(idx);
                    }
                    if (hex.containsTerrain(Terrains.WOODS)) {
                        woodedTerrain.set(idx);
                    }
                    if (hex.containsAnyTerrainOf(Terrains.HAZARDS)) {
                        hazardousTerrain.set(idx);
                    }
                    if (hex.isClearHex()) {
                        clearTerrain.set(idx);
                    }
                    if (hex.hasDepth1WaterOrDeeper()) {
                        hasWaterLevel.set(idx);
                    }
                    boardTerrainLevels[idx] = level;
                }
            }
        }
    }

    private double calculateHeightCover(Coords position, int baseHeight) {
        return boardTerrainLevels[position.getY() * board.getWidth() + position.getX()] > baseHeight ? 0.2 : 0;
    }

    private double calculateCoverDensity(Coords position, int radius) {
        return (double) position.allAtDistanceOrLess(radius).stream()
            .filter(c -> bonusCover(position, c) > 0.0)
            .count() / (6.0 * radius * (radius + 1)/2.0);
    }

    private double applyBehaviorState(BehaviorState state, double baseUtility,
                                      UnitAction action, Map<Integer, UnitState> states,
                                      Parameters params) {
        return switch (state) {
            case SCOUTING -> baseUtility *
                (1 + params.get(Parameter.SCOUTING_BONUS) * calculateScoutingBonus(action, states));
            case AGGRESSIVE -> baseUtility *
                (1 + params.get(Parameter.AGGRESSION_BONUS) * calculateAggressiveBonus(action, states));
            case DEFENSIVE -> baseUtility *
                (1 + params.get(Parameter.DEFENSIVE_BONUS) * calculateDefensiveBonus(action));
        };
    }

    private double calculateDefensiveBonus(UnitAction action) {
        return calculateCoverDensity(action.finalPosition(), 2);
    }

    private double calculateFlankingPosition(int maxRange, Coords unitPos, List<Coords> strategicGoals) {
        if (strategicGoals.isEmpty()) {
            return 0;
        }
        double sumX = 0, sumY = 0;
        for (Coords goal : strategicGoals) {
            sumX += goal.getX();
            sumY += goal.getY();
        }
        int avgX = (int) Math.round(sumX / strategicGoals.size());
        int avgY = (int) Math.round(sumY / strategicGoals.size());
        Coords avgGoal = new Coords(avgX, avgY);
        double distance = unitPos.distance(avgGoal);
        return clamp01(distance / (maxRange + 1));
    }

    private double calculateScoutingBonus(UnitAction action, Map<Integer, UnitState> states) {
        UnitState unitState = states.get(action.id());
        return calculateFlankingPosition(unitState.maxRange(), action.finalPosition(),
            swarmContext.getStrategicGoalsOnCoordsQuadrant(action.finalPosition()));
    }

    private boolean hasLineOfSight(Coords from, Coords to) {
        List<Coords> line = getHexLine(from, to);
        int startingLevel = boardTerrainLevels[from.getY() * board.getWidth() + from.getX()];
        int numberOfWoods = 0;
        for (int i = 1; i < line.size() - 1; i++) {
            if (boardTerrainLevels[line.get(i).getY() * board.getWidth() + line.get(i).getX()] > startingLevel + 1) {
                return false;
            }
            numberOfWoods += woodedTerrain.get(line.get(i).getY() * board.getWidth() + line.get(i).getX()) ? 1: 0;
            if (numberOfWoods > 1) {
                return false;
            }
        }
        return true;
    }

    private List<Coords> getHexLine(Coords a, Coords b) {
        CubeCoords ac = a.toCube();
        CubeCoords bc = b.toCube();
        int N = a.distance(b);
        List<Coords> results = new ArrayList<>();
        for (int i = 0; i <= N; i++) {
            double t = (N == 0) ? 0.0 : (double) i / N;
            CubeCoords lerped = CubeCoords.lerp(ac, bc, t);
            CubeCoords rounded = lerped.roundToNearestHex();
            results.add(rounded.toOffset());
        }
        return results;
    }

    private double bonusCover(Coords position, Coords hexLocation) {
        double bonus = 0;
        // Add bonus if there is a building at the target position.
        if (woodedTerrain.get(position.getY() * board.getWidth() + position.getX())) {
            bonus += 0.05;
        }

        // Check the surrounding hexes for an elevation advantage.
        int baseLevel = boardTerrainLevels[position.getY() * board.getWidth() + position.getX()];
        for (Coords c : hexLocation.allAtDistanceOrLess(2)) {
            if (woodedTerrain.get(c.getY() * board.getWidth() + c.getX())) {
                bonus += 0.1;
            }
            if (boardTerrainLevels[c.getY() * board.getWidth() + c.getX()] > baseLevel + 1) {
                bonus += 0.5;
            }
        }

        return bonus;
    }

    private double calculateAggressiveBonus(UnitAction action, Map<Integer, UnitState> states) {
        double damagePotential = getDamageAtRange(
            distanceToClosestNonAlliedUnit(action, states),
            states.get(action.id())
        );
        double movementBonus = action.hexesMoved() > 5 ? 0.3 : 0;
        return damagePotential/100 + movementBonus;
    }

    private static int getDamageAtRange(int distance, UnitState unitState) {
        var entity = unitState.entity();
        if (entity == null) {
            return 0;
        }
        return entity.getWeaponList().stream()
            .filter(w -> w.getType().longRange >= distance)
            .mapToInt(w -> w.getType().getDamage(distance))
            .sum();
    }

    private static List<UnitState> alliedUnits(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates) {
        var playerId = currentUnitStates.get(unitAction.id()).playerId();
        return currentUnitStates.values().stream().filter(
            u -> u.playerId() == playerId
        ).toList();
    }

    private static List<UnitState> nonAlliedUnits(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates) {
        var playerId = currentUnitStates.get(unitAction.id()).playerId();
        return currentUnitStates.values().stream().filter(
            u -> u.playerId() != playerId
        ).toList();
    }

    private static int distanceToClosestNonAlliedUnit(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates) {
        var position = unitAction.finalPosition();
        return nonAlliedUnits(unitAction, currentUnitStates).stream()
            .map(UnitState::position)
            .mapToInt(c -> c.distance(position))
            .min().orElse(0);
    }

    private static Coords closestNonAlliedUnit(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates) {
        var position = unitAction.finalPosition();
        return nonAlliedUnits(unitAction, currentUnitStates).stream()
            .map(UnitState::position)
            .min(Comparator.comparingInt(c -> c.distance(position)))
            .orElse(null);
    }

    private static UnitState closestNonAlliedEntity(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates) {
        var position = unitAction.finalPosition();
        return nonAlliedUnits(unitAction, currentUnitStates).stream()
            .min(Comparator.comparingInt(u -> u.position().distance(position)))
            .orElse(null);
    }

    private double calculateAggressionMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        double distToEnemy = distanceToClosestNonAlliedUnit(unitAction, currentUnitStates);
        var self = currentUnitStates.get(unitAction.id()).type();
        boolean isInfantry = Objects.equals(self, "Infantry") || Objects.equals(self, "BattleAmor") || Objects.equals(self, "Mekwarrior")
            || Objects.equals(self, "EjectedCrew");

        if (distToEnemy == 0 && isInfantry) {
            distToEnemy = 2;
        }

        int maxRange = Math.max(1, currentUnitStates.get(unitAction.id()).maxRange());

        double weight = parameters.get(Parameter.AGGRESSION);
        double aggression = clamp01(maxRange / distToEnemy);
        return clamp01(1.1 - weight + aggression * weight);
    }

    private static double calculateFacingMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        int facingDiff = getFacingDiff(unitAction, currentUnitStates);
        return 1 - parameters.get(Parameter.FACING) + ((facingDiff * parameters.get(Parameter.FACING)) / (1 + parameters.get(Parameter.FACING)));
    }

    private static int getFacingDiff(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates) {
        Coords closest = closestNonAlliedUnit(unitAction, currentUnitStates);
        int desiredFacing = (closest.direction(unitAction.finalPosition()) + 3) % 6;
        int currentFacing = unitAction.facing();
        int facingDiff;
        if (currentFacing == desiredFacing) {
            facingDiff = 0;
        } else if ((currentFacing == ((desiredFacing + 1) % 6))
            || (currentFacing == ((desiredFacing + 5) % 6))) {
            facingDiff = 0;
        } else if ((currentFacing == ((desiredFacing + 2) % 6))
            || (currentFacing == ((desiredFacing + 4) % 6))) {
            facingDiff = 1;
        } else {
            facingDiff = 2;
        }
        return facingDiff;
    }

    private static boolean meksAndTanks(UnitState unitState) {
        return (Objects.equals(unitState.type(), "BipedMek") || Objects.equals(unitState.type(), "QuadMek")
            || Objects.equals(unitState.type(), "Tank") || Objects.equals(unitState.type(), "TripodMek"));
    }

    private static double calculateCrowdingTolerance(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        var self = currentUnitStates.get(unitAction.id());
        if (!(Objects.equals(self.type(), "BipedMek") || Objects.equals(self.type(), "QuadMek")
            || Objects.equals(self.type(), "Tank") || Objects.equals(self.type(), "TripodMek"))) {
            return 1.0;
        }

        var antiCrowding = parameters.get(Parameter.CROWDING);

        final double herdingDistance = 2;
        final double closingDistance = Math.ceil(Math.max(3.0, 12 * 0.6));
        var position = unitAction.finalPosition();
        var crowdingFriends = alliedUnits(unitAction, currentUnitStates).stream()
            .filter(UtilityPathRankerCostFunction::meksAndTanks)
            .map(UnitState::position)
            .filter(c -> c.distance(position) <= herdingDistance)
            .count();

        var crowdingEnemies = nonAlliedUnits(unitAction, currentUnitStates).stream()
            .map(UnitState::position)
            .filter(c -> c.distance(position) <= closingDistance)
            .count();
        double friendsCrowdingTolerance = antiCrowding * crowdingFriends;
        double enemiesCrowdingTolerance = antiCrowding * crowdingEnemies;
        return friendsCrowdingTolerance + enemiesCrowdingTolerance;
    }

    // P4
    private static double calculateMovementMod(UnitAction unitAction, Parameters parameters) {
        var tmmFactor = parameters.get(Parameter.MOVEMENT);
        var tmm = Compute.getTargetMovementModifier(unitAction.hexesMoved(), unitAction.jumping(), false, null);
        var tmmValue = clamp01(tmm.getValue() / 8.0);
        return tmmValue * tmmFactor;
    }

    // P3
    private static double getBraveryMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        var closestEnemy = closestNonAlliedEntity(unitAction, currentUnitStates);
        var distanceToClosestEnemy = distanceToClosestNonAlliedUnit(unitAction, currentUnitStates);
        int damageTaken = getDamageAtRange(distanceToClosestEnemy, closestEnemy);
        int damageCaused = getDamageAtRange(distanceToClosestEnemy, currentUnitStates.get(unitAction.id()));
        double successProbability = 1d - unitAction.chanceOfFailure();
        return clamp01(0.1 + clamp01((successProbability * damageCaused * parameters.get(Parameter.BRAVERY)) - damageTaken));
    }

    // P2
    private static double calculateFallMod(UnitAction unitAction, Parameters parameters) {
        double pilotingFailure = unitAction.chanceOfFailure();
        double fallShameFactor = parameters.get(Parameter.FALL);
        return clamp01((1 - fallShameFactor) + (1 - pilotingFailure) * fallShameFactor);
    }

    private static double calculateNearbyUnitsMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        double weight = parameters.get(Parameter.SWARM);
        double distance = distanceToClosestNonAlliedUnit(unitAction, currentUnitStates);
        return clamp01(1.1 - weight + (1.0 / (distance + 1)) * weight);
    }

    private static double calculateHealthMod(UnitAction unitAction, Parameters parameters) {
        double weight = parameters.get(Parameter.HEALTH);
        double health = (unitAction.armorP() + unitAction.internalP()) / 2;
        if (health < 0.7) {
            weight *= 1.5; // More cautious when damaged
        }
        return health * weight;
    }

    private double calculateSelfPreservationMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        double weight = parameters.get(Parameter.SELF_PRESERVATION);
        double health = (unitAction.armorP() + unitAction.internalP()) / 2;
        if (health < 0.7) {
            weight *= 1.5; // More cautious when damaged
        }
        if (currentUnitStates.get(unitAction.id()).crippled()) {
            int newDistanceToHome = distanceToHomeEdge(unitAction.finalPosition());
            int currentDistanceToHome = distanceToHomeEdge(new Coords(unitAction.fromX(), unitAction.fromY()));

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
        return clamp01(health * weight);
    }

    private int distanceToHomeEdge(Coords position) {
        return switch (homeEdge) {
            case SOUTH -> board.getHeight() - position.getY() - 1;
            case WEST -> position.getX();
            case EAST -> board.getWidth() - position.getX() - 1;
            default -> position.getY();
        };
    }

    private double calculateStrategicGoalMod(UnitAction unitAction, Parameters parameters) {
        // Existing strategic goal calculation
        double maxGoalUtility = 0.0;
        for (Coords goal : swarmContext.getStrategicGoalsOnCoordsQuadrant(unitAction.finalPosition())) {
            double distance = unitAction.finalPosition().distance(goal);
            double utility = (10.0 / (distance + 1.0));
            maxGoalUtility = Math.max(maxGoalUtility, utility);
        }
        if (maxGoalUtility == 0.0) {
            return 1.0;
        }
        return clamp01(maxGoalUtility * parameters.get(Parameter.STRATEGIC_GOAL));
    }

    private double calculateFormationModifier(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        if (currentUnitStates.get(unitAction.id()) != null) {
            CostFunctionSwarmContext.SwarmCluster cluster = swarmContext.getClusterFor(currentUnitStates.get(unitAction.id()));

            double lineMod = calculateLineFormationMod(cluster, unitAction, currentUnitStates, parameters);
            double spacingMod = calculateOptimalSpacingMod(cluster, unitAction, parameters);
            double coverageMod = calculateCoverageModifier(unitAction, currentUnitStates, parameters);

            return clamp01(lineMod * coverageMod * spacingMod  * parameters.get(Parameter.FORMATION));
        } else {
            return 1.0;
        }
    }

    private double calculateCoverageModifier(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        long coveringAllies = alliedUnits(unitAction, currentUnitStates).stream()
            .filter(a -> a.position().distance(unitAction.finalPosition()) <=
                a.maxRange() * 0.6)
            .count();

        return 0.8 + (coveringAllies * parameters.get(Parameter.COVER));
    }

    private double calculateOptimalSpacingMod(CostFunctionSwarmContext.SwarmCluster cluster, UnitAction unitAction, Parameters parameters) {
        double avgDistance = cluster.getMembers().stream()
            .filter(m -> m.id() != unitAction.id())
            .mapToDouble(m -> m.position().distance(unitAction.finalPosition()))
            .average()
            .orElse(0);

        // Ideal spacing between 3-5 hexes
        if (avgDistance < 3) return 0.8 * parameters.get(Parameter.ANTI_CROWDING);
        if (avgDistance > 5) return 0.9 * parameters.get(Parameter.ANTI_CROWDING);
        return parameters.get(Parameter.ANTI_CROWDING);
    }


    private double calculateLineFormationMod(CostFunctionSwarmContext.SwarmCluster cluster, UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        UnitState unit = currentUnitStates.get(unitAction.id());
        if (unit.entity() == null) {
            return 1.0;
        }

        Coords currentPos = new Coords(unitAction.fromX(), unitAction.fromY());
        Coords newPos = unitAction.finalPosition();
        double optimalRange = currentUnitStates.get(unitAction.id()) != null ? currentUnitStates.get(unitAction.id()).maxRange() * 0.6 : 0.0;

        Coords primaryThreat = calculatePrimaryThreatPosition(unitAction, currentUnitStates);
        int threatDirection = cluster.getCentroid().direction(primaryThreat);

        Coords idealPosition = calculateLinePosition(
            cluster.getCentroid(),
            threatDirection,
            cluster.getMembers().indexOf(currentUnitStates.get(unitAction.id())),
            optimalRange
        );

        double formationQuality = unit.role().equals(UnitRole.AMBUSHER) ||
            unit.role().equals(UnitRole.STRIKER) ||
            unit.role().equals(UnitRole.SCOUT)
            ? calculateOrbitModifier(unitAction, currentUnitStates, primaryThreat)
            : calculateFormationQuality(newPos, idealPosition);

        double rangeQuality = calculateRangeQuality(newPos, primaryThreat, optimalRange, unit.maxRange());
        double forwardBias = calculateForwardBias(currentPos, newPos, primaryThreat);
        double borderPenalty = calculateBorderPenalty(newPos);

        double positionQuality = ((rangeQuality * 0.5) +
            (formationQuality * 0.3) +
            (forwardBias * 0.2) -
            borderPenalty) * parameters.get(Parameter.LINE_FORMATION);

        return clamp01(positionQuality);
    }

    private Coords calculateLinePosition(Coords centroid, int threatDirection, int unitIndex, double optimalRange) {
        Coords basePosition = centroid.translated(threatDirection, (int) Math.round(optimalRange));

        int lateralDirection = (unitIndex % 2 == 0) ?
            (threatDirection + 2) % 6 :
            (threatDirection + 4) % 6;

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
        int boardWidth = board.getWidth();
        int boardHeight = board.getHeight();
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

    private Coords calculatePrimaryThreatPosition(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates) {
        var position = unitAction.finalPosition();
        var coords = nonAlliedUnits(unitAction, currentUnitStates).stream()
            .map(UnitState::position)
            .sorted(Comparator.comparingInt(c -> c.distance(position)))
            .limit(3)
            .toList();

        return Coords.average(coords);
    }

    private static final double ORBIT_RANGE_VARIANCE = 0.2; // 20% range flexibility

    private double calculateOrbitModifier(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Coords threatPos) {

        Coords currentPos = unitAction.currentPosition();
        Coords newPos = unitAction.finalPosition();


        double optimalRange = currentUnitStates.get(unitAction.id()).maxRange() * 0.6;
        int orbitDirection = calculateOrbitDirection(unitAction, threatPos);

        Coords orbitPos = calculateOrbitPosition(currentPos, threatPos, optimalRange, orbitDirection);
        double currentDistanceFromPosition = Math.max(1d, orbitPos.distance(newPos));
        double distanceQuality = 1.0 - Math.abs(newPos.distance(threatPos) - optimalRange)/(optimalRange * ORBIT_RANGE_VARIANCE) / currentDistanceFromPosition;
        double directionQuality = calculateOrbitDirectionQuality(currentPos, newPos, orbitDirection);
        double movementBonus = unitAction.hexesMoved() > 0 ? 1.1 : 0.9;

        return clamp01((distanceQuality * 0.6) + (directionQuality * 0.4)) * movementBonus;
    }

    private int calculateOrbitDirection(UnitAction unitAction, Coords threatPos) {
        // Alternate direction based on unit ID to create swirling pattern
        return (unitAction.id() % 2 == 0) ?
            (threatPos.direction(unitAction.finalPosition()) + 1) % 6 : // Clockwise
            (threatPos.direction(unitAction.finalPosition()) + 5) % 6;  // Counter-clockwise
    }

    private Coords calculateOrbitPosition(Coords currentPos, Coords threatPos, double targetRange, int orbitDirection) {
        double currentDistance = currentPos.distance(threatPos);
        Coords idealPos = threatPos.translated(orbitDirection, (int) Math.round(targetRange));

        if(currentDistance < targetRange * 0.8) {
            return idealPos.translated(orbitDirection, 1);
        }

        return idealPos.translated(
            (orbitDirection + 3) % 6, // Opposite direction
            (int) Math.round((currentDistance - targetRange)/2)
        );
    }

    private double calculateOrbitDirectionQuality(Coords oldPos, Coords newPos, int desiredDirection) {
        int actualDirection = oldPos.direction(newPos);
        int directionDiff = Math.min(
            Math.abs(desiredDirection - actualDirection),
            6 - Math.abs(desiredDirection - actualDirection)
        );

        // Quadratic penalty for wrong direction
        return 1.0 - (directionDiff * directionDiff * 0.05);
    }

    private double calculateExposurePenalty(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        UnitState unit = currentUnitStates.get(unitAction.id());
        if (unit.role() == UnitRole.AMBUSHER || unit.role() == UnitRole.SCOUT || unit.role() == UnitRole.MISSILE_BOAT || unit.role() == UnitRole.SNIPER) { // SCOUT/FLANKER
            long threateningEnemies = nonAlliedUnits(unitAction, currentUnitStates).stream()
                .filter(e -> e.maxRange() <= unitAction.finalPosition().distance(e.position()))
                .count();

            if (validateUnitCoverage(unitAction, currentUnitStates)) {
                double exposureScore = 1 + (threateningEnemies * 0.3);
                return 1.0 / exposureScore  * parameters.get(Parameter.EXPOSURE);
            } else {
                double exposureScore = 1 + (threateningEnemies * 0.5);
                return 1.0 / exposureScore * parameters.get(Parameter.EXPOSURE);
            }
        }
        return parameters.get(Parameter.EXPOSURE);
    }

    public boolean validateUnitCoverage(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates) {
        // Check 0.6x coverage from at least one ally
        return alliedUnits(unitAction, currentUnitStates).stream()
            .filter(e -> e.id() != unitAction.id())
            .filter(ally -> ally.position().distance(unitAction.finalPosition()) <= ally.maxRange() * 0.6)
            .count() >= 2;
    }

    public static class CostFunctionSwarmContext {

        private final Map<Integer, Integer> enemyTargetCounts = new HashMap<>();
        private final List<Coords> strategicGoals = new Vector<>();
        private Coords currentCenter;

        private int quadrantHeight = 0;
        private int quadrantWidth = 0;
        private int offsetX = 0;
        private int offsetY = 0;

        private int clusterUnitsSize = 0;

        private final Map<Integer, SwarmCluster> unitClusters = new HashMap<>();
        private final List<SwarmCluster> clusters = new ArrayList<>();

        public CostFunctionSwarmContext() {
        }

        /**
         * Record an enemy target, incrementing the number of units targeting the enemy this turn
         * @param enemyId The enemy id
         */
        @SuppressWarnings("unused")
        public void recordEnemyTarget(int enemyId) {
            enemyTargetCounts.put(enemyId, enemyTargetCounts.getOrDefault(enemyId, 0) + 1);
        }

        /**
         * Get the number of times an enemy has been targeted
         * @param enemyId The enemy id
         * @return The number of times the enemy has been targeted
         */
        @SuppressWarnings("unused")
        public int getEnemyTargetCount(int enemyId) {
            return enemyTargetCounts.getOrDefault(enemyId, 0);
        }

        /**
         * Reset the enemy target counts
         */
        @SuppressWarnings("unused")
        public void resetEnemyTargets() {
            enemyTargetCounts.clear();
        }

        /**
         * Add a strategic goal to the list of goals, a strategic goal is simply a coordinate which we want to move towards,
         * it's mainly used for double-blind games where we don't know the enemy positions, the strategic goals help
         * distribute the map evenly accross the units inside the swarm to cover more ground and find the enemy faster
         * @param coords  The coordinates to add
         */
        public void addStrategicGoal(Coords coords) {
            strategicGoals.add(coords);
        }

        /**
         * Remove a strategic goal from the list of goals
         * @param coords The coordinates to remove
         */
        @SuppressWarnings("unused")
        public void removeStrategicGoal(Coords coords) {
            strategicGoals.remove(coords);
        }

        /**
         * Remove strategic goals in a radius around the given coordinates
         * @param coords The center coordinates
         * @param radius The radius to remove goals
         */
        @SuppressWarnings("unused")
        public void removeStrategicGoal(Coords coords, int radius) {
            for (var c : coords.allAtDistanceOrLess(radius)) {
                strategicGoals.remove(c);
            }
        }

        /**
         * Get the strategic goals on the quadrant of the given coordinates
         * @param coords The coordinates to check
         * @return A list of strategic goals on the quadrant
         */
        public List<Coords> getStrategicGoalsOnCoordsQuadrant(Coords coords) {
            QuadrantParameters quadrant = getQuadrantParameters(coords);
            Coords coord;
            List<Coords> goals = new Vector<>();
            for (int i = quadrant.startX(); i < quadrant.endX(); i++) {
                for (int j = quadrant.startY(); j < quadrant.endY(); j++) {
                    coord = new Coords(i, j);
                    if (strategicGoals.contains(coord)) {
                        goals.add(coord);
                    }
                }
            }
            return goals;
        }

        private QuadrantParameters getQuadrantParameters(Coords coords) {
            int x = coords.getX();
            int y = coords.getY();
            int startX = offsetX + (x / quadrantWidth) * quadrantWidth;
            int startY = offsetY + (y / quadrantHeight) * quadrantHeight;
            int endX = startX + quadrantWidth;
            int endY = startY + quadrantHeight;
            return new QuadrantParameters(startX, startY, endX, endY);
        }

        /**
         * Set the current center of the swarm
         * @param adjustedCenter The new center
         */
        @SuppressWarnings("unused")
        public void setCurrentCenter(Coords adjustedCenter) {
            this.currentCenter = adjustedCenter;
        }

        /**
         * Get the current center of the swarm
         * @return The current center
         */
        @SuppressWarnings("unused")
        public @Nullable Coords getCurrentCenter() {
            return currentCenter;
        }

        public List<SwarmCluster> getClusters() {
            return clusters;
        }


        private record QuadrantParameters(int startX, int startY, int endX, int endY) {
        }

        /**
         * Remove all strategic goals on the quadrant of the given coordinates
         * @param coords The coordinates to check
         */
        @SuppressWarnings("unused")
        public void removeAllStrategicGoalsOnCoordsQuadrant(Coords coords) {
            QuadrantParameters quadrant = getQuadrantParameters(coords);
            for (int i = quadrant.startX(); i < quadrant.endX(); i++) {
                for (int j = quadrant.startY(); j < quadrant.endY(); j++) {
                    strategicGoals.remove(new Coords(i, j));
                }
            }
        }

        /**
         * Initialize the strategic goals for the board
         * @param board The board to initialize the goals on
         * @param quadrantWidth The width of the quadrants
         * @param quadrantHeight The height of the quadrants
         */
        public void initializeStrategicGoals(Board board, int quadrantWidth, int quadrantHeight) {
            strategicGoals.clear();
            this.quadrantWidth = quadrantWidth;
            this.quadrantHeight = quadrantHeight;

            int boardWidth = board.getWidth();
            int boardHeight = board.getHeight();

            // Calculate extra space and offsets to center the quadrants
            int extraX = boardWidth % quadrantWidth;
            int extraY = boardHeight % quadrantHeight;
            offsetX = extraX / 2;
            offsetY = extraY / 2;

            // Iterate over each quadrant using the offsets
            for (int i = 0; i < (boardWidth - offsetX); i += quadrantWidth) {
                for (int j = 0; j < (boardHeight - offsetY); j += quadrantHeight) {
                    int startX = offsetX + i;
                    int startY = offsetY + j;
                    int endX = Math.min(startX + quadrantWidth, boardWidth);
                    int endY = Math.min(startY + quadrantHeight, boardHeight);

                    var xMidPoint = (startX + endX) / 2;
                    var yMidPoint = (startY + endY) / 2;
                    for (var coords : new Coords(xMidPoint, yMidPoint).allAtDistanceOrLess(3)) {
                        var hex = board.getHex(coords);
                        if (hex == null || hex.isClearHex() && hasNoHazards(hex)) {
                            addStrategicGoal(coords);
                            break;
                        }
                    }
                }
            }
        }

        private static final Set<Integer> HAZARDS = new HashSet<>(Arrays.asList(Terrains.FIRE,
            Terrains.MAGMA,
            Terrains.ICE,
            Terrains.WATER,
            Terrains.BUILDING,
            Terrains.BRIDGE,
            Terrains.BLACK_ICE,
            Terrains.SNOW,
            Terrains.SWAMP,
            Terrains.MUD,
            Terrains.TUNDRA));

        private boolean hasNoHazards(Hex hex) {
            var hazards = hex.getTerrainTypesSet();
            // Black Ice can appear if the conditions are favorable
            hazards.retainAll(HAZARDS);
            return hazards.isEmpty();
        }

        /**
         * Get the cluster for a unit
         * @param unit The unit to get the cluster for
         * @return The cluster for the unit, initializes a new cluster if it doesn't exist
         */
        public SwarmCluster getClusterFor(UnitState unit) {
            var cluster = unitClusters.get(unit.id());
            if (cluster == null) {
                cluster = new SwarmCluster();
                unitClusters.put(unit.id(), cluster);
                cluster.addMember(unit);
                clusterUnitsSize++;
            }
            return cluster;
        }

        public static class SwarmCluster {
            List<UnitState> members = new ArrayList<>();
            Coords centroid;
            int maxSize = 6;

            public List<UnitState> getMembers() {
                return members;
            }

            public Coords getCentroid() {
                return centroid;
            }

            public void addMember(UnitState unit) {
                if (members.size() >= maxSize) return;
                members.add(unit);
                updateCentroid();
            }

            private void updateCentroid() {
                if (members.isEmpty()) return;
                centroid = calculateClusterCentroid(members);
            }

            private Coords calculateClusterCentroid(List<UnitState> members) {
                double count = 0;
                double qSum = 0;
                double rSum = 0;
                double sSum = 0;

                for (UnitState unit : members) {
                    CubeCoords cube = unit.position().toCube();
                    qSum += cube.q;
                    rSum += cube.r;
                    sSum += cube.s;
                    count ++;
                }

                CubeCoords weightedCube = new CubeCoords(
                    qSum / count,
                    rSum / count,
                    sSum / count
                );

                return weightedCube.roundToNearestHex().toOffset();

            }
        }

        private int calculateOptimalClusterSize(int totalUnits) {
            if (totalUnits <= 4) return 4;
            if (totalUnits % 4 == 0) return 4;
            if (totalUnits % 5 == 0) return 5;
            return (totalUnits % 4) > (totalUnits % 5) ? 5 : 4;
        }

        // Cluster management methods

        /**
         * Assign units to clusters
         * @param allUnits The units to assign
         */
        public void assignClusters(Collection<UnitState> allUnits) {
            if (clusterUnitsSize == allUnits.size()){
                clusters.forEach(SwarmCluster::updateCentroid);
                return;
            }
            clusterUnitsSize = 0;
            clusters.clear();
            unitClusters.clear();
            int optimalSize = calculateOptimalClusterSize(allUnits.size());

            // Sort units by role for better distribution
            List<UnitState> sortedUnits = allUnits.stream()
                .sorted(Comparator.comparingInt(u -> u.role().ordinal()))
                .collect(Collectors.toList());

            // Create initial clusters
            while (!sortedUnits.isEmpty()) {
                SwarmCluster cluster = new SwarmCluster();
                for (int i = 0; i < optimalSize && !sortedUnits.isEmpty(); i++) {
                    var unit = sortedUnits.get(0);
                    unitClusters.put(unit.id(), cluster);
                    cluster.addMember(sortedUnits.remove(0));
                    clusterUnitsSize++;
                }
                clusters.add(cluster);
            }
        }
    }
}
