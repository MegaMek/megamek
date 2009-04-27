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
public class CLSRM6OS extends SRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5184043200202465163L;

    /**
     * 
     */
    public CLSRM6OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "SRM 6 (OS)";
        this.setInternalName("CLSRM6 (OS)");
        this.addLookupName("Clan OS SRM-6");
        this.addLookupName("Clan SRM 6 (OS)");
        this.heat = 4;
        this.rackSize = 6;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 12;
        this.flags |= F_NO_FIRES | F_ONESHOT;
        this.cost = 80000;
        this.shortAV = 8;
        this.maxRange = RANGE_SHORT;
    }
}
