package megamek.utilities.ai;

import java.util.*;

public class ExtendedCostFunction implements CostFunction {

    private final CostFunction costFunction;

    public ExtendedCostFunction(CostFunction costFunction) {
        this.costFunction = costFunction;
        StateVisitRegistry.reset();
    }

    @Override
    public double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Map<Integer, UnitState> nextUnitState, BehaviorParameters behaviorParameters) {
        double baseCost = computeBaseCost(unitAction, currentUnitStates, behaviorParameters);

        double boardControlTerm  = computeBoardControlTerm(currentUnitStates.get(unitAction.id()), nextUnitState);
        double victoryTerm       = computeVictoryTerm(boardControlTerm);
        double resourceTerm      = computeResourceTerm(currentUnitStates, unitAction, nextUnitState);
        double curiosityTerm     = computeCuriosityTerm(nextUnitState.get(unitAction.id()));

        // Weight for primary objective (victory)
        double alpha = behaviorParameters.p23();
        // Weight for resource-related term
        double beta = behaviorParameters.p24();
        // Weight for board-control term
        double gamma = behaviorParameters.p25();
        // Weight for exploration/curiosity term
        double curiosityW = behaviorParameters.p26();

        double extendedCost = baseCost
            + alpha * victoryTerm
            + beta * resourceTerm
            + gamma * boardControlTerm
            + curiosityW * curiosityTerm;

        return extendedCost;
    }

    @Override
    public double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
       throw new RuntimeException("This method should not be called in this context");
    }

    private double computeBaseCost(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters) {
        return costFunction.resolve(unitAction, currentUnitStates, behaviorParameters);
    }

    private double computeVictoryTerm(double control) {
        if (control > 0.6) return 1.0;
        if (control < 0.4) return -1.0;
        return 0.0;
    }

    private double computeResourceTerm(Map<Integer, UnitState> currentUnitStates, UnitAction action, Map<Integer, UnitState> nextUnitState) {
        // e.g., measure how many resources were gained this step, or net resource difference
        UnitState currentState = currentUnitStates.get(action.id());
        return calculateInfluence(nextUnitState).get(currentState.playerId()) - calculateInfluence(currentUnitStates).get(currentState.playerId());
    }

    private Map<Integer, Integer> calculateInfluence(Map<Integer, UnitState> unitStateMap) {
        Map<Integer, Integer> pointPerPlayer = new HashMap<>();
        for (UnitState unitState : unitStateMap.values()) {
            int player = unitState.playerId();
            int points = pointPerPlayer.getOrDefault(player, 0);
            points += unitState.entity() != null ? unitState.entity().getInitialBV() : 0;
            pointPerPlayer.put(player, points);
        }
        return pointPerPlayer;
    }
    BitSet[] controlledTerritories = new BitSet[]{new BitSet(255), new BitSet(255), new BitSet(255), new BitSet(255)};
    int[] playerControllerTerritories = new int[4];
    private double computeBoardControlTerm(UnitState unitState, Map<Integer, UnitState> nextUnitState) {
        var player = unitState.playerId();
        controlledTerritories[0].clear();
        controlledTerritories[1].clear();
        controlledTerritories[2].clear();
        controlledTerritories[3].clear();
        for (UnitState state : nextUnitState.values()) {
            int playerKey = state.playerId();
            controlledTerritories[playerKey].set(Math.min(15, (state.position().getX() / 3)) << 4 | Math.min(15, (state.position().getY() / 3)));
        }
        int totalAreas = 0;
        for (int i = 0; i < controlledTerritories.length; i++) {
            totalAreas += controlledTerritories[i].cardinality();
        }
        double controlledPercentage = controlledTerritories[player].cardinality() / (double) totalAreas;
        return controlledPercentage;
    }

    private double computeCuriosityTerm(UnitState nextUnitState) {
        // e.g., measure if this is a rarely visited state. This might require
        // a global or static map to track visitation counts.
        int visits = StateVisitRegistry.getVisits(nextUnitState);
        StateVisitRegistry.incrementVisits(nextUnitState);

        // Simple scheme: return positive if first-time visit, else 0
        // or some decreasing function of visits
        return (visits == 0) ? 1.0 : 0.0;
    }

    public static class StateVisitRegistry {
        private static final Map<Integer, Integer> visitCounts = new HashMap<>();

        public static int getVisits(UnitState state) {
            int key = state.hashCode();
            return visitCounts.getOrDefault(key, 0);
        }

        public static void incrementVisits(UnitState state) {
            int key = state.hashCode();
            visitCounts.put(key, getVisits(state) + 1);
        }

        public static void reset() {
            visitCounts.clear();
        }
    }

}
