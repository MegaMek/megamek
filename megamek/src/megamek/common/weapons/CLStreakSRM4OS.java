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
public class CLStreakSRM4OS extends StreakSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -8016331798107342544L;

    /**
     * 
     */
    public CLStreakSRM4OS() {
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Streak SRM 4 (OS)";
        this.setInternalName("CLStreakSRM4 (OS)");
        this.addLookupName("Clan OS Streak SRM-4");
        this.addLookupName("Clan Streak SRM 4 (OS)");
        this.heat = 3;
        this.rackSize = 4;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.tonnage = 2.5f;
        this.criticals = 1;
        this.flags |= F_NO_FIRES | F_ONESHOT;
        this.bv = 16;
        this.cost = 90000;
        this.shortAV = 8;
        this.medAV = 8;
        this.maxRange = RANGE_MED;
    }
}
