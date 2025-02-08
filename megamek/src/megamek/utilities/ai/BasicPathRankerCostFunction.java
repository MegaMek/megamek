/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */
package megamek.utilities.ai;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.CardinalEdge;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Coords;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Cost function for the basic path ranker. This function is used to evaluate the utility of a given action.
 * @param homeEdge The edge of the board that is considered the home edge
 * @param board The board that the action is being evaluated on
 * @author Luana Coppio
 */
public record BasicPathRankerCostFunction(CardinalEdge homeEdge, Board board) implements CostFunction {

    @Override
    public double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        BehaviorSettings behaviorSettings = behaviorSettingsFrom(behaviorParameters);
        double fallMod = calculateFallMod(unitAction, behaviorSettings);
        double braveryMod = getBraveryMod(unitAction, currentUnitStates, behaviorSettings);
        double aggressionMod = calculateAggressionMod(unitAction, currentUnitStates, behaviorSettings);
        double herdingMod = calculateHerdingMod(unitAction, currentUnitStates, behaviorSettings);
        double movementMod = calculateMovementMod(unitAction, currentUnitStates, behaviorSettings);
        double facingMod = calculateFacingMod(unitAction, currentUnitStates);
        double crowdingTolerance = calculateCrowdingTolerance(unitAction, currentUnitStates, behaviorSettings);
        double selfPreservationMod = calculateSelfPreservationMod(unitAction, currentUnitStates, behaviorSettings);
        double offBoardMod = calculateOffBoardMod(unitAction, currentUnitStates);

