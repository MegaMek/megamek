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
public class CLATM12 extends ATMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -7902048944230263372L;

    /**
     * 
     */
    public CLATM12() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "ATM 12";
        this.setInternalName("CLATM12");
        this.addLookupName("Clan ATM-12");
        this.heat = 8;
        this.rackSize = 12;
        this.minimumRange = 4;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 7.0f;
        this.criticals = 5;
        this.bv = 212;
        this.cost = 350000;
        this.shortAV = 20;
        this.medAV = 20;
        this.maxRange = RANGE_MED;
    }
}
