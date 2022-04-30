package megamek.common.pathfinder;

import megamek.common.*;
import megamek.common.MovePath.MoveStepType;
import org.apache.logging.log4j.LogManager;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of MovePathFinder designed to find the shortest path between
 * two hexes or finding shortest paths from a single hex to all surrounding.
 * 
 * @author Saginatio
 */
public class ShortestPathFinder extends MovePathFinder<MovePath> {

    /**
     * Returns true if last processed move path had final position equal to
     * destination.
     */
    public static class DestinationReachedStopCondition implements StopCondition<MovePath> {
        private final Coords destination;

        public DestinationReachedStopCondition(Coords destination) {
            this.destination = Objects.requireNonNull(destination);
        }

        @Override
        public boolean shouldStop(MovePath e) {
            return destination.equals(e.getFinalCoords());
        }
    }

    /**
     * Compares MovePaths based on distance from final position to destination.
     * If those distances are equal then spent movement points are compared.
     */
    public static class MovePathGreedyComparator implements Comparator<MovePath> {
        private final Coords destination;

        public MovePathGreedyComparator(Coords destination) {
            this.destination = Objects.requireNonNull(destination);
        }

        /**
         * Compares MovePaths based on distance from final position to
         * destination. If those distances are equal then spent movement points
         * are compared.
         */
        @Override
        public int compare(MovePath mp1, MovePath mp2) {
            int d1 = mp1.getFinalCoords().distance(destination);
            int d2 = mp2.getFinalCoords().distance(destination);
            if (d1 != d2) {
                return d1 - d2;
            } else {
                return mp1.getMpUsed() - mp2.getMpUsed();
            }
        }
    }

    /**
     * @see #shouldStay(MovePath)
     */
    public static class MovePathGreedyFilter extends Filter<MovePath> {
        private Coords dest;

        public MovePathGreedyFilter(Coords dest) {
            this.dest = dest;
        }

        /**
         * Returns true if last step reduces distance to destination or if the
         * last step is a turn, get_up...
         */
        @Override
        public boolean shouldStay(MovePath movePath) {
            if (movePath.length() < 2) {
                return true;
            }
            MoveStep prevStep = movePath.getSecondLastStep();
            Coords prevC = prevStep.getPosition();
            int prevDist = dest.distance(prevC), mpDist = dest.distance(movePath.getFinalCoords());
            if (prevDist > mpDist) {
                return true;
            } else if (prevDist == mpDist) {
                // the distance has not changed
                // if we are in the same hex, then we are changing facing and it's ok.
                return prevC.equals(movePath.getFinalCoords());
            }

            return false;
        }
    }

    /**
     * Relaxes edge by favouring MovePaths that end in a not prone stance.
     */
    public static class MovePathRelaxer
            implements AbstractPathFinder.EdgeRelaxer<MovePath, MovePath> {
        @Override
        public MovePath doRelax(MovePath v, MovePath e, Comparator<MovePath> comparator) {
            if (v == null) {
                return e;
            }

            // We have to be standing to be able to move
            // Maybe I should replace this extra condition with a flag in node(?)
            boolean vprone = v.getFinalProne(), eprone = e.getFinalProne();
            if (vprone != eprone) {
                return vprone ? e : null;
            }
            if (!(v.getEntity() instanceof Tank)) {
                boolean vhdown = v.getFinalHullDown(), ehdown = e.getFinalHullDown();
                if (vhdown != ehdown) {
                    return vhdown ? e : null;
                }
            }

            return comparator.compare(e, v) < 0 ? e : null;
        }
    }
    
