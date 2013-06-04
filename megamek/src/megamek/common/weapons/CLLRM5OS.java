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
public class CLLRM5OS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 767564661100835293L;

    /**
     *
     */
    public CLLRM5OS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "LRM 5 (OS)";
        setInternalName("CLLRM5 (OS)");
        addLookupName("CLLRM5OS");
        addLookupName("Clan OS LRM-5");
        addLookupName("Clan LRM 5 (OS)");
        heat = 2;
        rackSize = 5;
        minimumRange = WEAPON_NA;
        tonnage = 1.5f;
        criticals = 1;
        bv = 11;
        flags = flags.or(F_ONESHOT);
        cost = 15000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        maxRange = RANGE_LONG;
        introDate = 2676;
        techLevel.put(2676, techLevel.get(3071));
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        techRating = RATING_F;

    }
}
