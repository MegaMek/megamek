package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
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

/**
 * This set of classes is intended for use for pathfinding by aerodyne units on ground maps with an atmosphere
 * Usage anywhere else may result in "unpredictable" behavior
 * 
 * @author NickAragua
 *
 */
public class AeroGroundPathFinder extends AbstractPathFinder<AeroGroundPathFinder.AeroGroundPathNode, MovePath, MovePath> {
    
    private IGame game;
    
    private AeroGroundPathFinder(DestinationMap<AeroGroundPathFinder.AeroGroundPathNode, MovePath> edgeDestinationMap, 
            EdgeRelaxer<MovePath, MovePath> edgeRelaxer, AdjacencyMap<MovePath> edgeAdjacencyMap, Comparator<MovePath> edgeComparator, IGame game) {
        super(edgeDestinationMap, edgeRelaxer, edgeAdjacencyMap, edgeComparator);
        this.game = game;
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return getPathCostMap().values();
    }
    
    public static AeroGroundPathFinder getInstance(IGame game) {
        AeroGroundPathFinder apf = new AeroGroundPathFinder(
                                        new AeroGroundDestinationMap(),
                                        new ShortestPathFinder.AeroMovePathRelaxer(),
                                        new NextStepsAeroGroundAdjacencyMap(MoveStepType.FORWARDS),
                                        new MovePathFinder.MovePathVelocityCostComparator(),
                                        game);
        
        apf.addFilter(new MovePathVelocityFilter());
        apf.addFilter(new MovePathLegalityFilter(game));
        apf.addFilter(new AeroGroundOffBoardFilter());
        return apf;
    }
    
