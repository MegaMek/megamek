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
public class ISUAC2 extends UACWeapon {
    private static final long serialVersionUID = -6894947564166021652L;

    public ISUAC2() {
        super();
        name = "Ultra AC/2";
        setInternalName("ISUltraAC2");
        addLookupName("IS Ultra AC/2");
        sortingName = "Ultra AC/02";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 3;
        shortRange = 8;
        mediumRange = 17;
        longRange = 25;
        extremeRange = 34;
        tonnage = 7.0;
        criticals = 3;
        bv = 56;
        cost = 120000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        extAV = 3;
        maxRange = RANGE_EXT;
        explosionDamage = damage;
        rulesRefs = "208, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
    }
}
