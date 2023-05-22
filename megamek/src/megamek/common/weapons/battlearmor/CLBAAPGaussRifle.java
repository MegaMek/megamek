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
package megamek.common.weapons.battlearmor;

import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Sebastian Brocks
 * @since Oct 19, 2004
 */
public class CLBAAPGaussRifle extends Weapon {
    private static final long serialVersionUID = 3055904827702262063L;

    public CLBAAPGaussRifle() {
        super();
        name = "Gauss Rifle [Anti-personnel Gauss Rifle]";
        shortName = "AP Gauss";
        setInternalName("CLBAAPGaussRifle");
        heat = 1;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 0.2;
        criticals = 2;
        bv = 21;
        cost = 10000;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        explosionDamage = 3;
        flags = flags.or(F_BA_WEAPON).or(F_BURST_FIRE).or(F_BALLISTIC).or(F_NO_FIRES).or(F_DIRECT_FIRE);
        rulesRefs = "255, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3066, 3069, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF);
    }
}
