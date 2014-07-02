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
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.battlearmor;

import megamek.common.TechConstants;
import megamek.common.weapons.TAGWeapon;


/**
 * @author Sebastian Brocks
 */
public class CLBALightTAG extends TAGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6411290826952751265L;

    public CLBALightTAG() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "Light TAG";
        setInternalName("CLBALightTAG");
        addLookupName("Clan BA Light TAG");
        tonnage = 0.035f;
        criticals = 1;
        hittable = true;
        spreadable = false;
        heat = 0;
        damage = 0;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 0;
        cost = 40000;
        introDate = 3054;
        techLevel.put(3054, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        techRating = RATING_F;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
    }
}
