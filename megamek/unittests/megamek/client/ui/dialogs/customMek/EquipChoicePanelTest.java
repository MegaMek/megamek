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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.Aero;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EquipChoicePanel#shouldSetupMunitions}. Guards against the regression in issue #8284, where the
 * manual ammo configuration section disappeared from the Equipment tab for BattleMeks, vehicles, and aerospace units.
 */
@DisplayName("EquipChoicePanel munitions-section gating")
class EquipChoicePanelTest {

    @Test
    @DisplayName("BattleMeks get the munitions section (regression #8284)")
    void mekGetsMunitionsSection() {
        assertTrue(EquipChoicePanel.shouldSetupMunitions(mock(BipedMek.class)));
    }

    @Test
    @DisplayName("Vehicles get the munitions section")
    void vehicleGetsMunitionsSection() {
        assertTrue(EquipChoicePanel.shouldSetupMunitions(mock(Tank.class)));
    }

    @Test
    @DisplayName("Aerospace units get the munitions section")
    void aerospaceGetsMunitionsSection() {
        assertTrue(EquipChoicePanel.shouldSetupMunitions(mock(Aero.class)));
    }

    @Test
    @DisplayName("Conventional infantry with a field weapon gets the munitions section")
    void conventionalInfantryWithFieldWeaponGetsMunitionsSection() {
        ConvInfantry convInfantry = mock(ConvInfantry.class);
        when(convInfantry.hasFieldWeapon()).thenReturn(true);
        assertTrue(EquipChoicePanel.shouldSetupMunitions(convInfantry));
    }

    @Test
    @DisplayName("Conventional infantry without a field weapon has no munitions section")
    void conventionalInfantryWithoutFieldWeaponHasNoMunitionsSection() {
        ConvInfantry convInfantry = mock(ConvInfantry.class);
        when(convInfantry.hasFieldWeapon()).thenReturn(false);
        assertFalse(EquipChoicePanel.shouldSetupMunitions(convInfantry));
    }
}
