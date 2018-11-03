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
package megamek.common.weapons.missiles;

/**
 * @author Sebastian Brocks
 */
public class CLATM6 extends ATMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 2196553902764762463L;

    /**
     *
     */
    public CLATM6() {
        super();
        name = "ATM 6";
        setInternalName("CLATM6");
        addLookupName("Clan ATM-6");
        heat = 4;
        rackSize = 6;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 3.5;
        criticals = 3;
        bv = 105;
        cost = 125000;
        shortAV = 8;
        medAV = 8;
        maxRange = RANGE_MED;
        rulesRefs = "229,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
            .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
            .setClanApproximate(true, true, true, false, false)
            .setPrototypeFactions(F_CCY)
            .setProductionFactions(F_CCY);
    }
}
