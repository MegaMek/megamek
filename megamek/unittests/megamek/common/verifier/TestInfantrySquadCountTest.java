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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.ConvInfantry;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.Test;

class TestInfantrySquadCountTest {
    @Test
    void excessiveSquadCountChangesValidityEvenBelowPlatoonSizeLimit() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setSquadSize(1);
        infantry.setSquadCount(5);
        TestInfantry verifier = new TestInfantry(infantry, new TestXMLOption(), null);
        StringBuffer messages = new StringBuffer();
        assertTrue(verifier.correctEntity(messages, infantry.getTechLevel()));
        assertEquals("", messages.toString());

        infantry.setSquadCount(6);
        assertFalse(verifier.correctEntity(messages, infantry.getTechLevel()));
        assertEquals("Maximum squad count is 5\n\n", messages.toString());
    }
}
