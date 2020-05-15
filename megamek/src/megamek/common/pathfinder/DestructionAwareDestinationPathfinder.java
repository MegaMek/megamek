package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.Board;
import megamek.common.BulldozerMovePath;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;

public class DestructionAwareDestinationPathfinder extends BoardEdgePathFinder {

    public Map<MovePath, Integer> additionalCosts;
    
    /**
     */
    public MovePath findPathToCoords(Entity entity, Coords destinationCoords) {
        BulldozerMovePath startPath = new BulldozerMovePath(entity.getGame(), entity);
        if(entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
            startPath.addStep(MoveStepType.CLIMB_MODE_OFF);
        } else {
            startPath.addStep(MoveStepType.CLIMB_MODE_ON);
        }
        
        additionalCosts = new HashMap<>();

        Comparator<BulldozerMovePath> movePathComparator = /*new ShortestPathFinder.MovePathAStarComparator(
                destinationCoords, MoveStepType.FORWARDS, entity.getGame().getBoard());*/
                new AStarComparator(destinationCoords);

        List<BulldozerMovePath> candidates = new ArrayList<>();
        candidates.add(startPath);

        // a collection of coordinates we've already visited, so we don't loop back.
        Set<Coords> visitedCoords = new HashSet<>();
        visitedCoords.add(startPath.getFinalCoords());
        BulldozerMovePath bestPath = new BulldozerMovePath(entity.getGame(), entity);

        while(!candidates.isEmpty()) {

            candidates.addAll(generateChildNodes(candidates.get(0), visitedCoords));
            
            if(candidates.get(0).getFinalCoords().equals(destinationCoords) &&
                    movePathComparator.compare(bestPath, candidates.get(0)) < 0) {
                bestPath = candidates.get(0);                
            }

            candidates.remove(0);
            candidates.sort(movePathComparator);
        }

        return bestPath;
    }
    
    /**
     * Function that generates all possible "legal" moves resulting from the given path
     * and updates the set of visited coordinates so we don't visit them again.
     * @param parentPath The path for which to generate child nodes
     * @param visitedCoords Set of visited coordinates so we don't loop around
     * @return List of valid children. Between 0 and 3 inclusive.
     */
    protected List<BulldozerMovePath> generateChildNodes(BulldozerMovePath parentPath, Set<Coords> visitedCoords) {
        List<BulldozerMovePath> children = new ArrayList<>();

        // the children of a move path are:
        //      turn left and step forward one
        //      step forward one
        //      turn right and step forward one
        BulldozerMovePath leftChild = (BulldozerMovePath) parentPath.clone();
        leftChild.addStep(MoveStepType.TURN_LEFT);
        leftChild.addStep(MoveStepType.FORWARDS);
        processChild(leftChild, children, visitedCoords);
        
        BulldozerMovePath leftleftChild = (BulldozerMovePath) parentPath.clone();
        leftleftChild.addStep(MoveStepType.TURN_LEFT);
        leftleftChild.addStep(MoveStepType.TURN_LEFT);
        leftleftChild.addStep(MoveStepType.FORWARDS);
        processChild(leftleftChild, children, visitedCoords);

        BulldozerMovePath centerChild = (BulldozerMovePath) parentPath.clone();
        centerChild.addStep(MoveStepType.FORWARDS);
        processChild(centerChild, children, visitedCoords);

        BulldozerMovePath rightChild = (BulldozerMovePath) parentPath.clone();
        rightChild.addStep(MoveStepType.TURN_RIGHT);
        rightChild.addStep(MoveStepType.FORWARDS);
        processChild(rightChild, children, visitedCoords);
        
        BulldozerMovePath rightrightChild = (BulldozerMovePath) parentPath.clone();
        rightrightChild.addStep(MoveStepType.TURN_RIGHT);
        rightrightChild.addStep(MoveStepType.TURN_RIGHT);
        rightrightChild.addStep(MoveStepType.FORWARDS);
        processChild(rightrightChild, children, visitedCoords);

        return children;
    }
    
    /**
     * Helper function that handles logic related to potentially adding a generated child path
     * to the list of child paths.
     */
    protected void processChild(BulldozerMovePath child, List<BulldozerMovePath> children, Set<Coords> visitedCoords) {
        if(!visitedCoords.contains(child.getFinalCoords()) &&
                (isLegalMove((MovePath) child) || child.needsLeveling())) {
            visitedCoords.add(child.getFinalCoords());
            children.add(child);
        }
    }
    
    /**
     * Comparator implementation useful in comparing how much closer a given path is to the internal
     * "destination edge" than the other.
     * @author NickAragua
     *
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

        private int gFunction(BulldozerMovePath path) {
            return path.getMpUsed();
        }
        
        private int hFunction(BulldozerMovePath path) {
            return path.getFinalCoords().distance(destination);
        }
        
        /**
         * compare the first move path to the second
         * Favors paths that move closer to the destination edge first.
         * in case of tie, favors paths that cost less MP
         */
        public int compare(BulldozerMovePath first, BulldozerMovePath second) {
            IBoard board = first.getGame().getBoard();
            boolean backwards = false;//first.getStep(0).getS == MoveStepType.BACKWARDS;
            int h1 = first.getFinalCoords().distance(destination)
                    + ShortestPathFinder.getFacingDiff(first, destination, backwards)
                    + ShortestPathFinder.getLevelDiff(first, destination, board)
                    + ShortestPathFinder.getElevationDiff(first, destination, board, first.getEntity())
                    + additionalCosts.getOrDefault(first, 0);
            int h2 = second.getFinalCoords().distance(destination)
                    + ShortestPathFinder.getFacingDiff(second, destination, backwards)
                    + ShortestPathFinder.getLevelDiff(second, destination, board)
                    + ShortestPathFinder.getElevationDiff(second, destination, board, second.getEntity())
                    + additionalCosts.getOrDefault(second, 0);
    
            int dd = (first.getMpUsed() + h1) - (second.getMpUsed() + h2);
    
            if (dd != 0) {
                return dd;
            } else {
                return first.getHexesMoved() - second.getHexesMoved();
            }           
        }
    }
}
