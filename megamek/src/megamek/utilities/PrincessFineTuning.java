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
package megamek.utilities;

import megamek.client.bot.princess.CardinalEdge;
import megamek.common.Board;
import megamek.common.MapSettings;
import megamek.common.util.BoardUtilities;
import megamek.utilities.ai.*;

import java.io.File;

/**
 * This class is used to fine-tune the parameters of the Princess AI and experimental bots.
 * It is also very useful to explore new Cost Functions and their parameters.
 * @author Luana Coppio
 */
public class PrincessFineTuning {

    /**
     * Main method to run the fine-tuning process.
     *
     * @param args costFunction iterations? game_actions_log.tsv...
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: [costFunction] [iterations] [game_actions_log.tsv...]");
            System.out.println("costFunction: princess, utility, extendedUtility");
            System.out.println("iterations: number of iterations to run, I suggest 50000");
            System.out.println("game_actions_log.tsv: one or more game actions log files to use as dataset");
            return;
        }

        int maxIterations = Integer.parseInt(args[1]);
        int counter = 0;
        DatasetParser datasetParser = new DatasetParser();
        for (int i = 2; i < args.length; i++) {
            File file = new File(args[i]);
            if (!file.exists()) {
                System.out.println("File not found: " + file);
                return;
            }
            datasetParser.parse(file);
            counter++;
            System.out.println("Loaded dataset " + counter + " of " + (args.length - 2));
        }
        System.out.println("Finished loading dataset, it contains " + datasetParser.getTrainingDataset().size() + " actions");
        TrainingDataset dataset = datasetParser.getTrainingDataset();
        var mapSettings = MapSettings.getInstance();
        mapSettings.setBoardSize(dataset.boardWidth(), dataset.boardHeight());
        mapSettings.setMapSize(1, 1);
        Board board = BoardUtilities.generateRandom(mapSettings);

        CostFunction costFunction = switch (args[0]) {
            case "princess" -> new BasicPathRankerCostFunction(CardinalEdge.NORTH, board);
            case "utility" -> new UtilityPathRankerCostFunction(CardinalEdge.NORTH, new UtilityPathRankerCostFunction.CostFunctionSwarmContext(), board);
            case "extendedUtility" -> new ExtendedCostFunction(new UtilityPathRankerCostFunction(CardinalEdge.NORTH, new UtilityPathRankerCostFunction.CostFunctionSwarmContext(), board));
            default -> throw new IllegalArgumentException("Invalid cost function: " + args[0]);
        };

        ParameterOptimizer optimizer =
            ParameterOptimizer.Builder.newBuilder(dataset, costFunction, 0.01)
                .withMaxIterations(maxIterations)
                .withPatience(50)
                .withCycleLength(5913)
                .build();

        // Then run full optimization with best LR
        BehaviorParameters optimalParams = optimizer.optimize();
        if (args[0].equals("princess")) {
            System.out.println("Optimal Parameters on first: " + BasicPathRankerCostFunction.behaviorSettingsFrom(optimalParams).toLog());
        } else {
            System.out.println("Optimal Parameters on first: " + optimalParams);
        }
    }
}
