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
public class CLSRT4 extends SRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 7390930190902369569L;

    /**
     * 
     */
    public CLSRT4() {
        super();
        this.techLevel = TechConstants.T_CLAN_LEVEL_2;
        this.name = "SRT 4";
        this.setInternalName("CLSRT4");
        this.addLookupName("Clan SRT-4");
        this.addLookupName("Clan SRT 4");
        this.heat = 3;
        this.rackSize = 4;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 39;
        this.flags |= F_NO_FIRES;
        this.cost = 60000;
    }
}
