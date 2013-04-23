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
public class CLLRT2 extends LRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8660981556295580874L;

    /**
     *
     */
    public CLLRT2() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "LRT 2";
        setInternalName("CLLRTorpedo2");
        setInternalName("CLLRT2");
        heat = 0;
        rackSize = 2;
        minimumRange = WEAPON_NA;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 0.4f;
        criticals = 0;
        bv = 24;
        introDate = 3060;
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
        techRating = RATING_C;
    }
}
