/*
* MegaMek -
* Copyright (C) 2020 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.client.bot.princess.CardinalEdge;
import megamek.common.BulldozerMovePath;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.IBoard;
import megamek.common.MiscType;
import megamek.common.util.BoardUtilities;

/**
 * This class handles the tracking of "clusters" of movable areas for various movement types, 
 * either with or without destruction awareness.
 */
public class BoardClusterTracker {
    /**
     * Movement types that are relevant for "destruction-aware pathfinding"
     * Have a close relationship but are not exactly one to one with entity movement modes.
     */
    public static enum MovementType {
        Walker,
        Wheeled,
        WheeledAmphi,
        Tracked,
        TrackedAmphi,
        Hover,
        Jump,
        Flyer,
        Water,
        None;
        
        /**
         * Figures out the relevant entity movement mode (for path caching) based on properties 
         * of the entity. Mostly just movement mode, but some complications exist for tracked/wheeled vehicles.
         */
        public static MovementType getMovementType(Entity entity) {
            switch(entity.getMovementMode()) {
                case BIPED:
                case TRIPOD:
                case QUAD:
                case INF_LEG:
                    return Walker;
                case TRACKED:
                    return entity.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ? TrackedAmphi : Tracked;
                    //technically  MiscType.F_AMPHIBIOUS and MiscType.F_LIMITED_AMPHIBIOUS apply here too, but are not implemented in general
                case WHEELED:
                    return entity.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ? WheeledAmphi : Wheeled;
                case HOVER:
                    return Hover;
                case BIPED_SWIM:
                case QUAD_SWIM:
                case INF_UMU:
                case SUBMARINE:
                case NAVAL:
                case HYDROFOIL:
                    return Water;
                case VTOL:
                    return Flyer;
                default:
                    return None;
            }
        }
    }
    
    private Map<MovementType, Map<Coords, BoardCluster>> movableAreas = new HashMap<>();
    private Map<MovementType, Map<Coords, BoardCluster>> movableAreasWithTerrainReduction = new HashMap<>();

    /**
     * Returns a set of coordinates on a given board edge that intersects with the cluster
     * in which the given entity resides. May return an empty set.
     */
    public Set<Coords> getDestinationCoords(Entity entity, CardinalEdge edge, boolean terrainReduction) {
        CardinalEdge actualEdge = edge;
        if(edge == CardinalEdge.NEAREST_OR_NONE) {
            actualEdge = BoardUtilities.getClosestEdge(entity);
        }
        
        updateMovableAreas(entity);
        
        MovementType movementType = MovementType.getMovementType(entity);
        BoardCluster entityCluster = null;
        
        if(terrainReduction) {
            entityCluster = movableAreasWithTerrainReduction.get(movementType).get(entity.getPosition());
        } else {
            entityCluster = movableAreas.get(movementType).get(entity.getPosition());
        }
        
        if(entityCluster != null) {
            return entityCluster.getIntersectingHexes(actualEdge, entity.getGame().getBoard());
        }
        
        return null;
    }
    
    /**
     * Resets board clusters
     */
    public void clearMovableAreas() {
        movableAreas.clear();
        movableAreasWithTerrainReduction.clear();
    }
    
    /**
     * Updates and stores accessible clusters for the given entity,
     * both for destruction and non-destruction-aware path finding.
     */
    public void updateMovableAreas(Entity entity) {
        MovementType movementType = MovementType.getMovementType(entity);
        
        movableAreas.putIfAbsent(movementType, generateClusters(entity, false));
        movableAreasWithTerrainReduction.putIfAbsent(movementType, generateClusters(entity, true));
    }

