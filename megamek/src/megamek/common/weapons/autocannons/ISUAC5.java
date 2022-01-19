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
package megamek.common.weapons.autocannons;

/**
 * @author Andrew Hunter
 * @since Oct 1, 2004
 */
public class ISUAC5 extends UACWeapon {
    private static final long serialVersionUID = -6307637324918648850L;

    public ISUAC5() {
        super();
        name = "Ultra AC/5";
        setInternalName("ISUltraAC5");
        addLookupName("IS Ultra AC/5");
        sortingName = "Ultra AC/05";
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 13;
        longRange = 20;
        extremeRange = 26;
        tonnage = 9.0;
        criticals = 5;
        bv = 112;
        cost = 200000;
        shortAV = 7;
        medAV = 7;
        longAV = 7;
        maxRange = RANGE_LONG;
        rulesRefs = "208, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(2635, 2640, 3040, 2915, 3035)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FS);
    }
}