    /**
     * Utility method that attempts to adjust each path in the given collection
     * to go to an "optimal" altitude. Optimal altitudes are:
     *                  // we want to take ourselves to the following important altitudes:
     * 5 (max altitude for bombing and striking)
     * 3 max altitude for strafing - skip for now, princess won't be strafing
     * 1 is a superb altitude for "attitude" bombing - skip for now, princess won't be altitude bombing
     * 7 guarantees you won't hit by AA fire at all, ever, good for dogfighting other aeros
     * @param paths The collection of paths to process
     * @param aero The entity moving around
     * @return
     */
    public static Collection<MovePath> getAdjustedHeightPaths(Collection<MovePath> paths, Entity mover) {
        Collection<MovePath> resultingPaths = new ArrayList<MovePath>();
        boolean enemyAircraftOnBoard = airborneEnemiesOnBoard(mover);
        boolean moverIsBomberWithBombs = mover.getBombs(AmmoType.F_GROUND_BOMB).size() > 0;
        
        for(MovePath path : paths) {
            // 5 is always a nice altitude to be at
            resultingPaths.add(reconstructMovePathWithAltitudeChange(path, 5));
            
            // 7 is good if there are enemy aircraft on the game board and we don't have bombs on board
            if(enemyAircraftOnBoard && !moverIsBomberWithBombs) {
                resultingPaths.add(reconstructMovePathWithAltitudeChange(path, 7));
            }
            // these two are disabled until we teach the Princess to altitude bomb and strafe
            //resultingPaths.add(reconstructMovePathWithAltitudeChange(path, 3)); 
            //resultingPaths.add(reconstructMovePathWithAltitudeChange(path, 1));
        }
        
        return resultingPaths;
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
     * Worker function that takes a given path and desired altitude and
     * returns a new path instance that attempts to get the given path to the desired altitude
     * by inserting UP and DOWN steps after any existing ACC/DEC steps.
     * Returns null if the starting path is already at the desired altitude.
     * @param startingPath The starting path.
     * @param desiredAltitude The desired altitude.
     * @return Altitude-adjusted path.
     */
    private static MovePath reconstructMovePathWithAltitudeChange(MovePath startingPath, int desiredAltitude)
    {
        // if the starting path is already at the desired altitude, then bug out
        // also sanity check for a path not having any steps
        if(startingPath.getFinalAltitude() == desiredAltitude ||
                startingPath.length() == 0) {
            return null;
        }
        
        MovePath result = new MovePath(startingPath.getGame(), startingPath.getEntity());
        
        // The general pattern is that we put all "ACC/DEC" move steps first
        // then insert as many UP or DOWN actions to get to the desired altitude as we need and can do
        // then insert the rest of the path
        // as an added bonus, we omit any "NONE" move steps
        
        Enumeration<MoveStep> stepEnum = startingPath.getSteps();
        MoveStep currentStep = stepEnum.nextElement();
        
        // insert all ACC/DEC steps
        while((currentStep.getType() == MoveStepType.ACC) || 
                (currentStep.getType() == MoveStepType.DEC))
        {
            result.addStep(currentStep.getType());
            currentStep = stepEnum.nextElement();
        }
        
        while(result.getFinalAltitude() != desiredAltitude) {
            // up steps use thrust. Two points, to be exact
            if(result.getFinalAltitude() < desiredAltitude &&
                    result.getMpUsed() < calculateMaxThrust((IAero) result.getEntity()) - 1) {
                result.addStep(MoveStepType.UP);
            }
            // down steps don't use thrust, but if we have more than one, we affect the unit's velocity, so let's don't do that
            // just add one "DOWN" and break out of the while loop
            else if(result.getFinalAltitude() > desiredAltitude &&
                    result.getFinalAltitude() == startingPath.getFinalAltitude()) {
                result.addStep(MoveStepType.DOWN);
                break;
            }
        }
        
        // now add all the rest of the steps
        while(stepEnum.hasMoreElements()) {
            result.addStep(currentStep.getType());
            currentStep = stepEnum.nextElement();
        }
        
        // this step should be the last step
        result.addStep(currentStep.getType());
        
        return result;
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
        maxThrust = Math.min(maxThrust, 4); // lol just kidding, you ain't going nowhere fast bro
        return maxThrust;
    }
    
    // A path node for an aero unit on a ground map with an atmosphere
    // Even though multiple paths may wind up in the same hex with the same facing, it's not really the same path, so we add extra information
    public static class AeroGroundPathNode {
        private Coords coords;
        private int facing;
        private boolean fliesOverEnemy;
        private int remainingVelocity;
        private boolean fliesOffBoard;
        private int altitude;
        
        public Coords getCoords() {
            return coords;
        }
        
        public int getFacing() {
            return facing;
        }
        
        public boolean getFliesOverEnemy() {
            return fliesOverEnemy;
        }
        
        public int getRemainingVelocity() {
            return remainingVelocity;
        }
        
        public boolean getFliesOffBoard() {
            return fliesOffBoard;
        }
        
        public int getAltitude() {
            return altitude;
        }
        
        public AeroGroundPathNode(Coords coords, int facing, int remainingVelocity, int altitude, boolean fliesOffBoard) {
            this.coords = coords;
            this.facing = facing;
            this.remainingVelocity = remainingVelocity;
            this.altitude = altitude;
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
            
            return (facing == otherNode.facing) &&
                    (Objects.equals(coords, otherNode.coords)) &&
                    (altitude == otherNode.altitude) &&
                    (remainingVelocity == otherNode.remainingVelocity);
        }
        
        @Override
        public int hashCode() {
            return facing + 7 * coords.hashCode() + 14 * remainingVelocity;
        }
    }

    /**
     * 
     * @author NickAragua
     *
     * @param <MovePath> a move path
     * 
     * This class filters out all but the shortest off-board path
     */
    public static class AeroGroundOffBoardFilter extends Filter<MovePath> {

        MovePath shortestPath;
        
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
                if (shouldStay(e)) {
                    filteredMoves.add(e);
                }
                else {
                    // if the path *does* go off board, we want to compare its length to 
                    // our current shortest off-board path and keep it in mind if it's shorter
                    // then add just one shortest off-board path at the end.
                    if(shortestPath == null || shortestPath.length() > e.length()) {
                        shortestPath = e;
                    }
                }
            }
            
            if(shortestPath != null) {
                filteredMoves.add(shortestPath);
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
     
        // Get the final AeroGroundPathNode of the given path
        public AeroGroundPathNode getDestination(MovePath mp) {
            return new AeroGroundPathNode(mp.getFinalCoords(), mp.getFinalFacing(), mp.getFinalVelocityLeft(), mp.getFinalAltitude(), mp.fliesOffBoard());
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
            
            Collection<MovePath> result = new ArrayList<MovePath>();
            MoveStep lastStep = mp.getLastStep();
            IGame game = mp.getEntity().getGame();
            IAero aero = (IAero) mp.getEntity();
            
            // if it's the first step of the move, this will add zero or more neighbor paths that change the aircraft's velocity
            result.addAll(generateValidAccelerations(mp));
            result.addAll(generateValidAltitudeChanges(mp, 5));
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
        private Collection<MovePath> generateValidAccelerations(MovePath startingPath)
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
            int upperBound = Math.min(12, currentVelocity + maxThrust); 
            
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
         * Generates paths 
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
            
            boolean terminate = false;
            int maxThrust = AeroGroundPathFinder.calculateMaxThrust((IAero) altitudePath.getEntity());
            
            while(altitudePath.getFinalAltitude() != desiredAltitude && 
                    !terminate) {
                // up steps use thrust. Two points, to be exact
                if(altitudePath.getFinalAltitude() < desiredAltitude &&
                        altitudePath.getMpUsed() < maxThrust - 1) {
                    altitudePath.addStep(MoveStepType.UP);
                }
                // down steps don't use thrust, but if we take more than two, that's a PSR so avoid that
                else if(altitudePath.getFinalAltitude() > desiredAltitude &&
                        altitudePath.getFinalAltitude() >=  startingPath.getFinalAltitude() - 2) {
                    altitudePath.addStep(MoveStepType.DOWN);
                }
                // we've either reached the desired altitude or have hit some other stopping condition so that's enough of that
                else {
                    terminate = true;
                }
                
                results.add(altitudePath);
                altitudePath = altitudePath.clone();
            }
            
            return results;
        }
    }
}