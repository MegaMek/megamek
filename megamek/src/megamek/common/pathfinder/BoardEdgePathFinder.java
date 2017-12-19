package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.Board;
import megamek.common.Building;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.MovePath;
import megamek.common.Terrains;
import megamek.common.MovePath.MoveStepType;

/**
 * This class is intended to be used to find a (potentially long) legal path 
 * given a movement type from a particular hex to the specified board edge
 * @author NickAragua
 *
 */
public class BoardEdgePathFinder {
    // This is a map that will tell us if a particular coordinate has a move path to a particular edge
    Map<Integer, Map<Coords, MovePath>> edgePathCache;
    
    // This is a map that will tell us the longest non-repeating path available to a particular coordinate
    // Useful in situations where a unit has no possible way to get from the deployment zone to the opposite edge
    // It is accumulated over multiple calls to findPathToEdge()
    // It will basically tell us the available "surface area" from a particular set of coordinates
    Map<Coords, MovePath> longestNonEdgePathCache;
    
    /**
     * Constructor - initializes internal caches
     */
    public BoardEdgePathFinder() {
        edgePathCache = new HashMap<>();
        longestNonEdgePathCache = new HashMap<>();
    }
    
    /**
     * Figures out the "opposite" edge for the given entity.
     * @param entity Entity to evaluate
     * @return the Board.START_ constant representing the "opposite" edge
     */
    private int determineOppositeEdge(Entity entity) {
        IBoard board = entity.getGame().getBoard();
        
        // the easiest part is if the entity is supposed to start on a particular edge. Just return the opposite edge.
        int oppositeEdge = board.getOppositeEdge(entity.getStartingPos());
        if(oppositeEdge != Board.START_NONE) {
            return oppositeEdge;
        }
        
        // otherwise, we determine which edge of the board is closest to current position (using math) and return the "opposite" edge.
        // the lesser of entity position x or y determines the opposite.
        // if they're even, pick one arbitrarily
        
        int edge = Board.START_NONE;
        
        // if x is closer to the west edge and less than the y coordinate, use east edge as opposite
        if(entity.getPosition().getX() < (board.getWidth() / 2) &&
                                entity.getPosition().getX() < entity.getPosition().getY()) {
            edge = Board.START_W;
        }

        // if x is closer to the east edge and greater than the y coordinate, use west edge as opposite
        else if(entity.getPosition().getX() >= (board.getWidth() / 2) &&
                entity.getPosition().getX() > entity.getPosition().getY()) {
            edge = Board.START_E;
        }
        
        // if y is closer to the north edge and greater than the x coordinate, use south edge as opposite
        else if(entity.getPosition().getY() < (board.getHeight() / 2) &&
                entity.getPosition().getY() < entity.getPosition().getX()) {
            edge = Board.START_N;
        }
        else if(entity.getPosition().getY() <= (board.getHeight() / 2) &&
                entity.getPosition().getY() > entity.getPosition().getX()) {
            edge = Board.START_S;
        }

        return board.getOppositeEdge(edge);
    }
    
    /**
     * Helper function to set the entity to an appropriate facing given the destination region
     * Changes the actual entity's facing.
     * @param entity The entity
     * @param destinationRegion The region
     */
    private void setAppropriateFacing(Entity entity, int destinationRegion) {
        switch(destinationRegion) {
        case Board.START_N:
            entity.setFacing(0);
            break;
        case Board.START_S:
            entity.setFacing(3);
            break;
        case Board.START_E:
            if(entity.getPosition().getY() < entity.getGame().getBoard().getHeight() / 2) {
                entity.setFacing(2);
            }
            else {
                entity.setFacing(1);
            }
            break;
        case Board.START_W:
            if(entity.getPosition().getY() < entity.getGame().getBoard().getHeight() / 2) {
                entity.setFacing(4);
            }
            else {
                entity.setFacing(5);
            }
            break;
        }
    }
    
    /**
     * Finds a legal path for the given entity to the "opposite" board edge
     * Completely ignores movement risk
     * Mostly ignores movement cost
     * "opposite" is defined as the cardinal edge furthest from the entity's current location
     * @param entity The entity for which to calculate the path
     * @return A legal move path from the entity's current location to the "opposite" edge (over several turns)
     */
    public MovePath findPathToEdge(Entity entity) {
        int destinationRegion = determineOppositeEdge(entity);
        
        // back up and restore the entity's original facing, as it's not nice to have side effects
        int originalFacing = entity.getFacing();
        setAppropriateFacing(entity, destinationRegion);
        MovePath pathToEdge = findPathToEdge(entity, destinationRegion);
        entity.setFacing(originalFacing);
        
        return pathToEdge;
    }
    
