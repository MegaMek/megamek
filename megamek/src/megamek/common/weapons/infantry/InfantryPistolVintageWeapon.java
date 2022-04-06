/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Ben Grills
 * @since Sep 7, 2005
 */
public class InfantryPistolVintageWeapon extends InfantryWeapon {
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryPistolVintageWeapon() {
        super();

        name = "Auto-Pistol (Vintage)";
        setInternalName(name);
        addLookupName("InfantryPistolVintage");
        addLookupName("Vintage Pistol");
        ammoType = AmmoType.T_INFANTRY;
        cost = 500;
        tonnage = .0005;
        bv = 0.0005;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.11;
        infantryRange = 1;
        ammoWeight = 0.00006;
        ammoCost = 12;
        shots = 9;
		rulesRefs = "195, AToW-C";
		techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
                .setTechRating(RATING_C)
		        .setAvailability(RATING_C, RATING_D, RATING_D, RATING_E);
    }
}
