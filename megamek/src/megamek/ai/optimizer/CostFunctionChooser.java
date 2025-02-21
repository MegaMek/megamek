/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 */
package megamek.ai.optimizer;

import megamek.ai.utility.*;
import megamek.client.bot.caspar.ai.utility.tw.Cluster;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public enum CostFunctionChooser {
    Princess,
    Utility,
    ExtendedUtility,
    NeuralNetwork,
    ExtendedNeuralNetwork;

    public static CostFunctionChooser fromString(String str) {
        return switch (str) {
            case "princess" -> Princess;
            case "utility" -> Utility;
            case "extendedUtility" -> ExtendedUtility;
            case "neuralNetwork" -> NeuralNetwork;
            default -> throw new IllegalArgumentException("Invalid cost function: " + str);
        };
    }

    public static String validCostFunctions() {
        return String.join(", ", Arrays.stream(values()).map(Enum::name).toArray(String[]::new));
    }

    public CostFunction createCostFunction(CardinalEdge edge, Board board, List<Consideration> considerations) {
        return switch (this) {
            case Princess -> new BasicPathRankerCostFunction(edge, board);
            case Utility -> new UtilityPathRankerCostFunction(edge, new UtilityPathRankerCostFunction.CostFunctionSwarmContext(), board);
            case ExtendedUtility -> new ExtendedCostFunction(new UtilityPathRankerCostFunction(edge,
                new UtilityPathRankerCostFunction.CostFunctionSwarmContext(), board));
            case NeuralNetwork -> new NeuralNetworkPathRankerCostFunction(
                NeuralNetworkFactory.createNeuralNetworkForConsiderationsAndThreatHeatmap(considerations.size(), 50),
                considerations,
                new StrategicGoalsManager().initializeStrategicGoals(board, 5, 5),
                BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR,
                board,
                new Game()
            );
            case ExtendedNeuralNetwork -> new ExtendedCostFunction(new NeuralNetworkPathRankerCostFunction(
                NeuralNetworkFactory.createNeuralNetworkForConsiderationsAndThreatHeatmap(considerations.size(), 50),
                considerations,
                new StrategicGoalsManager().initializeStrategicGoals(board, 5, 5),
                BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR,
                board,
                new Game()
            ));
        };
    }
}
