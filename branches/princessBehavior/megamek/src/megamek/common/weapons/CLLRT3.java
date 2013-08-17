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
public class CLLRT3 extends LRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -1924771696583001603L;

    /**
     * 
     */
    public CLLRT3() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_TW);
        this.name = "LRT 3";
        this.setInternalName("CLLRTorpedo3");
        this.setInternalName("CLLRT3");
        this.heat = 0;
        this.rackSize = 3;
        this.minimumRange = WEAPON_NA;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 0.6f;
        this.criticals = 0;
        this.bv = 35;
        introDate = 3060;
        techLevel.put(3060, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_C;
    }
}
