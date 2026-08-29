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
package megamek.common.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Covers reading a rank name out of a rank row.
 */
class RankSystemTest {

    private static RankSystem systemWithRow(String... row) {
        return new RankSystem("TEST", "Test System", List.<String[]>of(row));
    }

    /**
     * A rank spanning several levels records how many after a colon. "Precentor:25" is the Precentor
     * rank with twenty-five levels, and displaying it verbatim put "Precentor:25 Athdara Nairn" in the
     * force preview.
     */
    @Test
    void aRankLevelCountIsNotPartOfTheName() {
        assertEquals("Precentor", systemWithRow(new String[] { "Precentor:25" }).nameAt(0));
        assertEquals("Adept", systemWithRow(new String[] { "Adept:25" }).nameAt(0));
        assertEquals("Demi-Precentor",
              systemWithRow(new String[] { "Demi-Precentor:25 " }).nameAt(0),
              "trailing space after the count must not survive either");
    }

    @Test
    void aRankWithoutACountIsUnchanged() {
        assertEquals("Precentor Arms", systemWithRow(new String[] { "Precentor Arms" }).nameAt(0));
        assertEquals("Khan", systemWithRow(new String[] { "Khan" }).nameAt(0));
    }

    /**
     * Only a trailing count is stripped, so a rank whose name genuinely contains a colon survives.
     */
    @Test
    void aColonInsideTheNameIsKept() {
        assertEquals("Precentor: Martial",
              systemWithRow(new String[] { "Precentor: Martial" }).nameAt(0));
        assertEquals("Precentor: Martial",
              systemWithRow(new String[] { "Precentor: Martial:3" }).nameAt(0),
              "the count goes, the inner colon stays");
    }

    @Test
    void placeholdersAndMissingRowsResolveToNothing() {
        assertNull(systemWithRow(new String[] { "-" }).nameAt(0), "a dash means inherit, not a name");
        assertNull(systemWithRow(new String[] { "Precentor" }).nameAt(5), "index beyond the rows");
        assertNull(systemWithRow(new String[] { "Precentor" }).nameAt(-1));
    }

    /**
     * A row that names nothing in the default column still yields the first real name, so a naval-only
     * entry produces a label rather than nothing.
     */
    @Test
    void aLaterColumnIsUsedWhenTheDefaultIsAPlaceholder() {
        assertEquals("Spaceman Recruit",
              systemWithRow(new String[] { "-", "Spaceman Recruit:4" }).nameAt(0));
    }
}
