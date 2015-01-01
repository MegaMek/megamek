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
public class CLStreakSRM3 extends StreakSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 6130835636033954114L;

    /**
     * 
     */
    public CLStreakSRM3() {
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Streak SRM 3";
        this.setInternalName("CLStreakSRM3");
        this.heat = 0;
        this.rackSize = 3;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.tonnage = 1.5f;
        this.criticals = 0;
        this.bv = 59;
    }
}
