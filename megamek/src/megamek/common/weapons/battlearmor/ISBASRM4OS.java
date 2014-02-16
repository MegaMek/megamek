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
public class ISBASRM4OS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6674141690237272868L;

    /**
     *
     */
    public ISBASRM4OS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "SRM 4 (OS)";
        setInternalName("ISBASRM4OS");
        addLookupName("ISBASRM4 (OS)"); // mtf
        addLookupName("IS BA SRM 4 (OS)"); // tdb
        addLookupName("OS BA SRM-4"); // mep
        heat = 3;
        rackSize = 4;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 8;
        cost = 10000;
        introDate = 3050;
        techLevel.put(3050, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_B };
        techRating = RATING_E;
        tonnage = .150f;
        criticals = 3;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_ONESHOT).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
    }
    
}
