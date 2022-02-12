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
public class ISSRT2IOS extends SRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1737331597344761915L;

    /**
     *
     */
    public ISSRT2IOS() {
        super();
        name = "SRT 2 (I-OS)";
        setInternalName("ISSRT2IOS");
        addLookupName("ISSRT2 (IOS)"); // mtf
        addLookupName("IS SRT 2 (IOS)"); // tdb
        addLookupName("IOS SRT-2"); // mep
        heat = 2;
        rackSize = 2;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 0.5;
        criticals = 1;
        bv = 4;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        cost = 8000;
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
