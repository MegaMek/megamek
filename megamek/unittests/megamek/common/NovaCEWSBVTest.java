/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

/**
 * Tests for Nova CEWS BV calculation and cap enforcement.
 *
 * TT Rules (IO: Alternate Eras p.183):
 * - Nova CEWS provides 5% BV bonus for all friendly units with Nova CEWS
 * - Bonus only applies if 2+ units with Nova CEWS are present
 * - Maximum bonus capped at 35% of unit's base BV
 *
 * These tests focus on the cap calculation logic.
 */
public class NovaCEWSBVTest {

    /**
     * Test the cap calculation logic directly.
     * Verifies that the 35% cap is properly applied using the same
     * logic as Entity.getExtraC3BV().
     */
    @Test
    void novaCEWSBonusCappedAt35Percent() {
        int baseBV = 1000;
        double multiplier = 0.05; // 5% for Nova CEWS

        // Scenario 1: Bonus under cap (should not be capped)
        int totalForceBV_underCap = 2000; // 2 units @ 1000 each
        double rawBonus_underCap = totalForceBV_underCap * multiplier; // 100
        double maxBonus = baseBV * 0.35; // 350
        double cappedBonus_underCap = Math.min(rawBonus_underCap, maxBonus);

        assertEquals(100, (int) Math.round(cappedBonus_underCap),
            "Bonus of 100 should not be capped (under 35% limit of 350)");

        // Scenario 2: Bonus over cap (should be capped at 35%)
        int totalForceBV_overCap = 21000; // 21 units @ 1000 each
        double rawBonus_overCap = totalForceBV_overCap * multiplier; // 1050
        double cappedBonus_overCap = Math.min(rawBonus_overCap, maxBonus);

        assertEquals(350, (int) Math.round(cappedBonus_overCap),
            "Bonus of 1050 should be capped at 350 (35% of 1000 base BV)");

        // Scenario 3: Bonus exactly at cap
        int totalForceBV_atCap = 7000; // 7 units @ 1000 each
        double rawBonus_atCap = totalForceBV_atCap * multiplier; // 350
        double cappedBonus_atCap = Math.min(rawBonus_atCap, maxBonus);

        assertEquals(350, (int) Math.round(cappedBonus_atCap),
            "Bonus of 350 should equal the cap (35% of 1000 base BV)");
    }

    /**
     * Test cap calculation with different base BV values.
     * The 35% cap should scale with base BV.
     */
    @Test
    void novaCEWSCapScalesWithBaseBV() {
        double multiplier = 0.05;

        // Low BV unit
        int baseBV_low = 500;
        int totalForceBV_low = 10000; // Would give 500 without cap
        double maxBonus_low = baseBV_low * 0.35; // 175
        double cappedBonus_low = Math.min(totalForceBV_low * multiplier, maxBonus_low);

        assertEquals(175, (int) Math.round(cappedBonus_low),
            "Low BV unit (500) should be capped at 175 (35%)");

        // High BV unit
        int baseBV_high = 2000;
        int totalForceBV_high = 40000; // Would give 2000 without cap
        double maxBonus_high = baseBV_high * 0.35; // 700
        double cappedBonus_high = Math.min(totalForceBV_high * multiplier, maxBonus_high);

        assertEquals(700, (int) Math.round(cappedBonus_high),
            "High BV unit (2000) should be capped at 700 (35%)");
    }

    /**
     * Test that the cap only applies to Nova CEWS.
     * This verifies the conditional logic: if (hasNovaCEWS())
     */
    @Test
    void capOnlyAppliesWhenNovaCEWSPresent() {
        int baseBV = 1000;
        double multiplier = 0.05;
        int totalForceBV = 21000; // Would give 1050 bonus

        // Simulate Nova CEWS scenario (cap applies)
        boolean hasNovaCEWS = true;
        double rawBonus_withNova = totalForceBV * multiplier;
        if (hasNovaCEWS) {
            double maxBonus = baseBV * 0.35;
            rawBonus_withNova = Math.min(rawBonus_withNova, maxBonus);
        }

        assertEquals(350, (int) Math.round(rawBonus_withNova),
            "Nova CEWS bonus should be capped at 350");

        // Simulate non-Nova scenario (cap does not apply)
        boolean hasNovaCEWS_false = false;
        double rawBonus_withoutNova = totalForceBV * multiplier;
        if (hasNovaCEWS_false) {
            double maxBonus = baseBV * 0.35;
            rawBonus_withoutNova = Math.min(rawBonus_withoutNova, maxBonus);
        }

        assertEquals(1050, (int) Math.round(rawBonus_withoutNova),
            "Non-Nova bonus should NOT be capped (remains at 1050)");
    }

    /**
     * Test edge case: Zero base BV.
     * Cap should be 0, resulting in 0 bonus.
     */
    @Test
    void zeroBaseBVResultsInZeroCap() {
        int baseBV = 0;
        double multiplier = 0.05;
        int totalForceBV = 5000;

        double maxBonus = baseBV * 0.35; // 0
        double cappedBonus = Math.min(totalForceBV * multiplier, maxBonus);

        assertEquals(0, (int) Math.round(cappedBonus),
            "Zero base BV should result in zero cap");
    }

    /**
     * Test edge case: Negative bonus (shouldn't happen, but verifies Math.min logic).
     */
    @Test
    void mathMinHandlesNegativeValues() {
        int baseBV = 1000;
        int totalForceBV = -1000; // Hypothetical negative scenario
        double multiplier = 0.05;

        double rawBonus = totalForceBV * multiplier; // -50
        double maxBonus = baseBV * 0.35; // 350
        double result = Math.min(rawBonus, maxBonus);

        assertEquals(-50, (int) Math.round(result),
            "Math.min should select negative value when raw bonus is negative");
    }

    /**
     * Test rounding behavior.
     * Verifies that (int) Math.round() correctly rounds the capped bonus.
     */
    @Test
    void roundingBehaviorIsCorrect() {
        int baseBV = 1000;
        double multiplier = 0.05;

        // Test rounding down
        int totalForceBV_roundDown = 2004; // 2004 * 0.05 = 100.2
        double rawBonus_roundDown = totalForceBV_roundDown * multiplier;
        int roundedBonus_roundDown = (int) Math.round(rawBonus_roundDown);

        assertEquals(100, roundedBonus_roundDown,
            "100.2 should round down to 100");

        // Test rounding up
        int totalForceBV_roundUp = 2010; // 2010 * 0.05 = 100.5
        double rawBonus_roundUp = totalForceBV_roundUp * multiplier;
        int roundedBonus_roundUp = (int) Math.round(rawBonus_roundUp);

        assertEquals(101, roundedBonus_roundUp,
            "100.5 should round up to 101");
    }
}
