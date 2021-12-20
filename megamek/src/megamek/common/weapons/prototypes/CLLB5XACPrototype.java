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
 * Created on Oct 15, 2004
 *
 */
package megamek.common.weapons.prototypes;

import megamek.common.SimpleTechLevel;

/**
 * @author Andrew Hunter
 */
public class CLLB5XACPrototype extends CLLBXACPrototypeWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8003492051812171922L;

    /**
     *
     */
    public CLLB5XACPrototype() {
        super();
        name = "Prototype LB 5-X Autocannon";
        setInternalName("CLLBXAC5Prototype");
        shortName = "LB 5-X (P)";
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 3;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 8.0;
        criticals = 6;
        bv = 83;
        cost = 250000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        maxRange = RANGE_LONG;
        rulesRefs = "97, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
        .setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
        .setClanAdvancement(2820, DATE_NONE, DATE_NONE, 2825, DATE_NONE)
        .setClanApproximate(true, false, false,true, false)
        .setPrototypeFactions(F_CCY)
        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
