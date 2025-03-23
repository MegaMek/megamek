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
package megamek.client.bot.princess;

import megamek.common.*;
import megamek.common.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Context class to control the units as a swarm.
 * Contains information about the current state of the game
 */
public class SwarmContext {

    private final Map<Integer, Integer> enemyTargetCounts = new HashMap<>();
    private final List<Coords> strategicGoals = new Vector<>();
    private Coords currentCenter;

    private int quadrantHeight = 0;
    private int quadrantWidth = 0;
    private int offsetX = 0;
    private int offsetY = 0;

    private int clusterUnitsSize = 0;

    private final Map<Integer, SwarmCluster> unitClusters = new HashMap<>();
    private final List<SwarmCluster> clusters = new ArrayList<>();

    public SwarmContext() {;
    }

    /**
     * Record an enemy target, incrementing the number of units targeting the enemy this turn
     * @param enemyId The enemy id
     */
    @SuppressWarnings("unused")
    public void recordEnemyTarget(int enemyId) {
        enemyTargetCounts.put(enemyId, enemyTargetCounts.getOrDefault(enemyId, 0) + 1);
    }

    /**
     * Get the number of times an enemy has been targeted
     * @param enemyId The enemy id
     * @return The number of times the enemy has been targeted
     */
    @SuppressWarnings("unused")
    public int getEnemyTargetCount(int enemyId) {
        return enemyTargetCounts.getOrDefault(enemyId, 0);
    }

    /**
     * Reset the enemy target counts
     */
    public void resetEnemyTargets() {
        enemyTargetCounts.clear();
    }

    /**
     * Add a strategic goal to the list of goals, a strategic goal is simply a coordinate which we want to move towards,
     * its mainly used for double blind games where we don't know the enemy positions, the strategic goals help
     * distribute the map evenly accross the units inside the swarm to cover more ground and find the enemy faster
     * @param coords  The coordinates to add
     */
    public void addStrategicGoal(Coords coords) {
        strategicGoals.add(coords);
    }

    /**
     * Remove a strategic goal from the list of goals
     * @param coords The coordinates to remove
     */
    @SuppressWarnings("unused")
    public void removeStrategicGoal(Coords coords) {
        strategicGoals.remove(coords);
    }

    /**
     * Remove strategic goals in a radius around the given coordinates
     * @param coords The center coordinates
     * @param radius The radius to remove goals
     */
    @SuppressWarnings("unused")
    public void removeStrategicGoal(Coords coords, int radius) {
        for (var c : coords.allAtDistanceOrLess(radius)) {
            strategicGoals.remove(c);
        }
    }

    /**
     * Get the strategic goals on the quadrant of the given coordinates
     * @param coords The coordinates to check
     * @return A list of strategic goals on the quadrant
     */
    public List<Coords> getStrategicGoalsOnCoordsQuadrant(Coords coords) {
        QuadrantParameters quadrant = getQuadrantParameters(coords);
        Coords coord;
        List<Coords> goals = new Vector<>();
        for (int i = quadrant.startX(); i < quadrant.endX(); i++) {
            for (int j = quadrant.startY(); j < quadrant.endY(); j++) {
                coord = new Coords(i, j);
                if (strategicGoals.contains(coord)) {
                    goals.add(coord);
                }
            }
        }
        return goals;
    }

    private QuadrantParameters getQuadrantParameters(Coords coords) {
        int x = coords.getX();
        int y = coords.getY();
        int startX = offsetX + (x / quadrantWidth) * quadrantWidth;
        int startY = offsetY + (y / quadrantHeight) * quadrantHeight;
        int endX = startX + quadrantWidth;
        int endY = startY + quadrantHeight;
        return new QuadrantParameters(startX, startY, endX, endY);
    }

    /**
     * Set the current center of the swarm
     * @param adjustedCenter The new center
     */
    public void setCurrentCenter(Coords adjustedCenter) {
        this.currentCenter = adjustedCenter;
    }

    /**
     * Get the current center of the swarm
     * @return The current center
     */
    public @Nullable Coords getCurrentCenter() {
        return currentCenter;
    }

    public boolean isUnitTooFarFromCluster(Entity unit, double v) {
        SwarmCluster cluster = getClusterFor(unit);
        return unit.getPosition().distance(cluster.centroid) > v;
    }

    public List<SwarmCluster> getClusters() {
        return clusters;
    }


    private record QuadrantParameters(int startX, int startY, int endX, int endY) {
    }

    /**
     * Remove all strategic goals on the quadrant of the given coordinates
     * @param coords The coordinates to check
     */
    public void removeAllStrategicGoalsOnCoordsQuadrant(Coords coords) {
        QuadrantParameters quadrant = getQuadrantParameters(coords);
        for (int i = quadrant.startX(); i < quadrant.endX(); i++) {
            for (int j = quadrant.startY(); j < quadrant.endY(); j++) {
                strategicGoals.remove(new Coords(i, j));
            }
        }
    }