        double utility = -fallMod;
        utility += braveryMod;
        utility -= aggressionMod;
        utility -= herdingMod;
        utility += movementMod;
        utility -= crowdingTolerance;
        utility -= facingMod;
        utility -= selfPreservationMod;
        utility -= utility * offBoardMod;
        return utility;
    }

    public static BehaviorSettings behaviorSettingsFrom(BehaviorParameters behaviorParameters) {
        BehaviorSettings behaviorSettings = new BehaviorSettings();
        behaviorSettings.setBraveryIndex((int) Math.round(behaviorParameters.p1() * 10));
        behaviorSettings.setSelfPreservationIndex((int) Math.round(behaviorParameters.p2()* 10));
        behaviorSettings.setFallShameIndex((int) Math.round(behaviorParameters.p3()* 10));
        behaviorSettings.setFavorHigherTMM((int) Math.round(behaviorParameters.p4()* 10));
        behaviorSettings.setAntiCrowding((int) Math.round(behaviorParameters.p5()* 10));
        behaviorSettings.setHyperAggressionIndex((int) Math.round(behaviorParameters.p7()* 10));
        behaviorSettings.setHerdMentalityIndex((int) Math.round(behaviorParameters.p8()* 10));
        return behaviorSettings;
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

    private double calculateHerdingMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorSettings behaviorSetting) {
        if (alliedUnits(unitAction, currentUnitStates).size() == 1) {
            return 0;
        }

        double finalDistance = distanceToClosestNonAlliedUnit(unitAction, currentUnitStates);
        double herding = behaviorSetting.getHerdMentalityValue();
        return finalDistance * herding;
    }

    private double calculateAggressionMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorSettings behaviorSetting) {
        double distToEnemy = distanceToClosestNonAlliedUnit(unitAction, currentUnitStates);
        var self = currentUnitStates.get(unitAction.id()).type();
        boolean isInfantry = Objects.equals(self, "Infantry") || Objects.equals(self, "BattleAmor") || Objects.equals(self, "Mekwarrior")
            || Objects.equals(self, "EjectedCrew");

        if (distToEnemy == 0 && isInfantry) {
            distToEnemy = 2;
        }

        double aggression = behaviorSetting.getHyperAggressionValue();
        return distToEnemy * aggression;
    }

    private static double calculateFacingMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates) {
        int facingDiff = getFacingDiff(unitAction, currentUnitStates);
        return Math.max(0.0, 50 * (facingDiff - 1));
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
            facingDiff = 1;
        } else if ((currentFacing == ((desiredFacing + 2) % 6))
            || (currentFacing == ((desiredFacing + 4) % 6))) {
            facingDiff = 2;
        } else {
            facingDiff = 3;
        }
        return facingDiff;
    }

    private static boolean meksAndTanks(UnitState unitState) {
        return (Objects.equals(unitState.type(), "BipedMek") || Objects.equals(unitState.type(), "QuadMek")
            || Objects.equals(unitState.type(), "Tank") || Objects.equals(unitState.type(), "TripodMek"));
    }

    private static double calculateCrowdingTolerance(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorSettings behaviorSetting) {
        var self = currentUnitStates.get(unitAction.id());
        if (!(Objects.equals(self.type(), "BipedMek") || Objects.equals(self.type(), "QuadMek")
            || Objects.equals(self.type(), "Tank") || Objects.equals(self.type(), "TripodMek"))) {
            return 0.0;
        }
        var antiCrowding = behaviorSetting.getAntiCrowding();
        if (antiCrowding == 0) {
            return 0;
        }

        var antiCrowdingFactor = (10.0 / (11 - antiCrowding));
        final double herdingDistance = 2;
        final double closingDistance = Math.ceil(Math.max(3.0, 12 * 0.6));
        var position = unitAction.finalPosition();
        var crowdingFriends = alliedUnits(unitAction, currentUnitStates).stream()
            .filter(BasicPathRankerCostFunction::meksAndTanks)
            .map(UnitState::position)
            .filter(c -> c.distance(position) <= herdingDistance)
            .count();

        var crowdingEnemies = nonAlliedUnits(unitAction, currentUnitStates).stream()
            .map(UnitState::position)
            .filter(c -> c.distance(position) <= closingDistance)
            .count();
        double friendsCrowdingTolerance = antiCrowdingFactor * crowdingFriends;
        double enemiesCrowdingTolerance = antiCrowdingFactor * crowdingEnemies;
        return friendsCrowdingTolerance + enemiesCrowdingTolerance;
    }

    private static double calculateMovementMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorSettings behaviorSetting) {
        var favorHigherTMM = behaviorSetting.getFavorHigherTMM();
        boolean disabledFavorHigherTMM = favorHigherTMM == 0;
        if (!disabledFavorHigherTMM) {
            var tmm = Compute.getTargetMovementModifier(unitAction.hexesMoved(), unitAction.jumping(), false, null);
            double selfPreservation = behaviorSetting.getSelfPreservationValue();
            var tmmValue = tmm.getValue();
            return tmmValue * (selfPreservation + favorHigherTMM);
        }
        return 0.0;
    }

    private static double getBraveryMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorSettings behaviorSetting) {
        var closestEnemy = closestNonAlliedEntity(unitAction, currentUnitStates);
        var distanceToClosestEnemy = distanceToClosestNonAlliedUnit(unitAction, currentUnitStates);
        int damageTaken = getDamageAtRange(distanceToClosestEnemy, closestEnemy);
        int damageCaused = getDamageAtRange(distanceToClosestEnemy, currentUnitStates.get(unitAction.id()));
        double successProbability = 1d - unitAction.chanceOfFailure();
        return (successProbability * damageCaused * behaviorSetting.getBraveryValue()) - damageTaken;
    }

    private static double calculateFallMod(UnitAction unitAction, BehaviorSettings behaviorSettings) {
        double pilotingFailure = unitAction.chanceOfFailure();
        double fallShame = behaviorSettings.getBraveryValue();
        return pilotingFailure * (pilotingFailure == 1 ? -1000 : fallShame);
    }

    private double calculateSelfPreservationMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorSettings behaviorSetting) {
        if (currentUnitStates.get(unitAction.id()).crippled()) {
            int newDistanceToHome = distanceToHomeEdge(unitAction.finalPosition());
            double selfPreservation = behaviorSetting.getSelfPreservationValue();
            double selfPreservationMod;
            if (newDistanceToHome > 0) {
                selfPreservationMod = newDistanceToHome * selfPreservation;
            } else {
                selfPreservationMod = -250;
            }
            return selfPreservationMod;
        }
        return 0.0;
    }

    private int distanceToHomeEdge(Coords position) {
        return switch (homeEdge) {
            case SOUTH -> board.getHeight() - position.getY() - 1;
            case WEST -> position.getX();
            case EAST -> board.getWidth() - position.getX() - 1;
            default -> position.getY();
        };
    }

    private static double calculateOffBoardMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates) {
        if (currentUnitStates.get(unitAction.id()).offBoard()) {
            return 0.5;
        }
        return 0.0;
    }
}


