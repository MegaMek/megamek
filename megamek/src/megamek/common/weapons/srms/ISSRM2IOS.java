/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.srms;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class ISSRM2IOS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6918950640293828718L;

    /**
     *
     */
    public ISSRM2IOS() {
        super();
        name = "SRM 2 (I-OS)";
        setInternalName("ISSRM2IOS");
        addLookupName("ISSRM2 (IOS)"); // mtf
        addLookupName("IS SRM 2 (IOS)"); // tdb
        addLookupName("IOS SRM-2"); // mep
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 0.5;
        criticals = 1;
        bv = 4;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        cost = 8000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_B)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(DATE_NONE, 3056, 3081, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, true,false, false)
            .setPrototypeFactions(F_DC)
            .setProductionFactions(F_DC)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
