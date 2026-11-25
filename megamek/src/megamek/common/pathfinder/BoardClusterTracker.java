/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.client.bot.princess.CardinalEdge;
import megamek.common.BulldozerMovePath;
import megamek.common.Hex;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.moves.MoveStep;
import megamek.common.pathfinder.BoardEdgePathFinder.GroundElevationLimits;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrains;
import megamek.common.util.BoardUtilities;

/**
 * This class handles the tracking of "clusters" of movable areas for various movement types, either with or without
 * destruction awareness.
 */
public class BoardClusterTracker {

    private record MovementProfile(MovementType type, GroundElevationLimits elevations, double weight, int height,
          boolean cannotEnterSnow) {
        static MovementProfile forEntity(Entity entity) {
            return new MovementProfile(MovementType.getMovementType(entity), GroundElevationLimits.forEntity(entity),
                  entity.getWeight(), entity.height(),
                  entity instanceof BattlefieldSupportAsset asset && asset.cannotEnterSnow());
        }
    }

    private final Map<MovementProfile, Map<Coords, BoardCluster>> movableAreas = new HashMap<>();
    private final Map<MovementProfile, Map<Coords, BoardCluster>> movableAreasWithTerrainReduction = new HashMap<>();
    private final Map<MovementProfile, Map<Coords, BoardCluster>> movableAreasBridges = new HashMap<>();
    private final Map<MovementProfile, Map<Coords, BoardCluster>> movableAreasBridgesWithTerrainReduction =
          new HashMap<>();

    /**
     * Returns the size of the biggest terrain-reduced or non-terrain-reduced board cluster in which the given
     * coordinates currently reside.
     */
    public int getBoardClusterSize(Entity entity, Coords actualCoords, boolean terrainReduction) {
        MovementProfile profile = MovementProfile.forEntity(entity);

        updateMovableAreas(entity);

        if (terrainReduction) {
            BoardCluster noBridgeCluster = movableAreasWithTerrainReduction.get(profile).get(actualCoords);
            int noBridgeClusterSize = noBridgeCluster == null ? 0 : noBridgeCluster.contents.size();

            BoardCluster bridgeCluster = movableAreasBridgesWithTerrainReduction.get(profile).get(actualCoords);
            int bridgeClusterSize = bridgeCluster == null ? 0 : bridgeCluster.contents.size();

            return Math.max(noBridgeClusterSize, bridgeClusterSize);
        } else {
            BoardCluster noBridgeCluster = movableAreas.get(profile).get(actualCoords);
            int noBridgeClusterSize = noBridgeCluster == null ? 0 : noBridgeCluster.contents.size();

            BoardCluster bridgeCluster = movableAreasBridges.get(profile).get(actualCoords);
            int bridgeClusterSize = bridgeCluster == null ? 0 : bridgeCluster.contents.size();

            return Math.max(noBridgeClusterSize, bridgeClusterSize);
        }
    }

    /**
     * Determines whether, for the given entity, the two sets of coordinates share any cluster.
     */
    public boolean coordinatesShareCluster(Entity mover, Coords first, Coords second, int firstElevation,
          int secondElevation) {
        updateMovableAreas(mover);

        MovementProfile profile = MovementProfile.forEntity(mover);

        return coordinatesShareCluster(first, second, firstElevation, secondElevation,
              movableAreasBridges.get(profile).get(first)) ||
              coordinatesShareCluster(first, second, firstElevation, secondElevation,
                    movableAreasBridgesWithTerrainReduction.get(profile).get(first))
              ||
              coordinatesShareCluster(first, second, firstElevation, secondElevation,
                    movableAreas.get(profile).get(first))
              ||
              coordinatesShareCluster(first, second, firstElevation, secondElevation,
                    movableAreasWithTerrainReduction.get(profile).get(first));
    }

    /**
     * Determines if the given cluster contains both the given sets of coordinates.
     */
    public boolean coordinatesShareCluster(Coords first, Coords second, int firstElevation, int secondElevation,
          BoardCluster cluster) {
        // two coordinates are considered to share a cluster if they are both in it at
        // the precise elevations we're considering
        return (cluster != null) && cluster.contents.containsKey(first) && cluster.contents.containsKey(second) &&
              cluster.contents.get(first) == firstElevation && cluster.contents.get(second) == secondElevation;
    }

