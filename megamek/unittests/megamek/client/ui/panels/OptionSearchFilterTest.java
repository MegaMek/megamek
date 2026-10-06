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

package megamek.client.ui.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the shared matching behind the search boxes on the option lists (special pilot abilities, quirks).
 */
@DisplayName("Option search filter")
class OptionSearchFilterTest {

    private static boolean matches(String searchText, String filter) {
        return OptionSearchFilter.matches(OptionSearchFilter.normalize(searchText),
              OptionSearchFilter.normalize(filter));
    }

    @Test
    @DisplayName("An empty filter matches every row")
    void emptyFilterMatchesEverything() {
        assertTrue(matches("Blood Stalker (CamOps) -1 to-hit vs one designated enemy", ""));
        assertTrue(matches("Battle Fists (LA) Punch damage is doubled", ""));
        assertTrue(matches("", ""));
    }

    @Test
    @DisplayName("Matching is case-insensitive")
    void matchingIsCaseInsensitive() {
        assertTrue(matches("Blood Stalker (CamOps)", "blood"));
        assertTrue(matches("Blood Stalker (CamOps)", "BLOOD STALKER"));
        assertTrue(matches("Improved Targeting (Long)", "IMPROVED TARGETING"));
        assertTrue(matches("weak head armor (3)", "Head Armor"));
    }

    @Test
    @DisplayName("Effect and description text match, not just the name")
    void effectTextMatches() {
        String abilityText = "Sniper (CamOps) Halve range modifiers: medium +1, long +2";
        assertTrue(matches(abilityText, "range modifiers"));
        assertFalse(matches(abilityText, "cluster"));

        String quirkText = "Poor Cooling Jacket +1 to heat (SO pg 198)";
        assertTrue(matches(quirkText, "heat"));
        assertFalse(matches(quirkText, "ammo"));
    }

    @Test
    @DisplayName("A rules reference in the search text is matchable")
    void rulesReferenceMatches() {
        assertTrue(matches("Nimble Jumper The unit is especially nimble when jumping BMM p.85", "bmm p.85"));
    }

    @Test
    @DisplayName("Non-matching text hides the row")
    void nonMatchingTextHidesRow() {
        assertFalse(matches("Blood Stalker (CamOps)", "zweihander"));
        assertFalse(matches("Battle Fists (LA)", "searchlight"));
        assertFalse(matches("", "anything"));
    }

    @Test
    @DisplayName("Normalizing is idempotent, so pre-normalized text stays matchable")
    void normalizeIsIdempotent() {
        String normalizedOnce = OptionSearchFilter.normalize("Improved Targeting (Long)");
        assertEquals(normalizedOnce, OptionSearchFilter.normalize(normalizedOnce));
    }
}
