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

package megamek.common.alphaStrike.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.MPCalculationSetting;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ASMovementConverterTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void minimumConvertedMovementUsesTwoForAnyPositiveMovementBelowTwo() {
        assertEquals(0, ASMovementConverter.minimumConvertedMovement(0));
        assertEquals(2, ASMovementConverter.minimumConvertedMovement(1));
        assertEquals(2, ASMovementConverter.minimumConvertedMovement(2));
        assertEquals(4, ASMovementConverter.minimumConvertedMovement(4));
    }

    @Test
    void convertMovementUsesTwoInchesForZeroStarConventionalInfantry() {
        Infantry infantry = new Infantry();
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setOriginalWalkMP(1);
        infantry.setArmorEncumbering(true);

        InfantryWeapon supportWeapon = mock(InfantryWeapon.class);
        when(supportWeapon.getName()).thenReturn("Mock Support Weapon");
        when(supportWeapon.hasFlag(WeaponType.F_INF_SUPPORT)).thenReturn(true);
        infantry.setSecondaryWeapon(supportWeapon);
        infantry.setSecondaryWeaponsPerSquad(2);

        assertEquals(0, infantry.getWalkMP(MPCalculationSetting.AS_CONVERSION));
        assertTrue(infantry.hasMinimalGroundMP(MPCalculationSetting.AS_CONVERSION));

        Map<String, Integer> movement = ASMovementConverter.convertMovement(
              new ASConverter.ConversionData(infantry, new AlphaStrikeElement(), new DummyCalculationReport()));

        assertEquals(2, movement.get("f"));
    }
}