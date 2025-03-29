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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class CoordsTest {
    //========================================================
    // Note, this is for the underlying 0,0 coordinate system.
    // To convert to map number, add [+1 +1] to Coords:
    //
    //         Internal Rep.                  Map Rep.
    //           _____                         _____
    //          /     \                       /     \
    //    _____/ 0,-1  \_____           _____/  0100 \_____
    //   /     \       /     \         /     \       /     \
    //  / -1,-1 \_____/  1,-1 \       /  0000 \_____/  0200 \
    //  \       /     \       /       \       /     \       /
    //   \_____/  0,0  \_____/  ___\   \_____/  0101 \_____/
    //   /     \       /     \     /   /     \       /     \
    //  / -1,0  \_____/  1,0  \       /  0001 \_____/  0201 \
    //  \       /     \       /       \       /     \       /
    //   \_____/  0,1  \_____/         \_____/  0102 \_____/
    //         \       /                     \       /
    //          \_____/                       \_____/
    //
    //========================================================

    @Test
    void testTranslated() {
        assertEquals(new Coords(1, 0), new Coords(0, 0).translated(2));
        assertEquals(new Coords(2, 1), new Coords(1, 0).translated(2));
        assertEquals(new Coords(3, 1), new Coords(2, 1).translated(2));

        assertEquals(new Coords(0, 1), new Coords(0, 0).translated(3));
        assertEquals(new Coords(6, -2), new Coords(7, -2).translated(5));

        assertEquals(new Coords(3, 1), new Coords(0, 0).translated(2, 3));
        assertEquals(new Coords(10, 2), new Coords(10, 4).translated(0, 2));
        assertEquals(new Coords(10, 9), new Coords(10, 4).translated(3, 5));
        assertEquals(new Coords(7, 5), new Coords(10, 4).translated(4, 3));
    }

    @Test
    void testMedian() {
        var c1 = new Coords(19, 9);
        var c2 = new Coords(24, 9);
        var c3 = new Coords(19, 12);
        var c4 = new Coords(10, 10);
        var c5 = new Coords(8, 10);
        var teamBlue = List.of(c1, c2, c3, c4, c5);

        var a1 = new Coords(12, 11);

        var closestEnemy = a1.closestCoords(teamBlue);
        var medianPosition = Coords.median(teamBlue);
        assertEquals(c4, closestEnemy);
        assertEquals(new Coords(18, 9), medianPosition);
    }

    @Test
    void testDistance() {
        assertEquals(new Coords(13, 6).distance(new Coords(15, 1)), 6);
        assertEquals(new Coords(12, 2).distance(new Coords(9, 2)), 3);
    }

    @Test
    void testAdjacent() {
        assertEquals(new Coords(5, -5).allAtDistance(0).size(), 1);

        final List<Coords> expectedAdjacent = new ArrayList<>();
        expectedAdjacent.add(new Coords(0, -1));
        expectedAdjacent.add(new Coords(1, -1));
        expectedAdjacent.add(new Coords(1, 0));
        expectedAdjacent.add(new Coords(0, 1));
        expectedAdjacent.add(new Coords(-1, 0));
        expectedAdjacent.add(new Coords(-1, -1));
        assertEquals(new Coords(0, 0).allAdjacent().size(), 6);
        new Coords(0, 0).allAdjacent().forEach(coords -> assertTrue(expectedAdjacent.contains(coords)));

        // for a radius 2 donut we expect to see 12 hexes.
        final List<Coords> expectedAtDistance2 = new ArrayList<>();
        expectedAtDistance2.add(new Coords(0, -2));
        expectedAtDistance2.add(new Coords(1, -2));
        expectedAtDistance2.add(new Coords(2, -1));
        expectedAtDistance2.add(new Coords(2, 0));
        expectedAtDistance2.add(new Coords(2, 1));
        expectedAtDistance2.add(new Coords(1, 1));
        expectedAtDistance2.add(new Coords(0, 2));
        expectedAtDistance2.add(new Coords(-1, 1));
        expectedAtDistance2.add(new Coords(-2, 1));
        expectedAtDistance2.add(new Coords(-2, 0));
        expectedAtDistance2.add(new Coords(-2, -1));
        expectedAtDistance2.add(new Coords(-1, -2));

        assertEquals(new Coords(0, 0).allAtDistance(2).size(), 12);
        new Coords(0, 0).allAtDistance(2).forEach(coords -> assertTrue(expectedAtDistance2.contains(coords)));
    }

    @Test
    void testAllAtDistance() {
        assertEquals(new Coords(10, 10).allAtDistanceOrLess(1).size(), 7);
        assertEquals(new Coords(10, 10).allLessThanDistance(1).size(), 1);
        assertEquals(new Coords(10, 10).allAtDistanceOrLess(0).size(), 1);
    }

    List<Coords> generateLevel2NeighborsEvenX(Coords centroid){
        // Manually computed kernel based on map offsets; only for even-X coords
        return Arrays.asList(
            new Coords(centroid.getX(), centroid.getY()),
            // immediate neighbors
            new Coords(centroid.getX(), centroid.getY() - 1),
            new Coords(centroid.getX() + 1, centroid.getY() - 1),
            new Coords(centroid.getX() + 1, centroid.getY()),
            new Coords(centroid.getX(), centroid.getY() + 1),
            new Coords(centroid.getX() - 1, centroid.getY()),
            new Coords(centroid.getX() - 1, centroid.getY() - 1),
            // neighbors + 1
            new Coords(centroid.getX(), centroid.getY() - 2),
            new Coords(centroid.getX() + 1, centroid.getY() - 2),
            new Coords(centroid.getX() + 2, centroid.getY() - 1),
            new Coords(centroid.getX() + 2, centroid.getY()),
            new Coords(centroid.getX() + 2, centroid.getY() + 1),
            new Coords(centroid.getX() + 1, centroid.getY() + 1),
            new Coords(centroid.getX(), centroid.getY() + 2),
            new Coords(centroid.getX() - 1, centroid.getY() + 1),
            new Coords(centroid.getX() - 2, centroid.getY() + 1),
            new Coords(centroid.getX() - 2, centroid.getY()),
            new Coords(centroid.getX() - 2, centroid.getY() - 1),
            new Coords(centroid.getX() - 1, centroid.getY() - 2)
        );
    }

    List<Coords> generateLevel2NeighborsOddX(Coords centroid){
        // Manually computed kernel based on map offsets; only for even-X coords
        return Arrays.asList(
            new Coords(centroid.getX(), centroid.getY()),
            // immediate neighbors
            new Coords(centroid.getX(), centroid.getY() - 1),
            new Coords(centroid.getX() + 1, centroid.getY()),
            new Coords(centroid.getX() + 1, centroid.getY() + 1),
            new Coords(centroid.getX(), centroid.getY() + 1),
            new Coords(centroid.getX() - 1, centroid.getY() + 1),
            new Coords(centroid.getX() - 1, centroid.getY()),
            // neighbors + 1
            new Coords(centroid.getX(), centroid.getY() - 2),
            new Coords(centroid.getX() + 1, centroid.getY() - 1),
            new Coords(centroid.getX() + 2, centroid.getY() - 1),
            new Coords(centroid.getX() + 2, centroid.getY()),
            new Coords(centroid.getX() + 2, centroid.getY() + 1),
            new Coords(centroid.getX() + 1, centroid.getY() + 2),
            new Coords(centroid.getX(), centroid.getY() + 2),
            new Coords(centroid.getX() - 1, centroid.getY() + 2),
            new Coords(centroid.getX() - 2, centroid.getY() + 1),
            new Coords(centroid.getX() - 2, centroid.getY()),
            new Coords(centroid.getX() - 2, centroid.getY() - 1),
            new Coords(centroid.getX() - 1, centroid.getY() - 1)
        );
    }

    void testAllAtDistanceOrLessAlignedCorrectly(Coords centroid) {
        List<Coords> neighbors = centroid.allAtDistanceOrLess(2);
        List<Coords> expectedNeighbors = ((centroid.getX() & 1) == 1) ?
            generateLevel2NeighborsOddX(centroid) : generateLevel2NeighborsEvenX(centroid);
        assertEquals(19, neighbors.size());
        assertEquals(19, expectedNeighbors.size());

        // All generated neighbors must be in expectedNeighbors
        boolean allMatch = true;
        List<Coords> mismatches = new ArrayList<>();
        for (Coords coords : neighbors) {
            if (expectedNeighbors.stream().anyMatch(coords::equals)) {
                continue;
            }
            allMatch = false;
            mismatches.add(coords);
        }
        String mismatchString = mismatches.stream().map(Coords::toString).collect(Collectors.joining(", "));
        assertTrue(allMatch, mismatchString);

        // All generated expectedNeighbors must be in neighbors
        allMatch = true;
        mismatches = new ArrayList<>();
        for (Coords coords : expectedNeighbors) {
            if (neighbors.stream().anyMatch(coords::equals)) {
                continue;
            }
            allMatch = false;
            mismatches.add(coords);
        }
        mismatchString = mismatches.stream().map(Coords::toString).collect(Collectors.joining(", "));
        assertTrue(allMatch, mismatchString);
    }

    @Test
    void testAllAtDistanceOrLess() {
        Coords centroid = new Coords(6, 7);
        testAllAtDistanceOrLessAlignedCorrectly(centroid);
        centroid = new Coords(7, 7);
        testAllAtDistanceOrLessAlignedCorrectly(centroid);
    }

    @Test
    void testTranslation() {
        Coords center = new Coords(8, 9);
        assertEquals(new Coords(7, 9), center.translated(4, 1));
        assertEquals(new Coords(7, 8), center.translated(5, 1));
    }

    @Test
    void testHexRow() {
        Coords center = new Coords(3, 7);
        assertFalse(center.isOnHexRow(-1, new Coords(0, 0)));
        assertFalse(center.isOnHexRow(6, new Coords(0, 0)));
        assertFalse(center.isOnHexRow(2, center));
        assertFalse(center.isOnHexRow(5, center));
        assertFalse(center.isOnHexRow(2, null));

        Coords other = new Coords(3, 5);
        assertTrue(center.isOnHexRow(0, other));
        assertFalse(center.isOnHexRow(1, other));
        assertFalse(center.isOnHexRow(2, other));
        assertFalse(center.isOnHexRow(3, other));
        assertFalse(center.isOnHexRow(4, other));
        assertFalse(center.isOnHexRow(5, other));

        other = new Coords(3, -1);
        assertTrue(center.isOnHexRow(0, other));
        assertFalse(center.isOnHexRow(1, other));
        assertFalse(center.isOnHexRow(2, other));
        assertFalse(center.isOnHexRow(3, other));
        assertFalse(center.isOnHexRow(4, other));
        assertFalse(center.isOnHexRow(5, other));

        other = new Coords(4, -1);
        assertFalse(center.isOnHexRow(0, other));
        assertFalse(center.isOnHexRow(1, other));
        assertFalse(center.isOnHexRow(2, other));
        assertFalse(center.isOnHexRow(3, other));
        assertFalse(center.isOnHexRow(4, other));
        assertFalse(center.isOnHexRow(5, other));

        center = new Coords(8, 2);
        other = new Coords(8, 8);
        assertFalse(center.isOnHexRow(0, other));
        assertFalse(center.isOnHexRow(1, other));
        assertFalse(center.isOnHexRow(2, other));
        assertTrue(center.isOnHexRow(3, other));
        assertFalse(center.isOnHexRow(4, other));
        assertFalse(center.isOnHexRow(5, other));

        center = new Coords(4, 4);
        other = new Coords(8, 2);
        assertFalse(center.isOnHexRow(0, other));
        assertTrue(center.isOnHexRow(1, other));
        assertFalse(center.isOnHexRow(2, other));
        assertFalse(center.isOnHexRow(3, other));
        assertFalse(center.isOnHexRow(4, other));
        assertFalse(center.isOnHexRow(5, other));

        other = new Coords(8, 3);
        assertFalse(center.isOnHexRow(0, other));
        assertFalse(center.isOnHexRow(1, other));
        assertFalse(center.isOnHexRow(2, other));
        assertFalse(center.isOnHexRow(3, other));
        assertFalse(center.isOnHexRow(4, other));
        assertFalse(center.isOnHexRow(5, other));
    }

    @Test
    void testHex2HexInterveningDistance1() {
        Coords source = new Coords(5, 5);
        Coords target = new Coords(6, 5);

        List<Coords> intervening = Coords.intervening(source, target);
        assertFalse(intervening.isEmpty());
    }

    @Test
    void testHex2HexRadianDirections() {
        // Test identity - docs say equals 0?
        Coords source = new Coords(5, 5);
        Coords target1 = new Coords(5, 5);
        assertEquals(4.7, source.radian(target1), 0.1);
        assertEquals(5, source.direction(target1));

        // Test one away to north
        Coords target2 = new Coords(5, 4);
        assertEquals(0, source.radian(target2));
        assertEquals(0, source.direction(target2));

        // Test one away NE
        Coords target3 = new Coords(6, 5);
        assertEquals(1.047, source.radian(target3), 0.01);
        assertEquals(1, source.direction(target3));

        // Test one away SE
        Coords target4 = new Coords(6, 6);
        assertEquals(2.1, source.radian(target4), 0.1);
        assertEquals(2, source.direction(target4));

        // Test one away SE
        Coords target5 = new Coords(7, 5);
        assertEquals(Math.PI / 2, source.radian(target5), 0.1);
        assertEquals(2, source.direction(target5));

    }

    @TestFactory
    Stream<DynamicTest> testCoordsToCubeToOffset() {
        return generateCoords().map(target -> dynamicTest(
              "Test Coords(" + target.getX() + ", " + target.getY() + ") to Cube to Offset",
              () -> assertEquals(target, target.toCube().toOffset())
        ));
    }

    private Stream<Coords> generateCoords() {
        List<Coords> coordsList = new ArrayList<>();
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                coordsList.add(new Coords(x, y));
            }
        }
        return coordsList.stream();
    }

    @Test
    void testHex2HexDistance() {
        // Test of distance(), separate from donut and adjacency tests.
        // Dist from 7,1 (0802) to 6,1 (0702) should be 1
        Coords source = new Coords(7, 1);
        Coords target1 = new Coords(6, 1);
        assertEquals(1, source.distance(target1));

        // Reciprocal distance from 6,1 (0702) to 7,1 (0802) should be also 1
        source = new Coords(6, 1);
        target1 = new Coords(7, 1);
        assertEquals(1, source.distance(target1));

        // Dist from 6,2 (0703) to 7,2 (0803) should be 1
        source = new Coords(6, 2);
        target1 = new Coords(7, 2);
        assertEquals(1, source.distance(target1));

        source = new Coords(13, 5);
        target1 = new Coords(5, 6);
        assertEquals(8, source.distance(target1));

        source = new Coords(2, 16);
        target1 = new Coords(14, 10);
        assertEquals(12, source.distance(target1));
    }
}
