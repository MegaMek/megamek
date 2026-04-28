/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.codeUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MathUtilityTest {
    // region Linear Interpolation
    @Test
    void testLerpInt() {
        assertEquals(0, MathUtility.lerp(0, 1, 0d));
        assertEquals(1, MathUtility.lerp(0, 1, 1d));
        assertEquals(1, MathUtility.lerp(0, 1, 0.5));
        assertEquals(0, MathUtility.lerp(0, 1, 0.4999));
        assertEquals(5, MathUtility.lerp(0, 9, 0.5));
        assertEquals(4, MathUtility.lerp(0, 9, 0.4999));
        assertEquals(4, MathUtility.lerp(0, 8, 0.5));
    }

    @Test
    void testLerpDouble() {
        assertEquals(0d, MathUtility.lerp(0d, 1d, 0d));
        assertEquals(1d, MathUtility.lerp(0d, 1d, 1d));
        assertEquals(0.5, MathUtility.lerp(0d, 1d, 0.5));
        assertEquals(0.4999, MathUtility.lerp(0d, 1d, 0.4999));
        assertEquals(4.5, MathUtility.lerp(0d, 9d, 0.5));
        assertEquals(4.4991, MathUtility.lerp(0d, 9d, 0.4999));
        assertEquals(4d, MathUtility.lerp(0d, 8d, 0.5));
    }

    @Test
    void testLerpFloat() {
        assertEquals(0f, MathUtility.lerp(0f, 1f, 0f));
        assertEquals(1f, MathUtility.lerp(0f, 1f, 1f));
        assertEquals(0.5f, MathUtility.lerp(0f, 1f, 0.5f));
        assertEquals(0.4999f, MathUtility.lerp(0f, 1f, 0.4999f));
        assertEquals(4.5f, MathUtility.lerp(0f, 9f, 0.5f));
        assertEquals(4.4991f, MathUtility.lerp(0f, 9f, 0.4999f));
        assertEquals(4f, MathUtility.lerp(0f, 8f, 0.5f));
    }

    @Test
    void testLerpLong() {
        assertEquals(0L, MathUtility.lerp(0L, 1L, 0d));
        assertEquals(1L, MathUtility.lerp(0L, 1L, 1d));
        assertEquals(1L, MathUtility.lerp(0L, 1L, 0.5));
        assertEquals(0L, MathUtility.lerp(0L, 1L, 0.4999));
        assertEquals(5L, MathUtility.lerp(0L, 9L, 0.5));
        assertEquals(4L, MathUtility.lerp(0L, 9L, 0.4999));
        assertEquals(4L, MathUtility.lerp(0L, 8L, 0.5));
    }
    // endregion Linear Interpolation
    
    @Test
    void testStringToInt() {
        assertEquals(1, MathUtility.parseInt("1", 0));
        assertEquals(0, MathUtility.parseInt("A", 0));
        assertEquals(0, MathUtility.parseInt(null, 0));
        assertEquals(0, MathUtility.parseInt("", 0));
    }

    @Test
    void testStringToFloat() {
        assertEquals(1.0f, MathUtility.parseFloat("1", 0f));
        assertEquals(0.5f, MathUtility.parseFloat("0.5", 0f));
        assertEquals(0f, MathUtility.parseFloat("A", 0f));
        assertEquals(0f, MathUtility.parseFloat(null, 0f));
        assertEquals(0f, MathUtility.parseFloat("", 0f));
    }

    @Test
    void testStringToDouble() {
        assertEquals(1.0, MathUtility.parseDouble("1", 0.0));
        assertEquals(0.5, MathUtility.parseDouble("0.5", 0.0));
        assertEquals(0.0, MathUtility.parseDouble("A", 0.0));
        assertEquals(0.0, MathUtility.parseDouble(null, 0.0));
        assertEquals(0.0, MathUtility.parseDouble("", 0.0));
    }

}
