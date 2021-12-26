package megamek.common.weapons.prototypes;

import megamek.common.SimpleTechLevel;

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
 * Created on Oct 1, 2004
 *
 */

/**
 * @author Andrew Hunter
 */
public class CLUAC2Prototype extends CLPrototypeUACWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -4226217996532332434L;

    /**
     *
     */
    public CLUAC2Prototype() {
        super();
        name = "Prototype Ultra Autocannon/2";
        setInternalName("CLUltraAC2Prototype");
        shortName = "Ultra AC/2 (P)";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 3;
        shortRange = 8;
        mediumRange = 17;
        longRange = 25;
        extremeRange = 34;
        tonnage = 7.0;
        criticals = 4;
        bv = 56;
        cost = 120000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        extAV = 3;
        maxRange = RANGE_EXT;
        explosionDamage = damage;
        flags = flags.or(F_PROTOTYPE);
        rulesRefs = "97, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
        .setClanAdvancement(2820, DATE_NONE, DATE_NONE, 2827, DATE_NONE)
        .setClanApproximate(true, false, false,true, false)
        .setPrototypeFactions(F_CSF)
        .setProductionFactions(F_CGS)
        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
