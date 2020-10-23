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
import megamek.common.TechAdvancement;

/**
 * @author Ben Grills
 */
public class InfantryPistolAutoPistolNissanWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryPistolAutoPistolNissanWeapon() {
        super();

        name = "Auto-Pistol (Nissan SG-12)";
        setInternalName(name);
        addLookupName("InfantryAutopistolNissan");
        addLookupName("Auto Pistol Nissan");
        ammoType = AmmoType.T_INFANTRY;
        cost = 250;
        bv = 0.19;
        tonnage = .0027;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.17;
        infantryRange = 1;
        ammoWeight = 0.00016;
        ammoCost = 3;
        shots = 8;
        rulesRefs = "176,HBHK";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2600, 2620, 2630);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_B, RATING_B, RATING_B, RATING_B });
    }
}