    /**
     * Finds a legal path for the given entity to the given board edge (please pass in a cardinal edge)
     * Completely ignores movement risk
     * Mostly ignores movement cost
     * @param entity The entity for which to calculate the path
     * @param destinationRegion The destination edge
     * @return A legal move path from the entity's current location to the edge (over several turns)
     */
    public MovePath findPathToEdge(Entity entity, int destinationRegion) {
        MovePath startPath = new MovePath(entity.getGame(), entity);
        Comparator<MovePath> movePathComparator = new SortByDistanceToEdge(destinationRegion);
        
        List<MovePath> candidates = new ArrayList<>();
        candidates.add(startPath);
        
        // a collection of coordinates we've already visited, so we don't loop back.
        Set<Coords> visitedCoords = new HashSet<>();
        visitedCoords.add(startPath.getFinalCoords());
        longestNonEdgePathCache.put(entity.getPosition(), startPath);
        
        while(!candidates.isEmpty()) {
            if(isOnBoardEdge(candidates.get(0), destinationRegion) ||
                    coordinatesHaveCachedPath(candidates.get(0).getFinalCoords(), destinationRegion)) {
                cacheGoodPath(candidates.get(0), destinationRegion);
                return candidates.get(0);
            }
            
            candidates.addAll(generateChildNodes(candidates.get(0), visitedCoords));
            
            // if this path moved around more than the current 'longest path', store it, just in case
            if(candidates.get(0).getHexesMoved() > longestNonEdgePathCache.get(entity.getPosition()).getHexesMoved()) {
                longestNonEdgePathCache.put(entity.getPosition(), candidates.get(0));
            }
            
            candidates.remove(0);
            candidates.sort(movePathComparator);
        }
        
        return null;
    }
    
    /**
     * Gets the currently stored longest non-edge path from the given entity's current position
     * @param coords The coordinates to check
     * @return A move path or null if these coordinates haven't been evaluated.
     */
    public MovePath getLongestNonEdgePath(Coords coords) {
        return longestNonEdgePathCache.get(coords);
    }
    
    /**
     * Helper function that tells us if the given set of coordinates have a path cached already
     * @param coords Coordinates to check
     * @param destinationRegion Where we're going
     * @return True or false
     */
    private boolean coordinatesHaveCachedPath(Coords coords, int destinationRegion) {
        return edgePathCache.containsKey(destinationRegion) && edgePathCache.get(destinationRegion).containsKey(coords);
    }
    
    /**
     * Worker function that caches a path that gets to the destination region
     * @param path The path to cache
     * @param destinationRegion The region of the board to which the path moves
     */
    private void cacheGoodPath(MovePath path, int destinationRegion) {
        Map<Coords, MovePath> coordinatePathMap;
        
        if(!edgePathCache.containsKey(destinationRegion)) {
            coordinatePathMap = new HashMap<>();
            edgePathCache.put(destinationRegion, coordinatePathMap);
        }
        else {
            coordinatePathMap = edgePathCache.get(destinationRegion);
        }
        
        // cache the path for the set of coordinates if one doesn't yet exist
        // or if the current path is better than the cached one
        for(Coords coords : path.getCoordsSet()) {
            if(!coordinatePathMap.containsKey(coords) ||
                    coordinatePathMap.get(coords).getMpUsed() > path.getMpUsed()) {
                coordinatePathMap.put(coords, path);
                
            }
        }
    }
    
    /**
     * Function that generates all possible "legal" moves resulting from the given path
     * and updates the set of visited coordinates so we don't visit them again.
     * @param parentPath The path for which to generate child nodes
     * @param visitedCoords Set of visited coordinates so we don't loop around
     * @return List of valid children. Between 0 and 3 inclusive.
     */
    private List<MovePath> generateChildNodes(MovePath parentPath, Set<Coords> visitedCoords) {
        List<MovePath> children = new ArrayList<>();
        
        // the children of a move path are:
        //      turn left and step forward one
        //      step forward one
        //      turn right and step forward one
        MovePath leftChild = parentPath.clone();
        leftChild.addStep(MoveStepType.TURN_LEFT);
        leftChild.addStep(MoveStepType.FORWARDS);
        processChild(leftChild, children, visitedCoords);

        MovePath centerChild = parentPath.clone();
        centerChild.addStep(MoveStepType.FORWARDS);
        processChild(centerChild, children, visitedCoords);
        
        MovePath rightChild = parentPath.clone();
        rightChild.addStep(MoveStepType.TURN_RIGHT);
        rightChild.addStep(MoveStepType.FORWARDS);
        processChild(rightChild, children, visitedCoords);
        
        return children;
    }
    
