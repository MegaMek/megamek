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
package megamek.common.weapons.prototypes;

import megamek.common.SimpleTechLevel;
import megamek.common.weapons.tag.TAGWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISPrototypeTAG extends TAGWeapon {

    /**
     *
     */
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
        rulesRefs = "73,IO";
        flags = flags.or(F_PROTOTYPE);
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
            .setISAdvancement(2593, DATE_NONE, DATE_NONE, 2600, DATE_NONE)
            .setISApproximate(false, false, false,true, false)
            .setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
