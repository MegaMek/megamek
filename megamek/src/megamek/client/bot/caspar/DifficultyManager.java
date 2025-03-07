/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
package megamek.client.bot.caspar;

import megamek.client.bot.princess.RankedPath;
import megamek.common.MovePath;

import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the difficulty level of the CASPAR AI.
 * Difficulty affects decision quality, tactical risk-taking, and overall performance.
 * @author Luana Coppio
 */
public class DifficultyManager {
    private final DifficultyLevel difficultyLevel;
    private final Random random;
    private final NeuralNetwork.ModelType modelType;

    /**
     * Creates a difficulty manager with the specified level and model type.
     *
     * @param difficultyLevel The difficulty level
     * @param modelType The neural network model type to use
     */
    public DifficultyManager(DifficultyLevel difficultyLevel, NeuralNetwork.ModelType modelType) {
        this.difficultyLevel = difficultyLevel;
        this.modelType = modelType;
        this.random = ThreadLocalRandom.current();
    }

    /**
     * Selects a movement path from the top-scored paths based on difficulty.
     * Easy difficulty introduces randomness, while Hard mostly picks the best path.
     *
     * @param scoredPaths Ordered set of scored movement paths
     * @return The selected movement path
     */
    public MovePath selectFromTopPaths(TreeSet<RankedPath> scoredPaths) {
        if (scoredPaths.isEmpty()) {
            return null;
        }

        // Get the top N paths based on difficulty
        int topPathsToConsider = difficultyLevel.getTopPathsToConsider();

        // Extract the top paths
        List<RankedPath> topPaths = scoredPaths.stream()
            .limit(topPathsToConsider)
            .toList();

        // Select based on difficulty
        return switch (difficultyLevel) {
            case EASY -> topPaths.get(random.nextInt(topPaths.size())).getPath();
            case MEDIUM, HARD -> weightedSelection(topPaths);
            case HARDCORE -> topPaths.get(0).getPath();
        };
    }

    /**
     * Performs a weighted random selection based on scores.
     *
     * @param scoredPaths List of scored movement paths
     * @return Selected movement path
     */
    private MovePath weightedSelection(List<RankedPath> scoredPaths) {
        // Calculate the sum of all scores
        double totalScore = scoredPaths.stream()
            .mapToDouble(RankedPath::getRank)
            .sum();

        // Pick a random point within the total score
        double randomPoint = random.nextDouble() * totalScore;

        // Find which path contains this point
        double cumulativeScore = 0;
        for (RankedPath path : scoredPaths) {
            cumulativeScore += path.getRank();
            if (randomPoint <= cumulativeScore) {
                return path.getPath();
            }
        }

        // Fallback (should never happen)
        return scoredPaths.get(0).getPath();
    }

    /**
     * Creates a neural network appropriate for the current difficulty level.
     *
     * @return A neural network instance
     */
    public NeuralNetwork createNeuralNetwork() {
        return NeuralNetwork.loadModel(modelType, difficultyLevel);
    }

    /**
     * Gets the probability that the AI will take a tactical risk.
     * Higher difficulties take calculated risks more often.
     *
     * @return The risk-taking probability (0-1)
     */
    public double getRiskTakingProbability() {
        return switch (difficultyLevel) {
            case EASY -> 0.1;
            case MEDIUM -> 0.2;
            case HARD -> 0.4;
            case HARDCORE -> 0.5;
        };
    }

    public double getMaxSearchDepth() {
        return switch (difficultyLevel) {
            case EASY -> 1.5d;
            case MEDIUM -> 3d;
            case HARD -> 5d;
            case HARDCORE -> 10d;
        };
    }

    public double adjustDecisionQuality(double prediction) {
        return switch (difficultyLevel) {
            case EASY -> prediction * 0.8;
            case MEDIUM -> prediction * 0.9;
            case HARD -> prediction;
            case HARDCORE -> prediction * 1.1;
        };
    }
}
