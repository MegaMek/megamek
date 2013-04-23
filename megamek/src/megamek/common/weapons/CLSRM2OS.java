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

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLSRM2OS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3798165725512473586L;

    /**
     *
     */
    public CLSRM2OS() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "SRM 2 (OS)";
        setInternalName("CLSRM2 (OS)");
        addLookupName("CLSRM2OS");
        addLookupName("Clan OS SRM-2");
        addLookupName("Clan SRM 2 (OS)");
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 1;
        criticals = 1;
        bv = 4;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        cost = 5000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        this.introDate = 2676;
        availRating = new int[]{RATING_C,RATING_C,RATING_C};
        techRating = RATING_F;
    }
}
