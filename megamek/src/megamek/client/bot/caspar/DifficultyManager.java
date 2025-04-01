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

import megamek.ai.neuralnetwork.NeuralNetwork;
import megamek.client.bot.common.DifficultyLevel;
import megamek.client.bot.common.behavior.MovementPreference;
import megamek.client.bot.princess.RankedPath;
import megamek.common.MovePath;

import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the difficulty level of the CASPAR AI.
 * Difficulty affects decision quality.
 * @author Luana Coppio
 */
public class DifficultyManager {
    private final DifficultyLevel difficultyLevel;
    private final Random random;

    /**
     * Creates a difficulty manager with the specified level and model type.
     *
     * @param difficultyLevel The difficulty level
     */
    public DifficultyManager(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
        this.random = ThreadLocalRandom.current();
    }

    /**
     * Selects a movement path from the top-scored paths based on difficulty.
     * Easy difficulty introduces randomness, while Hard mostly picks the best path.
     *
     * @param scoredPaths Ordered set of scored movement paths
     * @return The selected movement path
     */
    public RankedPath selectFromTopPaths(TreeSet<RankedPath> scoredPaths) {
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
            case MEDIUM, HARD -> weightedSelection(topPaths);
            case HARDCORE -> topPaths.get(0);
            default -> topPaths.get(random.nextInt(topPaths.size()));
        };
    }

    /**
     * Performs a weighted random selection based on scores.
     *
     * @param scoredPaths List of scored movement paths
     * @return Selected movement path
     */
    private RankedPath weightedSelection(List<RankedPath> scoredPaths) {
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
                return path;
            }
        }

        // Fallback (should never happen)
        return scoredPaths.get(0);
    }

    /**
     * Gets the current difficulty level.
     * @return The difficulty level
     */
    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

}
