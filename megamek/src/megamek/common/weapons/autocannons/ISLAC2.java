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
public class ISLAC2 extends LACWeapon {
    private static final long serialVersionUID = 3128546525878614842L;

    public ISLAC2() {
        super();
        name = "Light AC/2";
        setInternalName("Light Auto Cannon/2");
        addLookupName("IS Light AutoCannon/2");
        addLookupName("ISLAC2");
        addLookupName("LAC/2");
        addLookupName("IS Light Autocannon/2");
        heat = 1;
        damage = 2;
        rackSize = 2;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 4.0;
        criticals = 1;
        bv = 30;
        cost = 100000;
        explosionDamage = damage;
        maxRange = RANGE_MED;
        shortAV = 2;
        medAV = 2;
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
