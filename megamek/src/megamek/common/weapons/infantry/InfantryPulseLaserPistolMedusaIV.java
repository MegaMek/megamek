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

public class InfantryPulseLaserPistolMedusaIV extends InfantryWeapon {

    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryPulseLaserPistolMedusaIV() {
        super();

        name = "Pulse Laser Pistol (Medusa IV)";
        setInternalName(name);
        addLookupName("MEDUSAIV");
        ammoType = AmmoType.T_INFANTRY;
        cost = 1500;
        bv = 0.063;
        tonnage = 0.0001;
        infantryDamage = 0.21;
        infantryRange = 1;
        shots = 3;
        bursts = 1;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        rulesRefs = "Shrapnel #9";

        techAdvancement
                .setTechBase(TechAdvancement.TECH_BASE_IS)
                .setTechRating(TechAdvancement.RATING_E) // Assuming X-E-E-D simplifies to E
                .setAvailability(new int[]{TechAdvancement.RATING_X, TechAdvancement.RATING_E, TechAdvancement.RATING_E, TechAdvancement.RATING_D})
                .setISAdvancement(TechAdvancement.DATE_NONE, TechAdvancement.DATE_NONE, 2800, TechAdvancement.DATE_NONE, TechAdvancement.DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(TechAdvancement.F_MC);
    }
}
