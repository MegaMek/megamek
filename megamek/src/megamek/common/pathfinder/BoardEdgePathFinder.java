package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;

import megamek.client.bot.princess.HomeEdge;
import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.EntityMovementMode;
import megamek.common.Hex;
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
    public MovePath FindPathToEdge(MovePath pathToHere, int destinationRegion) {
        Comparator<MovePath> movePathComparator = new SortByDistanceToEdge(destinationRegion);
        
        List<MovePath> candidates = new ArrayList<>();
        candidates.add(pathToHere);
        
        while(!candidates.isEmpty()) {
            if(isOnBoardEdge(candidates.get(0), destinationRegion)) {
                return candidates.get(0);
            }
            
            candidates.addAll(GenerateChildNodes(candidates.get(0)));
            candidates.remove(0);
            candidates.sort(movePathComparator);
        }
        
        return pathToHere;
    }
    
    private List<MovePath> GenerateChildNodes(MovePath parentPath) {
        List<MovePath> children = new ArrayList<>();
        
        // the children of a move path are:
        //      turn left and step forward one
        //      step forward one
        //      turn right and step forward one
        MovePath leftChild = parentPath.clone();
        leftChild.addStep(MoveStepType.TURN_LEFT);
        leftChild.addStep(MoveStepType.FORWARDS);
        if(isLegalMove(leftChild)) {
            children.add(leftChild);
        }

        MovePath centerChild = parentPath.clone();
        centerChild.addStep(MoveStepType.FORWARDS);
        if(isLegalMove(centerChild)) {
            children.add(centerChild);
        }
        
        MovePath rightChild = parentPath.clone();
        rightChild.addStep(MoveStepType.TURN_RIGHT);
        rightChild.addStep(MoveStepType.FORWARDS);
        if(isLegalMove(rightChild)) {
            children.add(rightChild);
        }
        
        //rightChild.getFinalCoords().is
        
        return children;
    }
    
    /**
     * A "light-weight" version of the logic found in "isMovementPossible" in MoveStep.java
     * 
     * @param movePath
     * @return
     */
    private boolean isLegalMove(MovePath movePath) {
        Coords dest = movePath.getFinalCoords();
        IBoard board = movePath.getGame().getBoard();
        IHex destHex = board.getHex(dest);
        
        
        boolean destinationInBounds = board.contains(dest);
        if(!destinationInBounds) {
            return false;
        }
        
        boolean destinationImpassable = destHex.containsTerrain(Terrains.IMPASSABLE);
        boolean destinationHasBuilding = board.getBuildingAt(dest) != null;
        
        // we only need to be able to legally move into the hex from the previous hex. 
        // we don't care about stacking limits, remaining unit mp or other transient data
        
        // some criteria:
        // destination has to be on the board
        // destination has to not be "impassable"
        // mechs can't walk up more than two elevation
        // mechs can't hop down more than two elevation unless "leaping" rule is turned on
        // tanks can only go up or down one elevation
        // tanks can't go into depth 1 water
        // tanks can't go into heavy woods unless there's a road
        // wheeled tanks can't go into rough terrain, rubble, woods unless there's a road
        // hovercraft can't go into woods, jungle unless there's also a road
        // naval units can't go onto land or depth 0 water
        // non-infantry units "can't" go into buildings
        
        return !destinationImpassable &&
                !destinationHasBuilding;
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
            return coords.getX() == 0;
        case Board.START_W:
            return coords.getX() == movePath.getGame().getBoard().getWidth() -1;
        default:
            return false;
        }
    }
    
    private class SortByDistanceToEdge implements Comparator<MovePath> {
        private int targetRegion;
        
        public SortByDistanceToEdge(int targetRegion) {
            this.targetRegion = targetRegion;
        }
        
        /**
         * compare the first move path to the second
         * Favors paths that move closer to the destination edge first. 
         * in case of tie, favors paths that cost less
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
