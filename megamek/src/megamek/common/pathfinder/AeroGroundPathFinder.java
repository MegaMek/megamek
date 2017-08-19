package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

import megamek.common.Aero;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IAero;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.UnitType;
import megamek.common.MovePath.MoveStepType;
import megamek.common.pathfinder.AbstractPathFinder.AdjacencyMap;
import megamek.common.pathfinder.AbstractPathFinder.DestinationMap;
import megamek.common.pathfinder.AbstractPathFinder.EdgeRelaxer;
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
    
    private AeroGroundPathFinder(DestinationMap<AeroGroundPathFinder.AeroGroundPathNode, MovePath> edgeDestinationMap, 
            EdgeRelaxer<MovePath, MovePath> edgeRelaxer, AdjacencyMap<MovePath> edgeAdjacencyMap, Comparator<MovePath> edgeComparator) {
        super(edgeDestinationMap, edgeRelaxer, edgeAdjacencyMap, edgeComparator);
    }

    public Collection<MovePath> getAllComputedPathsUncategorized() {
        return getPathCostMap().values();
    }
    
    public static AeroGroundPathFinder getInstance(IGame game) {
        AeroGroundPathFinder apf = new AeroGroundPathFinder(
                                        new AeroGroundDestinationMap(),
                                        new ShortestPathFinder.AeroMovePathRelaxer(),
                                        new NextStepsAeroGroundAdjacencyMap(MoveStepType.FORWARDS),
                                        new MovePathFinder.MovePathVelocityCostComparator());
        
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
            
            return (facing == otherNode.facing) &&
                    (Objects.equals(coords, otherNode.coords)) &&
                    //(fliesOverEnemy == otherNode.fliesOverEnemy) &&
                    (remainingVelocity == otherNode.remainingVelocity);
        }
        
        @Override
        public int hashCode() {
            return facing + 7 * coords.hashCode() + 14 * remainingVelocity + 21 * altitude;
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
        //this class is not tested, yet.

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
            
            
            // if we haven't done anything else yet, and we are an aerodyne unit on a ground map with atmosphere, 
            // some ground (and game) rules for accelerations:
            //  if we are at velocity 0 and it's the first step, then we need to accelerate and that's the only thing we do
            //  if we are at velocity > 0, we *can* accelerate
            //      but not if we already have 11+ acceleration steps
            //      or have accelerated equal to our SI
            //      or have accelerated equal to our current thrust
            if(!mp.containsAnyOther(MoveStepType.ACC))
            {
                // this is the "we're not moving and haven't accelerated" case, only one option
                if(mp.length() == 0 && mp.getFinalVelocityLeft() == 0)
                {
                    result.add(mp.clone().addStep(MoveStepType.ACC));
                    return result;
                }
                // if we haven't accelerated more than we should, we can accelerate
                else if (mp.length() < aero.getCurrentThrust() &&
                        mp.length() < aero.getSI() &&
                        mp.length() < 12)
                {
                    result.add(mp.clone().addStep(MoveStepType.ACC));
                }
            }
            // we also don't want to bother decelerating to 0 on ground maps, as that'll just crash our aircraft
            else if(!mp.containsAnyOther(MoveStepType.DEC) && 
                    mp.getFinalVelocityLeft() > 1)
            {
                result.add(mp.clone().addStep(MoveStepType.DEC));
            }
            
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
                
                while(longJump.getLastStep() != null &&
                      !longJump.getLastStep().canAeroTurn(game) &&
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
            if(lastStep != null && lastStep.canAeroTurn(game))
            {
                result.add(mp.clone().addStep(MoveStepType.TURN_RIGHT));
                result.add(mp.clone().addStep(MoveStepType.TURN_LEFT));
                
                if(mp.getFinalVelocityLeft() > 0) {
                    result.add(mp.clone().addStep(MoveStepType.FORWARDS));
                }
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
            
            AddUpAndDown(result, lastStep, mp.getEntity(), mp);
            
            return result;
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