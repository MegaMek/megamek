/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.enums.VariableRangeTargetingMode;
import org.junit.jupiter.api.Test;

/**
 * Tests for Variable Range Targeting quirk functionality (BMM pg. 86).
 * <p>
 * Rules Summary: - Player selects Long or Short mode during End Phase for the NEXT turn - LONG mode: -1 TN at long
 * range, +1 TN at short range - SHORT mode: -1 TN at short range, +1 TN at long range - Medium range is unaffected by
 * either mode
 */
public class VariableRangeTargetingTest {

    //region Enum Tests

    @Test
    void testDefaultModeIsLong() {
        assertEquals(VariableRangeTargetingMode.LONG, VariableRangeTargetingMode.defaultMode(),
              "Default Variable Range Targeting mode should be LONG");
    }

    @Test
    void testIsLong() {
        assertTrue(VariableRangeTargetingMode.LONG.isLong(),
              "LONG mode should return true for isLong()");
        assertFalse(VariableRangeTargetingMode.SHORT.isLong(),
              "SHORT mode should return false for isLong()");
    }

    @Test
    void testIsShort() {
        assertTrue(VariableRangeTargetingMode.SHORT.isShort(),
              "SHORT mode should return true for isShort()");
        assertFalse(VariableRangeTargetingMode.LONG.isShort(),
              "LONG mode should return false for isShort()");
    }

    //endregion Enum Tests

    //region Modifier Tests

    @Test
    void testLongModeShortRangeModifier() {
        // LONG mode: +1 at short range (penalty)
        assertEquals(1, VariableRangeTargetingMode.LONG.getShortRangeModifier(),
              "LONG mode should have +1 modifier at short range");
    }

    @Test
    void testLongModeLongRangeModifier() {
        // LONG mode: -1 at long range (bonus)
        assertEquals(-1, VariableRangeTargetingMode.LONG.getLongRangeModifier(),
              "LONG mode should have -1 modifier at long range");
    }

    @Test
    void testShortModeShortRangeModifier() {
        // SHORT mode: -1 at short range (bonus)
        assertEquals(-1, VariableRangeTargetingMode.SHORT.getShortRangeModifier(),
              "SHORT mode should have -1 modifier at short range");
    }

    @Test
    void testShortModeLongRangeModifier() {
        // SHORT mode: +1 at long range (penalty)
        assertEquals(1, VariableRangeTargetingMode.SHORT.getLongRangeModifier(),
              "SHORT mode should have +1 modifier at long range");
    }

    //endregion Modifier Tests
}
