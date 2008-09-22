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
public class CLLRM20OS extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 1725628953852049901L;

    /**
     * 
     */
    public CLLRM20OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRM 20 (OS)";
        this.setInternalName("CLLRM20 (OS)");
        this.addLookupName("Clan OS LRM-20");
        this.addLookupName("Clan LRM 20 (OS)");
        this.heat = 6;
        this.rackSize = 20;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 5.5f;
        this.criticals = 4;
        this.bv = 44;
        this.flags |= F_ONESHOT;
        this.cost = 250000;
        this.shortAV = 12;
        this.medAV = 12;
        this.longAV = 12;
        this.maxRange = RANGE_LONG;
    }
}
