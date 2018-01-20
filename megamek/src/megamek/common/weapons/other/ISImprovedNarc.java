/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.other;

import megamek.common.AmmoType;

/**
 * @author Sebastian Brocks
 */
public class ISImprovedNarc extends NarcWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6803482374426042321L;

    /**
     *
     */
    public ISImprovedNarc() {
        super();

        this.name = "iNarc";
        this.setInternalName("ISImprovedNarc");
        this.addLookupName("IS iNarc Beacon");
        this.addLookupName("IS iNarc Missile Beacon");
        this.ammoType = AmmoType.T_INARC;
        this.heat = 0;
        this.rackSize = 1;
        this.shortRange = 4;
        this.mediumRange = 9;
        this.longRange = 15;
        this.extremeRange = 18;
        this.tonnage = 5.0f;
        this.criticals = 3;
        this.bv = 75;
        this.cost = 250000;
        rulesRefs = "232,TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(3054, 3062, 3070, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false ,false, false)
            .setPrototypeFactions(F_CS)
            .setProductionFactions(F_CS/F_WB);
    }
}
