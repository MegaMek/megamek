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
public class CLLRM5OS extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 767564661100835293L;

    /**
     * 
     */
    public CLLRM5OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRM 5 (OS)";
        this.setInternalName("CLLRM5 (OS)");
        this.addLookupName("CLLRM5OS");
        this.addLookupName("Clan OS LRM-5");
        this.addLookupName("Clan LRM 5 (OS)");
        this.heat = 2;
        this.rackSize = 5;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 1.5f;
        this.criticals = 1;
        this.bv = 11;
        this.flags |= F_ONESHOT;
        this.cost = 30000;
        this.shortAV = 3;
        this.medAV = 3;
        this.longAV = 3;
        this.maxRange = RANGE_LONG;
    }
}
