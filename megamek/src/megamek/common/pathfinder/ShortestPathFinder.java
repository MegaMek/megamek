package megamek.common.pathfinder;

import java.util.Comparator;
import java.util.Map;

import megamek.common.Aero;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.Tank;

/**
 * Implementation of MovePathFinder designed to find the shortest path between
 * two hexes or finding shortest paths from a single hex to all surrounding.
 * 
 * @author Saginatio
 * 
 */
public class ShortestPathFinder extends MovePathFinder<MovePath> {

    /**
     * Returns true if last processed move path had final position equal to
     * destination.
     * 
     */
    public static class DestinationReachedStopCondition implements StopCondition<MovePath> {
        private final Coords destination;

        public DestinationReachedStopCondition(Coords destination) {
            if (destination == null)
                throw new NullPointerException();
            this.destination = destination;
        }

        public boolean shouldStop(MovePath e) {
            return destination.equals(e.getFinalCoords());
        }
    }

    /**
     * Compares MovePaths based on distance from final position to destination.
     * If those distances are equal then spent movement points are compared.
     * 
     */
    public static class MovePathGreedyComparator implements Comparator<MovePath> {
        private final Coords destination;

        public MovePathGreedyComparator(Coords destination) {
            if (destination == null)
                throw new NullPointerException();
            this.destination = destination;
        }

        /**
         * Compares MovePaths based on distance from final position to
         * destination. If those distances are equal then spent movement points
         * are compared.
         * 
         */
        @Override
        public int compare(MovePath mp1, MovePath mp2) {
            int d1 = mp1.getFinalCoords().distance(destination);
            int d2 = mp2.getFinalCoords().distance(destination);
            if (d1 != d2)
                return d1 - d2;
            else
                return mp1.getMpUsed() - mp2.getMpUsed();

        }
    }

    /**
     * @see #shouldStay(MovePath)
     * 
     */
    public static class MovePathGreedyFilter extends Filter<MovePath> {
        private Coords dest;

        public MovePathGreedyFilter(Coords dest) {
            this.dest = dest;
        }

        /**
         * Returns true if last step reduces distance to destination or if the
         * last step is a turn, get_up... .
         */
        @Override
        public boolean shouldStay(MovePath movePath) {
            if (movePath.length() < 2)
                return true;
            MoveStep prevStep = movePath.getSecondLastStep();
            Coords prevC = prevStep.getPosition();
            int prevDist = dest.distance(prevC), mpDist = dest.distance(movePath.getFinalCoords());
            if (prevDist > mpDist)
                return true;
            if (prevDist == mpDist) {
                //the distance has not changed 
                //if we are in the same hex, then we are changing facing and it's ok.
                return prevC.equals(movePath.getFinalCoords());
            }

            return false;
        }
    }

    /**
     * Relaxes edge by favouring MovePaths that end in a not prone stance.
     * 
     */
    public static class MovePathRelaxer
            implements AbstractPathFinder.EdgeRelaxer<MovePath, MovePath> {
        @Override
        public MovePath doRelax(MovePath v, MovePath e, Comparator<MovePath> comparator) {
            if (v == null)
                return e;

            // We have to be standing to be able to move
            // Maybe I should replace this extra condition with a flag in node(?)
            boolean vprone= v.getFinalProne(), eprone= e.getFinalProne();
            if( vprone != eprone )
                return vprone ? e : null;
            if( !(v.getEntity() instanceof Tank)){
                boolean vhdown= v.getFinalHullDown(), ehdown= e.getFinalHullDown();
                if( vhdown != ehdown )
                    return vhdown ? e : null;
            }

            return comparator.compare(e, v) < 0 ? e : null;
        }
    }

    /**
     * A MovePath comparator that compares movement points spent and distance to
     * destination. If those are equal then MovePaths with more HexesMoved are
     * Preferred. This should considerably speed A* when multiple shortest paths
     * are present.
     * 
     * This comparator is used by A* algorithm.
     * 
     */
    public static class MovePathAStarComparator implements Comparator<MovePath> {
        Coords destination;

