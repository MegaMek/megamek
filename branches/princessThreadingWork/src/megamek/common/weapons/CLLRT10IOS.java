/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLLRT10IOS extends LRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 4402946418858772353L;

    /**
     *
     */
    public CLLRT10IOS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        name = "LRT 10 (I-OS)";
        setInternalName("CLLRTorpedo10 (IOS)");
        addLookupName("Clan IOS LRT-10");
        addLookupName("Clan LRT 10 (IOS)");
        addLookupName("CLLRT10IOS");
        heat = 4;
        rackSize = 10;
        minimumRange = WEAPON_NA;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 2.0f;
        criticals = 1;
        bv = 22;
        flags = flags.or(F_ONESHOT);
        cost = 80000;
        techRating = RATING_B;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3058;
        techLevel.put(3058, techLevel.get(3071));
        techLevel.put(3081, TechConstants.T_CLAN_TW);
    }
}