    /**
     * Helper function that handles logic related to potentially adding a generated child path
     * to the list of child paths.
     */
    private void processChild(MovePath child, List<MovePath> children, Set<Coords> visitedCoords) {
        if(!visitedCoords.contains(child.getFinalCoords()) && isLegalMove(child)) {
            visitedCoords.add(child.getFinalCoords());
            children.add(child);
        }
    }
    
    /**
     * A "light-weight" version of the logic found in "isMovementPossible" in MoveStep.java
     * 
     * @param movePath The move path to process
     * @return Whether or not the given move path is "legal" in the context of this pathfinder.
     */
    private boolean isLegalMove(MovePath movePath) {
        Coords dest = movePath.getFinalCoords();
        IBoard board = movePath.getGame().getBoard();
        IHex destHex = board.getHex(dest);
        Coords src = movePath.getSecondLastStep().getPosition();
        IHex srcHex = board.getHex(src);
        Entity entity = movePath.getEntity();
        
        boolean destinationInBounds = board.contains(dest);
        if(!destinationInBounds) {
            return false;
        }

        // we only need to be able to legally move into the hex from the previous hex. 
        // we don't care about stacking limits, remaining unit mp or other transient data
        
        Building destinationBuilding = board.getBuildingAt(dest);
        
        // quadvees are not considered "tracked" for the purposes of this exercise because they can transform
        boolean isTracked = entity.getMovementMode() == EntityMovementMode.TRACKED && !entity.hasETypeFlag(Entity.ETYPE_QUADVEE);
        boolean isHovercraft = entity.getMovementMode() == EntityMovementMode.HOVER;
        boolean isWheeled = entity.getMovementMode() == EntityMovementMode.WHEELED;
        boolean destHexHasRoad = destHex.containsTerrain(Terrains.ROAD);
        // jumpers can clear higher objects than walkers and crawlers
        int maxUpwardElevationChange = Math.max(entity.getJumpMP(), entity.getMaxElevationChange());
        // jumpers can just hop down wherever they want
        int maxDownwardElevationChange = entity.getJumpMP() > 0 ? 999 : entity.getMaxElevationDown(); 
        int destHexElevation = calculateUnitElevationInHex(destHex, entity);
        int srcHexElevation = calculateUnitElevationInHex(srcHex, entity);
        
        boolean destinationImpassable = destHex.containsTerrain(Terrains.IMPASSABLE);
        boolean destinationHasBuilding = destinationBuilding != null;
        
        // if we're going to step onto a bridge that will collapse, let's not consider going there
        boolean destinationHasWeakBridge = destinationHasBuilding && destHex.containsTerrain(Terrains.BRIDGE_CF) &&
                destinationBuilding.getCurrentCF(dest) > entity.getWeight();
                
        // if we're going to step onto a building that will collapse, let's not consider going there
        boolean destinationHasWeakBuilding = destinationHasBuilding && destHex.containsTerrain(Terrains.BLDG_CF) &&
                destinationBuilding.getCurrentCF(dest) > entity.getWeight();
                
        // this condition indicates that that we are unable to go to the destination because it's too high compared to the source
        boolean goingUpTooHigh = destHexElevation - srcHexElevation > maxUpwardElevationChange; 
        
        // this condition indicates that we are unable to go to the destination because it's too low compared to the source
        boolean goingDownTooLow = srcHexElevation - destHexElevation > maxDownwardElevationChange;
        
        // tanks cannot go into jungles or heavy woods unless there is a road
        boolean tankIntoHeavyWoods = isTracked && 
                (destHex.terrainLevel(Terrains.JUNGLE) > 0 || destHex.terrainLevel(Terrains.WOODS) > 1) && !destHexHasRoad;
        
        // hovercraft and wheeled units cannot go into jungles or woods unless there is a road
        boolean weakTankIntoWoods = (isHovercraft || isWheeled) && 
                (destHex.terrainLevel(Terrains.JUNGLE) > 0 || destHex.terrainLevel(Terrains.WOODS) > 0) && !destHexHasRoad;
        
        // wheeled tanks cannot go into rough terrain or rubble of any kind, or buildings for that matter
        // even if you level them they still turn to rubble. Additionally, they cannot go into deep snow.
        boolean wheeledTankRestriction = isWheeled &&
                (destHex.containsTerrain(Terrains.ROUGH) || destHex.containsTerrain(Terrains.RUBBLE) 
                || destHex.containsTerrain(Terrains.BLDG_CF) || (destHex.containsTerrain(Terrains.SNOW) && destHex.terrainLevel(Terrains.SNOW) > 1));
        
        // tracked and wheeled tanks cannot go into water without a bridge
        boolean groundTankIntoWater = isTracked && !isHovercraft &&
                destHex.containsTerrain(Terrains.WATER) && (destHex.depth() > 0) && !destHex.containsTerrain(Terrains.BRIDGE);
        
        // naval units cannot go out of water
        boolean shipOutofWater = entity.isSurfaceNaval() &&
                (!destHex.containsTerrain(Terrains.WATER) || destHex.depth() < 1);
        
        return !destinationImpassable &&
                !destinationHasWeakBridge &&
                !destinationHasWeakBuilding &&
                !goingUpTooHigh &&
                !goingDownTooLow &&
                !tankIntoHeavyWoods &&
                !weakTankIntoWoods &&
                !wheeledTankRestriction &&
                !groundTankIntoWater &&
                !shipOutofWater;
    }
    
