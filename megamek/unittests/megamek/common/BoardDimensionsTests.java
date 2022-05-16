/*
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoardDimensionsTests {

    @Test
    public final void testInstantiation() {
        BoardDimensions b = new BoardDimensions(1, 1);
        assertEquals(1, b.width());
        assertEquals(1, b.height());
        assertEquals(1, b.numHexes());

        b = new BoardDimensions(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, b.width());
        assertEquals(Integer.MAX_VALUE, b.height());
        assertEquals((long) Integer.MAX_VALUE * Integer.MAX_VALUE, b.numHexes());

        b = new BoardDimensions(10, 10);
        assertEquals(10, b.width());
        assertEquals(10, b.height());
        assertEquals(100, b.numHexes());

        b = new BoardDimensions(100, 80);
        assertEquals(100, b.width());
        assertEquals(80, b.height());
        assertEquals(8000, b.numHexes());
    }

    @Test
    public final void testCopyConstructor() {
        BoardDimensions b = new BoardDimensions(20, 10);
        BoardDimensions c = new BoardDimensions(b);

        assertEquals(20, c.width());
        assertEquals(10, c.height());
    }

    @Test
    public final void testEqualsObject() {
        BoardDimensions b = new BoardDimensions(10, 10);
        assertEquals(b, b);

        Object x = new BoardDimensions(10, 10);
        assertEquals(b, x);
        assertEquals(x, b);

        BoardDimensions d = new BoardDimensions(10, 10);
        assertEquals(b, d);
        assertEquals(d, b);
    }

    @Test
    public final void testNotEqualsObject() {
        BoardDimensions b = new BoardDimensions(10, 10);
        assertNotEquals(b, new BoardDimensions(10, 5));
        assertNotEquals(b, new BoardDimensions(5, 10));
    }

    @Test
    public final void testClone() {
        BoardDimensions b = new BoardDimensions(10, 10);
        assertNotSame(b.clone(), b);
        assertEquals(b.clone(), b);
    }

    @Test
    public final void testToString() {
        assertEquals("10x10", new BoardDimensions(10, 10).toString());
        assertEquals("80x50", new BoardDimensions(80, 50).toString());
    }

    @Test
    public final void testCompareTo() {
        assertEquals(0,
                new BoardDimensions(10, 10).compareTo(new BoardDimensions(10, 10)));
        assertEquals(0, new BoardDimensions(Integer.MAX_VALUE,
                Integer.MAX_VALUE).compareTo(new BoardDimensions(
                Integer.MAX_VALUE, Integer.MAX_VALUE)));
        assertEquals(-1,
                new BoardDimensions(10, 10).compareTo(new BoardDimensions(
                        Integer.MAX_VALUE, Integer.MAX_VALUE)));
        assertEquals(1, new BoardDimensions(Integer.MAX_VALUE,
                Integer.MAX_VALUE).compareTo(new BoardDimensions(10, 10)));
        assertEquals(-1,
                new BoardDimensions(10, 20).compareTo(new BoardDimensions(20, 10)));
        assertEquals(1,
                new BoardDimensions(20, 10).compareTo(new BoardDimensions(10, 20)));
    }
}
