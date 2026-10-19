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
package megamek.common.preference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The lobby shows unit IDs by default so players can find them for bot targets (#9173), while unit names elsewhere in
 * the game keep their usual form.
 */
class LobbyShowUnitIdPreferenceTest {

    @Test
    void theLobbyShowsUnitIdsByDefault() {
        ClientPreferences preferences = new ClientPreferences(new PreferenceStore());

        assertTrue(preferences.getLobbyShowUnitId());
    }

    @Test
    void unitNamesInReportsKeepTheirDefault() {
        ClientPreferences preferences = new ClientPreferences(new PreferenceStore());

        assertFalse(preferences.getShowUnitId());
    }

    @Test
    void turningTheLobbyIdsOffLeavesReportNamesAlone() {
        ClientPreferences preferences = new ClientPreferences(new PreferenceStore());

        preferences.setLobbyShowUnitId(false);
        preferences.setShowUnitId(true);

        assertFalse(preferences.getLobbyShowUnitId());
        assertTrue(preferences.getShowUnitId());
    }
}
