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
public class CLLRM15OS extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5658731828818701699L;

    /**
     * 
     */
    public CLLRM15OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRM 15 (OS)";
        this.setInternalName("CLLRM15 (OS)");
        this.addLookupName("Clan OS LRM-15");
        this.addLookupName("Clan LRM 15 (OS)");
        this.heat = 5;
        this.rackSize = 15;
        this.minimumRange = WEAPON_NA;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 4.0f;
        this.criticals = 2;
        this.bv = 33;
        this.flags |= F_ONESHOT;
        this.cost = 175000;
        this.shortAV = 9;
        this.medAV = 9;
        this.longAV = 9;
        this.maxRange = RANGE_LONG;
    }
}
