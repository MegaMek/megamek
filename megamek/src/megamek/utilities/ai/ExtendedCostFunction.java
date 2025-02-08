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

import java.util.*;

/**
 * Experimental set of extra parameters for the cost function. Can be "added" to any other cost function.
 * @author Luana Coppio
 */
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

    /**
     * Compute the victory term based on the board control term. The team with the most BVs is considered "winning".
     * @param control The board control term.
     * @return The victory term.
     */
    private double computeVictoryTerm(double control) {
        if (control > 0.6) return 1.0;
        if (control < 0.4) return -1.0;
        return 0.0;
    }

    /**
     * Compute the resource term based on the current and next unit states.
     * @param currentUnitStates The current unit states.
     * @param action The action to take.
     * @param nextUnitState The next unit states.
     * @return The resource term.
     */
    private double computeResourceTerm(Map<Integer, UnitState> currentUnitStates, UnitAction action, Map<Integer, UnitState> nextUnitState) {
        // e.g., measure how many resources were gained this step, or net resource difference
        UnitState currentState = currentUnitStates.get(action.id());
        return calculateInfluence(nextUnitState).get(currentState.playerId()) - calculateInfluence(currentUnitStates).get(currentState.playerId());
    }

    /**
     * Calculate the influence of each player based on the unit states.
     * @param unitStateMap The unit states.
     * @return A map of player IDs to influence. Influence is the sum of the BVs of the units of a player.
     */
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

    // set of controlled territories for each player
    // each bit set represents a 3x3 area of the board
    private final BitSet[] controlledTerritories = new BitSet[]{new BitSet(65535), new BitSet(65535), new BitSet(65535), new BitSet(65535)};

    private double computeBoardControlTerm(UnitState unitState, Map<Integer, UnitState> nextUnitState) {
        var player = unitState.playerId();
        controlledTerritories[0].clear();
        controlledTerritories[1].clear();
        controlledTerritories[2].clear();
        controlledTerritories[3].clear();
        for (UnitState state : nextUnitState.values()) {
            int playerKey = state.playerId();
            controlledTerritories[playerKey].set(Math.min(255, (state.position().getX() / 3)) << 8 | Math.min(255, (state.position().getY() / 3)));
        }
        int totalAreas = 0;
        for (BitSet controlledTerritory : controlledTerritories) {
            totalAreas += controlledTerritory.cardinality();
        }
        double controlledPercentage = controlledTerritories[player].cardinality() / (double) totalAreas;
        return controlledPercentage;
    }

    private double computeCuriosityTerm(UnitState nextUnitState) {
        // e.g., measure if this is a rarely visited state. This might require
        // a global or static map to track visitation counts.
        int hashCode = nextUnitState.x() * 1000 + nextUnitState.y() + nextUnitState.playerId() * 100000;
        int visits = StateVisitRegistry.getVisits(hashCode);
        StateVisitRegistry.incrementVisits(hashCode);

        // Simple scheme: return positive if first-time visit, else 0
        // or some decreasing function of visits
        return (visits == 0) ? 1.0 : 0.0;
    }

    public static class StateVisitRegistry {
        private static final Map<Integer, Integer> visitCounts = new HashMap<>();

        public static int getVisits(int key) {
            return visitCounts.getOrDefault(key, 0);
        }

        public static void incrementVisits(int key) {
            visitCounts.put(key, getVisits(key) + 1);
        }

        public static void reset() {
            visitCounts.clear();
        }
    }

}