    /**
     * Returns a set of coordinates on a given board edge that intersects with the cluster in which the given entity
     * resides. May return an empty set.
     */
    public Set<Coords> getDestinationCoords(Entity entity, CardinalEdge edge, boolean terrainReduction) {
        CardinalEdge actualEdge = edge;
        if (edge == CardinalEdge.NEAREST) {
            actualEdge = BoardUtilities.getClosestEdge(entity);
        } else if (edge == CardinalEdge.NONE) {
            return Collections.emptySet();
        }

        updateMovableAreas(entity);

        MovementProfile profile = MovementProfile.forEntity(entity);
        BoardCluster entityCluster;

        if (terrainReduction) {
            entityCluster = movableAreasWithTerrainReduction.get(profile).get(entity.getPosition());
        } else {
            entityCluster = movableAreas.get(profile).get(entity.getPosition());
        }

        Set<Coords> retVal = Collections.emptySet();

        if (entityCluster != null) {
            retVal = entityCluster.getIntersectingHexes(actualEdge, entity.getGame().getBoard(entity));
        }

        // try with bridges
        if (retVal.isEmpty()) {
            if (terrainReduction) {
                entityCluster = movableAreasBridgesWithTerrainReduction.get(profile).get(entity.getPosition());
            } else {
                entityCluster = movableAreasBridges.get(profile).get(entity.getPosition());
            }

            if (entityCluster != null) {
                retVal = entityCluster.getIntersectingHexes(actualEdge, entity.getGame().getBoard(entity));
            }
        }

        return retVal;
    }

    /**
     * Returns a set of coordinates that intersect with the cluster in which the given entity resides.
     */
    public Set<Coords> getDestinationCoords(Entity entity, Coords destination, boolean terrainReduction) {
        updateMovableAreas(entity);

        MovementProfile profile = MovementProfile.forEntity(entity);
        Set<Coords> retVal = Collections.emptySet();

        BoardCluster entityCluster = getEntityCluster(entity, profile, terrainReduction, false);
        if (entityCluster != null) {
            retVal = entityCluster.getIntersectingHexes(destination, 3);
        }

        if (retVal.isEmpty()) {
            entityCluster = getEntityCluster(entity, profile, terrainReduction, true);
            if (entityCluster != null) {
                retVal = entityCluster.getIntersectingHexes(destination, 3);
            }
        }

        return retVal;
    }

    private BoardCluster getEntityCluster(Entity entity, MovementProfile profile, boolean terrainReduction,
          boolean useBridges) {
        if (terrainReduction) {
            return useBridges ? movableAreasBridgesWithTerrainReduction.get(profile).get(entity.getPosition())
                  : movableAreasWithTerrainReduction.get(profile).get(entity.getPosition());
        } else {
            return useBridges ? movableAreasBridges.get(profile).get(entity.getPosition())
                  : movableAreas.get(profile).get(entity.getPosition());
        }
    }

    /**
     * Resets board clusters
     */
    public void clearMovableAreas() {
        movableAreas.clear();
        movableAreasWithTerrainReduction.clear();
        movableAreasBridges.clear();
        movableAreasBridgesWithTerrainReduction.clear();
    }

    /**
     * Updates and stores accessible clusters for the given entity, both for destruction and non-destruction-aware path
     * finding.
     */
    public void updateMovableAreas(Entity entity) {
        MovementProfile profile = MovementProfile.forEntity(entity);

        if (!movableAreas.containsKey(profile)) {
            movableAreas.put(profile, generateClusters(entity, false, false));
        }

        if (!movableAreasWithTerrainReduction.containsKey(profile)) {
            movableAreasWithTerrainReduction.putIfAbsent(profile, generateClusters(entity, true, false));
        }

        if (!movableAreasBridges.containsKey(profile)) {
            movableAreasBridges.put(profile, generateClusters(entity, false, true));
        }

        if (!movableAreasBridgesWithTerrainReduction.containsKey(profile)) {
            movableAreasBridgesWithTerrainReduction.putIfAbsent(profile, generateClusters(entity, true, true));
        }
    }

