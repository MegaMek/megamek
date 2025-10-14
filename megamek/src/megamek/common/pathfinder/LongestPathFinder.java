/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.pathfinder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import megamek.client.bot.princess.MinefieldUtil;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.pathfinder.comparators.MovePathMPCostComparator;
import megamek.common.pathfinder.filters.MovePathLegalityFilter;
import megamek.common.pathfinder.filters.MovePathLengthFilter;
import megamek.common.units.Infantry;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;

/**
 * Pathfinder that specialises in finding paths that can enter a single hex multiple times. For example the longest path
 * searches.
 *
 * @author Saginatio
 */
public class LongestPathFinder extends MovePathFinder<Deque<MovePath>> {
    private static final MMLogger logger = MMLogger.create(LongestPathFinder.class);

    private boolean aero = false;

    protected LongestPathFinder(EdgeRelaxer<Deque<MovePath>, MovePath> edgeRelaxer,
          AdjacencyMap<MovePath> edgeAdjacencyMap, Comparator<MovePath> comparator,
          Game game) {
        super(edgeRelaxer, edgeAdjacencyMap, comparator, game);
    }

    /**
     * Produces a pathfinder that searches for all paths that travel the max distance ( since the last direction change
     * ). This pathfinder also finds (shorter) the longest paths that require less mp to travel.
     *
     * @param maxMP    - the maximal movement points available for an entity
     * @param stepType - if equal to MoveStepType.BACKWARDS, then searcher also includes backward steps. Otherwise, only
     *                 forward movement is allowed
     * @param game     The current {@link Game}
     *
     * @return the longest pathfinder
     */
    public static LongestPathFinder newInstanceOfLongestPath(int maxMP, MoveStepType stepType, Game game) {
        LongestPathFinder lpf = new LongestPathFinder(new LongestPathRelaxer(),
              new NextStepsAdjacencyMap(stepType),
              new MovePathMinMPMaxDistanceComparator(),
              game);
        lpf.addFilter(new MovePathLengthFilter(maxMP));
        lpf.addFilter(new MovePathLegalityFilter(game));
        return lpf;
    }

    /**
     * Produces a pathfinder for aero units that searches for all paths that travel the max distance. On a ground map
     * this can be very computational heavy.
     *
     * @param maxMP - the maximal thrust points available for an aero
     * @param game  The current {@link Game}
     *
     * @return the longest pathfinder for aerospace
     */
    public static LongestPathFinder newInstanceOfAeroPath(int maxMP, Game game) {
        LongestPathFinder lpf = new LongestPathFinder(new AeroMultiPathRelaxer(!game.getBoard().isSpace()),
              new NextStepsAdjacencyMap(MoveStepType.FORWARDS),
              new AeroMultiPathComparator(),
              game);
        lpf.aero = true;
        lpf.addFilter(new MovePathLengthFilter(maxMP));
        lpf.addFilter(new MovePathLegalityFilter(game));
        return lpf;
    }

    /**
     * Comparator that sorts MovePaths based on lexicographical order of pairs:<br>
     * <p>
     * <p>
     * {@code ( movement points used; -(hexes moved) )}
     */
    public static class MovePathMinMPMaxDistanceComparator extends MovePathMPCostComparator {
        @Override
        public int compare(MovePath first, MovePath second) {
            int s = super.compare(first, second);
            if (s != 0) {
                return s;
            } else {
                return second.getHexesMoved() - first.getHexesMoved();
            }
        }
    }

    /**
     * Comparator that sorts MovePaths based on, in order, the following criteria: Minefield hazard (stepping on fewer
     * mines is better) The Least MP used Most distance moved
     */
    public static class MovePathMinefieldAvoidanceMinMPMaxDistanceComparator
          extends MovePathMinMPMaxDistanceComparator {
        @Override
        public int compare(MovePath first, MovePath second) {
            double firstMinefieldScore = MinefieldUtil.calcMinefieldHazardForHex(first.getLastStep(),
                  first.getEntity(), first.isJumping(), false);
            double secondMinefieldScore = MinefieldUtil.calcMinefieldHazardForHex(second.getLastStep(),
                  second.getEntity(), second.isJumping(), false);

            return (Double.compare(secondMinefieldScore, firstMinefieldScore) == 0)
                  ? super.compare(first, second)
                  : 0;
        }
    }

    /**
     * Relaxer for longest path movement. Current implementation needs Comparator that preserves
     * MovePathMinMPMaxDistanceComparator contract.
     * <p>
     * It adds a path to 'interesting' paths in a hex when candidate traveled more hexes.
     */
    static public class LongestPathRelaxer implements EdgeRelaxer<Deque<MovePath>, MovePath> {
        @Override
        public Deque<MovePath> doRelax(Deque<MovePath> possibleMovePaths, MovePath movePathCandidate,
              Comparator<MovePath> comparator) {
            if (movePathCandidate == null) {
                throw new NullPointerException("MovePath candidate is null");
            }
            if (possibleMovePaths == null) {
                return new ArrayDeque<>(Collections.singleton(movePathCandidate));
            }
            // Should be a comparator instead of this function call
            if (candidateIsWorseThanTopOfStack(possibleMovePaths, movePathCandidate)) {
                return null;
            }
            possibleMovePaths.addLast(movePathCandidate);
            return possibleMovePaths;
        }
    }

