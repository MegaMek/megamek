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
public class CLLRM18 extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 4616668322148799167L;

    /**
     * 
     */
    public CLLRM18() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRM 18";
        this.setInternalName("CLLRM18");
        this.heat = 0;
        this.rackSize = 18;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 3.6f;
        this.criticals = 0;
        this.bv = 217;
    }
}