    /**
     * Returns accessible clusters for the given entity.
     */
    public Map<Coords, BoardCluster> generateClusters(Entity entity, boolean destructionAware, boolean useBridgeTop) {
        Map<Coords, BoardCluster> clusters = new HashMap<>();

        // start with (0, 0)
        // for each hex:
        // if hex is not accessible to this entity, move on
        // if hex is accessible, check if its neighbors are in a cluster
        // for each neighbor in a cluster, check if we can move from this hex to the
        // neighbor and back
        // if yes, flag for joining neighbor's cluster
        // note that if any other neighbor is in a different cluster, and we can move
        // back and forth, we merge the two clusters
        // if no neighbors or cannot move back and forth between hex and neighbors,
        // start new cluster

        if (entity == null || entity.getGame() == null) {
            return clusters;
        }

        Board board = entity.getGame().getBoard(entity);
        int clusterID = 0;

        MovementType movementType = MovementType.getMovementType(entity);
        boolean isHovercraft = movementType == MovementType.Hover;
        boolean isAmphibious = movementType == MovementType.WheeledAmphibious ||
              movementType == MovementType.TrackedAmphibious;

        boolean canUseBridge = MovementType.canUseBridge(movementType);
        GroundElevationLimits elevationLimits = GroundElevationLimits.forEntity(entity);
        Set<Coords> roadOnlyHexes = new HashSet<>();
        Set<Coords> bridgeExitOnlyHexes = new HashSet<>();

        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);

                Hex hex = board.getHex(c);
                boolean onDeck = useBridgeTop && hex.hasBridge() && canUseBridge
                      && entity.getWeight() <= board.getBuildingAt(c).getCurrentCF(c);
                int myElevation = BoardEdgePathFinder.calculateUnitElevationInHex(hex, entity, isHovercraft,
                      isAmphibious, onDeck);
                int relativeElevation = myElevation - hex.getLevel();
                if (hex.containsTerrain(Terrains.IMPASSABLE)
                      || (entity instanceof BattlefieldSupportAsset asset && asset.isSnowProhibited(hex))
                      || (!onDeck && hex.hasBridge() && (!entity.isElevationValid(relativeElevation, hex)
                            || relativeElevation == hex.terrainLevel(Terrains.BRIDGE_ELEV)))) {
                    continue;
                }
                if (!onDeck && (entity.isLocationProhibited(c, board.getBoardId(), relativeElevation)
                      || buildingPlowThroughRequired(entity, movementType, c))
                      && (!destructionAware || !canLevel(entity, c))) {
                    continue;
                }

                if (onDeck && MoveStep.mustUseBridgeExits(entity, board.getBoardId(), hex, relativeElevation, c)) {
                    bridgeExitOnlyHexes.add(c);
                }
                if (BoardEdgePathFinder.requiresRoad(entity, board, c, hex, myElevation)
                      && (!destructionAware || !canLevel(entity, c))) {
                    roadOnlyHexes.add(c);
                }

                List<Coords> neighborsToJoin = new ArrayList<>();
                BoardCluster biggestNeighbor = null;

                // hex is accessible one way or another
                for (int direction = 0; direction < 6; direction++) {
                    Coords neighbor = c.translated(direction);

                    if (clusters.containsKey(neighbor)) {
                        Hex neighborHex = board.getHex(neighbor);
                        int neighborElevation = clusters.get(neighbor).contents.get(neighbor);

                        // if we can't reach from here to the neighbor due to elevation differences,
                        // move on
                        // buildings require special handling - while a tank technically CAN plow
                        // through a building
                        // it is highly inadvisable, and we will avoid it for now.
                        if (!elevationLimits.permits(hex, myElevation, neighborHex, neighborElevation, direction)
                              || buildingPlowThroughRequired(entity, movementType, neighbor)) {
                            continue;
                        }
                        if ((bridgeExitOnlyHexes.contains(c) && !hex.containsTerrainExit(Terrains.BRIDGE, direction))
                              || (bridgeExitOnlyHexes.contains(neighbor)
                                    && !neighborHex.containsTerrainExit(Terrains.BRIDGE, (direction + 3) % 6))) {
                            continue;
                        }
                        if ((roadOnlyHexes.contains(c) || roadOnlyHexes.contains(neighbor))
                              && !Compute.isRoadStep(hex, myElevation - hex.getLevel(), neighborHex,
                                    neighborElevation - neighborHex.getLevel(), direction, false)) {
                            continue;
                        }

                        // we can "freely" go back and forth between ourselves and the neighbor, so
                        // let's join that cluster
                        neighborsToJoin.add(neighbor);

                        if ((biggestNeighbor == null) ||
                              (clusters.get(neighbor).contents.size() > biggestNeighbor.contents.size())) {
                            biggestNeighbor = clusters.get(neighbor);
                        }
                    }
                }

