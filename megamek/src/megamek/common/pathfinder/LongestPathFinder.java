/*
* Copyright (c) 2014-2022 - The MegaMek Team. All Rights Reserved.
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common.pathfinder;

import megamek.client.bot.princess.MinefieldUtil;
import megamek.common.*;
import megamek.common.MovePath.MoveStepType;
import megamek.common.annotations.Nullable;

import java.util.*;

/**
 * Path finder that specialises in finding paths that can enter a single hex
 * multiple times. For example longest path searches.
 *
 * @author Saginatio
 */
public class LongestPathFinder extends MovePathFinder<Deque<MovePath>> {
    private boolean aero = false;

    protected LongestPathFinder(EdgeRelaxer<Deque<MovePath>, MovePath> edgeRelaxer,
            AdjacencyMap<MovePath> edgeAdjacencyMap, Comparator<MovePath> comparator,
            Game game) {
        super(edgeRelaxer, edgeAdjacencyMap, comparator, game);
    }

    /**
     * Produces a path finder that searches for all paths that travel the max
     * distance ( since the last direction change ). This path finder also finds
     * (shorter) longest paths that require less mp to travel.
     *
     * @param maxMP - the maximal movement points available for an entity
     * @param stepType - if equal to MoveStepType.BACKWARDS, then searcher also
     *            includes backward steps. Otherwise only forward movement is
     *            allowed
     * @param game The current {@link Game}
     * @return a longest path finder
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
     * Produces a path finder for aero units that searches for all paths that
     * travel the max distance. On a ground map this can be very computational
     * heavy.
     *
     * @param maxMP - the maximal thrust points available for an aero
     * @param game The current {@link Game}
     * @return a longest path finder for aeros
     */
    public static LongestPathFinder newInstanceOfAeroPath(int maxMP, Game game) {
        LongestPathFinder lpf = new LongestPathFinder(new AeroMultiPathRelaxer(!game.getBoard().inSpace()),
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
     *
     *
     * {@code ( movement points used; -(hexes moved) )}
     *
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
     * Comparator that sorts MovePaths based on, in order, the following criteria:
     * Minefield hazard (stepping on less mines is better)
     * Least MP used
     * Most distance moved
     */
    public static class MovePathMinefieldAvoidanceMinMPMaxDistanceComparator extends MovePathMinMPMaxDistanceComparator {
        @Override
        public int compare(MovePath first, MovePath second) {
            double firstMinefieldScore = MinefieldUtil.calcMinefieldHazardForHex(first.getLastStep(),
                    first.getEntity(), first.isJumping(), false);
            double secondMinefieldScore = MinefieldUtil.calcMinefieldHazardForHex(second.getLastStep(),
                    second.getEntity(), second.isJumping(), false);
               
            return (Double.compare(secondMinefieldScore, firstMinefieldScore) == 0)
                    ? super.compare(first, second) : 0;
        }
    }

    /**
     * Relaxer for longest path movement. Current implementation needs
     * Comparator that preserves MovePathMinMPMaxDistanceComparator contract.
     *
     * It adds a path to 'interesting' paths in a hex when candidate travelled more hexes.
     */
    static public class LongestPathRelaxer implements EdgeRelaxer<Deque<MovePath>, MovePath> {
        @Override
        public Deque<MovePath> doRelax(Deque<MovePath> v, MovePath mpCandidate, Comparator<MovePath> comparator) {
            if (mpCandidate == null) {
                throw new NullPointerException();
            }
            if (v == null) {
                return new ArrayDeque<>(Collections.singleton(mpCandidate));
            }
            while (!v.isEmpty()) { //we could get rid of this loop, since we require a proper comparator
                MovePath topMP = v.getLast();

                //standing up is always reasonable for mechs
                boolean vprone = topMP.getFinalProne(), eprone = mpCandidate.getFinalProne();
                if (vprone != eprone) {
                    if (vprone) {
                        break;
                    } else {
                        return null;
                    }
                }
                if (!(topMP.getEntity() instanceof Tank)) {
                    boolean vhdown = topMP.getFinalHullDown(), ehdown = mpCandidate.getFinalHullDown();
                    if (vhdown != ehdown) {
                        if (vhdown) {
                            break;
                        } else {
                            return null;
                        }
                    }
                }
                /*
                 * We require that the priority queue v of MovePath is sorted
                 * lexicographically using product MPUsed x (-HexesMoved).
                 */
                int topMpUsed = topMP.getMpUsed(), mpCMpUsed = mpCandidate.getMpUsed();

                if (topMpUsed > mpCMpUsed) {
                    /*
                     * topMP should have less or equal 'movement points used'
                     * since it was taken from candidates priority queue earlier
                     *
                     * Current implementation of doRelax() assumes that v is
                     * sorted in such way that this situation is impossible.
                     */
                    System.err.println(new IllegalStateException(
                            "Top Move Path uses more MPs than Move Path Candidate."));
                    return null;
                } else {
                    if (topMP.getHexesMoved() > mpCandidate.getHexesMoved()) {
                        return null; //topMP path is longer and uses less or same mp.
                    }
                    if (topMP.getHexesMoved() == mpCandidate.getHexesMoved()) {
                        //we want to preserve both forward and backward movements
                        //that end in the same spot with the same cost.
                        MoveStep topStep = topMP.getLastStep();
                        boolean topBackwards = topStep != null && topStep.isThisStepBackwards();
                        MoveStep mpCandStep = mpCandidate.getLastStep();
                        boolean mpCandBackwars = mpCandStep != null && mpCandStep.isThisStepBackwards();
                        if (!(topMP.getEntity() instanceof Infantry) && topBackwards != mpCandBackwars) {
                            break;
                        }
                        return null;
                    }

                    if (topMpUsed == mpCMpUsed) {
                        //mpCandidate is not strictly better than topMp so we won't use it.
                        return null;
                    } else if (topMpUsed < mpCMpUsed) {
                        //topMP travels less but also uses less movement points so we should keep it
                        // and add mpCandidate to the list of optimal longest paths.
                        break;
                    }
                }
            }
            v.addLast(mpCandidate);
            return v;
        }

    }

    /**
     * Comparator that sorts MovePaths based on lexicographical order of
     * triples:<br>
     * {@code (hexes traveled; thrust used; 0 - (hexes flown in a straight line))}
     * <br>
     * Works only with aeros.
     */
    public static class AeroMultiPathComparator implements Comparator<MovePath> {
        /**
         * compares MovePaths based on lexicographical order of triples
         * (hexes traveled; thrust used; 0 - (hexes flown in a straight line))
         */
        @Override
        public int compare(MovePath mp1, MovePath mp2) {
            if (!mp1.getEntity().isAero()) {
                throw new IllegalArgumentException("wanted aero got:" + mp1.getClass().toString());
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
     * Relaxer for aero movement. Current implementation needs Comparator that
     * preserves AeroMultiPathComparator contract.
     *
     * It adds a path to 'interesting' paths in a hex when: candidate 1) traveled
     * more hexes or either 2a) thrust_used is less or 2b) straight_hexes_flown is
     * greater than current top of the stack.
     */
    public static class AeroMultiPathRelaxer implements EdgeRelaxer<Deque<MovePath>, MovePath> {
        boolean inAtmosphere;

        private AeroMultiPathRelaxer(boolean inAtmosphere) {
            this.inAtmosphere = inAtmosphere;
        }

        @Override
        public @Nullable Deque<MovePath> doRelax(Deque<MovePath> v, MovePath mpCandidate, Comparator<MovePath> comparator) {
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
                System.err.println(new IllegalStateException("Top Move Path moved more than Move Path Candidate."));
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
                System.err.println(new IllegalStateException(
                        "Top Move Path uses more MPs than Move Path Candidate. "
                        + "while traveling the same distance"));
            }

            if (!inAtmosphere) {
                // there is no point considering hexes flown straight if we are not in atmosphere
                return null;
            }

            // while in atmosphere we should consider paths that have higher thrust used but flew more hexes straight
            MoveStep topLastStep = topMP.getLastStep();
            MoveStep candidateLastStep = mpCandidate.getLastStep();
            int hs1 = topLastStep == null ? 0 : topLastStep.getNStraight();
            int hs2 = candidateLastStep == null ? 0 : candidateLastStep.getNStraight();
            int dHS = hs1 - hs2;

            if (-dHS > 0) {
                if (dMP >= 0) {
                    System.err.println(new IllegalStateException(
                            "Top Move Path uses more MPs than Move Path Candidate and " +
                            "Top Move Path moves a shorter straight line distance."));
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
     * Returns the longest move path to a hex at given coordinates. If multiple
     * paths reach coords with different final facings, the best one is chosen.
     * If none paths are present then {@code null} is returned.
     *
     * @param coords - the coordinates of the hex
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
                    MovePath qlast = tq.removeLast();
                    if (mp.getHexesMoved() == qlast.getHexesMoved() && mp.getMpUsed() > qlast.getMpUsed()) {
                        mp = qlast;
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
     * Returns a map of all computed longest paths. This also includes paths
     * that are shorter but use strictly less movement points.
     *
     * @return a map of all computed shortest paths.
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
     * Returns a map of all computed longest paths. This only includes one
     * longest path to one Coords, Facing pair.
     *
     * @return a map of all computed shortest paths.
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
