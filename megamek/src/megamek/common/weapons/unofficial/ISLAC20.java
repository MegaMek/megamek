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
package megamek.common.weapons.unofficial;

import megamek.common.weapons.autocannons.LACWeapon;

/**
 * @since Sep 25, 2004
 */
public class ISLAC20 extends LACWeapon {
    private static final long serialVersionUID = 7135078308771443835L;

    public ISLAC20() {
        super();
        name = "LAC/20";
        setInternalName("Light Auto Cannon/20");
        addLookupName("IS Light AutoCannon/20");
        addLookupName("ISLAC20");
        addLookupName("IS Light Autocannon/20");
        heat = 7;
        damage = 20;
        rackSize = 20;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        tonnage = 9.0;
        criticals = 6;
        bv = 118;
        cost = 325000;
        explosionDamage = damage;
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_C)
                .setISAdvancement(3062, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
    }
}
