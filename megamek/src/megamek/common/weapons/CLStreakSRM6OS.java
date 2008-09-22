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
public class CLStreakSRM6OS extends StreakSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -3098137789514566838L;

    /**
     * 
     */
    public CLStreakSRM6OS() {
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Streak SRM 6 (OS)";
        this.setInternalName("CLStreakSRM6 (OS)");
        this.addLookupName("Clan OS Streak SRM-6");
        this.addLookupName("Clan Streak SRM 6 (OS)");
        this.heat = 4;
        this.rackSize = 6;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.tonnage = 3.5f;
        this.criticals = 2;
        this.flags |= F_NO_FIRES | F_ONESHOT;
        this.bv = 24;
        this.cost = 120000;
        this.shortAV = 12;
        this.medAV = 12;
        this.maxRange = RANGE_MED;
    }
}
