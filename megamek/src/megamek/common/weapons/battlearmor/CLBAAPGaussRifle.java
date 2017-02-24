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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons.battlearmor;

import megamek.common.TechConstants;
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Sebastian Brocks
 */
public class CLBAAPGaussRifle extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 3055904827702262063L;

    /**
     *
     */
    public CLBAAPGaussRifle() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "Gauss Rifle [Anti-personnel Gauss Rifle]";
        setInternalName("CLBAAPGaussRifle");
        heat = 1;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 0.2f;
        criticals = 1;
        bv = 21;
        cost = 10000;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        explosionDamage = 3;
        flags = flags.or(F_BA_WEAPON).or(F_BURST_FIRE).or(F_BALLISTIC).or(F_NO_FIRES).or(F_DIRECT_FIRE);
        introDate = 3061;
        techLevel.put(3061, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(3069, TechConstants.T_CLAN_ADVANCED);
        techLevel.put(3072, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_E ,RATING_D};
        techRating = RATING_F;
        rulesRefs = "255, TM";

        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(3061, 3069, 3071);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_E, RATING_D });
    }

}