        public MovePathAStarComparator(Coords destination) {
            if (destination == null)
                throw new NullPointerException();
            this.destination = destination;
        }

        @Override
        public int compare(MovePath first, MovePath second) {

            int h1 = 0, h2 = 0;
            if (!(first.getEntity() instanceof Aero)) {
                h1 = first.getFinalCoords().distance(destination);
                h2 = second.getFinalCoords().distance(destination);
            } else {
                //we cannot estimate the needed cost for aeros - maybe a facing change cost would be apropiate
            }
            int dd = (first.getMpUsed() + h1) - (second.getMpUsed() + h2);
            if (dd != 0)
                return dd;
            else
                return -(first.getHexesMoved() - second.getHexesMoved());
        }
    }

    private ShortestPathFinder(EdgeRelaxer<MovePath, MovePath> costRelaxer,
            Comparator<MovePath> comparator, final MoveStepType stepType, IGame game) {
        super(costRelaxer, new NextStepsAdjacencyMap(stepType), comparator, game);
    }

    /**
     * Produces a new instance of shortest path between starting points and a
     * destination finder. Algorithm will halt after reaching destination.
     * 
     * Current implementation uses AStar algorithm.
     * 
     * @param destination
     * @param stepType
     * @param game
     */
    public static ShortestPathFinder newInstanceOfAStar(final Coords destination, final MoveStepType stepType, final IGame game) {
        final ShortestPathFinder spf = new ShortestPathFinder(
                new ShortestPathFinder.MovePathRelaxer(),
                new ShortestPathFinder.MovePathAStarComparator(destination),
                stepType, game);

        spf.addStopCondition(new DestinationReachedStopCondition(destination));
        spf.addFilter(new MovePathLegalityFilter(game));
        return spf;
    }

    /**
     * Produces new instance of shortest path searcher. It will find all the
     * shortest paths between starting points that are reachable with at most
     * maxMp move points.
     * 
     * @param maxMP maximum MP that entity can use
     * @param stepType
     * @param game
     */
    public static ShortestPathFinder newInstanceOfOneToAll(final int maxMP, final MoveStepType stepType, final IGame game) {
        final ShortestPathFinder spf =
                new ShortestPathFinder(
                        new ShortestPathFinder.MovePathRelaxer(),
                        new ShortestPathFinder.MovePathMPCostComparator(),
                        stepType, game);
        spf.addFilter(new MovePathLengthFilter(maxMP));
        spf.addFilter(new MovePathLegalityFilter(game));
        return spf;
    }

    /**
     * Constructs a greedy algorithms. It considers only the moves end closer to
     * destination. Ignores legality of the move. Stops after reaching
     * destination.
     */
    public static ShortestPathFinder newInstanceOfGreedy(final Coords destination, final MoveStepType stepType,
            final IGame game) {

        final ShortestPathFinder spf =
                new ShortestPathFinder(new ShortestPathFinder.MovePathRelaxer(),
                        new MovePathGreedyComparator(destination),
                        stepType, game);

        spf.addStopCondition(new DestinationReachedStopCondition(destination));
        spf.addFilter(new MovePathGreedyFilter(destination));
        return spf;
    }

    /**
     * Returns the shortest move path to a hex at given coordinates or
     * {@code null} if none is present. If multiple path are present with
     * different final facings, the minimal one is chosen.
     * 
     * 
     * @param coordinates - the coordinates of the hex
     * @return the shortest move path to hex at given coordinates or
     *         {@code null}
     */
    public MovePath getComputedPath(Coords coordinates) {
        return getCost(coordinates, getComparator());
    }

    /**
     * Returns a map of all computed shortest paths. If multiple paths to a
     * single hex, each with different final facing, are present, then the
     * minimal one is chosen for each hex.
     * 
     * @return a map of all computed shortest paths.
     */
    public Map<Coords, MovePath> getAllComputedPaths() {
        return super.getAllComputedCosts(getComparator());
    }
}
