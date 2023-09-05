/*
 * MegaMek - Copyright (C) 2020 - The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import megamek.common.weapons.infantry.InfantryWeapon;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ammo for light and medium weapons used by small support vehicles. These are the same as
 * infantry weapons, but infantry do not track ammo use.
 */
public class SmallWeaponAmmoType extends AmmoType {

    private final int bursts;

    public SmallWeaponAmmoType(InfantryWeapon weapon) {
        ammoType = T_INFANTRY;
        setInternalName(generateInternalName(weapon));
        name = weapon.name + " Ammo";
        if (weapon.getInternalName().endsWith("Inferno")) {
            munitionType = EnumSet.of(AmmoType.Munitions.M_INFERNO);
            name += " (Inferno)";
        } else {
            munitionType = EnumSet.of(AmmoType.Munitions.M_STANDARD);
        }
        tonnage = weapon.getAmmoWeight();
        cost = weapon.getAmmoCost();
        shots = weapon.getShots();
        bursts = weapon.getBursts();
        techAdvancement = new TechAdvancement(weapon.getTechAdvancement());
        rulesRefs = weapon.rulesRefs;
    }

    private String generateInternalName(EquipmentType weapon) {
        return weapon.getInternalName().replace("Inferno", "") + " Ammo";
    }

    public int getBursts() {
        return bursts;
    }

    /**
     * @param weapon The weapon to check
     * @return       Whether this is the ammo type for the weapon
     */
    public boolean isAmmoFor(EquipmentType weapon) {
        return getInternalName().equals(generateInternalName(weapon));
    }

    /**
     * Creates a new ammo for each infantry weapon that uses it. This must be called
     * after WeaponType is initialized.
     */
    public static void initializeTypes() {
        List<InfantryWeapon> weapons = allTypes.stream()
                .filter(et -> (et instanceof InfantryWeapon)
                    && (((InfantryWeapon) et).getAmmoType() == AmmoType.T_INFANTRY))
                .map(et -> (InfantryWeapon) et).collect(Collectors.toList());
        for (InfantryWeapon weapon : weapons) {
            addType(new SmallWeaponAmmoType(weapon));
        }
    }
}
