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
public class CLLRT15OS extends LRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 2935323332234777496L;

    /**
     *
     */
    public CLLRT15OS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "LRT 15 (OS)";
        setInternalName("CLLRTorpedo15 (OS)");
        addLookupName("Clan OS LRT-15");
        addLookupName("Clan LRT 15 (OS)");
        addLookupName("CLLRT15OS");
        heat = 5;
        rackSize = 15;
        minimumRange = WEAPON_NA;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 4.0f;
        criticals = 2;
        bv = 33;
        flags = flags.or(F_ONESHOT);
        cost = 87500;
        introDate = 2824;
        techLevel.put(2824, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_C, RATING_B };
        techRating = RATING_C;

    }
}
