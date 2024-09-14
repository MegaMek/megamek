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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CoordsTest {

    @Test
    void testTranslated() {
        assertEquals(new Coords(0, 0).translated(2), new Coords(1, 0));
        assertEquals(new Coords(1, 0).translated(2), new Coords(2, 1));
        assertEquals(new Coords(2, 1).translated(2), new Coords(3, 1));

        assertEquals(new Coords(0, 0).translated(3), new Coords(0, 1));
        assertEquals(new Coords(7, -2).translated(5), new Coords(6, -2));

        assertEquals(new Coords(0, 0).translated(2, 3), new Coords(3, 1));
        assertEquals(new Coords(10, 4).translated(0, 2), new Coords(10, 2));
        assertEquals(new Coords(10, 4).translated(3, 5), new Coords(10, 9));
        assertEquals(new Coords(10, 4).translated(4, 3), new Coords(7, 5));
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
        expectedAdjacent.add(new Coords(1, -1));
        expectedAdjacent.add(new Coords(1, 0));
        expectedAdjacent.add(new Coords(0, -1));
        expectedAdjacent.add(new Coords(0, 1));
        expectedAdjacent.add(new Coords(-1, 0));
        expectedAdjacent.add(new Coords(-1, -1));
        assertEquals(new Coords(0, 0).allAdjacent().size(), 6);
        new Coords(0, 0).allAdjacent().forEach(coords -> assertTrue(expectedAdjacent.contains(coords)));

        // for a radius 2 donut we expect to see 12 hexes.
        final List<Coords> expectedAtDistance2 = new ArrayList<>();
        expectedAtDistance2.add(new Coords(-2, 0));
        expectedAtDistance2.add(new Coords(0, -2));
        expectedAtDistance2.add(new Coords(1, 1));
        expectedAtDistance2.add(new Coords(-2, 1));
        expectedAtDistance2.add(new Coords(1, -2));
        expectedAtDistance2.add(new Coords(-2, -1));
        expectedAtDistance2.add(new Coords(2, 1));
        expectedAtDistance2.add(new Coords(-1, -2));
        expectedAtDistance2.add(new Coords(2, -1));
        expectedAtDistance2.add(new Coords(2, 0));
        expectedAtDistance2.add(new Coords(0, 2));
        expectedAtDistance2.add(new Coords(-1, 1));

        assertEquals(new Coords(0, 0).allAtDistance(2).size(), 12);
        new Coords(0, 0).allAtDistance(2).forEach(coords -> assertTrue(expectedAtDistance2.contains(coords)));
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
}
