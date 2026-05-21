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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.TechConstants;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FrankenMekTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void donorArmorCopiesFrontAndRearArmorPoints() {
        Mek target = new BipedMek();
        target.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
        target.setWeight(25.0);
        target.setFrankenMek(true);
        target.initializeArmor(1, Mek.LOC_RIGHT_TORSO);
        target.initializeRearArmor(1, Mek.LOC_RIGHT_TORSO);

        Mek donor = new BipedMek();
        donor.initializeArmor(16, Mek.LOC_RIGHT_TORSO);
        donor.initializeRearArmor(7, Mek.LOC_RIGHT_TORSO);

        target.applyFrankenMekDonorLocationArmor(Mek.LOC_RIGHT_TORSO, donor);

        assertEquals(16, target.getOArmor(Mek.LOC_RIGHT_TORSO));
        assertEquals(16, target.getArmor(Mek.LOC_RIGHT_TORSO));
        assertEquals(7, target.getOArmor(Mek.LOC_RIGHT_TORSO, true));
        assertEquals(7, target.getArmor(Mek.LOC_RIGHT_TORSO, true));
    }

    @Test
    void donorArmorUsesDonorPatchworkLocationArmorType() {
        Mek target = new BipedMek();
        target.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
        target.setWeight(25.0);
        target.setFrankenMek(true);
        target.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        target.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX);

        Mek donor = new BipedMek();
        donor.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        donor.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX);
        donor.setArmorType(EquipmentType.T_ARMOR_FERRO_FIBROUS, Mek.LOC_LEFT_ARM);
        donor.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX, Mek.LOC_LEFT_ARM);

        target.applyFrankenMekDonorLocationArmor(Mek.LOC_RIGHT_ARM, donor);

        assertFalse(target.hasPatchworkArmor());
        assertEquals(EquipmentType.T_ARMOR_STANDARD, target.getArmorType(Mek.LOC_RIGHT_ARM));

        target.applyFrankenMekDonorLocationArmor(Mek.LOC_LEFT_ARM, donor);

        assertTrue(target.hasPatchworkArmor());
        assertEquals(EquipmentType.T_ARMOR_FERRO_FIBROUS, target.getArmorType(Mek.LOC_LEFT_ARM));
        assertEquals(EquipmentType.T_ARMOR_STANDARD, target.getArmorType(Mek.LOC_RIGHT_ARM));
    }
}