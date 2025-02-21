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
import megamek.ai.optimizer.*;
import megamek.ai.utility.Consideration;
import megamek.ai.utility.DefaultCurve;
import megamek.ai.utility.LogisticCurve;
import megamek.client.bot.caspar.ai.utility.tw.considerations.*;
import megamek.client.bot.caspar.ai.utility.tw.profile.TWProfile;
import megamek.client.bot.princess.CardinalEdge;
import megamek.common.Board;
import megamek.common.MapSettings;
import megamek.common.util.BoardUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * This class is used to fine-tune the parameters of the Princess AI and experimental bots.
 * It is also very useful to explore new Cost Functions and their parameters.
 * @author Luana Coppio
 */
public class PrincessFineTuning {

    /**
     * Command line arguments for the PrincessFineTuning class.
     */
    private static class PrincessCommandLineArguments {
        private CostFunctionChooser costFunction;
        private int iterations;
        private File[] gameActionsLogFiles;

        public PrincessCommandLineArguments(String[] args) {
            if (args.length < 3) {
                System.out.println("Usage: [costFunction] [iterations] [game_actions_log.tsv...]");
                System.out.println("costFunction: " + String.join("\t", CostFunctionChooser.validCostFunctions()));
                System.out.println("iterations: number of iterations to run, I suggest 50000");
                System.out.println("game_actions_log.tsv: one or more game actions log files to use as dataset");
                return;
            }

            try {
                this.costFunction = CostFunctionChooser.fromString(args[0]);
            } catch (IllegalArgumentException e){
                System.out.println("Invalid cost function: " + args[0] + ", valid options are: " + CostFunctionChooser.validCostFunctions());
                return;
            }

            this.iterations = Integer.parseInt(args[1]);
            this.gameActionsLogFiles = new File[args.length - 2];
            for (int i = 2; i < args.length; i++) {
                this.gameActionsLogFiles[i - 2] = new File(args[i]);
                if (this.gameActionsLogFiles[i - 2].isDirectory()) {
                    System.out.println("File is a directory: " + this.gameActionsLogFiles[i - 2]);
                    return;
                } else if (!this.gameActionsLogFiles[i - 2].exists()) {
                    System.out.println("File not found: " + this.gameActionsLogFiles[i - 2]);
                    return;
                } else if (!this.gameActionsLogFiles[i - 2].getName().endsWith(".tsv")) {
                    System.out.println("File is not a tsv file: " + this.gameActionsLogFiles[i - 2]);
                    return;
                } else if (!this.gameActionsLogFiles[i - 2].canRead()) {
                    System.out.println("File is not readable: " + this.gameActionsLogFiles[i - 2]);
                    return;
                }
            }
        }

        public boolean isValid() {
            return this.costFunction != null && this.iterations > 0 && this.gameActionsLogFiles.length > 0;
        }
    }


    private final ParameterOptimizer optimizer;

    public PrincessFineTuning(
        TrainingDataset trainingDataset,
        CostFunction costFunction,
        int iterations,
        int patience,
        int cycleLength,
        double learningRate)
    {
        optimizer =
            ParameterOptimizer.Builder.newBuilder(trainingDataset, costFunction, learningRate)
                .withMaxIterations(iterations)
                .withPatience(patience)
                .withCycleLength(cycleLength)
                .build();
    }

