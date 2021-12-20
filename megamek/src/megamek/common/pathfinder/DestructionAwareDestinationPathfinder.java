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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.*;
import megamek.common.MovePath.MoveStepType;

/**
 * Handles the generation of ground-based move paths that contain information relating to the destruction 
 * of terrain necessary to accomplish that path.
 */
public class DestructionAwareDestinationPathfinder extends BoardEdgePathFinder {

    private Comparator<BulldozerMovePath> movePathComparator;
    private int maximumCost = Integer.MAX_VALUE;
    private Map<Coords, Boolean> friendlyFireCheckResults = new HashMap<>();
    
    /**
     * Uses an A* search to find the "optimal" path to the destination coordinates.
     * Ignores move cost and makes note of hexes that need to be cleared for the path to
     * be viable.
     */
    public BulldozerMovePath findPathToCoords(Entity entity, Set<Coords> destinationCoords, BoardClusterTracker clusterTracker) {
        return findPathToCoords(entity, destinationCoords, false, clusterTracker);
    }
    
    /**
     * Uses an A* search to find the "optimal" path to the destination coordinates.
     * Ignores move cost and makes note of hexes that need to be cleared for the path to
     * be viable.
     */
    public BulldozerMovePath findPathToCoords(Entity entity, Set<Coords> destinationCoords, boolean jump, BoardClusterTracker clusterTracker) {
        BulldozerMovePath startPath = new BulldozerMovePath(entity.getGame(), entity);
        
        // if we're calculating a jump path and the entity has jump mp and can jump, start off with a jump
        // if we're trying to calc a jump path and the entity does not have jump mp, we're done
        if (jump && (startPath.getCachedEntityState().getJumpMPWithTerrain() > 0) &&
                !entity.isProne() && !entity.isHullDown() && 
                (entity.getGame().getPlanetaryConditions().getWindStrength() != PlanetaryConditions.WI_TORNADO_F4)) {
            startPath.addStep(MoveStepType.START_JUMP);
        // if we specified a jump path, but can't actually jump
        } else if (jump) {
            return null;
        // can't "climb into" anything while jumping
        } else { 
            if (entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
                startPath.addStep(MoveStepType.CLIMB_MODE_OFF);
            } else {
                startPath.addStep(MoveStepType.CLIMB_MODE_ON);
            }
        }
        
        // if we're on the ground, let's try to get up first before moving 
        if (entity.isProne() || entity.isHullDown()) {
            startPath.addStep(MoveStepType.GET_UP);
            
            // if we can't even get up, no need to do anything else
            if (!startPath.isMoveLegal()) {
                return null;
            }
        }

        Coords closest = getClosestCoords(destinationCoords, entity);
        // if we can't at all get to the coordinates with this entity, don't bother with the rest 
        if (closest == null) {
            return null;
        }
        
        movePathComparator = new AStarComparator(closest);
        maximumCost = Integer.MAX_VALUE;
        
        TreeSet<BulldozerMovePath> candidates = new TreeSet<>(movePathComparator);
        candidates.add(startPath);

        // a collection of coordinates we've already visited, so we don't loop back.
        Map<Coords, BulldozerMovePath> shortestPathsToCoords = new HashMap<>();
        shortestPathsToCoords.put(startPath.getFinalCoords(), startPath);
        BulldozerMovePath bestPath = null;

        while (!candidates.isEmpty()) {
            BulldozerMovePath currentPath = candidates.pollFirst();
            
            candidates.addAll(generateChildNodes(currentPath, shortestPathsToCoords, clusterTracker, closest));      
            
            if (destinationCoords.contains(currentPath.getFinalCoords()) &&
                    ((bestPath == null) || (movePathComparator.compare(bestPath, currentPath) > 0))) {
                bestPath = currentPath;
                maximumCost = bestPath.getMpUsed() + bestPath.getLevelingCost();
            }
        }
  
        return bestPath;
    }
    
    /**
     * Calculates the closest coordinates to the given entity
     * Coordinates which you have to blow up to get into are considered to be further
     */
    public static Coords getClosestCoords(Set<Coords> destinationRegion, Entity entity) {
        Coords bestCoords = null;
        int bestDistance = Integer.MAX_VALUE;
        
        for (Coords coords : destinationRegion) {
            if (!entity.getGame().getBoard().contains(coords)) {
                continue;
            }
            
            int levelingCost = BulldozerMovePath.calculateLevelingCost(coords, entity);
            boolean canLevel = levelingCost > BulldozerMovePath.CANNOT_LEVEL;
            
            if (!entity.isLocationProhibited(coords) || canLevel) {
                int distance = coords.distance(entity.getPosition()) + (canLevel ? levelingCost : 0);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestCoords = coords;
                }
            }
        }
        