    /**
     * Evaluates if the candidate is worse than the top of the stack or not.
     *
     * @param possibleMovePaths the stack of possible move paths
     * @param movePathCandidate the candidate move path
     *
     * @return returns false if the candidate is better than the top of the stack, true otherwise
     */
    private static boolean candidateIsWorseThanTopOfStack(Deque<MovePath> possibleMovePaths,
          MovePath movePathCandidate) {
        if (possibleMovePaths.isEmpty()) {
            return false;
        }

        MovePath topMP = possibleMovePaths.getLast();

        if (topMP.getFinalProne() != movePathCandidate.getFinalProne()) {
            return !topMP.getFinalProne();
        }
        if (!(topMP.getEntity() instanceof Tank)) {
            if (topMP.getFinalHullDown() != movePathCandidate.getFinalHullDown()) {
                return !topMP.getFinalHullDown();
            }
        }
        /*
         * We require that the priority queue v of MovePath is sorted
         * lexicographically using product MPUsed x (-HexesMoved).
         */
        int topMpUsed = topMP.getMpUsed();
        int mpCMpUsed = movePathCandidate.getMpUsed();

        if (topMpUsed > mpCMpUsed) {
            /*
             * topMP should have less or equal 'movement points used'
             * since it was taken from candidates priority queue earlier
             *
             * Current implementation of doRelax() assumes that v is
             * sorted in such way that this situation is impossible.
             */
            logger.debug(
                  "Top Move Path uses more MPs than Move Path Candidate. Top={} Candidate={}",
                  topMpUsed, mpCMpUsed);
            return true;
        }

        if (topMP.getHexesMoved() > movePathCandidate.getHexesMoved()) {
            return true;
        }
        if (topMP.getHexesMoved() == movePathCandidate.getHexesMoved()) {
            // we want to preserve both forward and backward movements
            // that end in the same spot with the same cost.
            MoveStep topStep = topMP.getLastStep();
            boolean topBackwards = topStep != null && topStep.isThisStepBackwards();
            MoveStep candidateLastStep = movePathCandidate.getLastStep();
            boolean lastStepIsBackwards = (candidateLastStep != null) && candidateLastStep.isThisStepBackwards();
            if (!(topMP.getEntity() instanceof Infantry) && topBackwards != lastStepIsBackwards) {
                return false;
            }

            // 2b) Then *also* compare distance to the desired destination:
            // If we want to favor whichever is closer to `desiredDestination`,
            // we measure from each path's final position:
            if (movePathCandidate.hasWaypoint()
                  && topMP.getFinalCoords() != null
                  && movePathCandidate.getFinalCoords() != null) {
                double topDist = topMP.getFinalCoords().distance(movePathCandidate.getWaypoint());
                double candidateDist = movePathCandidate.getFinalCoords().distance(movePathCandidate.getWaypoint());

                // if the candidate is strictly closer, prefer it:
                // break out of loop so we add candidate to `v`
                // candidate is not better, discard it
                return !(candidateDist < topDist);
            }
            return true;
        }

        // mpCandidate is not strictly better than topMp so we won't use it.
        // topMP travels less but also uses less movement points so we should keep it
        // and add mpCandidate to the list of optimal longest paths.
        return topMpUsed == mpCMpUsed;

    }

    /**
     * Comparator that sorts MovePaths based on lexicographical order of triples:<br>
     * {@code (hexes traveled; thrust used; 0 - (hexes flown in a straight line))}
     * <br>
     * Works only with aerospace.
     */
    public static class AeroMultiPathComparator implements Comparator<MovePath> {
        /**
         * compares MovePaths based on lexicographical order of triples (hexes traveled; thrust used; 0 - (hexes flown
         * in a straight line))
         */
        @Override
        public int compare(MovePath mp1, MovePath mp2) {
            if (!mp1.getEntity().isAero()) {
                throw new IllegalArgumentException("wanted aero got:" + mp1.getClass());
            }
            // we want to process shorter paths first
            int dHT = mp1.getHexesMoved() - mp2.getHexesMoved();
            if (dHT != 0) {
                return dHT;
            }
            // then those which used less thrust
            int dMP = mp1.getMpUsed() - mp2.getMpUsed();
            if (dMP != 0) {
                return dMP;
            }
            // lastly those with more hexes flown straight.
            MoveStep lms1 = mp1.getLastStep(), lms2 = mp2.getLastStep();
            int hs1 = lms1 == null ? 0 : lms1.getNStraight();
            int hs2 = lms2 == null ? 0 : lms2.getNStraight();
            int dHS = hs1 - hs2;

            return -dHS;
        }
    }

