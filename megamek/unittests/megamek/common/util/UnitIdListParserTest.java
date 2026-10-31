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
package megamek.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Covers the unit ID list typed into the bot's Add Unit box (#9173): before this, {@code 3-7} added only unit 3 and
 * {@code 3, 4} added only unit 3, with no warning either time.
 */
class UnitIdListParserTest {

    @Test
    void rangeAddsEveryUnitFromStartToEnd() {
        UnitIdListParser.Result result = UnitIdListParser.parse("3-7");

        assertEquals(List.of(3, 4, 5, 6, 7), List.copyOf(result.unitIds()));
        assertTrue(result.isFullyRead());
    }

    @Test
    void spaceAfterCommaIsIgnored() {
        UnitIdListParser.Result result = UnitIdListParser.parse("3, 4");

        assertEquals(List.of(3, 4), List.copyOf(result.unitIds()));
    }

    @Test
    void spacesAroundTheDashAreIgnored() {
        UnitIdListParser.Result result = UnitIdListParser.parse(" 3 - 5 ,10 ");

        assertEquals(List.of(3, 4, 5, 10), List.copyOf(result.unitIds()));
        assertTrue(result.isFullyRead());
    }

    @Test
    void reversedRangeReadsTheSameAsForwards() {
        UnitIdListParser.Result result = UnitIdListParser.parse("7-3");

        assertEquals(List.of(3, 4, 5, 6, 7), List.copyOf(result.unitIds()));
    }

    @Test
    void duplicatesAndOverlapsCountOnce() {
        UnitIdListParser.Result result = UnitIdListParser.parse("4, 3-5, 5");

        assertEquals(List.of(3, 4, 5), List.copyOf(result.unitIds()));
    }

    @Test
    void unreadableEntriesAreReportedAndTheRestStillAdded() {
        UnitIdListParser.Result result = UnitIdListParser.parse("abc, 4, 2-x, -5");

        assertEquals(List.of(4), List.copyOf(result.unitIds()));
        assertEquals(List.of("abc", "2-x", "-5"), result.unreadableEntries());
        assertFalse(result.isFullyRead());
    }

    @Test
    void emptyEntriesAndBlankTextAddNothingAndWarnAboutNothing() {
        UnitIdListParser.Result blank = UnitIdListParser.parse("   ");
        UnitIdListParser.Result strayCommas = UnitIdListParser.parse("3,, ,4,");

        assertTrue(blank.unitIds().isEmpty());
        assertTrue(blank.isFullyRead());
        assertEquals(List.of(3, 4), List.copyOf(strayCommas.unitIds()));
        assertTrue(strayCommas.isFullyRead());
    }

    @Test
    void nullTextReadsAsAnEmptyList() {
        UnitIdListParser.Result result = UnitIdListParser.parse(null);

        assertTrue(result.unitIds().isEmpty());
        assertTrue(result.isFullyRead());
    }

    @Test
    void rangeOverTheCapIsRefusedRatherThanBuilt() {
        int lastAllowed = UnitIdListParser.MAXIMUM_RANGE_SIZE - 1;
        UnitIdListParser.Result atCap = UnitIdListParser.parse("0-" + lastAllowed);
        UnitIdListParser.Result overCap = UnitIdListParser.parse("1-99999");

        assertEquals(UnitIdListParser.MAXIMUM_RANGE_SIZE, atCap.unitIds().size());
        assertTrue(overCap.unitIds().isEmpty());
        assertEquals(List.of("1-99999"), overCap.unreadableEntries());
    }

    @Test
    void numberTooLargeForAnIdIsReportedNotThrown() {
        UnitIdListParser.Result result = UnitIdListParser.parse("99999999999, 2");

        assertEquals(List.of(2), List.copyOf(result.unitIds()));
        assertEquals(List.of("99999999999"), result.unreadableEntries());
    }
}
