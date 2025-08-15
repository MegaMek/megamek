/*
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import megamek.codeUtilities.MathUtility;
import org.junit.jupiter.api.Test;

class BoardDimensionsTests {

    @Test
    final void testInstantiation() {
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
    final void testEqualsObject() {
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
    final void testNotEqualsObject() {
        BoardDimensions b = new BoardDimensions(10, 10);
        assertNotEquals(b, new BoardDimensions(10, 5));
        assertNotEquals(b, new BoardDimensions(5, 10));
    }

    @Test
    final void testToString() {
        assertEquals("10 x 10", new BoardDimensions(10, 10).toString());
        assertEquals("80 x 50", new BoardDimensions(80, 50).toString());
    }

    @Test
    final void testCompareTo() {
        assertEquals(0,
              new BoardDimensions(10, 10).compareTo(new BoardDimensions(10, 10)));
        assertEquals(0, new BoardDimensions(Integer.MAX_VALUE,
              Integer.MAX_VALUE).compareTo(
              new BoardDimensions(
                    Integer.MAX_VALUE, Integer.MAX_VALUE)));

        int result = MathUtility.clamp(new BoardDimensions(10, 10).compareTo(new BoardDimensions(
              Integer.MAX_VALUE, Integer.MAX_VALUE)), -1, 1);
        assertEquals(-1, result);

        result = MathUtility.clamp(new BoardDimensions(Integer.MAX_VALUE,
              Integer.MAX_VALUE).compareTo(new BoardDimensions(10, 10)), -1, 1);
        assertEquals(1, result);

        result = MathUtility.clamp(
              new BoardDimensions(10, 20).compareTo(new BoardDimensions(20, 10)),
              -1, 1);
        assertEquals(-1, result);

        result = MathUtility.clamp(
              new BoardDimensions(20, 10).compareTo(new BoardDimensions(10, 20)),
              -1, 1);
        assertEquals(1, result);
    }
}
