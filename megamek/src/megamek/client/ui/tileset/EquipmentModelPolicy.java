/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.tileset;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.StructureType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.weapons.bayWeapons.BayWeapon;

/** Type gates shared by model tooling and the modular renderer, before looking up art or reserving sockets. */
public enum EquipmentModelPolicy {
    NONE,
    WEAPON,
    MEMBERS,
    PHYSICAL_WEAPON,
    OPTIONAL_MISC;

    public static EquipmentModelPolicy forType(EquipmentType type) {
        // Armor and structure inherit MiscType; their exclusion must precede misc flags and art mappings.
        if ((type instanceof StructureType) || (type instanceof ArmorType) || (type instanceof AmmoType)) {
            return NONE;
        }
        if (type instanceof WeaponType weapon) {
            if (weapon.hasFlag(WeaponTypeFlag.INTERNAL_REPRESENTATION)) {
                return NONE;
            }
            return ((weapon instanceof BayWeapon) || weapon.hasFlag(WeaponType.F_MGA)) ? MEMBERS : WEAPON;
        }
        if (type instanceof MiscType misc) {
            return misc.hasFlag(MiscType.F_PHYSICAL_WEAPON) ? PHYSICAL_WEAPON : OPTIONAL_MISC;
        }
        return NONE;
    }

    /** Optional misc needs an explicit visual mapping; grouping devices use their members' visual policies. */
    public boolean allowsFallback() {
        return (this == WEAPON) || (this == PHYSICAL_WEAPON);
    }
}
