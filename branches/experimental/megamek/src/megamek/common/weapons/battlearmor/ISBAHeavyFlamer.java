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

public class ISBAHeavyFlamer extends BAFlamerWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2482256971276582340L;

    public ISBAHeavyFlamer() {
        super();
        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "Heavy Flamer";
        setInternalName("ISBAHeavyFlamer");
        addLookupName("IS BA Heavy Flamer");
        heat = 5;
        damage = 4;
        infDamageClass = WeaponType.WEAPON_BURST_6D6;
        shortRange = 2;
        mediumRange = 3;
        longRange = 4;
        extremeRange = 6;
        tonnage = .35f;
        criticals = 1;
        bv = 15;
        cost = 11250;
        techRating = RATING_C;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3070;
        techLevel.put(3070, techLevel.get(3071));
        techLevel.put(3073, TechConstants.T_IS_TW_NON_BOX);
        flags = flags.or(F_FLAMER).or(F_ENERGY).or(F_BA_WEAPON)
                .or(F_BURST_FIRE).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
    }
}
