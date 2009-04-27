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
public class CLATM3 extends ATMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 107949833660086492L;

    /**
     * 
     */
    public CLATM3() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "ATM 3";
        this.setInternalName("CLATM3");
        this.addLookupName("Clan ATM-3");
        this.heat = 2;
        this.rackSize = 3;
        this.minimumRange = 4;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 1.5f;
        this.criticals = 2;
        this.bv = 53;
        this.cost = 50000;
        this.shortAV = 4;
        this.medAV = 4;
        this.maxRange = RANGE_MED;
    }
}
