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
package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Guards the inert support-gear equipment (specialized repair kits, tool kits, field/medical/computer kits) MekHQ's
 * quartermaster issues to personnel. These carry no combat behaviour: they occupy no critical slots, mount on no unit,
 * and exist only so a kit can be bought, stocked, and carried.
 */
class SupportGearEquipmentTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /** Every support-gear item the quartermaster reasons about, by internal name. */
    private static final List<String> SUPPORT_GEAR = List.of(
          // Specialized repair kits
          "Aerospace Repair Kit",
          "Bionic Maintenance Kit",
          "Cutting/Joining Kit",
          "Electronics Repair Kit",
          "Fission/Fusion Repair Kit",
          "Myomer/Actuator Repair Kit",
          "Vehicle Repair Kit",
          "Weapon Repair Kit",
          // General tool kits and diagnostic scanners
          "Basic Toolkit",
          "Deluxe Toolkit",
          "Descartes MK XXI",
          "Descartes MK XXV",
          // Field, medical, computer and other skill kits
          "Advanced Field Kit",
          "Basic Field Kit",
          "Compass",
          "Electronic Compass",
          "Advanced Medical Kit",
          "Field Surgical Kit",
          "Medical Kit",
          "Compad",
          "Noteputer",
          "Personal Computer",
          "Pocket Transcriber",
          "Telescan");

    @ParameterizedTest
    @ValueSource(strings = {
          "Aerospace Repair Kit", "Bionic Maintenance Kit", "Cutting/Joining Kit", "Electronics Repair Kit",
          "Fission/Fusion Repair Kit", "Myomer/Actuator Repair Kit", "Vehicle Repair Kit", "Weapon Repair Kit",
          "Basic Toolkit", "Deluxe Toolkit", "Descartes MK XXI", "Descartes MK XXV",
          "Advanced Field Kit", "Basic Field Kit", "Compass", "Electronic Compass",
          "Advanced Medical Kit", "Field Surgical Kit", "Medical Kit",
          "Compad", "Noteputer", "Personal Computer", "Pocket Transcriber", "Telescan" })
    void eachSupportGearItemRegistersAsInertMiscType(String internalName) {
        EquipmentType type = EquipmentType.get(internalName);
        assertNotNull(type, "'" + internalName + "' is not registered");
        MiscType misc = assertInstanceOf(MiscType.class, type, "'" + internalName + "' should be a MiscType");
        assertEquals(0.0, misc.getBaseCriticalSlots(), "'" + internalName + "' should occupy no critical slots");
        assertTrue(misc.isIndustrial(), "'" + internalName + "' should be industrial support gear");
        assertTrue(misc.getTonnage(null) > 0.0, "'" + internalName + "' should have a positive weight");
    }

    @Test
    void allSupportGearIsDistinctlyRegistered() {
        assertEquals(SUPPORT_GEAR.size(), SUPPORT_GEAR.stream().distinct().count(), "duplicate names in the list");
        for (String name : SUPPORT_GEAR) {
            assertNotNull(EquipmentType.get(name), "'" + name + "' is not registered");
        }
    }

    @Test
    void spotCheckKitWeightsAndCostsAsRegressionGuards() {
        // A handful of exact values so an accidental edit to the kit tables is caught.
        assertKit("Basic Toolkit", 0.01, 250);
        assertKit("Deluxe Toolkit", 0.05, 750);
        assertKit("Field Surgical Kit", 0.0115, 800);
        assertKit("Personal Computer", 0.003, 250);
        assertKit("Telescan", 0.00075, 100);
    }

    private static void assertKit(String internalName, double tonnage, double cost) {
        MiscType misc = (MiscType) EquipmentType.get(internalName);
        assertNotNull(misc, "'" + internalName + "' is not registered");
        assertEquals(tonnage, misc.getTonnage(null), 1e-9, "'" + internalName + "' weight");
        assertEquals(cost, misc.getCost(null, false, -1), 1e-6, "'" + internalName + "' cost");
    }
}
