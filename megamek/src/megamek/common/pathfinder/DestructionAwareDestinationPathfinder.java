package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.Board;
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
        MovePath startPath = new MovePath(entity.getGame(), entity);
        if(entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
            startPath.addStep(MoveStepType.CLIMB_MODE_OFF);
        } else {
            startPath.addStep(MoveStepType.CLIMB_MODE_ON);
        }
        
        additionalCosts = new HashMap<>();

        Comparator<MovePath> movePathComparator = new ShortestPathFinder.MovePathAStarComparator(
                destinationCoords, MoveStepType.FORWARDS, entity.getGame().getBoard());//new AStarComparator(destinationCoords);

        List<MovePath> candidates = new ArrayList<>();
        candidates.add(startPath);

        // a collection of coordinates we've already visited, so we don't loop back.
        Set<Coords> visitedCoords = new HashSet<>();
        visitedCoords.add(startPath.getFinalCoords());
        //longestNonEdgePathCache.put(entity.getPosition(), startPath);
        MovePath bestPath = new MovePath(entity.getGame(), entity);

        while(!candidates.isEmpty()) {
            /*MovePath cachedPath = this.getCachedPathForCoordinates(candidates.get(0).getFinalCoords(), destinationRegion);

            if(cachedPath != null || isOnBoardEdge(candidates.get(0), destinationRegion)) {
                // if we've found a cached path and the length of the current candidate is 1
                // (it's always at least 1 due to adding the climb mode switch explicitly),
                // then we should return the cached path instead
                MovePath returnPath = ((candidates.get(0).length() == 1) && (cachedPath != null)) ? cachedPath : candidates.get(0);

                cacheGoodPath(returnPath, destinationRegion);
                return returnPath;
            }*/

            candidates.addAll(generateChildNodes(candidates.get(0), visitedCoords));

            // if this path moved around more than the current 'longest path', store it, just in case
            //if(candidates.get(0).getHexesMoved() > longestNonEdgePathCache.get(entity.getPosition()).getHexesMoved()) {
            //    longestNonEdgePathCache.put(entity.getPosition(), candidates.get(0));
            //}
            
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
     * Comparator implementation useful in comparing how much closer a given path is to the internal
     * "destination edge" than the other.
     * @author NickAragua
     *
     */
    private class AStarComparator implements Comparator<MovePath> {
        private Coords destination;

        /**
         * Constructor - initializes the destination edge.
         * @param targetRegion Destination edge
         */
        public AStarComparator(Coords destination) {
            this.destination = destination;
        }

        private int gFunction(MovePath path) {
            return path.getMpUsed();
        }
        
        private int hFunction(MovePath path) {
            return path.getFinalCoords().distance(destination);
        }
        
        /**
         * compare the first move path to the second
         * Favors paths that move closer to the destination edge first.
         * in case of tie, favors paths that cost less MP
         */
        public int compare(MovePath first, MovePath second) {
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
