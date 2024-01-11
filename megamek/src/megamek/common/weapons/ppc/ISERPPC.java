/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.ppc;

/**
 * @author Andrew Hunter
 * @since Sep 13, 2004
 */
public class ISERPPC extends PPCWeapon {
    private static final long serialVersionUID = 7175778897598535734L;

    public ISERPPC() {
        super();
        name = "ER PPC";
        setInternalName("ISERPPC");
        addLookupName("IS ER PPC");
        sortingName = "PPC ER";
        heat = 15;
        damage = 10;
        shortRange = 7;
        mediumRange = 14;
        longRange = 23;
        extremeRange = 28;
        waterShortRange = 4;
        waterMediumRange = 10;
        waterLongRange = 16;
        waterExtremeRange = 20;
        tonnage = 7.0;
        criticals = 3;
        bv = 229;
        cost = 300000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_LONG;
        // with a capacitor
        explosive = true;
        rulesRefs = "234, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(2740, 2751, 3042, 2860, 3037)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2740, 2751, DATE_NONE, 2860, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_DC);
    }
}
