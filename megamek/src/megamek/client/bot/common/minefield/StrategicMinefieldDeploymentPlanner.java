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

import java.util.*;

/**
 * Deploys minefields in strategic clusters of 3, prioritizing terrain features like bridges,
 * woods, roads, and valley passages, while avoiding high elevations.
 * @author Luana Coppio
 */
public class StrategicMinefieldDeploymentPlanner implements MinefieldDeploymentPlanner {
    // Strategy constants
    private static final int CLUSTER_SIZE = 3;

    private final Board board;
    private final DeploymentScorer scorer;
    private final ClusterGenerator clusterGenerator;

    /**
     * Creates a new strategic minefield deployment planner.
     * @param board The game board
     */
    public StrategicMinefieldDeploymentPlanner(Board board) {
        this.board = board;
        this.scorer = new TerrainScorer(board);
        this.clusterGenerator = new AdjacentClusterGenerator();
    }

    @Override
    public Deque<Coords> getRandomMinefieldPositions(int numberOfCoords) {
        // Adjust to get the right number of clusters
        int numClusters = (int) Math.ceil(numberOfCoords / (double) CLUSTER_SIZE);

        // Generate strategic clusters
        List<List<Coords>> clusters = generateStrategicClusters(numClusters);

        // Flatten clusters into a single queue
        Deque<Coords> result = new ArrayDeque<>();
        for (List<Coords> cluster : clusters) {
            result.addAll(cluster);
        }

        // Trim if we have too many coords
        while (result.size() > numberOfCoords) {
            result.removeLast();
        }

        return result;
    }

    /**
     * Generates strategic clusters of minefields.
     * @param numClusters Number of clusters to generate
     * @return List of coordinate clusters
     */
    private List<List<Coords>> generateStrategicClusters(int numClusters) {
        // Get all valid deployment positions
        List<CandidateCell> candidates = findAllValidCandidates();

        // Sort by score (highest first)
        candidates.sort(Comparator.comparing(CandidateCell::score).reversed());

        // Generate clusters from the best candidates
        return clusterGenerator.generateClusters(candidates, numClusters, CLUSTER_SIZE);
    }

    /**
     * Finds all valid candidate positions on the board.
     * @return List of candidate cells
     */
    private List<CandidateCell> findAllValidCandidates() {
        List<CandidateCell> candidates = new ArrayList<>();

        // Scan the entire board
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                Coords coord = new Coords(x, y);
                Hex hex = board.getHex(coord);

                // Skip invalid hexes
                if (hex == null || hex.hasDepth1WaterOrDeeper()) {
                    continue;
                }

                // Get adjacent valid cells
                List<Coords> validAdjacent = getValidAdjacentCells(coord);

                // Skip if we can't form a cluster here
                if (validAdjacent.size() < CLUSTER_SIZE - 1) {
                    continue;
                }

                // Calculate score and add to candidates
                double score = scorer.scorePosition(coord, validAdjacent);
                candidates.add(new CandidateCell(coord, score, validAdjacent));
            }
        }

        return candidates;
    }

    /**
     * Gets valid adjacent cells (not water and within map bounds).
     * @param coord The coordinate to check around
     * @return List of valid adjacent coordinates
     */
    private List<Coords> getValidAdjacentCells(Coords coord) {
        List<Coords> validAdjacent = new ArrayList<>();

        for (Coords adj : coord.allAdjacent()) {
            // Skip if out of bounds
            if (!board.contains(adj)) {
                continue;
            }

            Hex hex = board.getHex(adj);

            // Skip water or null hexes
            if (hex == null || hex.hasDepth1WaterOrDeeper()) {
                continue;
            }

            validAdjacent.add(adj);
        }

        return validAdjacent;
    }
}

