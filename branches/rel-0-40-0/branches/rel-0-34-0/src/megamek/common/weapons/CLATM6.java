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
public class CLATM6 extends ATMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 2196553902764762463L;

    /**
     * 
     */
    public CLATM6() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "ATM 6";
        this.setInternalName("CLATM6");
        this.addLookupName("Clan ATM-6");
        this.heat = 4;
        this.rackSize = 6;
        this.minimumRange = 4;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 3.5f;
        this.criticals = 3;
        this.bv = 105;
        this.cost = 125000;
        this.shortAV = 8;
        this.medAV = 8;
        this.maxRange = RANGE_MED;
    }
}
