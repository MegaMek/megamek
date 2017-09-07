package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

import megamek.client.bot.princess.FireControl;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Facing;
import megamek.common.IAero;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.UnitType;
import megamek.common.MovePath.MoveStepType;
import megamek.common.pathfinder.AbstractPathFinder.AdjacencyMap;
import megamek.common.pathfinder.AbstractPathFinder.DestinationMap;
import megamek.common.pathfinder.AbstractPathFinder.EdgeRelaxer;
import megamek.common.pathfinder.AbstractPathFinder.Filter;
import megamek.common.pathfinder.MovePathFinder.CoordsWithFacing;
import megamek.common.pathfinder.MovePathFinder.MovePathLegalityFilter;
import megamek.common.pathfinder.MovePathFinder.MovePathVelocityFilter;
import megamek.common.pathfinder.MovePathFinder.NextStepsAdjacencyMap;
import megamek.common.weapons.DiveBombAttack;

/**
 * This set of classes is intended for use for pathfinding by aerodyne units on ground maps with an atmosphere
 * Usage anywhere else may result in "unpredictable" behavior
 * 
 * @author NickAragua
 *
 */
public class AeroGroundPathFinder extends AbstractPathFinder<CoordsWithFacing, MovePath, MovePath> {
    
    private IGame game;
    private AeroGroundOffBoardFilter offBoardFilter;
    private List<MovePath> aeroGroundPaths;
    
    private AeroGroundPathFinder(DestinationMap<CoordsWithFacing, MovePath> edgeDestinationMap, 
            EdgeRelaxer<MovePath, MovePath> edgeRelaxer, AdjacencyMap<MovePath> edgeAdjacencyMap, Comparator<MovePath> edgeComparator, IGame game) {
        super(edgeDestinationMap, edgeRelaxer, edgeAdjacencyMap, edgeComparator);
        this.offBoardFilter = new AeroGroundOffBoardFilter();
        this.addFilter(offBoardFilter); // special case, as the offBoardFilter maintains state
        this.game = game;
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return aeroGroundPaths;
    }
    
    /**
     * Computes shortest paths to nodes in the graph.
     * 
     * @param startingEdges a collection of possible starting edges.
     */
    @Override
    public void run(Collection<MovePath> startingEdges) {
        try {
            aeroGroundPaths = new ArrayList<MovePath>();
            for(MovePath path : startingEdges) {
                Collection<MovePath> validAccelerations = NextStepsAeroGroundAdjacencyMap.generateValidAccelerations(path);
                
                for(MovePath acceleratedPath : validAccelerations) {
                    aeroGroundPaths.addAll(getAltitudeAdjustedPaths(GenerateAllPaths(acceleratedPath)));
                }
            }
        } catch (OutOfMemoryError e) {
            /*
             * Some implementations can run out of memory if they consider and
             * save in memory too many paths. Usually we can recover from this
             * by ending prematurely while preserving already computed results.
             */

            candidates = null;
            //candidates = new PriorityQueue<E>(100, comparator);
            e.printStackTrace();
            System.err.println("Not enough memory to analyse all options."//$NON-NLS-1$
                    + " Try setting time limit to lower value, or "//$NON-NLS-1$
                    + "increase java memory limit.");//$NON-NLS-1$
        } catch(Exception e) {
            e.printStackTrace(); //do something, don't just swallow the exception, good lord
        }
    }
    
    /**
     * Gets the shortest of all paths generated so far that explicitly goes off-board.
     */
    public MovePath getShortestOffBoardPath() {
        return offBoardFilter.getShortestPath();
    }
    
    public static AeroGroundPathFinder getInstance(IGame game) {
        AeroGroundPathFinder apf = new AeroGroundPathFinder(
                                        new MovePathFinder.MovePathDestinationMap(),
                                        new ShortestPathFinder.AeroMovePathRelaxer(),
                                        new NextStepsAeroGroundAdjacencyMap(MoveStepType.FORWARDS),
                                        new MovePathFinder.MovePathVelocityCostComparator(),
                                        game);
        
        apf.addFilter(new MovePathVelocityFilter());
        apf.addFilter(new MovePathLegalityFilter(game));
        return apf;
    }
    