    /**
     * Relaxer for aero movement. Current implementation needs Comparator that preserves AeroMultiPathComparator
     * contract.
     * <p>
     * It adds a path to 'interesting' paths in a hex when: candidate 1) traveled more hexes or either 2a) thrust_used
     * is less or 2b) straight_hexes_flown is greater than current top of the stack.
     */
    public static class AeroMultiPathRelaxer implements EdgeRelaxer<Deque<MovePath>, MovePath> {
        boolean inAtmosphere;

        private AeroMultiPathRelaxer(boolean inAtmosphere) {
            this.inAtmosphere = inAtmosphere;
        }

        @Override
        public @Nullable Deque<MovePath> doRelax(Deque<MovePath> v, MovePath mpCandidate,
              Comparator<MovePath> comparator) {
            Objects.requireNonNull(mpCandidate);
            if (v == null) {
                return new ArrayDeque<>(Collections.singleton(mpCandidate));
            }
            MovePath topMP = v.getLast();

            /*
             * consider hexes travelled. If we enter the hex with different
             * hexes travelled then totally different paths will emerge
             */
            int dHT = topMP.getHexesMoved() - mpCandidate.getHexesMoved();
            if (dHT > 0) {
                /*
                 * Current implementation of doRelax() assumes that v is sorted
                 * in such way that this situation is impossible.
                 */
                logger.error(
                      "Top Move Path moved more than Move Path Candidate.",
                      new IllegalStateException());
            }

            if (dHT < 0) {
                v.addLast(mpCandidate);
                return v;
            }

            int dMP = topMP.getMpUsed() - mpCandidate.getMpUsed();
            if (dMP > 0) {
                /*
                 * Current implementation of doRelax() assumes that v is sorted
                 * in such way that this situation is impossible.
                 */
                logger.error(
                      "Top Move Path uses more MPs than Move Path Candidate while traveling the same distance",
                      new IllegalStateException());
            }

            if (!inAtmosphere) {
                // there is no point considering hexes flown straight if we are not in
                // atmosphere
                return null;
            }

            // while in atmosphere we should consider paths that have higher thrust used but
            // flew more hexes straight
            MoveStep topLastStep = topMP.getLastStep();
            MoveStep candidateLastStep = mpCandidate.getLastStep();
            int hs1 = topLastStep == null ? 0 : topLastStep.getNStraight();
            int hs2 = candidateLastStep == null ? 0 : candidateLastStep.getNStraight();
            int dHS = hs1 - hs2;

            if (-dHS > 0) {
                if (dMP >= 0) {
                    logger.error(
                          "Top Move Path uses more MPs than Move Path Candidate and Top Move Path moves a shorter straight line distance.",
                          new IllegalStateException());
                }
                if (topLastStep != null && !topLastStep.dueFreeTurn()) {
                    v.add(mpCandidate);
                    return v;
                }
            }

            return null;
        }
    }

    /**
     * Returns the longest move path to a hex at given coordinates. If multiple paths reach coords with different final
     * facings, the best one is chosen. If none paths are present then {@code null} is returned.
     *
     * @param coords - the coordinates of the hex
     *
     * @return the shortest move path to hex at given coordinates
     */
    public @Nullable MovePath getComputedPath(Coords coords) {
        Deque<MovePath> q = getCost(coords, (q1, q2) -> {
            MovePath mp1 = q1.getLast(), mp2 = q2.getLast();
            int t = mp2.getHexesMoved() - mp1.getHexesMoved();
            if (t != 0) {
                return t;
            } else {
                return mp1.getMpUsed() - mp2.getMpUsed();
            }
        });
        if (q != null) {
            if (!aero) {
                return q.getLast();
            } else {
                ArrayDeque<MovePath> tq = new ArrayDeque<>(q);
                MovePath mp = tq.removeLast();
                while (!tq.isEmpty()) {
                    MovePath queueLast = tq.removeLast();
                    if (mp.getHexesMoved() == queueLast.getHexesMoved() && mp.getMpUsed() > queueLast.getMpUsed()) {
                        mp = queueLast;
                    } else {
                        break;
                    }
                }
                return mp;
            }
        } else {
            return null;
        }
    }

    /**
     * Returns a map of all computed longest paths. This also includes paths that are shorter but use strictly less
     * movement points.
     *
     * @return a list of all computed shortest paths.
     */
    public List<MovePath> getAllComputedPathsUnordered() {
        Collection<Deque<MovePath>> queues = getPathCostMap().values();
        List<MovePath> l = new ArrayList<>();
        for (Deque<MovePath> q : queues) {
            l.addAll(q);
        }
        return l;
    }

    /**
     * Returns a map of all computed longest paths. This only includes one longest path to one Coords, Facing pair.
     *
     * @return a list of all computed shortest paths.
     */
    public List<MovePath> getLongestComputedPaths() {
        Collection<Deque<MovePath>> queues = getPathCostMap().values();
        List<MovePath> l = new ArrayList<>();
        for (Deque<MovePath> q : queues) {
            l.add(q.getLast());
        }
        return l;
    }
}
