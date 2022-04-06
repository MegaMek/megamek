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
public class CLHAG40 extends HAGWeapon {
    private static final long serialVersionUID = -8369909187223849480L;

    public CLHAG40() {
        super();

        name = "HAG/40";
        setInternalName("CLHAG40");
        addLookupName("Clan HAG/40");
        heat = 8;
        rackSize = 40;
        minimumRange = 2;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 16.0;
        criticals = 10;
        bv = 535;
        cost = 600000;
        shortAV = 32;
        medAV = 24;
        longAV = 24;
        maxRange = RANGE_LONG;
        explosionDamage = rackSize / 2;
        rulesRefs = "219, TM";
        flags = flags.andNot(F_PROTO_WEAPON);
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
