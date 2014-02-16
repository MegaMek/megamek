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
public class ISBASRM2OS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6918950640293828718L;

    /**
     *
     */
    public ISBASRM2OS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "SRM 2 (OS)";
        setInternalName("ISBASRM2OS");
        addLookupName("ISBASRM2 (OS)"); // mtf
        addLookupName("IS BA SRM 2 (OS)"); // tdb
        addLookupName("OS BA SRM-2"); // mep
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 4;
        cost = 5000;
        introDate = 3050;
        techLevel.put(3050, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_B };
        techRating = RATING_E;
        tonnage = .075f;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_ONESHOT).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
    }
}
