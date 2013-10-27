/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLSniper extends ArtilleryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -599648142688689572L;

    /**
     *
     */
    public CLSniper() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        name = "Sniper";
        setInternalName("CLSniper");
        addLookupName("CLSniperArtillery");
        addLookupName("Clan Sniper");
        flags = flags.or(F_AERO_WEAPON);
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.T_SNIPER;
        shortRange = 1; //
        mediumRange = 2;
        longRange = 18;
        extremeRange = 18; // No extreme range.
        tonnage = 20f;
        criticals = 20;
        bv = 85;
        cost = 300000;
        techRating = RATING_B;
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        introDate = 2800;
    }

}
