package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IAero;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.LogLevel;
import megamek.common.logging.MMLogger;
import megamek.common.MovePath.MoveStepType;
import megamek.common.pathfinder.AbstractPathFinder.Filter;

/**
 * This set of classes is intended for use for pathfinding by aerodyne units on ground maps with an atmosphere
 * Usage anywhere else may result in "unpredictable" behavior
 * 
 * @author NickAragua
 *
 */
public class AeroGroundPathFinder {
    
    public static final int OPTIMAL_STRIKE_ALTITUDE = 5; // also great for dive bombs
    public static final int NAP_OF_THE_EARTH = 1;
    public static final int OPTIMAL_STRAFE_ALTITUDE = 3; // future use
    private static final String LOGGER_CATEGORY = "megamek.common.pathfinder.AeroGroundPathFinder";
    
    private IGame game;
    private List<MovePath> aeroGroundPaths;
    private int maxThrust;
    private MMLogger logger;
    
    private AeroGroundPathFinder(IGame game) {
        this.game = game;
        getLogger().setLogLevel(LOGGER_CATEGORY, LogLevel.DEBUG);
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return aeroGroundPaths;
    }
    
    private MMLogger getLogger() {
        return logger == null ? logger = DefaultMmLogger.getInstance() : logger;
    }
    
    /**
     * Computes shortest paths to nodes in the graph.
     * 
     * @param startingEdges a collection of possible starting edges.
     */
    public void run(MovePath startingEdge) {
        final String METHOD_NAME = "run";
        
        try {
            aeroGroundPaths = new ArrayList<MovePath>();
            
            // recalculate max thrust for the given path's entity
            maxThrust = calculateMaxSafeThrust((IAero) startingEdge.getEntity());                
            Collection<MovePath> validAccelerations = generateValidAccelerations(startingEdge);
            
            for(MovePath acceleratedPath : validAccelerations) {
                aeroGroundPaths.addAll(getAltitudeAdjustedPaths(GenerateAllPaths(acceleratedPath)));
            }
        } catch (OutOfMemoryError e) {
            /*
             * Some implementations can run out of memory if they consider and
             * save in memory too many paths. Usually we can recover from this
             * by ending prematurely while preserving already computed results.
             */

            final String memoryMessage = "Not enough memory to analyse all options."//$NON-NLS-1$
                    + " Try setting time limit to lower value, or "//$NON-NLS-1$
                    + "increase java memory limit.";
            
            getLogger().log(this.getClass(), METHOD_NAME, LogLevel.ERROR, memoryMessage, e);
        } catch(Exception e) {
            getLogger().log(this.getClass(), METHOD_NAME, e); //do something, don't just swallow the exception, good lord
        }
    }
    
    public static AeroGroundPathFinder getInstance(IGame game) {
        AeroGroundPathFinder apf = new AeroGroundPathFinder(game);

        return apf;
    }
    
    /**
     * Worker function that indicates if there is an enemy aircraft on the board for the current path.
     * Assumes we are on a ground map
     * @param path the current path we're evaluating
     * @return Whether or not there's an enemy airborne entity on the board.
     */
    @SuppressWarnings("unused")
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
    public static int calculateMaxSafeThrust(IAero aero) {
        int maxThrust = Math.min(aero.getCurrentThrust(), aero.getSI());    // we should only thrust up to our SI
        return maxThrust;
    }
    
    /**
     * 
     * @author NickAragua
     *
     * @param <MovePath> a move path
     * 
     * This class removes all off-board paths, but keeps track of the shortest of all the paths removed
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
                } else {
                    // if the path *does* go off board, we want to compare its length to 
                    // our current shortest off-board path and keep it in mind if it's both shorter and safe
                    // at the end of the process, we will add the shortest path off board to the list of moves
                    if(shortestPath == null || 
                            (shortestPath.length() > e.length()) && 
                            AeroPathUtil.isSafePathOffBoard(e)) {
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
        public boolean shouldStay(MovePath path) {
            return !path.fliesOffBoard();
        }
        
    }

    // This object contains a hex and the shortest non-off-board path that has flown over it.
    Map<Coords, MovePath> visitedHexes;
    
    /**
     * Generates paths that begin with all valid acceleration sequences for this aircraft.
     * Avoids sequences that will cause a PSR (just no)
     * @param startingPath
     * @return
     */
    private Collection<MovePath> generateValidAccelerations(MovePath startingPath) {
        // the possible velocities at ground level are 1 - 12 (we ignore "0" velocity as that will just stall our aircraft)
        // with an additional lower and upper bound of "current velocity" -/+ walk MP (investigate utility of adjusting it to be run MP instead)

        Collection<MovePath> paths = new ArrayList<MovePath>();
        
        // sanity check: if we've already done something else with the path, there's no acceleration to be done
        if(startingPath.length() > 0) {
            return paths;
        }
        
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
        for(int desiredVelocity = lowerBound; desiredVelocity < currentVelocity; desiredVelocity++) {
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
        for(int desiredVelocity = currentVelocity; desiredVelocity < upperBound; desiredVelocity++) {
            MovePath path = startingPath.clone();
            for(int deltaVelocity = 0; deltaVelocity < upperBound - desiredVelocity; deltaVelocity++) {
                path.addStep(MoveStepType.ACC);
            }
            
            paths.add(path);
        }
        
        return paths;
    }
    
    /**
     * Given a list of MovePaths, applies adjustTowardsDesiredAltitude to each of them.
     * @param startingPaths The collection of paths to process
     * @return The new collection of resulting paths.
     */
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
            MovePath desiredAltitudePath = adjustTowardsDesiredAltitude(start, OPTIMAL_STRIKE_ALTITUDE);
            
            if(choppedOffFlyOff) {
                desiredAltitudePath.addStep(MoveStepType.RETURN);
            }
            
            desiredAltitudes.add(desiredAltitudePath);
        }
        
