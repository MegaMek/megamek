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
package megamek.common.compute.scatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.board.Coords;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScatterTest {

    @Test
    @DisplayName("standard scatter distance is the magnitude of the margin of failure")
    void standardDistanceUsesMagnitude() {
        assertEquals(5, Scatter.standardDistance(5));
        assertEquals(5, Scatter.standardDistance(-5));
        assertEquals(0, Scatter.standardDistance(0));
    }

    @Test
    @DisplayName("reduced scatter distance subtracts the reduction and never goes below zero")
    void reducedDistanceClampsAtZero() {
        assertEquals(3, Scatter.reducedDistance(5, 2));
        assertEquals(3, Scatter.reducedDistance(-5, 2));
        assertEquals(0, Scatter.reducedDistance(2, 2));
        assertEquals(0, Scatter.reducedDistance(1, 2));
        assertEquals(5, Scatter.reducedDistance(5, 0));
    }

    @Test
    @DisplayName("advanced scatter rolls one die per leg for every two points of margin of failure")
    void diceCountRoundsUpHalfTheMargin() {
        assertEquals(1, Scatter.diceCount(1));
        assertEquals(1, Scatter.diceCount(2));
        assertEquals(2, Scatter.diceCount(3));
        assertEquals(2, Scatter.diceCount(4));
        assertEquals(3, Scatter.diceCount(5));
        assertEquals(3, Scatter.diceCount(6));
        assertEquals(3, Scatter.diceCount(-5));
    }

    @Test
    @DisplayName("standard omnidirectional scatter lands exactly the given distance away on a straight line")
    void omnidirectionalLandsOnStraightLine() {
        for (int startX : new int[] { 6, 7 }) {
            Coords target = new Coords(startX, 9);
            for (int distance : new int[] { 1, 3, 5 }) {
                for (int trial = 0; trial < 200; trial++) {
                    ScatterResult result = Scatter.omnidirectional(target, distance);
                    assertEquals(distance, target.distance(result.landing()),
                          "omnidirectional scatter must land exactly " + distance + " hexes away");
                    assertEquals(distance, result.distanceHexes(),
                          "the reported distance must match the actual distance");
                    assertTrue(isOnStraightLine(target, result.landing()),
                          "omnidirectional scatter must land on one of the six straight-line directions");
                }
            }
        }
    }

    @Test
    @DisplayName("advanced scatter lands within the dice range and can reach hexes off the straight-line spines")
    void advancedScatterRangeAndReach() {
        Coords target = new Coords(6, 9);
        int marginOfFailure = 5; // three dice per leg
        int dice = Scatter.diceCount(marginOfFailure);
        boolean reachedOffSpineHex = false;

        for (int trial = 0; trial < 400; trial++) {
            ScatterResult result = Scatter.advanced(target, marginOfFailure, 0);
            int distance = target.distance(result.landing());

            assertEquals(distance, result.distanceHexes(), "the reported distance must match the actual distance");
            assertTrue((distance >= dice) && (distance <= 6 * dice),
                  "advanced scatter distance " + distance + " must be within [" + dice + ", " + (6 * dice) + "]");

            if (!isOnStraightLine(target, result.landing())) {
                reachedOffSpineHex = true;
            }
        }

        assertTrue(reachedOffSpineHex, "advanced scatter must be able to land off the six straight-line spines");
    }

    @Test
    @DisplayName("advanced altitude bombing scatter stays within the dice range")
    void advancedAltitudeScatterRange() {
        Coords target = new Coords(6, 9);
        int marginOfFailure = 5; // three dice per leg
        int dice = Scatter.diceCount(marginOfFailure);

        for (int trial = 0; trial < 300; trial++) {
            ScatterResult result = Scatter.advancedAltitude(target, 1, marginOfFailure, 0);
            int distance = target.distance(result.landing());

            assertEquals(distance, result.distanceHexes(), "the reported distance must match the actual distance");
            assertTrue((distance >= dice) && (distance <= 6 * dice),
                  "advanced altitude scatter distance " + distance + " must be within [" + dice + ", " + (6 * dice)
                        + "]");
        }
    }

    @Test
    @DisplayName("standard scatter uses the supplied standard distance and ignores the margin of failure")
    void standardScatterUsesSuppliedDistance() {
        Coords target = new Coords(6, 9);
        for (int trial = 0; trial < 200; trial++) {
            ScatterResult result = ScatterMethod.STANDARD.omnidirectional(target, 7, 3, 0);
            assertEquals(7, result.distanceHexes(), "standard scatter must use the supplied standard distance");
        }
    }

    @Test
    @DisplayName("standard scatter subtracts the reduction from the supplied standard distance")
    void standardScatterAppliesReduction() {
        Coords target = new Coords(6, 9);
        for (int trial = 0; trial < 200; trial++) {
            ScatterResult result = ScatterMethod.STANDARD.omnidirectional(target, 7, 3, 2);
            assertEquals(5, result.distanceHexes(), "the reduction must subtract from the supplied standard distance");
        }
    }

    /** @return whether {@code hex} lies on one of the six straight-line directions from {@code target} */
    private static boolean isOnStraightLine(Coords target, Coords hex) {
        int distance = target.distance(hex);
        for (int direction = 0; direction < 6; direction++) {
            if (target.translated(direction, distance).equals(hex)) {
                return true;
            }
        }
        return false;
    }
}
