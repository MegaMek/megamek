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
public class CLLRM15 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 6075797537673614837L;

    /**
     *
     */
    public CLLRM15() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "LRM 15";
        setInternalName("CLLRM15");
        addLookupName("Clan LRM-15");
        addLookupName("Clan LRM 15");
        heat = 5;
        rackSize = 15;
        minimumRange = WEAPON_NA;
        tonnage = 3.5f;
        criticals = 2;
        bv = 164;
        cost = 175000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        maxRange = RANGE_LONG;
        introDate = 2824;
        availRating = new int[]{RATING_C,RATING_C,RATING_C};
        techRating = RATING_F;
    }
}
