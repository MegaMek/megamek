/*
 * MegaMek - Copyright (C) 2013 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.other;

import megamek.common.SimpleTechLevel;

public class ISTSEMPCannon extends TSEMPWeapon {

    private static final long serialVersionUID = -4861067053206502295L;

    public ISTSEMPCannon() {
        cost = 800000;
        bv = 488;
        name = "TSEMP Cannon";
        setInternalName(name);
        addLookupName("ISTSEMP");
        tonnage = 6;
        criticals  = 5;
		rulesRefs = "84, IO:AE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
		techAdvancement.setTechBase(TechBase.IS)
                .setIntroLevel(false).setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E)
                .setISAdvancement(3085, 3109, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.RS)
                .setProductionFactions(Faction.RS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}
