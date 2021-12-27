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
 * Created on Oct 2, 2004
 *
 */

/**
 * @author Andrew Hunter
 */
public class CLUAC20Prototype extends CLPrototypeUACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -8297688910484314546L;

    /**
     *
     */
    public CLUAC20Prototype() {
        super();
        name = "Prototype Ultra Autocannon/20";
        setInternalName("CLUltraAC20Prototype");
        shortName = "Ultra AC/20 (P)";
        heat = 8;
        damage = 20;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 7;
        longRange = 10;
        extremeRange = 14;
        tonnage = 15.0;
        criticals = 11;
        bv = 281;
        cost = 480000;
        shortAV = 30;
        medAV = 30;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        rulesRefs = "97, IO";
        flags = flags.or(F_PROTOTYPE).andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
        .setClanAdvancement(2820, DATE_NONE, DATE_NONE, 2825, DATE_NONE)
        .setClanApproximate(true, false, false,true, false)
        .setPrototypeFactions(F_CSF)
        .setProductionFactions(F_CSV)
        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
