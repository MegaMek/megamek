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
public class CLSRM6 extends SRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -5174394587928057034L;

    /**
     * 
     */
    public CLSRM6() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "SRM 6";
        this.setInternalName("CLSRM6");
        this.addLookupName("Clan SRM-6");
        this.addLookupName("Clan SRM 6");
        this.heat = 4;
        this.rackSize = 6;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 1.5f;
        this.criticals = 1;
        this.bv = 59;
        this.flags |= F_NO_FIRES;
        this.cost = 80000;
        this.shortAV = 8;
        this.maxRange = RANGE_SHORT;
    }
}
