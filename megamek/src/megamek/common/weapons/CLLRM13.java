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
public class CLLRM13 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5052163720015100850L;

    /**
     *
     */
    public CLLRM13() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "LRM 13";
        setInternalName("CLLRM13");
        heat = 0;
        rackSize = 13;
        minimumRange = WEAPON_NA;
        tonnage = 2.6f;
        criticals = 0;
        bv = 161;
        introDate = 3060;
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
        techRating = RATING_F;
    }
}
