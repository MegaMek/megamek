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
public class CLLRM13 extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -5052163720015100850L;

    /**
     * 
     */
    public CLLRM13() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRM 13";
        this.setInternalName("CLLRM13");
        this.heat = 0;
        this.rackSize = 13;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 2.6f;
        this.criticals = 0;
        this.bv = 161;
    }
}
