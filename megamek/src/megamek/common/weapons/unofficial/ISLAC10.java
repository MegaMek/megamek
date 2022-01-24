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

/**
 * @since Sep 25, 2004
 */
import megamek.common.weapons.autocannons.LACWeapon;

public class ISLAC10 extends LACWeapon {
    private static final long serialVersionUID = 7715730019995031625L;

    public ISLAC10() {
        super();
        name = "LAC/10";
        setInternalName("Light Auto Cannon/10");
        addLookupName("IS Light AutoCannon/10");
        addLookupName("ISLAC10");
        addLookupName("IS Light Autocannon/10");
        heat = 3;
        damage = 10;
        rackSize = 10;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 8.0;
        criticals = 4;
        bv = 74;
        cost = 225000;
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
