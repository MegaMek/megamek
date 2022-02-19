/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.gaussrifles;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 * @since Oct 19, 2004
 */
public class CLHAG20 extends HAGWeapon {
    private static final long serialVersionUID = -1150472287591805766L;

    public CLHAG20() {
        super();

        name = "HAG/20";
        setInternalName("CLHAG20");
        addLookupName("Clan HAG/20");
        heat = 4;
        rackSize = 20;
        minimumRange = 2;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 10.0;
        criticals = 6;
        bv = 267;
        cost = 400000;
        shortAV = 16;
        medAV = 12;
        longAV = 12;
        maxRange = RANGE_LONG;
        explosionDamage = rackSize / 2;
        rulesRefs = "219, TM";
        //Jan 22 - Errata issued by CGL (Greekfire) for HAGs        
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setClanAdvancement(DATE_NONE, 3062, 3068, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
