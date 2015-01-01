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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons.battlearmor;

import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Sebastian Brocks
 */
public class ISMicroGrenadeLauncher extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 5856065014622975919L;

    /**
     *
     */
    public ISMicroGrenadeLauncher() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Micro Grenade Launcher";
        setInternalName(name);
        addLookupName("ISMicroGrenadeLauncher");
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_HALFD6;
        minimumRange = WEAPON_NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 2;
        extremeRange = 4;
        tonnage = 0.075f;
        criticals = 1;
        bv = 1;
        cost = 1950;
        flags = flags.or(F_BALLISTIC).or(F_BA_WEAPON).or(F_BURST_FIRE);
        introDate = 2100;
        techLevel.put(2100, techLevel.get(3071));
        availRating = new int[] { RATING_B, RATING_B, RATING_B };
        techRating = RATING_B;
    }
}