    /**
     * Worker function that indicates if there is an enemy aircraft on the board for the current path.
     * Assumes we are on a ground map
     * @param path the current path we're evaluating
     * @return Whether or not there's an enemy airborne entity on the board.
     */
    private static boolean airborneEnemiesOnBoard(Entity mover) {
        for(Iterator<Entity> entityIter = mover.getGame().getAllEnemyEntities((Entity) mover); entityIter.hasNext();) {
            Entity currentEntity = entityIter.next();
            if(currentEntity.isAero()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Helper function to calculate the maximum thrust we should use for a particular aircraft
     * We limit ourselves to the lowest of "safe thrust" and "structural integrity"...
     *  ... or something a little lower to prevent using gigabyte after gigabyte of memory on high-thrust paths
     *  that don't do anything useful.
     * @param aero The aero entity for which to calculate max thrust.
     * @return The max thrust.
     */
    private static int calculateMaxThrust(IAero aero) {
        int maxThrust = Math.min(aero.getCurrentThrust(), aero.getSI());    // we should only thrust up to our SI
        //maxThrust = Math.min(maxThrust, artificialCap); // lol just kidding, you ain't going nowhere fast bro
        return maxThrust;
    }
    
    /** 
     * A path node for an aero on a ground map. 
     * These things are pretty complicated and there's not much overlap between paths, so we basically compare the whole move sequence.
     */
    public static class AeroGroundPathNode {
        private List<Coords> pathVertices;
        private int remainingVelocity;
        private boolean fliesOffBoard;
        
        public List<Coords> getPathVertices() {
            return pathVertices;
        }
        
        public int getRemainingVelocity() {
            return remainingVelocity;
        }
        
        public boolean getFliesOffBoard() {
            return fliesOffBoard;
        }
        
        public AeroGroundPathNode(List<Coords> pathVertices, int remainingVelocity, boolean fliesOffBoard) {
            this.pathVertices = pathVertices;
            this.remainingVelocity = remainingVelocity;
            this.fliesOffBoard = fliesOffBoard;
        }
        
        @Override
        public boolean equals(Object other) {
            if(!(other instanceof AeroGroundPathNode))
            {
                return false;
            }
            
            AeroGroundPathNode otherNode = (AeroGroundPathNode) other;
            
            // all nodes that fly off board might as well be the same
            // the MovePathVelocityCostComparator will pick the roughly shortest one
            if(fliesOffBoard && otherNode.getFliesOffBoard()) {
                return true;
            }
            
            // if we don't have the same move sequence length, then we sure as hell aren't on the same path
            if(pathVertices.size() != otherNode.getPathVertices().size()) {
                return false;
            }
            
            // now we run along the path, and if the two paths deviate at any point, the two paths are not the same
            for(int pos = 0; pos < pathVertices.size(); pos++) {
                if(!pathVertices.get(pos).equals(otherNode.getPathVertices().get(pos))) {
                    return false;
                }
            }
            
            // we may have the same path so far, but it won't be the same in the future
            return remainingVelocity == otherNode.getRemainingVelocity();
        }
        
        @Override
        public int hashCode() {
            return pathVertices.hashCode() + 7 * remainingVelocity + (fliesOffBoard ? 1 : 0);
        }
    }

    /**
     * 
     * @author NickAragua
     *
     * @param <MovePath> a move path
     * 
     * This class removes all off-board paths, but does kee
     */
    public static class AeroGroundOffBoardFilter extends Filter<MovePath> {

        MovePath shortestPath;
        
        public MovePath getShortestPath() {
            return shortestPath;
        }
        
        /**
         * Returns filtered collection by removing those objects that fail
         * {@link #shouldStay(T)} test.
         * 
         * @param collection collection to be filtered
         * @return filtered collection
         */
        @Override
        public Collection<MovePath> doFilter(Collection<MovePath> collection) {
            List<MovePath> filteredMoves = new ArrayList<>();
            for (MovePath e : collection) {
                // if the path does not go off board, then we add it to the returned collection
                if (shouldStay(e)) {
                    filteredMoves.add(e);
                }
                else {
                    // if the path *does* go off board, we want to compare its length to 
                    // our current shortest off-board path and keep it in mind if it's shorter
                    // at the end of the process, we will add the shortest path off board to the list of moves
                    if(shortestPath == null || 
                            (shortestPath.length() > e.length())) {
                        shortestPath = e;
                    }
                }
            }
            
            return filteredMoves;
        }
        
        /**
         * Tests if the object should stay in the collection.
         * 
         * @param object tested object
         * @return true if the object should stay in the collection
         */
        @Override
        public boolean shouldStay(MovePath object) {
            MovePath path = (MovePath) object;
            return !path.fliesOffBoard();
        }
        
    }
    
    // Destination map for paths taken by aerodyne units on atmospheric ground maps
    public static class AeroGroundDestinationMap 
        implements AbstractPathFinder.DestinationMap<AeroGroundPathNode, MovePath>{
     
        /**
         * Gets a "node" representing the destination.
         */
        public AeroGroundPathNode getDestination(MovePath mp) {
            if(mp.length() == 25 && mp.getFliesOverEnemy()) {
                int alpha = 1;
            }
            
            List<Coords> pathVertices = new ArrayList<Coords>();
            for(MoveStep step : mp.getStepVector()) {
                if(step.getType() == MoveStepType.TURN_LEFT ||
                        step.getType() == MoveStepType.TURN_RIGHT) {
                    pathVertices.add(step.getPosition());
                }
            }
            
            return new AeroGroundPathNode(pathVertices, mp.getFinalVelocityLeft(), mp.fliesOffBoard());
        }
    }

    /**
     * Functional Interface for {@link #getAdjacent(MovePath)}
     */
    public static class NextStepsAeroGroundAdjacencyMap extends NextStepsAdjacencyMap {

        public NextStepsAeroGroundAdjacencyMap(MoveStepType stepType) {
            super(stepType);
        }

        /**
         * Extends the result produced by
         * {@linkNextStepsAdjacencyMap#getAdjacent(megamek.common.MovePath)}
         * with manoeuvres for Aero
         *
         * @see NextStepsAdjacencyMap#getAdjacent(megamek.common.MovePath)
         */
        @Override
        public Collection<MovePath> getAdjacent(MovePath mp) {
            // These steps are terminal, and shouldn't have anything after them
            if (mp.contains(MoveStepType.RETURN)
                    || mp.contains(MoveStepType.OFF)) {
                return new ArrayList<MovePath>();
            }
            
            if(mp.length() == 25 && mp.getFliesOverEnemy()) { //&& mp.getLastStep().getType() == MoveStepType.TURN_RIGHT) {
                int alpha = 1;
            }
            
            Collection<MovePath> result = new ArrayList<MovePath>();
            MoveStep lastStep = mp.getLastStep();
            IGame game = mp.getEntity().getGame();
            IAero aero = (IAero) mp.getEntity();
            
            // if it's the first step of the move, this will add zero or more neighbor paths that change the aircraft's velocity
            result.addAll(generateValidAccelerations(mp));
            //result.addAll(generateValidAltitudeChanges(mp, 5));
            
            // we can move forward if we have some velocity left
            // additionally, if we can't turn, we should move forward until we encounter one of these situations:
            //  can turn
            //  have run out of velocity
            //  have gone off the board
            // this will not add the single "forward" when the aircraft *can* turn, so we handle that subsequently 
            if(mp.getFinalVelocityLeft() > 0)
            {
                MovePath longJump = mp.clone();
                boolean stepAdded = false;
                        
                // little dirty hack to get around the problem where if this is the first step of a path
                // we have no last step
                MoveStep currentStep = longJump.getLastStep();
                if(currentStep == null) {
                    currentStep = new MoveStep(longJump, MoveStepType.NONE);
                }
                
                while(!currentStep.canAeroTurn(game) &&
                      longJump.getFinalVelocityLeft() > 0 && 
                      game.getBoard().contains(longJump.getFinalCoords())) {
                    
                    longJump.addStep(MoveStepType.FORWARDS);
                    currentStep = longJump.getLastStep();
                    stepAdded = true;
                }
                
                if(stepAdded)
                {
                    // chop off the last step if it took us off board
                    if(!game.getBoard().contains(longJump.getFinalCoords()))
                    {
                        longJump.removeLastStep();
                    }
                    
                    result.add(longJump);
                }
            }            
            
            // we can turn if we can turn. very philosophical.
            // Actually, though, don't turn on the last step of a move. It's pointless.
            if(lastStep != null && lastStep.canAeroTurn(game) && lastStep.getVelocityLeft() > 0)
            {
                result.add(mp.clone().addStep(MoveStepType.TURN_RIGHT));
                result.add(mp.clone().addStep(MoveStepType.TURN_LEFT));
                result.add(mp.clone().addStep(MoveStepType.FORWARDS));
            }
            
            // we can fly off of edge of board if we're at the edge of the board
            Coords c = mp.getFinalCoords();
            
            if (c.isOnBoardEdge(game.getBoard()) 
                && (mp.getFinalVelocity() > 0)) {
                result.add(mp.clone().addStep(MoveStepType.RETURN));
            }
                               
            // Maneuvers are temporarily disabled, as Princess is a little bit cavalier with them at the moment
            /*if (!mp.contains(MoveStepType.MANEUVER)) {
                // side slips
                result.add(mp.clone()
                             .addManeuver(ManeuverType.MAN_SIDE_SLIP_LEFT)
                             .addStep(MoveStepType.LATERAL_LEFT, true, true));
                result.add(mp.clone()
                             .addManeuver(ManeuverType.MAN_SIDE_SLIP_RIGHT)
                             .addStep(MoveStepType.LATERAL_RIGHT, true, true));
                boolean has_moved = mp.getHexesMoved() == 0;
                if (!has_moved) {
                    //result.add(mp.clone().addManeuver(ManeuverType.MAN_HAMMERHEAD).
                    //result.add(mp.clone().addManeuver(MoveStepType.YAW, true, true);
                    // immelman
                    if (mp.getFinalVelocity() > 2) {
                        result.add(mp.clone().addManeuver(ManeuverType.MAN_IMMELMAN));
                    }
                    // split s
                    if (mp.getFinalAltitude() > 2) {
                        result.add(mp.clone().addManeuver(ManeuverType.MAN_SPLIT_S));
                    }
                    // loop
                    if (mp.getFinalVelocity() > 4) {
                        result.add(mp.clone().addManeuver(ManeuverType.MAN_LOOP)
                                     .addStep(MoveStepType.LOOP, true, true));
                    }
                }
            }*/
            
            return result;
        }

        /**
         * Generates paths that begin with all valid acceleration sequences for this aircraft.
         * Avoids sequences that will cause a PSR (just no)
         * @param startingPath
         * @return
         */
        public static Collection<MovePath> generateValidAccelerations(MovePath startingPath)
        {
            // the possible velocities at ground level are 1 - 12 (we ignore "0" velocity as that will just stall our aircraft)
            // with an additional lower and upper bound of "current velocity" -/+ walk MP (investigate utility of adjusting it to be run MP instead)

            Collection<MovePath> paths = new ArrayList<MovePath>();
            
            // sanity check: if we've already done something else with the path, there's no acceleration to be done
            if(startingPath.length() > 0) {
                return paths;
            }
            
            int maxThrust = AeroGroundPathFinder.calculateMaxThrust((IAero) startingPath.getEntity());
            
            int currentVelocity = startingPath.getFinalVelocity();
            int lowerBound = Math.max(1, currentVelocity - maxThrust);
            // put a governor on that engine
            // while the rulebook max velocity on ground map is 12, in reality we want to limit ourselves to velocity 3, for two reasons:
            // 1: velocity 3 lets us do a 360 degree turn, so any further turning is superfluous
            // 2: velocity 4 generates about 6,000 paths which is pretty painful for performance, although we can probably improve
            //      that by updating PathRanker.getMovePathSuccessProbability() to skip most non-aero related checks
            int upperBound = Math.min(3, currentVelocity + maxThrust); 
            
            // we go from the lower bound to the current velocity and generate paths with the required number of DECs to get to
            // the desired velocity
            for(int desiredVelocity = lowerBound; desiredVelocity < currentVelocity; desiredVelocity++)
            {
                MovePath path = startingPath.clone();
                for(int deltaVelocity = 0; deltaVelocity < currentVelocity - desiredVelocity; deltaVelocity++) {
                    path.addStep(MoveStepType.DEC);
                }
                
                paths.add(path);
            }
            
            // If the unaltered starting path is within acceptable velocity bounds, it's also a valid "acceleration".
            if(startingPath.getFinalVelocity() <= upperBound &&
               startingPath.getFinalVelocity() >= lowerBound) {
                paths.add(startingPath.clone());
            }
            
            // we go from the current velocity to the upper bound and generate paths with the required number of DECs to get to
            // the desired velocity
            for(int desiredVelocity = currentVelocity; desiredVelocity < upperBound; desiredVelocity++)
            {
                MovePath path = startingPath.clone();
                for(int deltaVelocity = 0; deltaVelocity < upperBound - desiredVelocity; deltaVelocity++) {
                    path.addStep(MoveStepType.ACC);
                }
                
                paths.add(path);
            }
            
            return paths;
        }

        /**
         * Given a starting path, generates paths that go to the desired altitude (or as close as possible)
         * @param startingPath
         * @param desiredAltitude
         * @return
         */
        private Collection<MovePath> generateValidAltitudeChanges(MovePath startingPath, int desiredAltitude) {
            Collection<MovePath> results = new ArrayList<MovePath>();
            
            // sanity check: if we're already at the desired altitude, don't bother
            // additionally, quit out if the starting path's last step is not an acceleration 
            // (it's ok to continue if we have no steps)
            if(startingPath.getFinalAltitude() == desiredAltitude ||
                    (startingPath.getLastStep() != null && 
                        startingPath.getLastStep().getType() != MoveStepType.DEC &&
                        startingPath.getLastStep().getType() != MoveStepType.ACC)) {
                return results;
            }
            
            MovePath altitudePath = startingPath.clone();
            
            // special case: if we are at velocity 0, we don't want to stall, so just generate "double decelerate" and call it a day
            if(startingPath.getFinalVelocity() == 0) {
                altitudePath.addStep(MoveStepType.DOWN);
                altitudePath.addStep(MoveStepType.DOWN);
                results.add(altitudePath);
                return results;
            }
            
            boolean stepsAdded = false;
            int maxThrust = AeroGroundPathFinder.calculateMaxThrust((IAero) altitudePath.getEntity());
            
            // generate a path that involves making changes that go up or down towards the desired altitude as far as possible
            while(altitudePath.getFinalAltitude() != desiredAltitude) {
                // up steps use thrust. Two points, to be exact
                if(altitudePath.getFinalAltitude() < desiredAltitude &&
                        altitudePath.getMpUsed() < maxThrust - 1) {
                    altitudePath.addStep(MoveStepType.UP);
                    stepsAdded = true;
                }
                // down steps don't use thrust, but if we take more than two, that's a PSR so avoid that
                else if(altitudePath.getFinalAltitude() > desiredAltitude &&
                        altitudePath.getFinalAltitude() >=  startingPath.getFinalAltitude() - 2) {
                    altitudePath.addStep(MoveStepType.DOWN);
                    stepsAdded = true;
                }
                // we've either reached the desired altitude or have hit some other stopping condition so that's enough of that
                else {
                    break;
                }
            }
            
            if(stepsAdded) {
                results.add(altitudePath);
            }
            
            return results;
        }
    }

    // This object contains a hex and the shortest non-off-board path that has flown over it.
    HashMap<Coords, MovePath> visitedHexes;
    
    private List<MovePath> getAltitudeAdjustedPaths(List<MovePath> startingPaths) {
        List<MovePath> desiredAltitudes = new ArrayList<MovePath>();
        
        for(MovePath start : startingPaths) {
            boolean choppedOffFlyOff = false;
            
            // if we are going off board, we need to take some special actions, meaning chopping off the tail end
            if(start.fliesOffBoard()) {
                start.removeLastStep();
                choppedOffFlyOff = true;
            }
            
            // repeat with 1, 3, 7 when we settle things down?
            MovePath desiredAltitudePath = adjustTowardsDesiredAltitude(start, 5);
            
            if(choppedOffFlyOff) {
                desiredAltitudePath.addStep(MoveStepType.RETURN);
            }
            
            desiredAltitudes.add(desiredAltitudePath);
        }
        
        return desiredAltitudes;
    }
    
    // Moves a given path towards 
    private MovePath adjustTowardsDesiredAltitude(MovePath startingPath, int desiredAltitude) {
        MovePath altitudePath = startingPath.clone();
        int maxThrust = AeroGroundPathFinder.calculateMaxThrust((IAero) altitudePath.getEntity());
        
        // generate a path that involves making changes that go up or down towards the desired altitude as far as possible
        while(altitudePath.getFinalAltitude() != desiredAltitude) {
            // up steps use thrust. Two points, to be exact
            if(altitudePath.getFinalAltitude() < desiredAltitude &&
                    altitudePath.getMpUsed() < maxThrust - 1) {
                altitudePath.addStep(MoveStepType.UP);
            }
            // down steps don't use thrust, but if we take more than one, it changes the velocity, so just do one for now
            // if we take more than two, it's a PSR, so definitely avoid that
            else if(altitudePath.getFinalAltitude() > desiredAltitude &&
                    altitudePath.getFinalAltitude() >=  startingPath.getFinalAltitude() - 1) {
                altitudePath.addStep(MoveStepType.DOWN);
            }
            // we've either reached the desired altitude or have hit some other stopping condition so that's enough of that
            else {
                break;
            }
        }
        
        return altitudePath;
    }
    
    // the goal here is to generate a "tree-like" structure;
    //
    //               \\\|///
    //                \\|//
    //                 \|/
    //                  |
    //                  |
    //
    //  Then, generate smaller subtrees off the left and right sides of the tree, like so:
    //             /-\\\|///-\
    //            //--\\|//--\\
    //                 \|/
    //                  |
    //                  |
    // And so on until we've run out of velocity
    // if a branch runs off board, we discard it unless it's the shortest one so far, we'll want to keep one of those around
    // if a branch runs out of velocity, then we make note of all the hexes it has visited so far.
    // if a hex has already been visited by another branch, then we set the current one as the visitor only if it's shorter
    private List<MovePath> GenerateAllPaths(MovePath mp) {
        List<MovePath> retval = new ArrayList<MovePath>();
        
        for(MovePath path : generateSidePaths(mp, MoveStepType.TURN_RIGHT)) {
            // we want to avoid adding paths that don't visit new hexes
            // off-board paths will get thrown out later
            if(path.fliesOffBoard() || newHexVisited(path)) {
                retval.add(path);
            }
            else {
                int alpha = 1; // bitch say what;
            }
        }
        
        for(MovePath path : generateSidePaths(mp, MoveStepType.TURN_LEFT)) {
            // we want to avoid adding paths that don't visit new hexes
            // off-board paths will get thrown out later
            if(path.fliesOffBoard() || newHexVisited(path)) {
                retval.add(path);
            }
            else {
                int alpha = 1; // bitch say what;
            }
        }
        
        ForwardToTheEnd(mp);
        if(mp.fliesOffBoard() || newHexVisited(mp)) {
            retval.add(mp);
        }
        
        return retval;
    }
    
    // generates and returns all paths that stick straight lines out of the current path to the right or left
    private List<MovePath> generateSidePaths(MovePath mp, MoveStepType stepType) {
        List<MovePath> retval = new ArrayList<MovePath>();
        MovePath straightLine = mp.clone();
        
        while(straightLine.getFinalVelocityLeft() > 0 && 
                game.getBoard().contains(straightLine.getFinalCoords())) {
            boolean firstTurn = true;            
            
            // little dirty hack to get around the problem where if this is the first step of a path
            // we have no last step
            MoveStep currentStep = straightLine.getLastStep();
            if(currentStep == null) {
                currentStep = new MoveStep(straightLine, MoveStepType.NONE);
            }
         
            if(currentStep.canAeroTurn(game)) {
                MovePath tiltedPath = straightLine.clone();
                tiltedPath.addStep(stepType);
                
                // the first time we add a turn, we recurse in the same direction
                if(firstTurn) {
                    retval.addAll(generateSidePaths(tiltedPath, stepType));
                }
                
                ForwardToTheEnd(tiltedPath);
                retval.add(tiltedPath);
                firstTurn = false;
            }
            
            straightLine.addStep(MoveStepType.FORWARDS);
        }
        
        return retval;
    }
    
    // helper function to move the path forward until we run out of velocity or off the board
    // "respect" board edges by attempting a turn 
    private void ForwardToTheEnd(MovePath mp) {
        while(mp.getFinalVelocityLeft() > 0) {
                
                mp.addStep(MoveStepType.FORWARDS);
                
                // if we have arrived on the edge, 
                if(mp.getFinalCoords().isOnBoardEdge(game.getBoard())) {
                    // if we're up against the west edge and came from northeast, turn south
                    // if we're up against the west edge and came from southeast, turn north
                    // if we're up against the east edge and came from northwest, turn south
                    // if we're up against the east edge and came from southwest, turn north
                    // if we're up against the north edge and came from southwest, turn south
                    // if we're up against the north edge and came from 
                }
                
                // chop off the last step if it took us off board
                if(!game.getBoard().contains(mp.getFinalCoords()))
                {
                    mp.removeLastStep();
                    if(mp.getEntity().getDamageLevel() != Entity.DMG_CRIPPLED) {
                        mp.addStep(MoveStepType.RETURN);
                    }
                    // "I'm too beat up, bugging out!"
                    else {
                        mp.addStep(MoveStepType.FLEE);
                    }
                    return;
                }
        }
    }
    
    private HashMap<Coords, MovePath> visitedCoords = new HashMap<Coords, MovePath>();
    // goes through a path.
    // if it does not take us off-board, records the coordinates it visits
    // returns true if this path visits hexes that have not been visited before
    private boolean newHexVisited(MovePath mp) {
        boolean newHexVisited = false;
        
        if(!mp.fliesOffBoard()) {
            for(MoveStep step : mp.getStepVector()) {
                if(!visitedCoords.containsKey(step.getPosition())) {
                    visitedCoords.put(step.getPosition(), mp);
                    newHexVisited = true;
                }
            }
        }
        
        return newHexVisited;
    }
}