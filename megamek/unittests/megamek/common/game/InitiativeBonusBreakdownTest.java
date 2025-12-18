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
package megamek.common.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InitiativeBonusBreakdown} verifying the Xotl ruling on initiative stacking: - Negative modifiers
 * stack cumulatively (no floor limit) - Positive modifiers do NOT stack (only the highest one applies)
 *
 * @see <a href="https://battletech.com/forums/index.php?topic=85848.0">Xotl Ruling</a>
 */
class InitiativeBonusBreakdownTest {

    // ==================== Stacking Rules: total() ====================

    @Test
    void totalWithSinglePositiveReturnsValue() {
        var breakdown = new InitiativeBonusBreakdown(0, 0, null, 0, 0, 3, 0, 0, 0);
        assertEquals(3, breakdown.total());
    }

    @Test
    void totalWithMultiplePositivesReturnsHighestOnly() {
        // hq=2, quirk=1, tcp=3 -> should return 3 (highest)
        var breakdown = new InitiativeBonusBreakdown(2, 1, "Command Mek", 0, 0, 3, 0, 0, 0);
        assertEquals(3, breakdown.total());
    }

    @Test
    void totalWithSingleNegativeReturnsValue() {
        var breakdown = new InitiativeBonusBreakdown(0, 0, null, 0, 0, 0, -2, 0, 0);
        assertEquals(-2, breakdown.total());
    }

    @Test
    void totalWithMultipleNegativesReturnsCumulativeSum() {
        // constant=-1, compensation=-2, crew=-3 -> should return -6 (all stack)
        var breakdown = new InitiativeBonusBreakdown(0, 0, null, 0, 0, 0, -1, -2, -3);
        assertEquals(-6, breakdown.total());
    }

    @Test
    void totalWithMixedReturnsHighestPositivePlusAllNegatives() {
        // tcp=3, console=2 (positives), constant=-1, compensation=-2 (negatives)
        // Result: 3 (highest positive) + (-1) + (-2) = 0
        var breakdown = new InitiativeBonusBreakdown(0, 0, null, 2, 0, 3, -1, -2, 0);
        assertEquals(0, breakdown.total());
    }

    @Test
    void totalWithAllZerosReturnsZero() {
        var breakdown = InitiativeBonusBreakdown.zero();
        assertEquals(0, breakdown.total());
    }

    @Test
    void rawTotalReturnsSumOfAllComponents() {
        // Should sum all regardless of stacking rules
        var breakdown = new InitiativeBonusBreakdown(2, 1, null, 3, 0, 2, -1, 0, 0);
        assertEquals(7, breakdown.rawTotal()); // 2+1+3+2-1 = 7
    }

    // ==================== Display: toBreakdownString() ====================

    @Test
    void breakdownStringWithNoComponentsReturnsZero() {
        var breakdown = InitiativeBonusBreakdown.zero();
        assertEquals("0", breakdown.toBreakdownString());
    }

    @Test
    void breakdownStringWithSinglePositiveShowsNoStackingNote() {
        var breakdown = new InitiativeBonusBreakdown(0, 0, null, 0, 0, 3, 0, 0, 0);
        String result = breakdown.toBreakdownString();

        assertTrue(result.contains("+3 TCP"));
        assertFalse(result.contains("using highest modifier only"));
        assertFalse(result.contains("<b>")); // No bold for single positive
    }

    @Test
    void breakdownStringWithMultiplePositivesShowsStackingNote() {
        var breakdown = new InitiativeBonusBreakdown(2, 1, "Command Mek", 0, 0, 3, 0, 0, 0);
        String result = breakdown.toBreakdownString();

        assertTrue(result.contains("(using highest modifier only)"));
    }

    @Test
    void breakdownStringWithMultiplePositivesBoldsHighest() {
        var breakdown = new InitiativeBonusBreakdown(2, 1, "Command Mek", 0, 0, 3, 0, 0, 0);
        String result = breakdown.toBreakdownString();

        assertTrue(result.contains("<b>+3 TCP</b>"));
    }

    @Test
    void breakdownStringSortsHighestFirst() {
        // tcp=3, hq=2, quirk=1 -> should appear as "+3 TCP, +2 HQ, +1 Command Mek"
        var breakdown = new InitiativeBonusBreakdown(2, 1, "Command Mek", 0, 0, 3, 0, 0, 0);
        String result = breakdown.toBreakdownString();

        int tcpIndex = result.indexOf("+3 TCP");
        int hqIndex = result.indexOf("+2 HQ");
        int quirkIndex = result.indexOf("+1 Command Mek");

        assertTrue(tcpIndex < hqIndex, "TCP (+3) should appear before HQ (+2)");
        assertTrue(hqIndex < quirkIndex, "HQ (+2) should appear before quirk (+1)");
    }

    @Test
    void breakdownStringShowsNegativesWithoutBold() {
        var breakdown = new InitiativeBonusBreakdown(0, 0, null, 0, 0, 3, -2, 0, 0);
        String result = breakdown.toBreakdownString();

        assertTrue(result.contains("-2 Base"));
        assertFalse(result.contains("<b>-2 Base</b>"));
    }

    @Test
    void breakdownStringUsesQuirkNameWhenProvided() {
        var breakdown = new InitiativeBonusBreakdown(0, 2, "Battle Computer", 0, 0, 0, 0, 0, 0);
        String result = breakdown.toBreakdownString();

        assertTrue(result.contains("Battle Computer"));
        assertFalse(result.contains("Quirk"));
    }

    @Test
    void breakdownStringUsesDefaultQuirkLabelWhenNameNull() {
        var breakdown = new InitiativeBonusBreakdown(0, 2, null, 0, 0, 0, 0, 0, 0);
        String result = breakdown.toBreakdownString();

        assertTrue(result.contains("Quirk"));
    }

    // ==================== add() method ====================

    @Test
    void addCombinesAllComponents() {
        var first = new InitiativeBonusBreakdown(1, 0, null, 2, 0, 0, 0, 0, 0);
        var second = new InitiativeBonusBreakdown(0, 1, "Command Mek", 0, 1, 3, 0, 0, 0);

        var combined = first.add(second);

        assertEquals(1, combined.hq());
        assertEquals(1, combined.quirk());
        assertEquals(2, combined.console());
        assertEquals(1, combined.crewCommand());
        assertEquals(3, combined.tcp());
    }

    @Test
    void addKeepsHigherQuirkName() {
        var first = new InitiativeBonusBreakdown(0, 3, "Battle Computer", 0, 0, 0, 0, 0, 0);
        var second = new InitiativeBonusBreakdown(0, 1, "Command Mek", 0, 0, 0, 0, 0, 0);

        var combined = first.add(second);

        assertEquals("Battle Computer", combined.quirkName());
    }

    // ==================== Factory methods ====================

    @Test
    void zeroCreatesBreakdownWithAllZeros() {
        var breakdown = InitiativeBonusBreakdown.zero();

        assertEquals(0, breakdown.hq());
        assertEquals(0, breakdown.quirk());
        assertEquals(0, breakdown.console());
        assertEquals(0, breakdown.crewCommand());
        assertEquals(0, breakdown.tcp());
        assertEquals(0, breakdown.constant());
        assertEquals(0, breakdown.compensation());
        assertEquals(0, breakdown.crew());
    }

    @Test
    void fromTotalAssignsValueToConstant() {
        var breakdown = InitiativeBonusBreakdown.fromTotal(5);

        assertEquals(5, breakdown.constant());
        assertEquals(5, breakdown.total());
    }
}
