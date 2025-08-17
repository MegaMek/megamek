/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.equipment.EquipmentType.T_ARMOR_FERRO_FIBROUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.interfaces.ITechnology;
import megamek.common.equipment.MiscType;
import megamek.common.units.SupportTank;
import megamek.common.equipment.ArmorType;
import megamek.common.verifier.TestSupportVehicle.ChassisModification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestSupportVehicleTest {

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testChassisModLookup() {
        for (ChassisModification mod : ChassisModification.values()) {
            assertNotNull(mod.equipment);
            assertTrue(mod.equipment.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT));
            assertTrue(mod.equipment.hasFlag(MiscType.F_CHASSIS_MODIFICATION));
        }
    }

    @Test
    void testBAR10ArmorCorrectSlots() {
        SupportTank st = new SupportTank();
        st.setArmorType(EquipmentType.T_ARMOR_SV_BAR_10);
        // Rating E should return CV slots for IS FF
        st.setArmorTechRating(ITechnology.TechRating.E);
        assertEquals(
              2,
              ArmorType.of(T_ARMOR_FERRO_FIBROUS, false).getSupportVeeSlots(st));

        // Rating F should return CV slots for Clan FF
        st.setArmorTechRating(ITechnology.TechRating.F);
        assertEquals(
              1,
              ArmorType.of(T_ARMOR_FERRO_FIBROUS, true).getSupportVeeSlots(st));
    }
}
