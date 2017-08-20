package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.Facing;
import megamek.common.IGame;
import megamek.common.ManeuverType;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.Tank;

/**
 * Generic implementation of AbstractPathFinder when we restrict graph nodes to
 * (coordinates x facing) and edges to MovePaths. Provides useful
 * implementations of functional interfaces defined in AbstractPathFinder.
 *
 * @param <C>
 */
public class MovePathFinder<C> extends AbstractPathFinder<MovePathFinder.CoordsWithFacing, C, MovePath> {

    /**
     * Node defined by coordinates and unit facing.
     *
     */
    /**
     * @author Saginatio
     */
    public static class CoordsWithFacing {
        /**
         * Returns a list containing six instances of CoordsWithFacing, one for
         * each facing.
         *
         * @param c
         */
        public static List<CoordsWithFacing> getAllFacingsAt(Coords c) {
            List<ShortestPathFinder.CoordsWithFacing> allFacings = new ArrayList<>();
            for (int f = 0; f < 6; f++) {
                allFacings.add(new ShortestPathFinder.CoordsWithFacing(c, f));
            }
            return allFacings;
        }

        final private Coords coords;

        final private int facing;

        public CoordsWithFacing(Coords c, int facing) {
            if (c == null) {
                throw new NullPointerException();
            }
            this.coords = c;
            this.facing = facing;
        }