    private Parameters perform() {
        return optimizer.optimize();
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

    private static Board createBoard(TrainingDataset dataset) {
        var mapSettings = MapSettings.getInstance();
        mapSettings.setBoardSize(dataset.boardWidth(), dataset.boardHeight());
        mapSettings.setMapSize(1, 1);
        return BoardUtilities.generateRandom(mapSettings);
    }

    private static List<Consideration> considerations;
    static {
        considerations = new ArrayList<>();
        considerations.add(new BackSide("Backside", DefaultCurve.Logistic.getCurve()));
        considerations.add(new FrontSide("Frontside", DefaultCurve.Logistic.getCurve()));
        considerations.add(new RightSide());
        considerations.add(new LeftSide());
        considerations.add(new OverallArmor());
    }
    // @JsonSubTypes.Type(value = BackSide.class, name = "BackSide"),
    //@JsonSubTypes.Type(value = CoverFire.class, name = "CoverFire"),
    //@JsonSubTypes.Type(value = CrowdingEnemies.class, name = "CrowdingEnemies"),
    //@JsonSubTypes.Type(value = CrowdingFriends.class, name = "CrowdingFriends"),
    //@JsonSubTypes.Type(value = CurrentThreat.class, name = "CurrentThreat"),
    //    //@JsonSubTypes.Type(value = DamageOutput.class, name = "DamageOutput"),
    //@JsonSubTypes.Type(value = DecoyValue.class, name = "DecoyValue"),
    //@JsonSubTypes.Type(value = ECMCoverage.class, name = "ECMCoverage"),
    //@JsonSubTypes.Type(value = EnemyECMCoverage.class, name = "EnemyECMCoverage"),
    //@JsonSubTypes.Type(value = EnemyPositioning.class, name = "EnemyPositioning"),
    //@JsonSubTypes.Type(value = EnvironmentalCover.class, name = "EnvironmentalCover"),
    //@JsonSubTypes.Type(value = EnvironmentalHazard.class, name = "EnvironmentalHazard"),
    //@JsonSubTypes.Type(value = FacingTheEnemy.class, name = "FacingTheEnemy"),
    //@JsonSubTypes.Type(value = FavoriteTargetInRange.class, name = "FavoriteTargetInRange"),
    //@JsonSubTypes.Type(value = FireExposure.class, name = "FireExposure"),
    //@JsonSubTypes.Type(value = FlankingPosition.class, name = "FlankingPosition"),
    //@JsonSubTypes.Type(value = FormationCohesion.class, name = "FormationCohesion"),
    //@JsonSubTypes.Type(value = FriendlyArtilleryFire.class, name = "FriendlyArtilleryFire"),
    //@JsonSubTypes.Type(value = FriendlyPositioning.class, name = "FriendlyPositioning"),
    //@JsonSubTypes.Type(value = FriendsCoverFire.class, name = "FriendsCoverFire"),
    //@JsonSubTypes.Type(value = FrontSide.class, name = "FrontSide"),
    //@JsonSubTypes.Type(value = HeatVulnerability.class, name = "HeatVulnerability"),
    //@JsonSubTypes.Type(value = IsVIPCloser.class, name = "IsVIPCloser"),
    //@JsonSubTypes.Type(value = KeepDistance.class, name = "KeepDistance"),
    //@JsonSubTypes.Type(value = LeftSide.class, name = "LeftSide"),
    //@JsonSubTypes.Type(value = MyUnitBotSettings.class, name = "MyUnitBotSettings"),
    //@JsonSubTypes.Type(value = MyUnitHeatManagement.class, name = "MyUnitHeatManagement"),
    //@JsonSubTypes.Type(value = MyUnitIsCrippled.class, name = "MyUnitIsCrippled"),
    //@JsonSubTypes.Type(value = MyUnitIsMovingTowardsWaypoint.class, name = "MyUnitIsMovingTowardsWaypoint"),
    //@JsonSubTypes.Type(value = MyUnitMoved.class, name = "MyUnitMoved"),
    //@JsonSubTypes.Type(value = MyUnitRoleIs.class, name = "MyUnitRoleIs"),
    //@JsonSubTypes.Type(value = MyUnitTMM.class, name = "MyUnitTMM"),
    //@JsonSubTypes.Type(value = MyUnitUnderThreat.class, name = "MyUnitUnderThreat"),
    //@JsonSubTypes.Type(value = OverallArmor.class, name = "OverallArmor"),
    //@JsonSubTypes.Type(value = PilotingCaution.class, name = "PilotingCaution"),
    //@JsonSubTypes.Type(value = Retreat.class, name = "Retreat"),
    //@JsonSubTypes.Type(value = RightSide.class, name = "RightSide"),
    //@JsonSubTypes.Type(value = Scouting.class, name = "Scouting"),
    //@JsonSubTypes.Type(value = StandStill.class, name = "StandStill"),
    //@JsonSubTypes.Type(value = StrategicGoal.class, name = "StrategicGoal"),
    //@JsonSubTypes.Type(value = TargetUnitsArmor.class, name = "TargetUnitsArmor"),
    //@JsonSubTypes.Type(value = TargetWithinOptimalRange.class, name = "TargetWithinOptimalRange"),
    //@JsonSubTypes.Type(value = TargetWithinRange.class, name = "TargetWithinRange"),
    //@JsonSubTypes.Type(value = TurnsToEncounter.class, name = "TurnsToEncounter")

    /**
     * Main method to run the fine-tuning process.
     *
     * @param args costFunction iterations? game_actions_log.tsv...
     */
    public static void main(String[] args) {
        PrincessCommandLineArguments arguments = new PrincessCommandLineArguments(args);
        if (!arguments.isValid()) {
            return;
        }

        TrainingDataset dataset = loadTrainingDatasetFromFiles(arguments.gameActionsLogFiles);
        Board board = createBoard(dataset);

        List<Consideration> considerations = new ArrayList<>();

        CostFunction costFunction = arguments.costFunction.createCostFunction(CardinalEdge.NORTH, board, considerations);

        PrincessFineTuning princessFineTuning = new PrincessFineTuning(
            dataset, costFunction, arguments.iterations, 50, 5913, 0.01);
        Parameters optimalParams = princessFineTuning.perform();

        if (arguments.costFunction.equals(CostFunctionChooser.Princess)) {
            System.out.println("Optimal Parameters: " + BasicPathRankerCostFunction.behaviorSettingsFrom(optimalParams).toLog());
        } else {
            System.out.println("Optimal Parameters on first: " + optimalParams);
        }
    }
}