    /**
     * Relaxes edge based on the supplied comparator, with special
     * considerations for flying off the map (since this will likely always look
     * back to the comparator).
     */
    public static class AeroMovePathRelaxer
            implements AbstractPathFinder.EdgeRelaxer<MovePath, MovePath> {
        @Override
        public MovePath doRelax(MovePath v, MovePath e, Comparator<MovePath> comparator) {
            if (v == null) {
                return e;
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
     */
    public static class MovePathAStarComparator implements Comparator<MovePath>, Serializable {
        private static final long serialVersionUID = -2116704925028576850L;
        Coords destination;
        MoveStepType stepType;
        Board board;

        public MovePathAStarComparator(Coords destination, MoveStepType stepType, Board board) {
            this.destination = Objects.requireNonNull(destination);
            this.stepType = stepType;
            this.board = board;
        }

        @Override
        public int compare(MovePath first, MovePath second) {
            int h1 = 0, h2 = 0;
            // We cannot estimate the needed cost for aeros
            // However, DropShips basically follow ground movement rules
            if ((first.getEntity().isAero()) 
                    && !((IAero) first.getEntity()).isSpheroid()) {
                // We want to pick paths that use fewer MP, and are also shorter
                // unlike ground units which could benefit from better target
                // movement modifiers for longer paths
                int dd = (first.getMpUsed() + h1) - (second.getMpUsed() + h2);
                if (dd != 0) {
                    return dd;
                } else {
                    // Pick the shortest path
                    int hexesMovedDiff = first.getHexesMoved() - second.getHexesMoved();
                    if (hexesMovedDiff != 0) {
                        return hexesMovedDiff;
                    }
                    // If both are the same length, pick one with fewer steps
                    return (first.length() - second.length());
                }
            } else if (first.getEntity().getWalkMP() == 0) {
                // current implementation of movement cost allows a 0mp moves
                // for units with 0 mp.
            } else {
                boolean backwards = stepType == MoveStepType.BACKWARDS;
                h1 = first.getFinalCoords().distance(destination)
                        + getFacingDiff(first, destination, backwards)
                        + getLevelDiff(first, destination, board, first.isJumping())
                        + getElevationDiff(first, destination, board,
                                first.getEntity());
                h2 = second.getFinalCoords().distance(destination)
                        + getFacingDiff(second, destination, backwards)
                        + getLevelDiff(second, destination, board, second.isJumping())
                        + getElevationDiff(second, destination, board,
                                second.getEntity());
            }

            int dd = (first.getMpUsed() + h1) - (second.getMpUsed() + h2);

            if (dd != 0) {
                return dd;
            } else {
                return first.getHexesMoved() - second.getHexesMoved();
            }
        }
    }

    private ShortestPathFinder(EdgeRelaxer<MovePath, MovePath> costRelaxer,
            Comparator<MovePath> comparator, final MoveStepType stepType, Game game) {
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
     * @param game The current {@link Game}
     */
    public static ShortestPathFinder newInstanceOfAStar(final Coords destination,
                                                        final MoveStepType stepType, final Game game) {
        final ShortestPathFinder spf = new ShortestPathFinder(
                new ShortestPathFinder.MovePathRelaxer(),
                new ShortestPathFinder.MovePathAStarComparator(destination, stepType, game.getBoard()),
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
     * @param game The current {@link Game}
     */
    public static ShortestPathFinder newInstanceOfOneToAll(final int maxMP, final MoveStepType stepType, final Game game) {
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
            final Game game) {

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

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return getPathCostMap().values();
    }
    
    public static int getFacingDiff(final MovePath mp, Coords dest,
            boolean backward) {
        // Facing doesn't matter for jumping
        if (mp.isJumping()) {
            return 0;
        }

        // Ignore facing if we are on the destination
        if (mp.getFinalCoords().equals(dest)) {
            return 0;
        }

        // Direction dest hex is from current location, in hex facings
        int destDir = mp.getFinalCoords().direction(dest);
        // Direction dest hex is from current location, in degrees
        int destAngle = mp.getFinalCoords().degree(dest);
        // Estimate the number of facing changes to reach destination
        int firstFacing = Math.abs(((destDir + (backward ? 3 : 0)) % 6)
                - mp.getFinalFacing());

        // Adjust for circular nature of facing
        if (firstFacing > 3) {
            firstFacing = 6 - firstFacing;
        }

        // Ability to shift can ease facing costs
        if (mp.canShift()) {
            firstFacing = Math.max(0, firstFacing - 1);
        }

        // If the angle to the goal doesn't fall along a hex facing, then we
        // need to make another facing change, which needs to be accounted for
        if ((destAngle % 60) != 0) {
            firstFacing++;
        }
        return firstFacing;
    }
    
    /**
     * Computes the difference in levels between the current location and the
     * goal location. This prevents the heuristic from under-estimating when a
     * unit is on top of a hill.
     * 
     * @param mp MovePath to evaluate
     * @param dest Destination coordinates
     * @param board Board on which the move path takes place
     * @param ignore Whether to ignore this calculation and return 0
     * @return level difference between the final coordinates of the given move path and the destination coordinates
     */
    public static int getLevelDiff(final MovePath mp, Coords dest, Board board, boolean ignore) {
        // Ignore level differences if we're not on the ground
        if (ignore || !board.onGround() || (mp.getFinalElevation() != 0)) {
            return 0;
        }
        Hex currHex = board.getHex(mp.getFinalCoords());
        if (currHex == null) {
            LogManager.getLogger().debug("getLevelDiff: currHex was null!" +
                    "\nStart: " + mp.getStartCoords() +
                    "\ncurrHex:  " + mp.getFinalCoords() +
                    "\nPath: " + mp);
            return 0;
        }
        Hex destHex = board.getHex(dest);
        if (destHex == null) {
            LogManager.getLogger().debug("getLevelDiff: destHex was null!" +
                    "\nStart: " + mp.getStartCoords() +
                    "\ndestHex: " + dest +
                    "\nPath: " + mp);
            return 0;
        }
        return Math.abs(destHex.getLevel() - currHex.getLevel());
    }
    
    /**
     * Computes the difference in elevation between the current location and 
     * the goal location.  This is important for using determining when
     * elevation change steps should be used.
     * 
     * @param mp
     * @param dest
     * @param board
     * @param ent
     * @return
     */
    public static int getElevationDiff(final MovePath mp, Coords dest, Board board, Entity ent) {
        // We can ignore this if we're jumping
        if (mp.isJumping()) {
            return 0;
        }
        Hex destHex = board.getHex(dest);
        int currElevation = mp.getFinalElevation();
        // Get elevation in destination hex, ignoring buildings
        int destElevation = ent.elevationOccupied(destHex,
                mp.getFinalElevation());
        // If there's a building, we could stand on it
        if (destHex.containsTerrain(Terrains.BLDG_ELEV)) {
            // Assume that we stay on same level if building is high enough
            if (destHex.terrainLevel(Terrains.BLDG_ELEV) >= currElevation) {
                destElevation = currElevation;
            }
        // If there's a bridge, we could stand on it            
        } else if (destHex.containsTerrain(Terrains.BRIDGE_ELEV)) {
            // Assume that we stay on same level if bridge is high enough
            if (destHex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation) {
                destElevation = currElevation;
            }
        }
        int elevationDiff = Math.abs(currElevation - destElevation);
        // Infantry elevation changes are doubled 
        if (ent instanceof Infantry) {
            elevationDiff *= 2;
        }
        // Penalty for standing
        if (ent.isProne() && !(mp.contains(MoveStepType.GET_UP)
                || mp.contains(MoveStepType.CAREFUL_STAND))) {
            elevationDiff += 2;
        }
        return elevationDiff;
    }    
}