    /**
     * Returns accessible clusters for the given entity.
     */
    public Map<Coords, BoardCluster> generateClusters(Entity entity, boolean destructionAware) { 
        Map<Coords, BoardCluster> clusters = new HashMap<>();
        
        // start with (0, 0)
        // for each hex:
        // if hex is not accessible to this entity, move on
        // if hex is accessible, check if its neighbors are in a cluster
        // for each neighbor in a cluster, check if we can move from this hex to the neighbor and back 
        //      if yes, flag for joining neighbor's cluster
        //      note that if any other neighbor is in a different cluster, and we can move back and forth, we merge the two clusters
        // if no neighbors or cannot move back and forth between hex and neighbors, start new cluster
        
        if (entity == null || entity.getGame() == null) {
            return clusters;
        }
        
        IBoard board = entity.getGame().getBoard();
        int clusterID = 0;
        
        boolean isHovercraft = entity.getMovementMode() == EntityMovementMode.HOVER;
        boolean isAmphibious = entity.hasWorkingMisc(MiscType.F_AMPHIBIOUS) ||
                                entity.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ||
                                entity.hasWorkingMisc(MiscType.F_LIMITED_AMPHIBIOUS);
        
        for(int x = 0; x < board.getWidth(); x++) {
            for(int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                
                // hex is either inaccessible
                // or it is inaccessible AND we can't level it, then we move on
                if (entity.isLocationProhibited(c) && 
                        (!destructionAware || (destructionAware && !canLevel(entity, c)))) {
                    continue;
                }
                
                List<Coords> neighborsToJoin = new ArrayList<>();
                BoardCluster biggestNeighbor = null; 
                
                // hex is accessible one way or another
                for(int direction = 0; direction < 6; direction++) {
                    Coords neighbor = c.translated(direction);
                    
                    if(clusters.containsKey(neighbor)) {
                        int neighborElevation = BoardEdgePathFinder.calculateUnitElevationInHex(board.getHex(neighbor), entity, isHovercraft, isAmphibious);
                        int myElevation = BoardEdgePathFinder.calculateUnitElevationInHex(board.getHex(c), entity, isHovercraft, isAmphibious);
                        
                        // if we can't reach from here to the neighbor due to elevation differences, move on
                        int elevationDiff = Math.abs(neighborElevation - myElevation);
                        if(elevationDiff > entity.getMaxElevationChange()) {
                            continue;
                        }
                        
                        // we can "freely" go back and forth between ourselves and the neighbor, so let's join that cluster 
                        neighborsToJoin.add(neighbor);
                        
                        if((biggestNeighbor == null) || 
                                (clusters.get(neighbor).contents.size() > biggestNeighbor.contents.size())) {
                            biggestNeighbor = clusters.get(neighbor);
                        }
                    }
                }
                
                // start up a new cluster if we have no mutually accessible neighbors
                if(neighborsToJoin.isEmpty() || biggestNeighbor == null) {
                    BoardCluster newCluster = new BoardCluster(clusterID++);
                    newCluster.contents.add(c);
                    clusters.put(c, newCluster);
                // otherwise, join an existing cluster, bringing any other mutually accessible neighbors and their clusters with me
                // join the biggest neighbor to reduce shuffling.
                } else {
                    biggestNeighbor.contents.add(c);
                    clusters.put(c, biggestNeighbor);
                    
                    // merge any other clusters belonging to joined neighbors to this cluster
                    for(int neighborIndex = 0; neighborIndex < neighborsToJoin.size(); neighborIndex++) {
                        BoardCluster oldCluster = clusters.get(neighborsToJoin.get(neighborIndex));
                        if(oldCluster == biggestNeighbor) {
                            continue;
                        }
                        
                        for(Coords member : oldCluster.contents) {
                            biggestNeighbor.contents.add(member);
                            clusters.put(member, biggestNeighbor);
                        }
                    }
                    
                }
            }
        }
        
        return clusters;
    }
    
    /**
     * Indicates whether an entity would be able to pass through a given set of coordinates
     * if it were to degrade the terrain there sufficiently.
     */
    private boolean canLevel(Entity entity, Coords c) {
        return BulldozerMovePath.calculateLevelingCost(c, entity) > BulldozerMovePath.CANNOT_LEVEL;
    }
    
    /**
     * A data structure representing a set of coordinates to which an entity can move.
     */
    public static class BoardCluster {
        public Set<Coords> contents = new HashSet<>();
        public int id;
        
        public BoardCluster(int id) {
            this.id = id;
        }
        
        /**
         * Returns a set of coords in the current cluster that intersect the given board edge.
         */
        public Set<Coords> getIntersectingHexes(CardinalEdge edge, IBoard board) {
            int xStart, xEnd, yStart, yEnd;
            switch(edge) {
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
         * Returns a set of coordinates in the current cluster that intersect
         * an arbitrary rectangle.
         */
        public Set<Coords> getIntersectingHexes(int xStart, int xEnd, int yStart, int yEnd) {
            Set<Coords> retVal = new HashSet<>();
            
            for(int x = xStart; x < xEnd; x++) {
                for(int y = yStart; y < yEnd; y++) {
                    Coords coords = new Coords(x, y);
                    if(contents.contains(coords)) {
                        retVal.add(coords);
                    }
                }
            }
            
            return retVal;
        }
    }
}
