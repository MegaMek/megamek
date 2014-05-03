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
public class CLBASRM6OS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5184043200202465163L;

    /**
     *
     */
    public CLBASRM6OS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "SRM 6 (OS)";
        setInternalName("CLBASRM6 (OS)");
        addLookupName("Clan BA OS SRM-6");
        addLookupName("Clan BA SRM 6 (OS)");
        addLookupName("CLBASRM6OS");
        heat = 4;
        rackSize = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 12;
        tonnage = .120f;
        criticals = 4;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_ONESHOT).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        cost = 15000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        introDate = 2868;
        techLevel.put(2868, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_D, RATING_C };
        techRating = RATING_F;
    }
}
