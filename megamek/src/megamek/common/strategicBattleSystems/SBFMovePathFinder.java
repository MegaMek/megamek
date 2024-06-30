/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.strategicBattleSystems;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.pathfinder.AbstractPathFinder;

import java.util.*;

public class SBFMovePathFinder extends AbstractPathFinder<BoardLocation, SBFMovePath, SBFMovePath> {

    /**
     * @param edgeDestinationMap functional interface for retrieving destination
     *                           node of an edge.
     * @param edgeRelaxer        functional interface for calculating relaxed cost.
     * @param edgeAdjacencyMap   functional interface for retrieving neighbouring
     *                           edges.
     * @param edgeComparator     implementation of path comparator. Each path is
     *                           defined by its last edge. <i>(path:= edge concatenated with
     *                           best path to the source of the edge)</i>
     */
    public SBFMovePathFinder(SBFGame game,
                             DestinationMap<BoardLocation, SBFMovePath> edgeDestinationMap,
                             EdgeRelaxer<SBFMovePath, SBFMovePath> edgeRelaxer,
                             AdjacencyMap<SBFMovePath> edgeAdjacencyMap,
                             Comparator<SBFMovePath> edgeComparator) {
        super(edgeDestinationMap, edgeRelaxer, edgeAdjacencyMap, edgeComparator);
    }

    /**
     * Produces new instance of shortest path searcher. It will find all the
     * shortest paths between starting points that are reachable with at most
     * maxMp move points.
     *
     * @param maxMP maximum MP that entity can use
     * @param game The {@link SBFGame}
     */
    public static SBFMovePathFinder moveEnvelopeFinder(int maxMP, SBFGame game) {
        SBFMovePathFinder spf = new SBFMovePathFinder(game, SBFMovePath::getLastPosition, new MovePathRelaxer(),
                        new GroundMovementAdjacency(game), Comparator.comparingInt(SBFMovePath::getMpUsed));
        spf.addFilter(new MovePathLengthFilter(maxMP));
//        spf.addFilter(new MovePathLegalityFilter(game));
        return spf;
    }

    /**
     * This filter removes MovePaths that need more movement points than
     * specified in constructor invocation.
     */
    public static class MovePathLengthFilter extends Filter<SBFMovePath> {
        private final int maxMP;

        public MovePathLengthFilter(int maxMP) {
            this.maxMP = maxMP;
        }

        @Override
        public boolean shouldStay(SBFMovePath mp) {
            return (mp.getMpUsed() <= maxMP);

        }
    }

    /**
     * A MovePath comparator that compares movement points spent.
     *
     * Order paths with fewer used MP first.
     */
    public static class MovePathMPCostComparator implements Comparator<SBFMovePath> {
        @Override
        public int compare(final SBFMovePath first, final SBFMovePath second) {
            return Integer.compare(first.getMpUsed(), second.getMpUsed());
        }
    }

    public static class GroundMovementAdjacency implements AdjacencyMap<SBFMovePath> {
        private final SBFGame game;

        public GroundMovementAdjacency(SBFGame game) {
            this.game = game;
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
         * @see AdjacencyMap
         */
        @Override
        public Collection<SBFMovePath> getAdjacent(SBFMovePath mp) {
            List<SBFMovePath> result = new ArrayList<>();
            BoardLocation currentDestination = mp.getLastPosition();
            List<BoardLocation> possibleDestinations = currentDestination.allAdjacent();
            possibleDestinations.removeIf(bl -> !game.hasBoardLocation(bl));
            for (BoardLocation newDestination : possibleDestinations) {
                SBFMovePath newPath = SBFMovePath.createMovePathShallow(mp);
                newPath.addStep(SBFMoveStep.createGroundMovementStep(currentDestination, newDestination));
                result.add(newPath);
            }

            /*
             * If the unit is prone or hull-down it limits movement options,
             * such units can only turn or get up. (unless it's a tank; tanks
             * can just drive out of hull-down and they cannot be prone)
             */
//            if ((mp.getFinalHullDown() && !(entity instanceof Tank))) {
//                if (entity.isCarefulStand()) {
//                    result.add(mp.clone().addStep(MovePath.MoveStepType.CAREFUL_STAND));
//                } else {
//                    result.add(mp.clone().addStep(MovePath.MoveStepType.GET_UP));
//                }
//                return result;
//            }


            return result;
        }
    }

    /**
     * Relaxes edge by favouring MovePaths that end in a not prone stance.
     */
    public static class MovePathRelaxer
            implements AbstractPathFinder.EdgeRelaxer<SBFMovePath, SBFMovePath> {
        @Override
        public SBFMovePath doRelax(SBFMovePath v, SBFMovePath e, Comparator<SBFMovePath> comparator) {
            if (v == null) {
                return e;
            }

            // We have to be standing to be able to move
            // Maybe I should replace this extra condition with a flag in node(?)
//            boolean vprone = v.getFinalProne(), eprone = e.getFinalProne();
//            if (vprone != eprone) {
//                return vprone ? e : null;
//            }
//            if (!(v.getEntity() instanceof Tank)) {
//                boolean vhdown = v.getFinalHullDown(), ehdown = e.getFinalHullDown();
//                if (vhdown != ehdown) {
//                    return vhdown ? e : null;
//                }
//            }

            return comparator.compare(e, v) < 0 ? e : null;
        }
    }

    /**
     * Returns a map of all computed shortest paths. If multiple paths to a
     * single hex, each with different final facing, are present, then the
     * minimal one is chosen for each hex.
     *
     * @return a map of all computed shortest paths.
     */
    public Map<BoardLocation, SBFMovePath> getAllComputedPaths() {
        return getAllComputedCosts(getComparator());
    }

    /**
     * Returns a map of all computed shortest paths
     */
    protected Map<BoardLocation, SBFMovePath> getAllComputedCosts(Comparator<SBFMovePath> comp) {
        Set<BoardLocation> allDestinations = new HashSet<>(getPathCostMap().keySet());

        Map<BoardLocation, SBFMovePath> pathsMap = new HashMap<>();
        for (BoardLocation coords : allDestinations) {
            pathsMap.put(coords, getCost(coords, comp));
        }
        return pathsMap;
    }

    /**
     * Returns computed cost to reach the hex at c coordinates. If multiple path
     * are present with different final facings, the one minimal one is chosen.
     * If none paths are present then {@code null} is returned.
     *
     * @param coords
     * @param comp comparator used if multiple paths are present
     * @return shortest path to the hex at c coordinates or {@code null}
     */
    protected @Nullable SBFMovePath getCost(BoardLocation coords, Comparator<SBFMovePath> comp) {
//        List<SBFMovePath> paths = new ArrayList<>();
//        SBFMovePath cost = getCostOf(coords);
//        paths.add(cost);

        return getCostOf(coords);
    }
}
