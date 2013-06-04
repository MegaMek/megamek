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
package megamek.common.weapons;

import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 */
public class CLERFlamer extends FlamerWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 1414639280093120062L;

    /**
     *
     */
    public CLERFlamer() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        name = "ER Flamer";
        setInternalName("CLERFlamer");
        addLookupName("CL ER Flamer");
        heat = 4;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        shortRange = 3;
        mediumRange = 5;
        longRange = 7;
        extremeRange = 10;
        tonnage = 1f;
        criticals = 1;
        bv = 16;
        cost = 15000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        techRating = RATING_D;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3067;
        techLevel.put(3067, techLevel.get(3071));
        techLevel.put(3081, TechConstants.T_CLAN_TW);
    }
}
