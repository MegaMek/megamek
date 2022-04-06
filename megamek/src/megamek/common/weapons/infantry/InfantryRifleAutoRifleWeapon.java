/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;
import megamek.common.EquipmentTypeLookup;

/**
 * @author Ben Grills
 */
public class InfantryRifleAutoRifleWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleAutoRifleWeapon() {
        super();

        name = "Auto-Rifle (Modern, Generic)";
        setInternalName(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        addLookupName(name);
        addLookupName("Auto Rifle");
        addLookupName("Auto-Rifle");
        addLookupName("Infantry Automatic Rifle");
        addLookupName("InfantryAutoRifle");
        addLookupName("Infantry Auto Rifle");
        ammoType = AmmoType.T_INFANTRY;
        cost = 80;
        bv = 1.59;
        tonnage = .004;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.52;
        infantryRange = 1;
        ammoWeight = 0.00048;
        ammoCost = 2;
        shots = 30;
        bursts = 2;
        damage = 1;
        minimumRange = 0;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        rulesRefs = " 273, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A);

    }
}