                // start up a new cluster if we have no mutually accessible neighbors
                if (neighborsToJoin.isEmpty() || biggestNeighbor == null) {
                    BoardCluster newCluster = new BoardCluster(clusterID++);
                    newCluster.contents.put(c, myElevation);
                    clusters.put(c, newCluster);
                    // otherwise, join an existing cluster, bringing any other mutually accessible
                    // neighbors and their clusters with me
                    // join the biggest neighbor to reduce shuffling.
                } else {
                    biggestNeighbor.contents.put(c, myElevation);
                    clusters.put(c, biggestNeighbor);

                    // merge any other clusters belonging to joined neighbors to this cluster
                    for (Coords coords : neighborsToJoin) {
                        BoardCluster oldCluster = clusters.get(coords);
                        if (oldCluster == biggestNeighbor) {
                            continue;
                        }

                        for (Coords member : oldCluster.contents.keySet()) {
                            biggestNeighbor.contents.put(member, oldCluster.contents.get(member));
                            clusters.put(member, biggestNeighbor);
                        }
                    }

                }
            }
        }

        return clusters;
    }

    /**
     * Whether we are required to plow through a building if we enter this hex.
     */
    public static boolean buildingPlowThroughRequired(Entity entity, MovementType relevantMovementType,
          Coords coords) {
        // basic premise:
        // ground tanks cannot climb over buildings and must plow through
        // meks can climb over buildings that won't collapse under them
        // the relative height comparison is handled elsewhere

        Board board = entity.getGame().getBoard(entity);
        Hex hex = board.getHex(coords);

        if (!hex.containsTerrain(Terrains.BLDG_CF) && !hex.containsExit(Terrains.FUEL_TANK_CF)) {
            return false;
        } else if (relevantMovementType == MovementType.Walker) {
            final IBuilding building = board.getBuildingAt(coords);

            if (building == null) {
                return false;
            }

            int buildingCF = building.getCurrentCF(coords);

            return entity.getWeight() > buildingCF;
        } else {
            return (relevantMovementType != MovementType.Flyer) &&
                  (relevantMovementType != MovementType.Jump) &&
                  (relevantMovementType != MovementType.None) &&
                  (relevantMovementType != MovementType.Water);
        }
    }

    /**
     * Indicates whether an entity would be able to pass through a given set of coordinates if it were to degrade the
     * terrain there sufficiently.
     */
    private boolean canLevel(Entity entity, Coords c) {
        return BulldozerMovePath.calculateLevelingCost(c, entity) > BulldozerMovePath.CANNOT_LEVEL;
    }

    /**
     * A data structure representing a set of coordinates to which an entity can move.
     */
    public static class BoardCluster {
        public Map<Coords, Integer> contents = new HashMap<>();
        public int id;

        public BoardCluster(int id) {
            this.id = id;
        }

        /**
         * Returns a set of coordinates in the current cluster that intersect an arbitrary rectangle.
         */
        public Set<Coords> getIntersectingHexes(Coords coords, int size) {
            Set<Coords> retVal = new HashSet<>();
            var destinationCluster = coords.allAtDistanceOrLess(size);
            for (var coord : destinationCluster) {
                if (contents.containsKey(coord)) {
                    retVal.add(coords);
                }
            }

            return retVal;
        }


        /**
         * Returns a set of coords in the current cluster that intersect the given board edge.
         */
        public Set<Coords> getIntersectingHexes(CardinalEdge edge, Board board) {
            int xStart, xEnd, yStart, yEnd;
            switch (edge) {
                case NORTH:
                    xStart = 0;
                    xEnd = board.getWidth();
                    yStart = 0;
                    yEnd = 1;
                    break;
                case SOUTH:
                    xStart = 0;
                    xEnd = board.getWidth();
                    yStart = board.getHeight() - 1;
                    yEnd = board.getHeight();
                    break;
                case EAST:
                    xStart = board.getWidth() - 1;
                    xEnd = board.getWidth();
                    yStart = 0;
                    yEnd = board.getHeight();
                    break;
                case WEST:
                    xStart = 0;
                    xEnd = 1;
                    yStart = 0;
                    yEnd = board.getHeight();
                    break;
                default:
                    return null;
            }

            return getIntersectingHexes(xStart, xEnd, yStart, yEnd);
        }

        /**
         * Returns a set of coordinates in the current cluster that intersect an arbitrary rectangle.
         */
        public Set<Coords> getIntersectingHexes(int xStart, int xEnd, int yStart, int yEnd) {
            Set<Coords> retVal = new HashSet<>();

            for (int x = xStart; x < xEnd; x++) {
                for (int y = yStart; y < yEnd; y++) {
                    Coords coords = new Coords(x, y);
                    if (contents.containsKey(coords)) {
                        retVal.add(coords);
                    }
                }
            }

            return retVal;
        }
    }
}
