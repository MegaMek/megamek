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


public class InfantryPulseLaserPistolRDISunSwarmPulsar extends InfantryWeapon {

    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryPulseLaserPistolRDISunSwarmPulsar() {
        super();

        name = "Pulse Laser Pistol (RDI Sunswarm Pulsar)";
        setInternalName(name);
        addLookupName("SUNSWARM");
        ammoType = AmmoType.T_INFANTRY;
        cost = 1050;
        bv = 0.054;
        tonnage = 0.0001;
        infantryDamage = 0.18;
        infantryRange = 1;
        shots = 2;
        bursts = 4;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        rulesRefs = "Shrapnel #9";

        techAdvancement
                .setTechBase(TECH_BASE_IS)
                .setTechRating(RATING_C) // Assuming X-X-X-C simplifies to C
                .setAvailability(new int[]{RATING_X, RATING_X, RATING_X, RATING_C})
                .setISAdvancement(DATE_NONE, DATE_NONE, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_FW);
    }
}
