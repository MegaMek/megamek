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
 * Tests the pure filter predicate behind the SPA search box in {@link PilotOptionsPanel}.
 */
@DisplayName("SPA filter matching")
class PilotOptionsPanelTest {

    private static boolean matches(String searchText, String filter) {
        return PilotOptionsPanel.matchesFilter(PilotOptionsPanel.normalize(searchText),
              PilotOptionsPanel.normalize(filter));
    }

    @Test
    @DisplayName("An empty filter matches every row")
    void emptyFilterMatchesEverything() {
        assertTrue(matches("Blood Stalker (CamOps) -1 to-hit vs one designated enemy", ""));
        assertTrue(matches("", ""));
    }

    @Test
    @DisplayName("Matching is case-insensitive")
    void matchingIsCaseInsensitive() {
        assertTrue(matches("Blood Stalker (CamOps)", "blood"));
        assertTrue(matches("Blood Stalker (CamOps)", "BLOOD STALKER"));
        assertTrue(matches("terrain master (drag racer)", "Drag Racer"));
    }

    @Test
    @DisplayName("Effect text matches, not just the name")
    void effectTextMatches() {
        String searchText = "Sniper (CamOps) Halve range modifiers: medium +1, long +2";
        assertTrue(matches(searchText, "range modifiers"));
        assertFalse(matches(searchText, "cluster"));
    }

    @Test
    @DisplayName("Non-matching text hides the row")
    void nonMatchingTextHidesRow() {
        assertFalse(matches("Blood Stalker (CamOps)", "zweihander"));
        assertFalse(matches("", "anything"));
    }
}