        return desiredAltitudes;
    }
    
    /**
     * Moves a given path towards the desired altitude. 
     * @param startingPath The path to adjust
     * @param desiredAltitude The desired altitude
     * @return New instance of movepath with as much altitude adjustment as possible
     */
    private MovePath adjustTowardsDesiredAltitude(MovePath startingPath, int desiredAltitude) {
        MovePath altitudePath = startingPath.clone();
        
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
        }
        
        for(MovePath path : generateSidePaths(mp, MoveStepType.TURN_LEFT)) {
            // we want to avoid adding paths that don't visit new hexes
            // off-board paths will get thrown out later
            if(path.fliesOffBoard() || newHexVisited(path)) {
                retval.add(path);
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
        
        boolean firstTurn = true;        
        while(straightLine.getFinalVelocityLeft() > 0 && 
                game.getBoard().contains(straightLine.getFinalCoords())) {
            
            // little dirty hack to get around the problem where if this is the first step of a path
            // we have no last step
            MoveStep currentStep = straightLine.getLastStep();
            if(currentStep == null) {
                currentStep = new MoveStep(straightLine, MoveStepType.NONE);
            }
         
            int turnCost = currentStep.asfTurnCost(mp.getGame(), stepType, mp.getEntity());
            
            // if we can turn 
            // *and* it's a legal turn 
            // ("early" turns before the "free turn" use thrust, so if we go over max thrust, it's an illegal move
            //  and thus not worth evaluating further)
            if(currentStep.canAeroTurn(game) &&
                    currentStep.getMpUsed() <= mp.getEntity().getRunMP() - turnCost) { 
                MovePath tiltedPath = straightLine.clone();
                tiltedPath.addStep(stepType);
                
                // the first time we add a turn, we recurse in the same direction
                if(firstTurn) {
                    
                    // potential to do, depending on demonstrated princess performance:
                    // If we are going to run ourselves off the board, also generate side paths in the opposite direction of stepType
                    // This *may* result in a drastic increase in generated paths, so maybe hold off on it for now.
                    
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
                // don't generate an illegal move that flies off the board
                if(!mp.nextForwardStepOffBoard()) {
                    mp.addStep(MoveStepType.FORWARDS);
                }
                
                // if we have arrived on the edge, and we can turn, then attempt to "bounce off" or "ride" the edge. 
                // as long as we're not trying to go off board, then forget about it
                // this slightly increases the area covered by the aero in cases where the path takes it near the edge
                if(mp.nextForwardStepOffBoard() &&
                        mp.getEntity().getDamageLevel() != Entity.DMG_CRIPPLED &&
                        mp.getLastStep().canAeroTurn(game)) {
                    
                    // we want to generate a path that looks like this:
                    // ||
                    // ||
                    // |\
                    // | \   
                    // |  \
                    // at this point, we can know that going forward will take us off board
                    // we can either turn right or left
                    // if turning right and going forwards still takes us off board, then we go left
                    // you can approach the top and bottom edges of the board in such a way that neither left
                    // nor right produces an off-board path
                    // there's probably a better mathematical way to formulate this but I'm not really a math guy.
                    mp.addStep(MoveStepType.TURN_RIGHT);
                    if(mp.nextForwardStepOffBoard()) {
                        mp.removeLastStep();
                    }
                    
                    mp.addStep(MoveStepType.TURN_LEFT);
                    if(mp.nextForwardStepOffBoard()) {
                        mp.removeLastStep();
                    }
                }
                
                // if we're going off board (even after all this turning stuff)
                if(mp.nextForwardStepOffBoard())
                {
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
    
    private Map<Coords, MovePath> visitedCoords = new HashMap<Coords, MovePath>();
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