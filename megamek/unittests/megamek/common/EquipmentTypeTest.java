/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.StructureType;
import megamek.common.weapons.bayWeapons.TeleOperatedMissileBayWeapon;
import org.junit.jupiter.api.Test;

class EquipmentTypeTest {
    @Test
    void structureCostArraySameLengthAsStructureNames() {
        assertEquals(EquipmentType.structureCosts.length, EquipmentType.structureNames.length);
    }

    @Test
    void replacingInternalNameRemovesInheritedInternalName() {
        EquipmentType teleOperatedBay = new TeleOperatedMissileBayWeapon();

        assertEquals(EquipmentTypeLookup.TELE_CAPITAL_MISSILE_BAY, teleOperatedBay.getInternalName());
        assertIterableEquals(List.of(EquipmentTypeLookup.TELE_CAPITAL_MISSILE_BAY),
              Collections.list(teleOperatedBay.getNames()));
    }

    @Test
    void replacingInternalNameRemovesSuperclassAliases() {
        EquipmentType equipmentType = new EquipmentTypeWithInheritedLookupNames();

        assertEquals("Child", equipmentType.getInternalName());
        assertIterableEquals(List.of("Child"), Collections.list(equipmentType.getNames()));
    }

    @Test
    void structureAndArmorLookups() {
        EquipmentType.initializeTypes();

        ArmorType standardArmor = EquipmentType.getArmorFromName("IS Standard");
        StructureType standardStructure = EquipmentType.getStructureFromName("IS Standard");
        ArmorType industrialArmor = EquipmentType.getArmorFromName("Clan Industrial");
        StructureType industrialStructure = EquipmentType.getStructureFromName("Clan Industrial");
        ArmorType heavyIndustrialArmor = EquipmentType.getArmorFromName("IS Heavy Industrial");

        assertNotNull(standardArmor);
        assertNotNull(standardStructure);
        assertNotNull(industrialArmor);
        assertNotNull(industrialStructure);
        assertNotNull(heavyIndustrialArmor);
        assertEquals(EquipmentType.T_ARMOR_STANDARD, standardArmor.getArmorType());
        assertEquals(EquipmentType.T_STRUCTURE_STANDARD, standardStructure.getStructureTypeId());
        assertEquals(EquipmentType.T_ARMOR_INDUSTRIAL, industrialArmor.getArmorType());
        assertEquals(EquipmentType.T_STRUCTURE_INDUSTRIAL, industrialStructure.getStructureTypeId());
        assertEquals(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL, heavyIndustrialArmor.getArmorType());
        assertEquals(EquipmentType.T_STRUCTURE_UNKNOWN, EquipmentType.getStructureType(standardArmor));
    }

    @Test
    void allEquipmentLookupNamesAreUnique() {
        EquipmentType.initializeTypes();

        assertTrue(EquipmentType.getLookupCollisions().isEmpty(),
              () -> "Equipment lookup name collisions:\n" + EquipmentType.getLookupCollisions().entrySet().stream()
                  .map(collision -> collision.getKey() + ": " + collision.getValue().stream()
                      .map(EquipmentType::getInternalName)
                      .sorted()
                      .collect(Collectors.joining(", ")))
                  .collect(Collectors.joining("\n")));
    }

    @Test
    void armorLookupSupportsDisplayInternalAndLegacyNames() {
        ArmorType prototypeFerroAluminum = EquipmentType.getArmorFromName("Prototype Ferro-Aluminum");

        assertNotNull(prototypeFerroAluminum);
        assertEquals(EquipmentType.T_ARMOR_FERRO_ALUM_PROTO, prototypeFerroAluminum.getArmorType());
        assertEquals(prototypeFerroAluminum, EquipmentType.getArmorFromName("IS Prototype Ferro-Aluminum"));
        assertEquals(prototypeFerroAluminum, EquipmentType.getArmorFromName("IS Ferro-Alum Armor Prototype"));
        assertEquals(prototypeFerroAluminum, EquipmentType.getArmorFromName(" prototype ferro-aluminum "));
        assertNull(EquipmentType.getArmorFromName("Unknown Armor Type"));
    }

    private static class EquipmentTypeWithInheritedLookupNames extends EquipmentType {
        private EquipmentTypeWithInheritedLookupNames() {
            setInternalName("Parent");
            addLookupName("AAAA");
            addLookupName("BBBB");
            clearLookupNames();
            setInternalName("Child");
        }
    }

}
