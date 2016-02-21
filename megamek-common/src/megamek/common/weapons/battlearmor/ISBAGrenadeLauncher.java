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
public class ISBAGrenadeLauncher extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 5856065014622975919L;

    /**
     *
     */
    public ISBAGrenadeLauncher() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Grenade Launcher";
        setInternalName("ISBAGrenadeLauncher");
        addLookupName("IS BA Grenade Launcher");
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_HALFD6;
        minimumRange = WEAPON_NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.1f;
        criticals = 1;
        bv = 1;
        cost = 3000;
        flags = flags.or(F_BALLISTIC).or(F_BA_WEAPON).or(F_BURST_FIRE).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 3050;
        techLevel.put(3050, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_D };
        techRating = RATING_C;
    }
}
