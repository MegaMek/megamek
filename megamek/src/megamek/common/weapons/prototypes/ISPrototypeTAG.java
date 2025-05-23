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
package megamek.common.weapons.prototypes;

import megamek.common.SimpleTechLevel;
import megamek.common.weapons.tag.TAGWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 7, 2005
 */
public class ISPrototypeTAG extends TAGWeapon {
    private static final long serialVersionUID = -2474477168563228542L;

    public ISPrototypeTAG() {
        super();
        name = "Prototype TAG";
        setInternalName("ISProtoTypeTAG");
        addLookupName("IS Prototype TAG");
        shortName = "TAG (P)";
        tonnage = 1.5;
        criticals = 1;
        hittable = true;
        spreadable = false;
        heat = 0;
        damage = 0;
        shortRange = 5;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        bv = 0;
        cost = 150000;
        rulesRefs = "73, IO";
        flags = flags.or(F_PROTOTYPE);
        techAdvancement.setTechBase(TechBase.IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
                .setISAdvancement(2593, DATE_NONE, DATE_NONE, 2600, DATE_NONE)
                .setISApproximate(false, false, false, true, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
