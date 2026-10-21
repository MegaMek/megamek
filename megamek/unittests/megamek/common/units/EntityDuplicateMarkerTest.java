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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #9297: two units of the same type ended up with the same "#N" name, because a unit that
 * arrived already numbered (MekHQ numbers units across the whole hangar) kept its old name when the game numbered it
 * #1.
 */
class EntityDuplicateMarkerTest {

    private boolean savedShowUnitId;

    /** The "show unit ID" client setting replaces the "#N" suffix, so pin it off whatever the local settings say. */
    @BeforeEach
    void hideUnitIds() {
        ClientPreferences clientPreferences = PreferenceManager.getClientPreferences();
        savedShowUnitId = clientPreferences.getShowUnitId();
        clientPreferences.setShowUnitId(false);
    }

    @AfterEach
    void restoreShowUnitId() {
        PreferenceManager.getClientPreferences().setShowUnitId(savedShowUnitId);
    }

    private static Entity createUnit() {
        BipedMek mek = new BipedMek();
        mek.setChassis("Coyotl");
        mek.setModel("Prime");
        return mek;
    }

    @Test
    void markerOneRemovesAnEarlierNumberFromTheName() {
        Entity unit = createUnit();
        unit.setDuplicateMarker(4);
        assertEquals("Coyotl Prime #4", unit.getShortName());

        unit.setDuplicateMarker(1);

        assertEquals("Coyotl Prime", unit.getShortName());
        assertEquals("Coyotl Prime", unit.getDisplayName());
    }

    @Test
    void unitsArrivingWithCampaignNumbersGetDistinctNames() {
        // As in the reported game: the hangar had already named this unit #2, and it is the first of its type
        // to be added to the battle.
        Entity alreadyNumberedUnit = createUnit();
        alreadyNumberedUnit.setDuplicateMarker(2);
        Entity secondUnit = createUnit();

        UnitNameTracker tracker = new UnitNameTracker();
        tracker.add(alreadyNumberedUnit);
        tracker.add(secondUnit);

        assertEquals("Coyotl Prime", alreadyNumberedUnit.getShortName());
        assertEquals("Coyotl Prime #2", secondUnit.getShortName());
        assertNotEquals(alreadyNumberedUnit.getShortName(), secondUnit.getShortName());
    }
}
