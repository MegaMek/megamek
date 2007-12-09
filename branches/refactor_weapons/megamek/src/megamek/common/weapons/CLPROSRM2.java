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
 *
 */
public class CLPROSRM2 extends SRMWeapon {

    /**
     * 
     */
    public CLPROSRM2() {
        super(); 
        this.techLevel = TechConstants.T_CLAN_LEVEL_2;
        this.name = "SRM 2";
        this.setInternalName("CLPROSRM2");
        this.heat = 0;
        this.rackSize = 2;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 0.5f;
        this.criticals = 0;
        this.bv = 21;
        this.flags |= F_PROTOMECH;
    }
}
