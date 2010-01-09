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
public class CLLRT20IOS extends LRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 4540170151130434608L;

    /**
     *
     */
    public CLLRT20IOS() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "LRT 20 (IOS)";
        setInternalName("CLLRTorpedo20 (IOS)");
        addLookupName("Clan IOS LRT-20");
        addLookupName("Clan LRT 20 (IOS)");
        heat = 6;
        rackSize = 20;
        minimumRange = WEAPON_NA;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 5.0f;
        criticals = 4;
        bv = 44;
        flags = flags.or(F_ONESHOT);
        cost = 250000;
    }
}
