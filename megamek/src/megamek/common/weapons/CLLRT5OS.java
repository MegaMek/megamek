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
public class CLLRT5OS extends LRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3651580547253252279L;

    /**
     *
     */
    public CLLRT5OS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "LRT 5 (OS)";
        setInternalName("CLLRTorpedo5 (OS)");
        addLookupName("Clan OS LRT-5");
        addLookupName("Clan LRT 5 (OS)");
        addLookupName("CLLRT5OS");
        heat = 2;
        rackSize = 5;
        minimumRange = WEAPON_NA;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 1.5f;
        criticals = 1;
        bv = 11;
        flags = flags.or(F_ONESHOT);
        cost = 15000;
        introDate = 2676;
        techLevel.put(2676, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_C;
    }
}
