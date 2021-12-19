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
public class CLUAC10Prototype extends CLPrototypeUACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 6937673199956551674L;

    /**
     *
     */
    public CLUAC10Prototype() {
        super();
        name = "Prototype Ultra Autocannon/10";
        setInternalName("CLUltraAC10Prototype");
        shortName = "Ultra AC/10 (P)";
        heat = 4;
        damage = 10;
        rackSize = 10;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 13.0;
        criticals = 8;
        bv = 210;
        cost = 320000;
        shortAV = 15;
        medAV = 15;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        rulesRefs = "97, IO";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
        .setClanAdvancement(2820, DATE_NONE, DATE_NONE, 2825, DATE_NONE)
        .setClanApproximate(true, false, false,true, false)
        .setPrototypeFactions(F_CSF)
        .setProductionFactions(F_CSF)
        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
