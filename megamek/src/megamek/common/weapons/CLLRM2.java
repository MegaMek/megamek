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
public class CLLRM2 extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -5274335014393603612L;

    /**
     * 
     */
    public CLLRM2() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRM 2";
        this.setInternalName("CLLRM2");
        this.heat = 0;
        this.rackSize = 2;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 0.4f;
        this.criticals = 0;
        this.bv = 25;
    }
}
