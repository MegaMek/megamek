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

package megamek.client.ui.dialogs.advancedsearch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.loaders.MekSummary;
import org.junit.jupiter.api.Test;

class MekSearchFilterTest {

    @Test
    void matchesSourceFilterMatchesSource() {
        MekSummary mek = new MekSummary();
        mek.setSource("Technical Readout: 3039");
        mek.setPublished("Record Sheets: 3050 Upgrade");

        assertTrue(MekSearchFilter.matchesSourceFilter(mek, "readout 3039"));
    }

    @Test
    void matchesSourceFilterMatchesPublished() {
        MekSummary mek = new MekSummary();
        mek.setSource("Technical Readout: 3039");
        mek.setPublished("Record Sheets: 3050 Upgrade");

        assertTrue(MekSearchFilter.matchesSourceFilter(mek, "3050 upgrade"));
    }

    @Test
    void matchesSourceFilterMatchesAnySourceInList() {
        MekSummary mek = new MekSummary();
        mek.setSource("Technical Readout: 3039");
        mek.setPublished("Record Sheets: 3050 Upgrade");

        assertTrue(MekSearchFilter.matchesSourceFilter(mek, "Interstellar Operations, readout 3039"));
        assertTrue(MekSearchFilter.matchesSourceFilter(mek, "Interstellar Operations, 3050 upgrade"));
    }

    @Test
    void matchesSourceFilterRejectsWhenNeitherFieldMatches() {
        MekSummary mek = new MekSummary();
        mek.setSource("Technical Readout: 3039");
        mek.setPublished("Record Sheets: 3050 Upgrade");

        assertFalse(MekSearchFilter.matchesSourceFilter(mek, "Interstellar Operations"));
    }

    @Test
    void matchesSourceFilterRejectsWhenNoSourceInListMatches() {
        MekSummary mek = new MekSummary();
        mek.setSource("Technical Readout: 3039");
        mek.setPublished("Record Sheets: 3050 Upgrade");

        assertFalse(MekSearchFilter.matchesSourceFilter(mek, "Interstellar Operations, Tactical Operations"));
    }
}
