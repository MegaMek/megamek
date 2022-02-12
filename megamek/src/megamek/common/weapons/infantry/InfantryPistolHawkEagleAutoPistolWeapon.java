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

/**
 * @author Ben Grills
 */
public class InfantryPistolHawkEagleAutoPistolWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryPistolHawkEagleAutoPistolWeapon() {
        super();

        name = "Auto-Pistol (Hawk Eagle)";
        setInternalName(name);
        addLookupName("InfantryHawkEagleAutopistol");
        addLookupName("Hawk Eagle Auto Pistol");
        ammoType = AmmoType.T_INFANTRY;
        cost = 100;
        bv = 0.25;
        tonnage = .0005;
        ammoWeight = 0.00011;
        ammoCost = 10;
        shots = 15;
        bursts = 5;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.27;
        infantryRange = 0;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3030, 3035, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_F, RATING_C, RATING_C);

    }
}
