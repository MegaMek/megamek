/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.verifier;

import static megamek.common.EquipmentType.T_ARMOR_FERRO_FIBROUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.MiscType;
import megamek.common.SupportTank;
import megamek.common.equipment.ArmorType;
import megamek.common.verifier.TestSupportVehicle.ChassisModification;

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
        st.setArmorTechRating(ITechnology.RATING_E);
        assertEquals(
                2,
                ArmorType.of(T_ARMOR_FERRO_FIBROUS, false).getSupportVeeSlots(st));

        // Rating F should return CV slots for Clan FF
        st.setArmorTechRating(ITechnology.RATING_F);
        assertEquals(
                1,
                ArmorType.of(T_ARMOR_FERRO_FIBROUS, true).getSupportVeeSlots(st));
    }
}
