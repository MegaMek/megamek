/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * Ammo for light and medium weapons used by small support vehicles. These are the same as infantry weapons, but
 * infantry do not track ammo use.
 */
public class SmallWeaponAmmoType extends AmmoType {

    private final int bursts;

    public SmallWeaponAmmoType(InfantryWeapon weapon) {
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
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
     *
     * @return Whether this is the ammo type for the weapon
     */
    public boolean isAmmoFor(EquipmentType weapon) {
        return getInternalName().equals(generateInternalName(weapon));
    }

    /**
     * Creates a new ammo for each infantry weapon that uses it. This must be called after WeaponType is initialized.
     */
    public static void initializeTypes() {
        List<InfantryWeapon> weapons = allTypes.stream()
              .filter(et -> (et instanceof InfantryWeapon)
                    && (((InfantryWeapon) et).getAmmoType() == AmmoType.AmmoTypeEnum.INFANTRY))
              .map(et -> (InfantryWeapon) et).collect(Collectors.toList());
        for (InfantryWeapon weapon : weapons) {
            addType(new SmallWeaponAmmoType(weapon));
        }
    }
}