    /**
     * Helper function that calculates the effective elevation for a unit standing there.
     * @param hex The hex to check
     * @param entity The entity to check
     * @return The effective elevation
     */
    private int calculateUnitElevationInHex(IHex hex, Entity entity) {
        // we calculate the height of a hex as the ceiling
        // unless we are naval going under a bridge, in which case the height is the water level (naval units go on the surface, mostly)
        // unless we are infantry not going onto a bridge, in which case the height is the floor (infantry don't climb up buildings)
        // unless we are non-infantry, non-naval going into water but not onto a bridge, in which case the height is the floor (mechs sink to the bottom)
        
        int hexElevation = hex.ceiling();
        if(entity.hasETypeFlag(Entity.ETYPE_INFANTRY) && !hex.containsTerrain(Terrains.BRIDGE)) {
            hexElevation = hex.floor();
        } else if(entity.isSurfaceNaval() && hex.containsTerrain(Terrains.BRIDGE)) { 
            hexElevation = hex.getLevel();
        } else if(!entity.hasETypeFlag(Entity.ETYPE_INFANTRY) && !entity.isSurfaceNaval() 
                && hex.containsTerrain(Terrains.WATER) && !hex.containsTerrain(Terrains.BRIDGE)) {
            hexElevation = hex.floor();
        }
        
        return hexElevation;
    }
    
    /**
     * Determines if the given move path ends on the given board edge
     * @param movePath The move path to check.
     * @param edge The edge to check for.
     * @return True or false.
     */
    private boolean isOnBoardEdge(MovePath movePath, int destinationRegion) {
        Coords coords = movePath.getFinalCoords();
        
        switch(destinationRegion) {
        case Board.START_N:
            return coords.getY() == 0;
        case Board.START_S:
            return coords.getY() == movePath.getGame().getBoard().getHeight() - 1;
        case Board.START_E:
            return coords.getX() == movePath.getGame().getBoard().getWidth() - 1;
        case Board.START_W:
            return coords.getX() == 0;
        default:
            return false;
        }
    }
    
    /**
     * Comparator implementation useful in comparing how much closer a given path is to the internal
     * "destination edge" than the other.
     * @author NickAragua
     *
     */
    private class SortByDistanceToEdge implements Comparator<MovePath> {
        private int targetRegion;
        
        /**
         * Constructor - initializes the destination edge.
         * @param targetRegion Destination edge
         */
        public SortByDistanceToEdge(int targetRegion) {
            this.targetRegion = targetRegion;
        }
        
        /**
         * compare the first move path to the second
         * Favors paths that move closer to the destination edge first. 
         * in case of tie, favors paths that cost less MP
         */
        public int compare(MovePath first, MovePath second) {            
            // normalize MP cost difference over max MP cost
            int costDifference = first.getMpUsed() - second.getMpUsed();
            int distanceDifference; 
            
            switch(targetRegion) {
            // if we're heading south, the one with the bigger y coordinate is further along
            case Board.START_S:
                distanceDifference = second.getFinalCoords().getY() - first.getFinalCoords().getY();
                break;
            // if we're heading north, the one with the smaller y coordinate is further along
            case Board.START_N:
                distanceDifference = first.getFinalCoords().getY() - second.getFinalCoords().getY();
                break;
            // if we're heading east, the one with the bigger x coordinate is further along
            case Board.START_E:
                distanceDifference = second.getFinalCoords().getX() - first.getFinalCoords().getX();
                break;
            // if we're heading west, the one with the smaller x coordinate is further along
            case Board.START_W:
                distanceDifference = first.getFinalCoords().getX() - second.getFinalCoords().getX();
                break;
            default:
                distanceDifference = 0;
                break;
            }
            
            return distanceDifference != 0 ? distanceDifference : costDifference;
        }
    }
}