        public CoordsWithFacing(MovePath mp) {
            this(mp.getFinalCoords(), mp.getFinalFacing());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CoordsWithFacing)) {
                return false;
            }
            CoordsWithFacing t = (CoordsWithFacing) obj;
            return (facing == t.facing) && Objects.equals(coords, t.coords);
        }

        public Coords getCoords() {
            return coords;
        }

        public int getFacing() {
            return facing;
        }

        @Override
        public int hashCode() {
            return facing + 7 * coords.hashCode();
        }

        @Override
        public String toString() {
            return String.format("%s f:%d", coords, facing);  //$NON-NLS-1$
        }
    }

    /**
     * Filters MovePaths that are forcing PSR.
     * <p/>
     * Current implementation uses MoveStep.isDanger(). This implementation is
     * broken :( It does not work properly for movement paths that use running
     * mp.
     */
    public static class MovePathRiskFilter extends AbstractPathFinder.Filter<MovePath> {
        @Override
        public boolean shouldStay(MovePath mp) {
            MoveStep step = mp.getLastStep();
            if (step == null) {
                return true;
            }
            return !step.isDanger();
        }
    }

    /**
     * Returns the final CoordsWithFacing for a given MovePath.
     */
    public static class MovePathDestinationMap
            implements AbstractPathFinder.DestinationMap<CoordsWithFacing, MovePath> {
        @Override
        public CoordsWithFacing getDestination(MovePath e) {
            MoveStep lastStep = e.getLastStep();

            /*
             * entity moving backwards is like entity with an opposite facing
             * moving forwards :) NOOOT
             */
            CoordsWithFacing cfw;
            if (lastStep != null && lastStep.isThisStepBackwards()) {
                Facing f = Facing.valueOfInt(e.getFinalFacing());
                cfw = new CoordsWithFacing(e.getFinalCoords(), f.getOpposite().getIntValue());
            } else {
                cfw = new CoordsWithFacing(e);
            }
            return cfw;
        }

        ;
    }

    /**
     * Filters edges that are illegal.
     * <p/>
     * Current implementation uses MoveStep.isMovementPossible() to verify
     * legality.
     */
    public static class MovePathLegalityFilter extends AbstractPathFinder.Filter<MovePath> {
        IGame game;

        public MovePathLegalityFilter(IGame game) {
            this.game = game;
        }

        @Override
        public boolean shouldStay(MovePath edge) {
            if (edge.getEntity().isAero()) {
                /*
                 * isMovemementPossible is currently not working for aero units,
                 * so we have to use a substitute.
                 */
                if (edge.length() == 0) {
                    return true;
                } else {
                    MoveStep lastStep = edge.getLastStep();
                    return (lastStep == null) || lastStep.getMovementType(true) != EntityMovementType.MOVE_ILLEGAL;
                }
            }
            Coords previousPosition;
            int previousElevation;
            if (edge.length() > 1) {
                MoveStep previousStep = edge.getSecondLastStep();
                previousElevation = previousStep.getElevation();
                previousPosition = previousStep.getPosition();
            } else {
                Entity entity = edge.getEntity();
                previousElevation = entity.getElevation();
                previousPosition = entity.getPosition();
            }
            return (edge.getLastStep().isMovementPossible(
                    game, previousPosition, previousElevation));
        }
    }

    /**
     * This filter removes MovePaths that need more movement points than
     * specified in constructor invocation.
     */
    public static class MovePathLengthFilter extends Filter<MovePath> {
        private final int maxMP;

        public MovePathLengthFilter(int maxMP) {
            this.maxMP = maxMP;
        }

        @Override
        public boolean shouldStay(MovePath mp) {
            return (mp.getMpUsed() <= maxMP);

        }
    }
    
    /**
     * This filter removes paths that have spent all of of the Aeros current
     * velocity.
     * 
     * @author arlith
     *
     */
    public static class MovePathVelocityFilter extends Filter<MovePath> {

        public MovePathVelocityFilter() {
        }

        @Override
        public boolean shouldStay(MovePath mp) {
            return mp.getFinalVelocity() >= 0;
        }
    }    

    /**
     * A MovePath comparator that compares movement points spent.
     * 
     * Order paths with fewer used MP first.
     */
    public static class MovePathMPCostComparator implements Comparator<MovePath> {
        @Override
        public int compare(final MovePath first, final MovePath second) {
            final int firstDist = first.getMpUsed();
            final int secondDist = second.getMpUsed();
            return firstDist - secondDist;
        }
    }
    
    /**
     * A MovePath comparator that compares number of steps. Generally used for
     * spheroids, which don't have the same velocity expenditure restrictions as
     * Aerodynes, however they can't compare based on MP like ground units.
     * Spheroids should be able to safely compare based upon path length.
     * 
     * Order paths with fewer steps first.
     */
    public static class MovePathLengthComparator implements
            Comparator<MovePath> {
        @Override
        public int compare(final MovePath first, final MovePath second) {
            final int firstSteps = first.getStepVector().size();
            final int secondSteps = second.getStepVector().size();
            return firstSteps - secondSteps;
        }
    }
    
    /**
     * A MovePath comparator that compares remaining velocity points.  Aerodynes
     * need to spend all of their remaining velocity, so should explore
     * paths that expend the velocity first.
     * 
     * Order paths with lower remaining velocities higher.
     */
    public static class MovePathVelocityCostComparator implements
            Comparator<MovePath> {
        @Override
        public int compare(final MovePath first, final MovePath second) {
            boolean firstFlyoff = first.contains(MoveStepType.OFF)
                    || first.contains(MoveStepType.RETURN);
            int velFirst = first.getFinalVelocityLeft();
            // If we are flying off, treat this as 0 remaining velocity
            if (firstFlyoff) {
                velFirst = 0;
            }
            boolean secondFlyoff = second.contains(MoveStepType.OFF)
                    || second.contains(MoveStepType.RETURN); 
            int velSecond = second.getFinalVelocityLeft();
            // If we are flying off, treat this as 0 remaining velocity
            if (secondFlyoff) {
                velSecond = 0;
            }
            
            // If both paths are flying off, compare on number of steps 
            // Shorter paths are better
            if (firstFlyoff && secondFlyoff) {
                return first.length() - second.length();
            } else {
                return velFirst - velSecond;
            }
        }
    }

    /**
     * Functional Interface for {@link #getAdjacent(MovePath)}
     */
    public static class NextStepsAdjacencyMap implements AdjacencyMap<MovePath> {
        protected final MoveStepType stepType;
        protected final boolean backwardsStep;

        /**
         * @param stepType
         */
        public NextStepsAdjacencyMap(MoveStepType stepType) {
            this.stepType = stepType;
            backwardsStep = stepType == MoveStepType.BACKWARDS;
        }

        /**
         * Produces set of MovePaths by extending MovePath mp with MoveSteps.
         * The set of extending steps include {F, L, R, UP, ShL, ShR} if
         * applicable. If stepType is equal to MoveStepType.BACKWARDS then
         * extending steps include also {B, ShBL, ShBR}. If stepType is equal to
         * MoveStep.DFA or MoveStep.CHARGE then it is added to the resulting
         * set.
         *
         * @param mp the MovePath to be extended
         * @see AbstractPathFinder.AdjacencyMap
         */
        @Override
        public Collection<MovePath> getAdjacent(MovePath mp) {
            final MoveStep last = mp.getLastStep();
            final MoveStepType lType = (last == null) ? null : last.getType();
            final Entity entity = mp.getEntity();

            final ArrayList<MovePath> result = new ArrayList<MovePath>();

            /*
             * In case we process Aero lets check if it have flown of the map,
             * if thats the case no more movements are possible and return empty
             * list.
             */
            if (entity.isAero() &&
                (lType == MoveStepType.OFF || lType == MoveStepType.RETURN)) {
                return result;
            }


            if (lType != MoveStepType.TURN_LEFT) {
                result.add(mp.clone().addStep(MoveStepType.TURN_RIGHT));
            }
            if (lType != MoveStepType.TURN_RIGHT) {
                result.add(mp.clone().addStep(MoveStepType.TURN_LEFT));
            }


            /*
             * If the unit is prone or hull-down it limits movement options,
             * such units can only turn or get up. (unless it's a tank; tanks
             * can just drive out of hull-down and they cannot be prone)
             */
            if (mp.getFinalProne() || (mp.getFinalHullDown() && !(entity instanceof Tank))) {
                if (entity.isCarefulStand()) {
                    result.add(mp.clone().addStep(MoveStepType.CAREFUL_STAND));
                } else {
                    result.add(mp.clone().addStep(MoveStepType.GET_UP));
                }
                return result;
            }

            if (mp.canShift()) {
                if (backwardsStep) {
                    result.add(mp.clone().addStep(MoveStepType.LATERAL_RIGHT_BACKWARDS));
                    result.add(mp.clone().addStep(MoveStepType.LATERAL_LEFT_BACKWARDS));
                } else { 
                    result.add(mp.clone().addStep(MoveStepType.LATERAL_RIGHT));
                    result.add(mp.clone().addStep(MoveStepType.LATERAL_LEFT));
                }
            }
            
            if (backwardsStep) {
                result.add(mp.clone().addStep(MoveStepType.BACKWARDS));
            } else {
                result.add(mp.clone().addStep(MoveStepType.FORWARDS));
            }
            
            Coords pos;
            int elevation;
            if (last != null) {
                pos = last.getPosition();
                elevation = last.getElevation();
            } else {
                pos = entity.getPosition();
                elevation = entity.getElevation();
            }                
             
            if (entity.canGoUp(elevation, pos)) {
                result.add(mp.clone().addStep(MoveStepType.UP));
            }
            if (entity.canGoDown(elevation, pos)) {
                result.add(mp.clone().addStep(MoveStepType.DOWN));
            }            

            return result;
        }
    }

    /**
     * Functional Interface for {@link #getAdjacent(MovePath)}
     */
    public static class NextStepsExtendedAdjacencyMap extends NextStepsAdjacencyMap {
        //this class is not tested, yet.

        public NextStepsExtendedAdjacencyMap(MoveStepType stepType) {
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
            Collection<MovePath> result = super.getAdjacent(mp);
            // fly off of edge of board
            Coords c = mp.getFinalCoords();
            IGame game = mp.getEntity().getGame();
            if (((c.getX() == 0) || (c.getY() == 0)
                    || (c.getX() == (game.getBoard().getWidth() - 1)) 
                    || (c.getY() == (game.getBoard().getHeight() - 1)))
                && (mp.getFinalVelocity() > 0)) {
                result.add(mp.clone().addStep(MoveStepType.RETURN));
            }
            if (!mp.contains(MoveStepType.MANEUVER)) {
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
            }
            return result;
        }
    }

    /**
     * Creates a new instance of MovePathFinder. Sets DestinationMap to
     * {@link MovePathDestinationMap} and adds {@link MovePathLegalityFilter}.
     * Rest of the methods needed by AbstractPathFinder have to be passed as a
     * parameter.
     */
    public MovePathFinder(EdgeRelaxer<C, MovePath> edgeRelaxer,
                          AdjacencyMap<MovePath> edgeAdjacencyMap,
                          Comparator<MovePath> comparator,
                          IGame game) {
        super(new MovePathDestinationMap(),
              edgeRelaxer,
              edgeAdjacencyMap,
              comparator);
    }

    /**
     * Returns a map of all computed shortest paths
     */
    protected Map<Coords, C> getAllComputedCosts(Comparator<C> comp) {
        Set<Coords> computedCoords = new HashSet<>();
        Map<ShortestPathFinder.CoordsWithFacing, C> nodes = getPathCostMap();
        for (ShortestPathFinder.CoordsWithFacing cf : nodes.keySet()) {
            computedCoords.add(cf.getCoords());
        }

        Map<Coords, C> pathsMap = new HashMap<>();
        for (Coords coords : computedCoords) {
            pathsMap.put(coords, getCost(coords, comp));
        }
        return pathsMap;
    }

    /**
     * Returns computed cost to reach the hex at c coordinates. If multiple path
     * are present with different final facings, the one minimal one is chosen.
     * If none paths are present then {@code null} is returned.
     *
     * @param c
     * @param comp - comparator used if multiple paths are present
     * @return shortest path to the hex at c coordinates or {@code null}
     */
    protected C getCost(Coords coords, Comparator<C> comp) {
        List<CoordsWithFacing> allFacings = CoordsWithFacing.getAllFacingsAt(coords);
        List<C> paths = new ArrayList<>();
        for (ShortestPathFinder.CoordsWithFacing n : allFacings) {
            C cost = getCostOf(n);
            if (cost != null) {
                paths.add(cost);
            }
        }

        return paths.size() > 0 ? Collections.min(paths, comp) : null;
    }

}