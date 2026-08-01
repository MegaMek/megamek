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

package megamek.client.ui.dialogs.customMek;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the pure filter predicate behind the quirk search box in {@link QuirksPanel}.
 */
@DisplayName("Quirk filter matching")
class QuirksPanelTest {

    private static boolean matches(String searchText, String filter) {
        return QuirksPanel.matchesFilter(QuirksPanel.normalize(searchText), QuirksPanel.normalize(filter));
    }

    @Test
    @DisplayName("An empty filter matches every row")
    void emptyFilterMatchesEverything() {
        assertTrue(matches("Battle Fists (LA) Punch damage is doubled", ""));
        assertTrue(matches("", ""));
    }

    @Test
    @DisplayName("Matching is case-insensitive")
    void matchingIsCaseInsensitive() {
        assertTrue(matches("Improved Targeting (Long)", "improved"));
        assertTrue(matches("Improved Targeting (Long)", "IMPROVED TARGETING"));
        assertTrue(matches("weak head armor (3)", "Head Armor"));
    }

    @Test
    @DisplayName("Description text matches, not just the quirk name")
    void descriptionTextMatches() {
        String searchText = "Poor Cooling Jacket +1 to heat (SO pg 198)";
        assertTrue(matches(searchText, "heat"));
        assertFalse(matches(searchText, "ammo"));
    }

    @Test
    @DisplayName("The rules reference is searchable")
    void rulesReferenceMatches() {
        assertTrue(matches("Nimble Jumper The unit is especially nimble when jumping BMM p.85", "bmm p.85"));
    }

    @Test
    @DisplayName("Non-matching text hides the row")
    void nonMatchingTextHidesRow() {
        assertFalse(matches("Battle Fists (LA)", "searchlight"));
        assertFalse(matches("", "anything"));
    }
}
