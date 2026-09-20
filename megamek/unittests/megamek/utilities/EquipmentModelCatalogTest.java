/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.StructureType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponTypeFlag;
import org.junit.jupiter.api.Test;

class EquipmentModelCatalogTest {
    @Test
    void registeredEquipmentIsCoveredWithoutLoadingUnitVariants() {
        List<EquipmentType> types = EquipmentType.allTypes();
        var entries = EquipmentModelCatalog.entries();
        assertEquals(types.size(), entries.size());
        assertEquals(types.stream().filter(WeaponType.class::isInstance)
              .filter(t -> !t.hasFlag(WeaponTypeFlag.INTERNAL_REPRESENTATION)).count(), entries.stream()
              .filter(e -> (e.policy() == EquipmentModelPolicy.WEAPON) || (e.policy() == EquipmentModelPolicy.MEMBERS))
              .count());
        for (EquipmentType type : types) {
            var entry = EquipmentModelCatalog.describe(type);
            if ((type instanceof ArmorType) || (type instanceof StructureType) || (type instanceof AmmoType)) {
                assertEquals(EquipmentModelPolicy.NONE, entry.policy(), type.getInternalName());
                assertFalse(entry.allowsFallback(), type.getInternalName());
                assertEquals("none", entry.family(), type.getInternalName());
            }
        }
    }

    @Test
    void miscInheritanceCannotBypassTheHardExclusions() {
        for (MiscType type : List.of(new ArmorType(), new StructureType(EquipmentType.T_STRUCTURE_STANDARD))) {
            type.getFlags().set(MiscType.F_PHYSICAL_WEAPON);
            assertEquals(EquipmentModelPolicy.NONE, EquipmentModelPolicy.forType(type));
        }
    }

    @Test
    void physicalEquipmentIncludesClawsAndShieldsAsWellAsClubWeapons() {
        for (MiscType type : List.of(MiscType.createHatchet(), MiscType.createSword(), MiscType.createISClaw(),
              MiscType.createISSmallShield())) {
            var entry = EquipmentModelCatalog.describe(type);
            assertEquals(EquipmentModelPolicy.PHYSICAL_WEAPON, entry.policy(), type.getInternalName());
            assertTrue(entry.allowsFallback());
            assertFalse(entry.family().equals("internal"));
        }
    }

    @Test
    void optionalMiscHasNoFallbackEvenWhenItsFamilyHasArt() {
        for (MiscType type : List.of(MiscType.createSearchlight(), MiscType.createGECM(), new MiscType())) {
            var entry = EquipmentModelCatalog.describe(type);
            assertEquals(EquipmentModelPolicy.OPTIONAL_MISC, entry.policy());
            assertFalse(entry.allowsFallback());
        }
        assertEquals("searchlight", EquipmentModelCatalog.describe(MiscType.createSearchlight()).family());
        assertEquals("ecm", EquipmentModelCatalog.describe(MiscType.createGECM()).family());
    }

    @Test
    void unknownWeaponsRequireFallbackButLogicalArraysUseMembers() {
        WeaponType weapon = new WeaponType();
        var unknown = EquipmentModelCatalog.describe(weapon);
        assertEquals(EquipmentModelPolicy.WEAPON, unknown.policy());
        assertTrue(unknown.allowsFallback());
        assertEquals("unmapped-weapon", unknown.family());
        weapon.getFlags().set(WeaponType.F_MGA);
        var array = EquipmentModelCatalog.describe(weapon);
        assertEquals(EquipmentModelPolicy.MEMBERS, array.policy());
        assertFalse(array.allowsFallback());
    }

    @Test
    void attackPlaceholdersDoNotCreateHardwareButBombLaunchersRemainEligible() {
        WeaponType weapon = new WeaponType();
        weapon.getFlags().set(WeaponType.F_BOMB_WEAPON);
        assertEquals(EquipmentModelPolicy.WEAPON, EquipmentModelPolicy.forType(weapon));
        weapon.getFlags().set(WeaponTypeFlag.INTERNAL_REPRESENTATION);
        var placeholder = EquipmentModelCatalog.describe(weapon);
        assertEquals(EquipmentModelPolicy.NONE, placeholder.policy());
        assertFalse(placeholder.allowsFallback());
        assertEquals("none", placeholder.family());
    }
}
