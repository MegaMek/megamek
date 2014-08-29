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
public class CLNarcOS extends NarcWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5553288957570246232L;

    /**
     *
     */
    public CLNarcOS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "Narc (OS)";
        setInternalName("CLNarcBeacon (OS)");
        addLookupName("Clan OS Narc Beacon");
        addLookupName("Clan Narc Missile Beacon (OS)");
        heat = 0;
        rackSize = 1;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 2.5f;
        criticals = 1;
        flags = flags.or(F_ONESHOT);
        bv = 6;
        cost = 100000;
        introDate = 2828;
        techLevel.put(2828, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_E, RATING_D };
        techRating = RATING_F;
    }
}
