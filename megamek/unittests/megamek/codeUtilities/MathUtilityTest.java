/*
 * Copyright (c) 2023-2023 - The MegaMek Team. All Rights Reserved.
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

    // region Clamp
    @Test
    void testClampInt() {
        assertEquals(5, MathUtility.clamp(5, 1, 10));
        assertEquals(5, MathUtility.clamp(5, 1, 5));
        assertEquals(5, MathUtility.clamp(5, 5, 10));
        assertEquals(6, MathUtility.clamp(5, 6, 10));
        assertEquals(5, MathUtility.clamp(6, 1, 5));
    }

    @Test
    void testClampDouble() {
        assertEquals(5.5, MathUtility.clamp(5.5, 1.5, 10.5));
        assertEquals(5.5, MathUtility.clamp(5.5, 1.5, 5.5));
        assertEquals(5.5, MathUtility.clamp(5.5, 5.5, 10.5));
        assertEquals(6.5, MathUtility.clamp(5.5, 6.5, 10.5));
        assertEquals(5.5, MathUtility.clamp(6.5, 1.5, 5.5));
    }

    @Test
    void testClampFloat() {
        assertEquals(5f, MathUtility.clamp(5f, 1f, 10f));
        assertEquals(5f, MathUtility.clamp(5f, 1f, 5f));
        assertEquals(5f, MathUtility.clamp(5f, 5f, 10f));
        assertEquals(6f, MathUtility.clamp(5f, 6f, 10f));
        assertEquals(5f, MathUtility.clamp(6f, 1f, 5f));
    }

    @Test
    void testClampLong() {
        assertEquals(5L, MathUtility.clamp(5L, 1L, 10L));
        assertEquals(5L, MathUtility.clamp(5L, 1L, 5L));
        assertEquals(5L, MathUtility.clamp(5L, 5L, 10L));
        assertEquals(6L, MathUtility.clamp(5L, 6L, 10L));
        assertEquals(5L, MathUtility.clamp(6L, 1L, 5L));
    }
    // endregion Clamp

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
