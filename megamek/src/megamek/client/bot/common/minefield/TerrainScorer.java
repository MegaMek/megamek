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
package megamek.client.bot.common.minefield;

import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.Hex;
import megamek.common.Terrains;

import java.util.ArrayList;
import java.util.List;

/**
 * Scores deployment positions based on terrain features.
 * @author Luana Coppio
 */
class TerrainScorer implements DeploymentScorer {
    // Weight multipliers for different terrain types and features
    private static final double BRIDGE_WEIGHT = 2.0;
    private static final double WOODS_WEIGHT = 1.8;
    private static final double PAVED_WEIGHT = 1.5;
    private static final double CLEAR_WEIGHT = 1.0;
    private static final double VALLEY_WEIGHT = 2.2;
    private static final double HIGH_ELEVATION_PENALTY = 0.5;

    // Minimum elevation difference to consider as a valley/canyon
    private static final int VALLEY_ELEVATION_DIFF = 3;

    private final Board board;

    public TerrainScorer(Board board) {
        this.board = board;
    }

    @Override
    public double scorePosition(Coords coord, List<Coords> adjacent) {
        Hex hex = board.getHex(coord);
        if (hex == null) {
            return 0;
        }

        // Start with base terrain score
        double score = calculateTerrainScore(hex);

        // Adjust for topography
        score = adjustForTopography(coord, score);

        return score;
    }

    /**
     * Calculates score based on terrain type.
     */
    private double calculateTerrainScore(Hex hex) {
        if (hex.hasDepth1WaterOrDeeper()) {
            return 0; // Water hexes are invalid
        }

        if (hex.containsTerrain(Terrains.BRIDGE)) {
            return BRIDGE_WEIGHT;
        } else if (hex.hasVegetation()) {
            return WOODS_WEIGHT;
        } else if (hex.hasPavementOrRoad()) {
            return PAVED_WEIGHT;
        } else {
            return CLEAR_WEIGHT;
        }
    }

    /**
     * Adjusts score based on topographical features.
     */
    private double adjustForTopography(Coords coord, double score) {
        int elevation = board.getHex(coord).getLevel();
        List<Integer> surroundingElevations = getSurroundingElevations(coord);

        // Check if this is a high point (penalize)
        if (elevation > getAverageElevation(surroundingElevations) + 2) {
            score *= HIGH_ELEVATION_PENALTY;
        }

        // Check if this is a valley/canyon
        if (isValley(elevation, surroundingElevations)) {
            score *= VALLEY_WEIGHT;
        }

        return score;
    }

    /**
     * Gets elevations of surrounding hexes.
     */
    private List<Integer> getSurroundingElevations(Coords coord) {
        List<Integer> elevations = new ArrayList<>();
        int radius = 3; // Check in radius of 2

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                // Skip the center
                if (dx == 0 && dy == 0) continue;

                Coords nearby = new Coords(coord.getX() + dx, coord.getY() + dy);

                // Skip if out of bounds
                if (!board.contains(nearby)) continue;

                Hex hex = board.getHex(nearby);
                if (hex != null) {
                    elevations.add(hex.getLevel());
                }
            }
        }

        return elevations;
    }

    /**
     * Determines if a hex is in a valley or canyon.
     */
    private boolean isValley(int elevation, List<Integer> surroundingElevations) {
        int maxElevation = Integer.MIN_VALUE;
        for (int e : surroundingElevations) {
            maxElevation = Math.max(maxElevation, e);
        }

        // Count how many surrounding hexes have significantly higher elevation
        long higherPoints = surroundingElevations.stream()
              .filter(e -> e > elevation + 2)
              .count();

        return (maxElevation - elevation >= VALLEY_ELEVATION_DIFF) && (higherPoints >= 2);
    }

    /**
     * Calculates average elevation of surrounding hexes.
     */
    private double getAverageElevation(List<Integer> elevations) {
        if (elevations.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (int e : elevations) {
            sum += e;
        }
        return sum / elevations.size();
    }
}
