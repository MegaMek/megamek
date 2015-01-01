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
public class CLStreakSRM2 extends StreakSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -7095997934846595420L;

    /**
     * 
     */
    public CLStreakSRM2() {
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Streak SRM 2";
        this.setInternalName("CLStreakSRM2");
        this.addLookupName("Clan Streak SRM-2");
        this.addLookupName("Clan Streak SRM 2");
        this.heat = 2;
        this.rackSize = 2;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.flags |= F_NO_FIRES;
        this.bv = 40;
        this.cost = 15000;
        this.shortAV = 4;
        this.medAV = 4;
        this.maxRange = RANGE_MED;
    }
}
