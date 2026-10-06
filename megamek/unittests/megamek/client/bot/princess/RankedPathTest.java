/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import megamek.common.moves.MovePath;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link RankedPath#compareTo(RankedPath)} implements a valid total order (issue #8436). The previous
 * final tie-break (`hashCode() > 0 ? 1 : -1`) ignored the argument and was not antisymmetric, corrupting the
 * {@code TreeSet<RankedPath>} that {@code PathRanker} uses for best-path selection.
 */
class RankedPathTest {

    private static RankedPath rankedPath(double rank, int hexesMoved, double expectedDamage) {
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getHexesMoved()).thenReturn(hexesMoved);
        RankedPath rankedPath = new RankedPath(rank, mockPath, "test");
        rankedPath.setExpectedDamage(expectedDamage);
        return rankedPath;
    }

    /**
     * @return A spread of paths: distinct ranks, ties on rank broken by hexes/damage, and a group fully tied on all
     *       three ranking keys (so only the creation-order tie-break separates them).
     */
    private static List<RankedPath> samplePaths() {
        List<RankedPath> paths = new ArrayList<>();
        paths.add(rankedPath(10.0, 3, 5.0));
        paths.add(rankedPath(10.0, 3, 8.0));
        paths.add(rankedPath(10.0, 5, 5.0));
        paths.add(rankedPath(-4.0, 2, 0.0));
        paths.add(rankedPath(139.17, 4, 12.0));
        // Three paths identical on every ranking key - the pathological tie the old comparator mishandled.
        paths.add(rankedPath(50.0, 4, 7.0));
        paths.add(rankedPath(50.0, 4, 7.0));
        paths.add(rankedPath(50.0, 4, 7.0));
        return paths;
    }

    @Test
    void comparatorIsAntisymmetricForEveryPair() {
        List<RankedPath> paths = samplePaths();
        for (RankedPath first : paths) {
            for (RankedPath second : paths) {
                int forward = first.compareTo(second);
                int backward = second.compareTo(first);
                assertEquals(Integer.signum(forward), -Integer.signum(backward),
                      "compareTo must be antisymmetric: sgn(a.compareTo(b)) == -sgn(b.compareTo(a))");
            }
        }
    }

    @Test
    void comparatorIsTransitive() {
        List<RankedPath> paths = samplePaths();
        for (RankedPath first : paths) {
            for (RankedPath second : paths) {
                for (RankedPath third : paths) {
                    boolean firstBeforeSecond = first.compareTo(second) < 0;
                    boolean secondBeforeThird = second.compareTo(third) < 0;
                    if (firstBeforeSecond && secondBeforeThird) {
                        assertTrue(first.compareTo(third) < 0,
                              "compareTo must be transitive: a < b and b < c implies a < c");
                    }
                }
            }
        }
    }

    @Test
    void everyDistinctInstanceComparesNonZeroAndIsSelfConsistent() {
        List<RankedPath> paths = samplePaths();
        for (RankedPath first : paths) {
            assertEquals(0, first.compareTo(first), "A path must compare equal to itself");
            for (RankedPath second : paths) {
                if (first != second) {
                    assertTrue(first.compareTo(second) != 0,
                          "Distinct instances must never tie, so the TreeSet keeps them all");
                }
            }
        }
    }

    @Test
    void treeSetRetainsAllFullyTiedPaths() {
        List<RankedPath> tiedPaths = List.of(
              rankedPath(50.0, 4, 7.0),
              rankedPath(50.0, 4, 7.0),
              rankedPath(50.0, 4, 7.0),
              rankedPath(50.0, 4, 7.0));
        TreeSet<RankedPath> treeSet = new TreeSet<>(tiedPaths);
        assertEquals(tiedPaths.size(), treeSet.size(),
              "A valid total order must not collapse paths that are tied on every ranking key");
    }

    @Test
    void reverseOrderedTreeSetBreaksTiesTowardTheEarliestPath() {
        // Among paths tied on every ranking key, the earliest-created (earliest-enumerated) path wins, so the
        // choice follows PathRanker's deterministic enumeration order rather than construction-timing luck.
        RankedPath firstEnumerated = rankedPath(50.0, 4, 7.0);
        RankedPath secondEnumerated = rankedPath(50.0, 4, 7.0);
        RankedPath thirdEnumerated = rankedPath(50.0, 4, 7.0);

        TreeSet<RankedPath> reverseOrdered = new TreeSet<>(Collections.reverseOrder());
        // Insert out of creation order to prove the winner depends on creation order, not insertion order.
        reverseOrdered.add(thirdEnumerated);
        reverseOrdered.add(firstEnumerated);
        reverseOrdered.add(secondEnumerated);

        assertSame(firstEnumerated, reverseOrdered.first(),
              "The earliest-created path must win a tie on every ranking key");
    }

    @Test
    void reverseOrderedTreeSetSelectsHighestRankFirst() {
        // Mirrors PathRanker.getBestPath, which reads the first element of a reverse-ordered TreeSet.
        RankedPath best = rankedPath(200.0, 4, 9.0);
        List<RankedPath> paths = new ArrayList<>(samplePaths());
        paths.add(best);
        Collections.shuffle(paths, new Random(1));

        TreeSet<RankedPath> reverseOrdered = new TreeSet<>(Collections.reverseOrder());
        reverseOrdered.addAll(paths);

        assertEquals(best, reverseOrdered.first(), "The highest-ranked path must be selected deterministically");
    }
}
