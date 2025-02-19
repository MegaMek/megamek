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

package megamek.client.bot.caspar.ai.utility.tw;

import megamek.common.Coords;
import megamek.common.Targetable;

import java.util.*;

/**
 * ClusteringService is a utility class that builds clusters of entities
 *
 * @author Luana Coppio
 */
public class ClusteringService {
    private final double maxDist;         // Distance threshold for BFS
    private final int maxClusterSize;     // Maximum number of elements in a cluster

    // Maps element ID -> cluster centroid (x,y)
    private final Map<Integer, Cluster> elementToCluster = new HashMap<>();
    private final Set<Cluster> clusters = new HashSet<>();

    /**
     * Constructor for ClusteringService.
     * @param maxDist Maximum distance entities must be within to be clustered together
     * @param maxClusterSize Maximum number of entities in a cluster
     */
    public ClusteringService(double maxDist, int maxClusterSize) {
        this.maxDist = maxDist;
        this.maxClusterSize = maxClusterSize;
    }

    public Cluster getCluster(Targetable entity) {
        return elementToCluster.get(entity.getId());
    }

    public void updateClusterCentroids() {
        for (Cluster c : clusters) {
            c.computeCentroid();
        }
    }

    /**
     * Build clusters from a list of elements.
     * @param elements List of elements to cluster
     */
    public void buildClusters(List<Targetable> elements) {
        elementToCluster.clear();

        Map<Integer, List<Targetable>> playerToElements = new HashMap<>();
        for (Targetable e : elements) {
            playerToElements
                .computeIfAbsent(e.getOwnerId(), k -> new ArrayList<>())
                .add(e);
        }

        // For each team, build BFS clusters
        for (Map.Entry<Integer, List<Targetable>> entry : playerToElements.entrySet()) {
            int team = entry.getKey();
            List<Targetable> playerElements = entry.getValue();

            Set<Integer> visited = new HashSet<>();
            for (Targetable elem : playerElements) {
                if (!visited.contains(elem.getId())) {
                    // BFS to find all elements reachable within maxDist
                    List<Targetable> bfsCluster = bfsCluster(playerElements, elem, visited);
                    // If that BFS cluster is bigger than maxClusterSize, split it up
                    List<List<Targetable>> splitClusters = splitLargeCluster(bfsCluster, maxClusterSize);

                    // Now, for each final sub-cluster, compute centroid and store element->centroid
                    for (List<Targetable> subClusterElements : splitClusters) {
                        Cluster c = new Cluster(team);
                        for (Targetable scElem : subClusterElements) {
                            c.addMember(scElem);
                        }
                        c.computeCentroid();
                        for (Targetable scElem : subClusterElements) {
                            elementToCluster.put(scElem.getId(), c);
                        }
                        clusters.add(c);
                    }
                }
            }
        }
        updateClusterCentroids();
    }

    /**
     * BFS to collect a cluster of elements from teamElements starting at 'start',
     * marking them visited. Returns the entire BFS-connected set of elements
     * (i.e., all that are within maxDist of each other transitively).
     */
    private List<Targetable> bfsCluster(List<Targetable> teamElements, Targetable start, Set<Integer> visited) {
        List<Targetable> cluster = new ArrayList<>();
        Queue<Targetable> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start.getId());

        while (!queue.isEmpty()) {
            Targetable current = queue.poll();
            cluster.add(current);

            // Check all other team elements to see if they connect
            for (Targetable e2 : teamElements) {
                if (!visited.contains(e2.getId())) {
                    if (distance(current, e2) <= maxDist) {
                        queue.add(e2);
                        visited.add(e2.getId());
                    }
                }
            }
        }
        return cluster;
    }

    /**
     * Splits a "large" BFS cluster of elements into sub-clusters
     * of size at most 'maxSize'.
     * For simplicity, we:
     * 1) Compute the centroid of the entire BFS cluster
     * 2) Sort elements by distance to that centroid
     * 3) Chunk them into sub-lists of size <= maxSize
     */
    private List<List<Targetable>> splitLargeCluster(List<Targetable> bigCluster, int maxSize) {
        if (bigCluster.size() <= maxSize) {
            // No need to split, just return a single cluster
            return Collections.singletonList(bigCluster);
        }
        // 1) Compute overall centroid
        double sumX = 0, sumY = 0;
        for (Targetable e : bigCluster) {
            sumX += e.getPosition().getX();
            sumY += e.getPosition().getY();
        }
        double centroidX = sumX / bigCluster.size();
        double centroidY = sumY / bigCluster.size();

        // 2) Sort elements by distance from that centroid
        bigCluster.sort((a, b) -> {
            double da = dist(a.getPosition().getX(), a.getPosition().getY(), centroidX, centroidY);
            double db = dist(b.getPosition().getX(), b.getPosition().getY(), centroidX, centroidY);
            return Double.compare(da, db);
        });

        // 3) Split into chunks of size <= maxSize
        List<List<Targetable>> subClusters = new ArrayList<>();
        int startIndex = 0;
        while (startIndex < bigCluster.size()) {
            int endIndex = Math.min(startIndex + maxSize, bigCluster.size());
            List<Targetable> subList = bigCluster.subList(startIndex, endIndex);
            subClusters.add(new ArrayList<>(subList)); // copy to a new list
            startIndex += maxSize;
        }

        return subClusters;
    }

    private double distance(Targetable e1, Targetable e2) {
        return e1.getPosition().distance(e2.getPosition());
    }

    private double dist(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx*dx + dy*dy);
    }

    /**
     * Retrieve the centroid (x, y) for any given element,
     * returns null if the element was never clustered.
     */
    public Coords getClusterMidpoint(Targetable e) {
        if (elementToCluster.get(e.getId()) == null) {
            return null;
        }
        return elementToCluster.get(e.getId()).getCentroid();
    }

    public Cluster getClosestEnemyCluster(Cluster cluster) {
        double minDist = Double.MAX_VALUE;
        Cluster closest = null;
        for (Cluster c : clusters) {
            if (c.getTeam() != cluster.getTeam()) {
                double dist = dist(c.getCentroidX(), c.getCentroidY(), cluster.getCentroidX(), cluster.getCentroidY());
                if (dist < minDist) {
                    minDist = dist;
                    closest = c;
                }
            }
        }
        return closest;
    }
}

