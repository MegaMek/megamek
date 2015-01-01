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
package megamek.common.weapons.battlearmor;

import megamek.common.TechConstants;
import megamek.common.weapons.LRMWeapon;


/**
 * @author Sebastian Brocks
 */
public class CLBALRM1 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5262579090950972046L;

    /**
     *
     */
    public CLBALRM1() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "LRM 1";
        setInternalName("CLBALRM1");
        heat = 0;
        rackSize = 1;
        minimumRange = WEAPON_NA;
        tonnage = 0.035f;
        criticals = 2;
        bv = 17;
        cost = 6000;
        introDate = 3060;
        techLevel.put(3060, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_F;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);

    }
}
