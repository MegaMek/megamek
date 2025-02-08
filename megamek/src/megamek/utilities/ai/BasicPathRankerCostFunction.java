package megamek.utilities.ai;

import megamek.client.bot.princess.CardinalEdge;
import megamek.common.Compute;
import megamek.common.Coords;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public record BasicPathRankerCostFunction(CardinalEdge homeEdge, int boardWidth, int boardHeight) implements CostFunction {

    @Override
    public double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        double fallMod = calculateFallMod(unitAction, currentUnitStates, behaviorParameters);
        double braveryMod = getBraveryMod(unitAction, currentUnitStates, behaviorParameters);
        double aggressionMod = calculateAggressionMod(unitAction, currentUnitStates, behaviorParameters);
        double herdingMod = calculateHerdingMod(unitAction, currentUnitStates, behaviorParameters);
        double movementMod = calculateMovementMod(unitAction, currentUnitStates, behaviorParameters);
        double facingMod = calculateFacingMod(unitAction, currentUnitStates, behaviorParameters);
        double crowdingTolerance = calculateCrowdingTolerance(unitAction, currentUnitStates, behaviorParameters);
        double selfPreservationMod = calculateSelfPreservationMod(unitAction, currentUnitStates, behaviorParameters);
        double offBoardMod = calculateOffBoardMod(unitAction, currentUnitStates, behaviorParameters);

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

    private double calculateHerdingMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        if (alliedUnits(unitAction, currentUnitStates).size() == 1) {
            return 0;
        }

        double finalDistance = distanceToClosestNonAlliedUnit(unitAction, currentUnitStates);
        double herding = behaviorParameters.p8();
        return finalDistance * herding;
    }

    private double calculateAggressionMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        double distToEnemy = distanceToClosestNonAlliedUnit(unitAction, currentUnitStates);
        var self = currentUnitStates.get(unitAction.id()).type();
        boolean isInfantry = Objects.equals(self, "Infantry") || Objects.equals(self, "BattleAmor") || Objects.equals(self, "Mekwarrior")
            || Objects.equals(self, "EjectedCrew");

        if (distToEnemy == 0 && isInfantry) {
            distToEnemy = 2;
        }

        double aggression = behaviorParameters.p7();
        return distToEnemy * aggression;
    }

    private static double calculateFacingMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        int facingDiff = getFacingDiff(unitAction, currentUnitStates);
        double facingMod = Math.max(0.0, 50 * (facingDiff - 1));
        return facingMod * behaviorParameters.p6();
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

    private static double calculateCrowdingTolerance(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        var self = currentUnitStates.get(unitAction.id());
        if (!(Objects.equals(self.type(), "BipedMek") || Objects.equals(self.type(), "QuadMek")
            || Objects.equals(self.type(), "Tank") || Objects.equals(self.type(), "TripodMek"))) {
            return 0.0;
        }
        var antiCrowding = behaviorParameters.p5();
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

    private static double calculateMovementMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        var favorHigherTMM = behaviorParameters.p4();
        boolean disabledFavorHigherTMM = favorHigherTMM == 0;
        if (!disabledFavorHigherTMM) {
            var tmm = Compute.getTargetMovementModifier(unitAction.hexesMoved(), unitAction.jumping(), false, null);
            double selfPreservation = behaviorParameters.p2();
            var tmmValue = tmm.getValue();
            return tmmValue * (selfPreservation + favorHigherTMM);
        }
        return 0.0;
    }

    private static double getBraveryMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        var closestEnemy = closestNonAlliedEntity(unitAction, currentUnitStates);
        var distanceToClosestEnemy = distanceToClosestNonAlliedUnit(unitAction, currentUnitStates);
        int damageTaken = getDamageAtRange(distanceToClosestEnemy, closestEnemy);
        int damageCaused = getDamageAtRange(distanceToClosestEnemy, currentUnitStates.get(unitAction.id()));
        double successProbability = 1d - unitAction.chanceOfFailure();
        return (successProbability * damageCaused * behaviorParameters.p1()) - damageTaken;
    }

    private static double calculateFallMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        double pilotingFailure = unitAction.chanceOfFailure();
        double fallShame = behaviorParameters.p3();
        return pilotingFailure * (pilotingFailure == 1 ? -1000 : fallShame);
    }

    private double calculateSelfPreservationMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        if (currentUnitStates.get(unitAction.id()).crippled()) {
            int newDistanceToHome = distanceToHomeEdge(unitAction.finalPosition());
            double selfPreservation = behaviorParameters.p2();
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
            case SOUTH -> boardHeight - position.getY() - 1;
            case WEST -> position.getX();
            case EAST -> boardWidth - position.getX() - 1;
            default -> position.getY();
        };
    }

    private static double calculateOffBoardMod(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        if (currentUnitStates.get(unitAction.id()).offBoard()) {
            return behaviorParameters.p9();
        }
        return 0.0;
    }
}


