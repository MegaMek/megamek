/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class CLHAG30 extends HAGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 7189182993830405980L;

    public CLHAG30() {
        super();

        name = "HAG/30";
        setInternalName("CLHAG30");
        addLookupName("Clan HAG/30");
        heat = 6;
        rackSize = 30;
        minimumRange = 2;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 13.0f;
        criticals = 8;
        bv = 401;
        cost = 500000;
        shortAV = 24;
        medAV = 18;
        longAV = 18;
        maxRange = RANGE_LONG;
        explosionDamage = rackSize / 2;
        rulesRefs = "219, TM";

        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(3057, 3068, 3072);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_E, RATING_D });
    }

}
