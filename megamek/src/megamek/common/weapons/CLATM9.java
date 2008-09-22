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
public class CLATM9 extends ATMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -3779719958622540629L;

    /**
     * 
     */
    public CLATM9() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "ATM 9";
        this.setInternalName("CLATM9");
        this.addLookupName("Clan ATM-9");
        this.heat = 6;
        this.rackSize = 9;
        this.minimumRange = 4;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 5.0f;
        this.criticals = 4;
        this.bv = 147;
        this.cost = 225000;
        this.shortAV = 14;
        this.medAV = 14;
        this.maxRange = RANGE_MED;
    }
}
