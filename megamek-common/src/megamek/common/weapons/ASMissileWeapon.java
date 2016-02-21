/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class ASMissileWeapon extends CapitalMissileWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 8263429182520693147L;

    public ASMissileWeapon() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "AS Missile";
        setInternalName(BombType.getBombWeaponName(BombType.B_AS));
        heat = 0;
        damage = 30;
        rackSize = 1;
        minimumRange = 9;
        shortRange = 17;
        mediumRange = 25;
        longRange = 32;
        extremeRange = 50;
        tonnage = 2;
        criticals = 0;
        hittable = false;
        bv = 0;
        cost = 15000;
        shortAV = 30;
        medAV = 30;
        longAV = 30;
        flags = flags.or(F_ANTI_SHIP);
        maxRange = RANGE_LONG;
        ammoType = AmmoType.T_AS_MISSILE;
        capital = false;
        introDate = 3071;
        techLevel.put(3071, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        techRating = RATING_D;
    }
}
