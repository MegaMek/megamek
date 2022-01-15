/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons.autocannons;

/**
 * @since Sep 25, 2004
 */
public class ISLAC5 extends LACWeapon {
    private static final long serialVersionUID = 6131945194809316957L;

    public ISLAC5() {
        super();
        name = "Light AC/5";
        setInternalName("Light Auto Cannon/5");
        addLookupName("IS Light Auto Cannon/5");
        addLookupName("LAC/5");
        addLookupName("ISLAC5");
        addLookupName("IS Light Autocannon/5");
        heat = 1;
        damage = 5;
        rackSize = 5;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 5.0;
        criticals = 2;
        bv = 62;
        cost = 150000;
        explosionDamage = damage;
        maxRange = RANGE_MED;
        shortAV = 5;
        medAV = 5;
        rulesRefs = "207, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_C)
                .setISAdvancement(3062, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
    }
}
