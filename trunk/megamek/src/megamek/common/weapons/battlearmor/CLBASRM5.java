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
import megamek.common.weapons.SRMWeapon;


/**
 * @author Sebastian Brocks
 */
public class CLBASRM5 extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 9051765359928076836L;

    /**
     *
     */
    public CLBASRM5() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "SRM 5";
        setInternalName("CLBASRM5");
        addLookupName("Clan BA SRM-5");
        addLookupName("Clan BA SRM 5");
        rackSize = 5;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 47;
        tonnage = .175f;
        criticals = 3;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        cost = 25000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        introDate = 2868;
        techLevel.put(2868, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_D };
        techRating = RATING_F;
    }
}
