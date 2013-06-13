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
public class CLLRT10 extends LRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 6160818215633786303L;

    /**
     * 
     */
    public CLLRT10() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_TW);
        this.name = "LRT 10";
        this.setInternalName("CLLRTorpedo10");
        this.addLookupName("Clan LRT-10");
        this.addLookupName("Clan LRT 10");
        this.setInternalName("CLLRT10");
        this.heat = 4;
        this.rackSize = 10;
        this.minimumRange = WEAPON_NA;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 2.5f;
        this.criticals = 1;
        this.bv = 109;
        this.cost = 100000;
        introDate = 2824;
        techLevel.put(2824, techLevel.get(3071));
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        techRating = RATING_C;
    }
}
