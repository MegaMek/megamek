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
public class CLLRM6 extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -1358764761444243790L;

    /**
     * 
     */
    public CLLRM6() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRM 6";
        this.setInternalName("CLLRM6");
        this.heat = 0;
        this.rackSize = 6;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 1.2f;
        this.criticals = 0;
        this.bv = 69;
    }
}
