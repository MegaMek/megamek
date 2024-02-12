/*
 * MegaMek - Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2018-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package megamek.common.weapons.infantry;

import megamek.common.AmmoType;


public class InfantryLaserPistolXingShanER extends InfantryWeapon {

    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryLaserPistolXingShanER() {
        super();

        name = "Laser Pistol (Xīng Shan ER)";
        setInternalName(name);
        addLookupName("XīngShanER");
        ammoType = AmmoType.T_INFANTRY;
        cost = 830;
        bv = 0.21; // The bv in the image seems incorrect, adjusted to the value from the text
        tonnage = 0.0016;
        infantryDamage = 0.35;
        infantryRange = 3;
        shots = 3;
        bursts = 1;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        rulesRefs = "Shrapnel #9";

        techAdvancement
                .setTechBase(TECH_BASE_IS)
                .setTechRating(RATING_E) // Assuming X-X-E-E simplifies to E
                .setAvailability(new int[]{RATING_X, RATING_X, RATING_E, RATING_E})
                .setISAdvancement(DATE_NONE, DATE_NONE, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_CC);
    }
}
