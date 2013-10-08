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

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Andrew Hunter
 */
public class CLHeavyGrenadeLauncher extends Weapon {

    /**
     *
     */
    private static final long serialVersionUID = 2728566278196446996L;

    public CLHeavyGrenadeLauncher() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Heavy Grenade Launcher";
        setInternalName(name);
        addLookupName("CLHeavyGL");
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        ammoType = AmmoType.T_NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.0f;
        criticals = 0;
        bv = 1;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_BURST_FIRE);
        introDate = 2900;
        techLevel.put(2900, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_D, RATING_C };
        techRating = RATING_C;
    }

}
