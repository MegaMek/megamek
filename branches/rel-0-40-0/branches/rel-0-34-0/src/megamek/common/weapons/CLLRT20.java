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
public class CLLRT20 extends LRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 6906914701393598726L;

    /**
     * 
     */
    public CLLRT20() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRT 20";
        this.setInternalName("CLLRTorpedo20");
        this.addLookupName("Clan LRT-20");
        this.addLookupName("Clan LRT 20");
        this.heat = 6;
        this.rackSize = 20;
        this.minimumRange = WEAPON_NA;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 5.0f;
        this.criticals = 4;
        this.bv = 220;
        this.cost = 250000;
    }
}
