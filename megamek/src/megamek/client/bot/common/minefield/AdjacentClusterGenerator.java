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

import megamek.common.Coords;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates clusters of cells based on adjacency.
 */
class AdjacentClusterGenerator implements ClusterGenerator {

    @Override
    public List<List<Coords>> generateClusters(
          List<CandidateCell> candidates,
          int numClusters,
          int clusterSize) {

        Set<String> usedCoords = new HashSet<>();
        List<List<Coords>> resultClusters = new ArrayList<>();

        for (CandidateCell candidate : candidates) {
            // Skip if this cell has already been used
            if (usedCoords.contains(candidate.coord().toString())) {
                continue;
            }

            // Get valid adjacent cells (not already used)
            List<Coords> availableAdjacent = filterUnusedCoords(candidate.adjacentCells(), usedCoords);

            // Skip if we don't have enough adjacent cells
            if (availableAdjacent.size() < clusterSize - 1) {
                continue;
            }

            // We can form a new cluster
            List<Coords> cluster = new ArrayList<>();
            cluster.add(candidate.coord());

            // Take N-1 adjacent cells
            for (int i = 0; i < clusterSize - 1 && i < availableAdjacent.size(); i++) {
                cluster.add(availableAdjacent.get(i));
            }

            // Mark these cells as used
            for (Coords c : cluster) {
                usedCoords.add(c.toString());
            }

            resultClusters.add(cluster);

            // If we have enough clusters, we're done
            if (resultClusters.size() >= numClusters) {
                break;
            }
        }

        return resultClusters;
    }

    /**
     * Filters out coordinates that have already been used.
     */
    private List<Coords> filterUnusedCoords(List<Coords> coords, Set<String> usedCoords) {
        List<Coords> unused = new ArrayList<>();
        for (Coords c : coords) {
            if (!usedCoords.contains(c.toString())) {
                unused.add(c);
            }
        }
        return unused;
    }
}
