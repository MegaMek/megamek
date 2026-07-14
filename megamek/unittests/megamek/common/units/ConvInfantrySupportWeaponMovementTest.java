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

import megamek.common.MPCalculationSetting;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConvInfantrySupportWeaponMovementTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void supportWeaponsReduceGroundAndJumpMovement() {
        ConvInfantry infantry = createJumpInfantry();
        infantry.setSecondaryWeapon(infantryWeapon(EquipmentTypeLookup.INFANTRY_MORTAR_LIGHT));

        assertEquals(0, infantry.getWalkMP(MPCalculationSetting.NO_GRAVITY));
        assertEquals(2, infantry.getJumpMP(MPCalculationSetting.NO_GRAVITY));
    }

    @Test
    void tagTroopsDoNotReduceGroundOrJumpMovement() {
        ConvInfantry infantry = createJumpInfantry();
        infantry.setSecondaryWeapon(infantryWeapon(EquipmentTypeLookup.INFANTRY_TAG));
        infantry.setSpecializations(ConvInfantry.TAG_TROOPS);

        assertEquals(1, infantry.getWalkMP(MPCalculationSetting.NO_GRAVITY));
        assertEquals(3, infantry.getJumpMP(MPCalculationSetting.NO_GRAVITY));
    }

    @Test
    void paramedicsDoNotReduceGroundOrJumpMovement() {
        ConvInfantry infantry = createJumpInfantry();
        infantry.setSecondaryWeapon(infantryWeapon(EquipmentTypeLookup.INFANTRY_MORTAR_LIGHT));
        infantry.setSpecializations(ConvInfantry.PARAMEDICS);

        assertEquals(1, infantry.getWalkMP(MPCalculationSetting.NO_GRAVITY));
        assertEquals(3, infantry.getJumpMP(MPCalculationSetting.NO_GRAVITY));
    }

    private static ConvInfantry createJumpInfantry() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setMovementMode(EntityMovementMode.INF_JUMP);
        infantry.setSecondaryWeaponsPerSquad(2);
        return infantry;
    }

    private static InfantryWeapon infantryWeapon(String internalName) {
        return (InfantryWeapon) EquipmentType.get(internalName);
    }
}