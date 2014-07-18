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

import megamek.common.Aero;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.IGame;
import megamek.common.Tank;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.MovePath;

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
     * 
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
            if (c == null)
                throw new NullPointerException();
            this.coords = c;
            this.facing = facing;
        }

        public CoordsWithFacing(MovePath mp) {
            this(mp.getFinalCoords(), mp.getFinalFacing());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CoordsWithFacing))
                return false;
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
     * 
     * Current implementation uses MoveStep.isDanger(). This implementation is
     * broken :( It does not work properly for movement paths that use running
     * mp.
     * 
     */
    public static class MovePathRiskFilter extends AbstractPathFinder.Filter<MovePath> {
        @Override
        public boolean shouldStay(MovePath mp) {
            MoveStep step = mp.getLastStep();
            if (step == null)
                return true;
            return !step.isDanger();
        }
    }

    /**
     * Returns the final CoordsWithFacing for a given MovePath.
     * 
     */
    public static class MovePathDestinationMap
            implements AbstractPathFinder.DestinationMap<CoordsWithFacing, MovePath> {
        @Override
        public CoordsWithFacing getDestination(MovePath e) {
            return new CoordsWithFacing(e);
        };
    }

    /**
     * Filters edges that are illegal.
     * 
     * Current implementation uses MoveStep.isMovementPossible() to verify
     * legality.
     * 
     */
    public static class MovePathLegalityFilter extends AbstractPathFinder.Filter<MovePath> {
        IGame game;

        public MovePathLegalityFilter(IGame game) {
            this.game = game;
        }

        @Override
        public boolean shouldStay(MovePath edge) {
            if (edge.getEntity() instanceof Aero) {
                /*
                 * isMovemementPossible is currently not working for aero units,
                 * so we have to use a substitute.
                 */
                if (edge.length() == 0)
                    return true;
                else
                    return edge.getLastStep().getMovementType() != EntityMovementType.MOVE_ILLEGAL;
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
     * 
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
     * A MovePath comparator that compares movement points spent.
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
     * Functional Interface for {@link #getAdjacent(MovePath)}
     */
    public static class NextStepsAdjacencyMap implements AdjacencyMap<MovePath> {
        private final MoveStepType stepType;
        private boolean walking;
        private boolean charge;

        /**
         * @param stepType
         */
        public NextStepsAdjacencyMap(MoveStepType stepType) {
            this.stepType = stepType;
            walking = stepType == MoveStepType.BACKWARDS;
            charge = (stepType == MoveStepType.CHARGE) || (stepType == MoveStepType.DFA);
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
         * 
         * @see AbstractPathFinder.AdjacencyMap
         * 
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
            if (entity instanceof Aero &&
                    (lType == MoveStepType.OFF || lType == MoveStepType.RETURN))
                return result;

            if (lType != MoveStepType.TURN_LEFT)
                result.add(mp.clone().addStep(MoveStepType.TURN_RIGHT));
            if (lType != MoveStepType.TURN_RIGHT)
                result.add(mp.clone().addStep(MoveStepType.TURN_LEFT));

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
                result.add(mp.clone().addStep(MoveStepType.LATERAL_RIGHT));
                result.add(mp.clone().addStep(MoveStepType.LATERAL_LEFT));
                if (walking) {
                    result.add(mp.clone().addStep(MoveStepType.LATERAL_RIGHT_BACKWARDS));
                    result.add(mp.clone().addStep(MoveStepType.LATERAL_LEFT_BACKWARDS));
                }
            }

            result.add(mp.clone().addStep(MoveStepType.FORWARDS));
            if (walking)
                result.add(mp.clone().addStep(MoveStepType.BACKWARDS));

            if (charge){
                result.add(mp.clone().addStep(stepType));
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
     * 
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
            if (cost != null)
                paths.add(cost);
        }

        return paths.size() > 0 ? Collections.min(paths, comp) : null;
    }

}