    /**
     * Initialize the strategic goals for the board
     * @param board The board to initialize the goals on
     * @param quadrantWidth The width of the quadrants
     * @param quadrantHeight The height of the quadrants
     */
    public void initializeStrategicGoals(Board board, int quadrantWidth, int quadrantHeight) {
        strategicGoals.clear();
        this.quadrantWidth = quadrantWidth;
        this.quadrantHeight = quadrantHeight;

        int boardWidth = board.getWidth();
        int boardHeight = board.getHeight();

        // Calculate extra space and offsets to center the quadrants
        int extraX = boardWidth % quadrantWidth;
        int extraY = boardHeight % quadrantHeight;
        offsetX = extraX / 2;
        offsetY = extraY / 2;

        // Iterate over each quadrant using the offsets
        for (int i = 0; i < (boardWidth - offsetX); i += quadrantWidth) {
            for (int j = 0; j < (boardHeight - offsetY); j += quadrantHeight) {
                int startX = offsetX + i;
                int startY = offsetY + j;
                int endX = Math.min(startX + quadrantWidth, boardWidth);
                int endY = Math.min(startY + quadrantHeight, boardHeight);

                var xMidPoint = (startX + endX) / 2;
                var yMidPoint = (startY + endY) / 2;
                for (var coords : new Coords(xMidPoint, yMidPoint).allAtDistanceOrLess(3)) {
                    var hex = board.getHex(coords);
                    if (hex == null || hex.isClearHex() && hasNoHazards(hex)) {
                        addStrategicGoal(coords);
                        break;
                    }
                }
            }
        }
    }

    private static final Set<Integer> HAZARDS = new HashSet<>(Arrays.asList(Terrains.FIRE,
        Terrains.MAGMA,
        Terrains.ICE,
        Terrains.WATER,
        Terrains.BUILDING,
        Terrains.BRIDGE,
        Terrains.BLACK_ICE,
        Terrains.SNOW,
        Terrains.SWAMP,
        Terrains.MUD,
        Terrains.TUNDRA));

    private boolean hasNoHazards(Hex hex) {
        var hazards = hex.getTerrainTypesSet();
        // Black Ice can appear if the conditions are favorable
        hazards.retainAll(HAZARDS);
        return hazards.isEmpty();
    }

    /**
     * Get the cluster for a unit
     * @param unit The unit to get the cluster for
     * @return The cluster for the unit, initializes a new cluster if it doesn't exist
     */
    public SwarmCluster getClusterFor(Entity unit) {
        var cluster = unitClusters.get(unit.getId());
        if (cluster == null) {
            cluster = new SwarmCluster();
            unitClusters.put(unit.getId(), cluster);
            cluster.addMember(unit);
            clusterUnitsSize++;
        }
        return cluster;
    }

    /**
     * Get the cluster center for a unit
     * @param unitId The unit to get the cluster for
     * @return The cluster for the unit, initializes a new cluster if it doesn't exist
     */
    public Coords getCenterForUnit(int unitId) {
        var cluster = unitClusters.get(unitId);
        if (cluster == null) {
            return getCurrentCenter();
        }
        return cluster.getCentroid();
    }

    public static class SwarmCluster {
        List<Entity> members = new ArrayList<>();
        Coords centroid;
        int maxSize = 6;

        public List<Entity> getMembers() {
            return members;
        }

        public Coords getCentroid() {
            return centroid;
        }

        public void addMember(Entity unit) {
            if (members.size() >= maxSize) return;
            members.add(unit);
            updateCentroid();
        }

        private void updateCentroid() {
            if (members.isEmpty()) return;
            members.removeIf(Entity::isDoomed);
            members.removeIf(Entity::isDestroyed);
            centroid = calculateClusterCentroid(members);
        }

        private Coords calculateClusterCentroid(List<Entity> members) {
            double count = 0;
            double qSum = 0;
            double rSum = 0;
            double sSum = 0;

            for (Entity unit : members) {
                CubeCoords cube = unit.getPosition().toCube();
                qSum += cube.q;
                rSum += cube.r;
                sSum += cube.s;
                count ++;
            }

            CubeCoords weightedCube = new CubeCoords(
                    qSum / count,
                    rSum / count,
                    sSum / count
            );

            return weightedCube.toOffset();

        }
    }

    private int calculateOptimalClusterSize(int totalUnits) {
        if (totalUnits <= 4) {
            return totalUnits;
        }
        if (totalUnits % 4 == 0) {
            return 4;
        }
        if (totalUnits % 5 == 0) {
            return 5;
        }
        return (totalUnits % 4) > (totalUnits % 5) ? 5 : 4;
    }

    // Cluster management methods

    /**
     * Assign units to clusters
     * @param allUnits The units to assign
     */
    public void assignClusters(List<Entity> allUnits) {
        if (clusterUnitsSize >= allUnits.size()){
            clusters.forEach(SwarmCluster::updateCentroid);
            return;
        }
        clusterUnitsSize = 0;
        clusters.clear();
        unitClusters.clear();
        int optimalSize = calculateOptimalClusterSize(allUnits.size());

        // Sort units by role for better distribution
        List<Entity> sortedUnits = allUnits.stream()
                .sorted(Comparator.comparingInt(u -> u.getRole().ordinal()))
                .collect(Collectors.toList());

        // Create initial clusters
        while (!sortedUnits.isEmpty()) {
            SwarmCluster cluster = new SwarmCluster();
            for (int i = 0; i < optimalSize && !sortedUnits.isEmpty(); i++) {
                var unit = sortedUnits.get(0);
                unitClusters.put(unit.getId(), cluster);
                cluster.addMember(sortedUnits.remove(0));
                clusterUnitsSize++;
            }
            clusters.add(cluster);
        }
    }
}
