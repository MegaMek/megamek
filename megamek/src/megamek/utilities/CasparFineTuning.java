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

import megamek.ai.dataset.DatasetParser;
import megamek.ai.dataset.TrainingDataset;
import megamek.ai.optimizer.BasicPathRankerCostFunction;
import megamek.ai.optimizer.CostFunction;
import megamek.ai.optimizer.CostFunctionChooser;
import megamek.ai.optimizer.ParameterOptimizer;
import megamek.ai.optimizer.Parameters;
import megamek.client.bot.caspar.DefaultInputAxisCalculator;
import megamek.client.bot.common.DifficultyLevel;
import megamek.client.bot.neuralnetwork.ActivationFunction;
import megamek.client.bot.neuralnetwork.Architecture;
import megamek.client.bot.neuralnetwork.ELU;
import megamek.client.bot.neuralnetwork.Linear;
import megamek.client.bot.neuralnetwork.NeuralNetworkTrainer;
import megamek.client.bot.princess.CardinalEdge;
import megamek.common.Board;
import megamek.common.MapSettings;
import megamek.common.util.BoardUtilities;

import java.io.File;
import java.io.IOException;

/**
 * This class is used to fine-tune the parameters of the Caspar AI.
 * @author Luana Coppio
 */
public class CasparFineTuning {

    /**
     * Command line arguments for the PrincessFineTuning class.
     */
    private static class CasparCommandLineArguments {
        private File[] gameActionsLogFiles;
        private int epochs;

        public CasparCommandLineArguments(String[] args) {
            if (args.length < 2) {
                System.out.println("Usage: [epochs] [game_actions_log.tsv...]");
                System.out.println("costFunction: " + String.join("\t", CostFunctionChooser.validCostFunctions()));
                System.out.println("iterations: number of iterations to run, I suggest 50000");
                System.out.println("game_actions_log.tsv: one or more game actions log files to use as dataset");
                return;
            }


            this.epochs = Integer.parseInt(args[0]);
            this.gameActionsLogFiles = new File[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                this.gameActionsLogFiles[i - 1] = new File(args[i]);
                if (this.gameActionsLogFiles[i - 1].isDirectory()) {
                    System.out.println("File is a directory: " + this.gameActionsLogFiles[i - 1]);
                    return;
                } else if (!this.gameActionsLogFiles[i - 1].exists()) {
                    System.out.println("File not found: " + this.gameActionsLogFiles[i - 1]);
                    return;
                } else if (!this.gameActionsLogFiles[i - 1].getName().endsWith(".tsv")) {
                    System.out.println("File is not a tsv file: " + this.gameActionsLogFiles[i - 1]);
                    return;
                } else if (!this.gameActionsLogFiles[i - 1].canRead()) {
                    System.out.println("File is not readable: " + this.gameActionsLogFiles[i - 1]);
                    return;
                }
            }
        }
    }

    /**
     * Load the datasets from the files.
     * @param files The files to load, must all be valid game_action_log.tsv files.
     * @return The {@code TrainingDataset}.
     */
    private static TrainingDataset loadTrainingDatasetFromFiles(File[] files) {
        int counter = 0;
        DatasetParser datasetParser = new DatasetParser();
        for (File file : files) {
            datasetParser.parse(file);
            counter++;
            System.out.println("Loaded dataset " + counter + " of " + files.length);
        }

        System.out.println("Finished loading dataset, it contains " + datasetParser.getTrainingDataset().size() + " actions");
        return datasetParser.getTrainingDataset();
    }

    /**
     * Main method to run the fine-tuning process.
     *
     * @param args costFunction iterations? game_actions_log.tsv...
     */
    public static void main(String[] args) throws IOException {
        CasparCommandLineArguments arguments = new CasparCommandLineArguments(args);

        var input = new DefaultInputAxisCalculator();
        var architecture = new Architecture(new int[] {input.getInputSize(), input.getInputSize(), input.getInputSize(), 1},
              new ActivationFunction[] {
                    new ELU(DifficultyLevel.MEDIUM.getAlphaValue()),
                    new ELU(DifficultyLevel.MEDIUM.getAlphaValue()),
                    new ELU(DifficultyLevel.MEDIUM.getAlphaValue()),
                    new Linear()});

        TrainingDataset dataset = loadTrainingDatasetFromFiles(arguments.gameActionsLogFiles);
        dataset = dataset.sampleTrainingDataset(dataset.size() / 5 * 4);
        var splits = dataset.split(0.8);
        var trainingDataset = splits.get(0);
        var validationDataset = splits.get(1);

        NeuralNetworkTrainer neuralNetworkTrainer = new NeuralNetworkTrainer();
        neuralNetworkTrainer.tuneHyperparameters("test_model", DifficultyLevel.MEDIUM,
              architecture,
              trainingDataset,
              validationDataset,
              arguments.epochs,
              32);
    }
}
