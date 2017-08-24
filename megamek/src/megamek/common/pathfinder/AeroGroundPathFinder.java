package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.PriorityQueue;

import megamek.client.bot.princess.FireControl;
import megamek.common.Aero;
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
    
    /**
     * Computes shortest paths to nodes in the graph.
     * 
     * @param startingEdges a collection of possible starting edges.
     */
    /*@Override
    public void run(Collection<MovePath> startingEdges) {
        try {
            if (candidates.size() > 0) {
                candidates.clear();
                pathsCosts.clear();
            }
            //candidates.addAll(startingEdges);
            
            // the "aero-on-ground" unit is actually a simple geometry problem rather than a search problem
            // we are interested in three types of paths:
            //  1. Paths that fly over an enemy unit for strafing/bombing purposes
            //  2. Paths that take us off the edge without a PSR
            //  3. Paths that leave us loitering on the board until next turn.
            
            for(MovePath edge : startingEdges)
            {
                generateEnemyOverflightPaths(edge);
            }
            
        } catch (OutOfMemoryError e) {*/
            /*
             * Some implementations can run out of memory if they consider and
             * save in memory too many paths. Usually we can recover from this
             * by ending prematurely while preserving already computed results.
             */

          /*  candidates = null;
            candidates = new PriorityQueue<MovePath>(100, getComparator());
            e.printStackTrace();
            System.err.println("Not enough memory to analyse all options."//$NON-NLS-1$
                    + " Try setting time limit to lower value, or "//$NON-NLS-1$
                    + "increase java memory limit.");//$NON-NLS-1$
        }
    }*/
    
    private void generateEnemyOverflightPaths(MovePath startingPoint)
    {
        Entity me = startingPoint.getEntity();
        
        for(Iterator<Entity> iter = game.getAllEnemyEntities(me); iter.hasNext();) {
            Entity enemy = iter.next();
            MovePath overflightPath = generateEnemyOverflightPath(startingPoint, enemy);
            AeroGroundPathNode overflightEndNode = getDestinationMap().getDestination(overflightPath);
            
            pathsCosts.put(overflightEndNode, overflightPath);
        }
    }
    
    private MovePath generateEnemyOverflightPath(MovePath startingPath, Entity enemy)
    {
        MovePath currentPath = startingPath.clone();
        // use up any remaining velocity before I can make a turn. If I pass over the enemy, great. I'm done. Return path. 
        //  If I go off board, I'm also done. Return path.
        // compare coordinates with my current position using Coords.radian(my coords, enemy coords)
        // Three cases: 
        //      1. enemy is in front of me. Super. Advance until I run out of velocity. If I go off board, make attempt to turn left or right
        //              to avoid board edge.
        //      2. enemy is within 1/3 PI radians (one turn) to the left or right of my current facing.
        //          advance until enemy is exactly 1/3 PI radians left or right of current facing. advance until out of velocity
        //      3. enemy is more than 1/3 PI radians left or right of my current facing. 
        //          return path resulting from best result of "turn left" or "turn right" 
        //      Process is self terminating due to finite velocity
        
        double angle = currentPath.getFinalCoords().radian(enemy.getPosition());
        Facing facing = Facing.valueOfInt(currentPath.getFinalFacing());
        Facing oneTurnClockwise = facing.getNextClockwise();
        Facing oneTurnCounterClockwise = facing.getNextCounterClockwise();
        
        if(angle == facing.getIntValue())
        {
            MoveForward(currentPath);
        }
        // the enemy unit is in the "sweet spot" between straight ahead and one turn clockwise
        else if (facing.isLeftOf(angle) && !oneTurnCounterClockwise.isLeftOf(angle))
        {
            MoveForward(currentPath, oneTurnCounterClockwise.getIntValue(), enemy.getPosition());
            currentPath.addStep(MoveStepType.TURN_RIGHT);
            return generateEnemyOverflightPath(currentPath, enemy);
        }
        // the enemy unit is in the "sweet spot" between straight ahead and one turn counter clockwise
        else if(!facing.isLeftOf(angle) && oneTurnClockwise.isLeftOf(angle))
        {
            MoveForward(currentPath, oneTurnClockwise.getIntValue(), enemy.getPosition());
            currentPath.addStep(MoveStepType.TURN_LEFT);
            return generateEnemyOverflightPath(currentPath, enemy);
        }
        else
        {
            MovePath leftPath = currentPath.clone();
            leftPath.addStep(MoveStepType.TURN_LEFT);
            
            MovePath rightPath = currentPath.clone();
            rightPath.addStep(MoveStepType.TURN_RIGHT);
            
            
        }
        
        return currentPath;
    }
    
    /**
     * Helper function that moves forward until we run out of velocity or reach the 
     * @param path              current path
     * @param desiredFacing     desired angle, in radians
     * @param enemyPosition     position of enemy target
     */
    private void MoveForward(MovePath path, int desiredFacing, Coords enemyPosition) {
        while((path.getLastStep() != null) &&
                (path.getFinalVelocityLeft() > 0) &&                                // has velocity left
                (desiredFacing != path.getFinalCoords().radian(enemyPosition)) &&   // has not reached the desired angle
                game.getBoard().contains(path.getFinalCoords())) {                  // is still on the board
                    path.addStep(MoveStepType.FORWARDS);
          }
    }
    
    /**
     * Helper function that moves forward until we run out of velocity, go off board or can turn.
     * @param path
     */
    private void MoveForward(MovePath path) {
        boolean stepAdded = false;
        
        while((path.getLastStep() != null) &&
                !path.getLastStep().canAeroTurn(game) &&                // can turn
                (path.getFinalVelocityLeft() > 0) &&                    // has velocity left
                game.getBoard().contains(path.getFinalCoords())) {      // is still on the board
                    path.addStep(MoveStepType.FORWARDS);
                    stepAdded = true;
          }
        
        if(stepAdded) {
            // chop off the last step if it took us off board
            if(!game.getBoard().contains(path.getFinalCoords())) {
                path.removeLastStep();
                path.addStep(MoveStepType.RETURN);
            }
        }
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
        return apf;
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
        
        public AeroGroundPathNode(Coords coords, int facing, boolean fliesOverEnemy, int remainingVelocity, int altitude) {
            this.coords = coords;
            this.facing = facing;
            this.fliesOverEnemy = fliesOverEnemy;
            this.remainingVelocity = remainingVelocity;
            this.altitude = altitude;
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
                    //(fliesOverEnemy == otherNode.fliesOverEnemy) &&
                    (remainingVelocity == otherNode.remainingVelocity);
        }
        
        @Override
        public int hashCode() {
            return facing + 7 * coords.hashCode() + 14 * remainingVelocity;
        }
    }

    // Destination map for paths taken by aerodyne units on atmospheric ground maps
    public static class AeroGroundDestinationMap 
        implements AbstractPathFinder.DestinationMap<AeroGroundPathNode, MovePath>{
     
        // Get the final AeroGroundPathNode of the given path
        public AeroGroundPathNode getDestination(MovePath mp) {
            return new AeroGroundPathNode(mp.getFinalCoords(), mp.getFinalFacing(), mp.getFliesOverEnemy(), mp.getFinalVelocityLeft(), mp.getFinalAltitude());
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
                        
                if(longJump.getLastStep() == null) {
                    longJump.addStep(MoveStepType.NONE);
                }
                
                while(!longJump.getLastStep().canAeroTurn(game) &&
                      longJump.getFinalVelocityLeft() > 0 && 
                      game.getBoard().contains(longJump.getFinalCoords())) {
                    longJump.addStep(MoveStepType.FORWARDS);
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
                               
            // Manuevers are temporarily disabled, as Princess is a little bit cavalier with them at the moment
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
                    // immelmen
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
            
            //AddUpAndDown(result, lastStep, mp.getEntity(), mp);
            
            return result;
        }

        private Collection<MovePath> generateValidAccelerations(MovePath startingPath)
        {
            // the possible velocities at ground level are 1 - 12 (we ignore "0" velocity as that will just stall our aircraft)
            // with an additional lower and upper bound of "current velocity" -/+ walk MP (investigate utility of adjusting it to be run MP instead)

            Collection<MovePath> paths = new ArrayList<MovePath>();
            
            // sanity check: if we've already done something else with the path, there's no acceleration to be done
            if(startingPath.length() > 0) {
                return paths;
            }
            
            IAero aero = (IAero) startingPath.getEntity();
            int maxThrust = Math.min(aero.getCurrentThrust(), aero.getSI());    // we should only thrust up to our SI
            
            int currentVelocity = startingPath.getFinalVelocity();
            int lowerBound = Math.max(1, currentVelocity - maxThrust);
            int upperBound = Math.min(12, currentVelocity + maxThrust);
            upperBound = 3; // lol just kidding you ain't going nowhere
            
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
         * Worker method that adds "UP" and "DOWN" steps to the given move path collection if
         * the entity is eligible to do so
         *
         * @param result The collection of move paths to improve
         * @param last The last step along the parent move path
         * @param entity The entity taking the move path
         * @param movePath The parent movePath
         * @see AbstractPathFinder.AdjacencyMap
         */
        protected void AddUpAndDown(Collection<MovePath> result, final MoveStep last, final Aero entity, final MovePath mp)
        {
            Coords pos;
            int elevation;
            pos = last != null ? last.getPosition() : entity.getPosition();
            elevation = last != null ? last.getElevation() : entity.getElevation();          
             
            // going up costs two thrust points, so...
            if (entity.canGoUp(elevation, pos) && last.getMpUsed() < entity.getSI() - 1) {
                result.add(mp.clone().addStep(MoveStepType.UP));
            }
            if (entity.canGoDown(elevation, pos)) {
                result.add(mp.clone().addStep(MoveStepType.DOWN));
            }            
        }
    }
}