        return bestCoords;
    }
    
    /**
     * Function that generates all possible "legal" moves resulting from the given path
     * and updates the set of visited coordinates so we don't visit them again.
     * @param parentPath The path for which to generate child nodes
     * @param visitedCoords Set of visited coordinates so we don't loop around
     * @return List of valid children. Between 0 and 3 inclusive.
     */
    protected List<BulldozerMovePath> generateChildNodes(BulldozerMovePath parentPath, Map<Coords, BulldozerMovePath> shortestPathsToCoords,
            BoardClusterTracker clusterTracker, Coords destinationCoords) {
        List<BulldozerMovePath> children = new ArrayList<>();

        // there are six possible children of a move path, defined in AeroPathUtil.TURNS
        for (List<MoveStepType> turns : AeroPathUtil.TURNS) {
            BulldozerMovePath childPath = (BulldozerMovePath) parentPath.clone();
            
            // apply the list of turn steps
            for (MoveStepType stepType : turns) {
                childPath.addStep(stepType);
            }
            
            // potentially apply UP so we can hop over unwanted terrain
            PathDecorator.AdjustElevationForForwardMovement(childPath);
            
            // move forward and process the generated child path
            childPath.addStep(MoveStepType.FORWARDS);
            processChild(childPath, children, shortestPathsToCoords, clusterTracker, destinationCoords);
        }

        return children;
    }
    
    /**
     * Helper function that handles logic related to potentially adding a generated child path
     * to the list of child paths.
     */
    protected void processChild(BulldozerMovePath child, List<BulldozerMovePath> children, 
            Map<Coords, BulldozerMovePath> shortestPathsToCoords, BoardClusterTracker clusterTracker, Coords destinationCoords) {
        // (if we haven't visited these coordinates before
        // or we have, and this is a shorter path)
        // and (it is a legal move
        // or it needs some "terrain adjustment" to become a legal move)
        // and we haven't already found a path to the destination that's cheaper than what we're considering
        // and we're not going off board 
        MoveLegalityIndicator mli = isLegalMove((MovePath) child);
        
        // if this path goes through terrain that can be leveled
        // but has other problems with it (e.g. elevation change, or the "reduced" terrain still won't let you through)
        // it still can't be leveled
        boolean canLevel = child.needsLeveling() &&
                !mli.outOfBounds &&
                !mli.destinationImpassable &&
                !mli.goingDownTooLow &&
                !mli.goingUpTooHigh &&
                !mli.wheeledTankRestriction &&
                !mli.destinationHasWeakBridge &&
                !mli.groundTankIntoWater;
        
        // legal jump moves are simpler:
        // can't go out of bounds, can't jump too high (unless we can destroy the obstacle)
        boolean legalJumpMove = child.isJumping() &&
                !mli.outOfBounds &&
                (!mli.goingUpTooHigh || child.needsLeveling());
        
        // this is true if we're jumping down further down than our max jump MP
        // or jumping down lower than our max elevation change
        boolean irreversibleJumpDown = 
                (child.isJumping() && (Math.abs(mli.elevationChange) > child.getCachedEntityState().getJumpMP())) ||
                (child.getEntity().getMaxElevationDown() == Entity.UNLIMITED_JUMP_DOWN) && (Math.abs(mli.elevationChange) > child.getEntity().getMaxElevationChange());
        
        boolean destinationUseBridge = child.getGame().getBoard().getHex(destinationCoords).getTerrain(Terrains.BRIDGE) != null;
        int destHexElevation = calculateUnitElevationInHex(child.getGame().getBoard().getHex(destinationCoords), 
                child.getEntity(), child.getEntity().getMovementMode() == EntityMovementMode.HOVER, child.getCachedEntityState().isAmphibious(),
                destinationUseBridge);
        
        // if we jumped into a hole and this results into us moving into a different cluster than the destination,
        // that's not great and we should not consider the possibility for long range path finding.
        if (irreversibleJumpDown && 
                !clusterTracker.coordinatesShareCluster(child.getEntity(), child.getFinalCoords(), destinationCoords,
                        mli.destHexElevation, destHexElevation)) {
            return;
        }
        
        // let's avoid pathing through buildings containing our immobile units - 
        // they're not going to get out of the way and we can probably do better than killing our own guys
        if (!child.isJumping() && friendlyFireCheck(child.getEntity(), child.getGame(), child.getFinalCoords(), false)) {
            return;
        }
        
        // if we have a leg armor breach and are not jumping, let's not consider it
        if (!child.isJumping() && underwaterLegBreachCheck(child)) {
            return;
        }
        
        if ((!shortestPathsToCoords.containsKey(child.getFinalCoords()) ||
                // shorter path to these coordinates
                (movePathComparator.compare(shortestPathsToCoords.get(child.getFinalCoords()), child) > 0)) &&
                // legal or needs leveling or jumping and not off-board
                (mli.isLegal() || canLevel || legalJumpMove) &&
                // better than existing path to ultimate destination
                (child.getMpUsed() + child.getLevelingCost() < maximumCost)) {
            shortestPathsToCoords.put(child.getFinalCoords(), child);
            children.add(child);
        }
    }
    
    /**
     * Utility function that returns true if an attack on the building in the given coordinates
     * will result in damage to friendly units. Computation is cached as it is somewhat expensive to perform for each possible path node.
     */
    private boolean friendlyFireCheck(Entity shooter, Game game, Coords position, boolean includeMobileUnits) {
        if (friendlyFireCheckResults.containsKey(position)) {
            return friendlyFireCheckResults.get(position);
        }
        
        Building building = game.getBoard().getBuildingAt(position);
        
        // no building, no problem
        if (building == null) {
            friendlyFireCheckResults.put(position, false);
            return false;
        }
        
        // check if there are any entities in the building that meet the following criteria:
        // - is friendly
        // - if we care only about mobile units, has no MP 
        for (Entity entity : game.getEntitiesVector(position, true)) {
            if (!entity.isEnemyOf(shooter) && (includeMobileUnits || (entity.getWalkMP(true, false) == 0))) {
                friendlyFireCheckResults.put(position, true);
                return true;
            }
        }
        
        friendlyFireCheckResults.put(position, false);
        return false;
    }
    
    /**
     * Simplified logic for whether a mech going into the given hex will flood 
     * breached legs and effectively immobilize it.
     */
    private boolean underwaterLegBreachCheck(BulldozerMovePath path) {        
        Hex hex = path.getGame().getBoard().getHex(path.getFinalCoords());
        
        // investigate: do we want quad mechs with a single breached leg
        // to risk this move? Currently not, but if we did, this is probably where
        // this logic would go.
        return path.getCachedEntityState().getNumBreachedLegs() > 0 && 
                hex != null &&
                hex.terrainLevel(Terrains.WATER) > 0;
    }
    
    /**
     * Comparator implementation useful in comparing how much closer a given path is to the internal
     * "destination edge" than the other.
     * @author NickAragua
     */
    private class AStarComparator implements Comparator<BulldozerMovePath> {
        private Coords destination;

        /**
         * Constructor - initializes the destination edge.
         * @param targetRegion Destination edge
         */
        public AStarComparator(Coords destination) {
            this.destination = destination;
        }
        
        /**
         * compare the first move path to the second
         * Favors paths that move closer to the destination edge first.
         * in case of tie, favors paths that cost less MP
         */
        @Override
        public int compare(BulldozerMovePath first, BulldozerMovePath second) {
            Board board = first.getGame().getBoard();
            boolean backwards = false;
            int h1 = first.getFinalCoords().distance(destination)
                    + ShortestPathFinder.getLevelDiff(first, destination, board, false)
                    + ShortestPathFinder.getElevationDiff(first, destination, board, first.getEntity());
            int h2 = second.getFinalCoords().distance(destination)
                    + ShortestPathFinder.getLevelDiff(second, destination, board, false)
                    + ShortestPathFinder.getElevationDiff(second, destination, board, second.getEntity());
    
            int dd = (first.getMpUsed() + first.getLevelingCost() + first.getAdditionalCost() + h1) 
                    - (second.getMpUsed() + second.getLevelingCost() + second.getAdditionalCost() + h2);
            
            // getFacingDiff returns a number between 0 and 3 inclusive. 
            // if the value diff is larger than 3, then it won't make a difference and we skip calculating it
            if (Math.abs(dd) < 4) {
                dd *= 10; // facing diff doesn't matter as much as the other stuff, only use it as a tie-breaker
                dd += ShortestPathFinder.getFacingDiff(first, destination, backwards);
                dd -= ShortestPathFinder.getFacingDiff(second, destination, backwards);
            }
    
            // dd != 0 implies that the two paths are not identical
            if (dd != 0) {
                return dd;
            } else {
                int tieBreakerDiff = first.getHexesMoved() - second.getHexesMoved();
                if (tieBreakerDiff == 0) {
                    tieBreakerDiff = first.getMpUsed() - second.getMpUsed();
                }
                
                return tieBreakerDiff;
            }
        }
    }
}
