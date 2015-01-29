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
public class CLLRM11 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3663737019476438714L;

    /**
     *
     */
    public CLLRM11() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "LRM 11";
        setInternalName("CLLRM11");
        heat = 0;
        rackSize = 11;
        minimumRange = WEAPON_NA;
        tonnage = 2.2f;
        criticals = 0;
        bv = 139;
        introDate = 3060;
        techLevel.put(3060, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_F;
    }
}
