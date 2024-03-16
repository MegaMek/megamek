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
import megamek.common.TechAdvancement;

public class InfantryLaserPistolAWAWilibyMk4LaserPistol extends InfantryWeapon {

    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryLaserPistolAWAWilibyMk4LaserPistol() {
        super();

        name = "Laser Pistol (AWA Wiliby MK4 LASER PISTOL)";
        setInternalName(name);
        addLookupName("WILIBYMK4");
        ammoType = AmmoType.T_INFANTRY;
        cost = 1500;
        bv = 0.028;
        tonnage = 0.0018;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        infantryDamage = 0.09;
        infantryRange = 1;
        // ammoWeight is not applicable (NA)
        shots = 1;
        // ammoCost is not applicable (NA)
        bursts = 1;
        rulesRefs = "Shrapnel #9";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(TechAdvancement.DATE_NONE, TechAdvancement.DATE_NONE, TechAdvancement.DATE_ES, TechAdvancement.DATE_NONE, TechAdvancement.DATE_NONE);
        techAdvancement.setTechRating(TechAdvancement.RATING_C);
        techAdvancement.setAvailability(new int[]{TechAdvancement.RATING_C, TechAdvancement.RATING_C, TechAdvancement.RATING_C, TechAdvancement.RATING_C});
        techAdvancement.setISApproximate(false, false, true, false, false);
        techAdvancement.setProductionFactions(TechAdvancement.F_FS);
    }
}
