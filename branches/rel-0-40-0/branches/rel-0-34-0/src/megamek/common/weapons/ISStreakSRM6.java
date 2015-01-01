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
public class ISStreakSRM6 extends StreakSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 3341440732332387700L;

    /**
     * 
     */
    public ISStreakSRM6() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Streak SRM 6";
        this.setInternalName("ISStreakSRM6");
        this.addLookupName("IS Streak SRM-6");
        this.addLookupName("IS Streak SRM 6");
        this.heat = 4;
        this.rackSize = 6;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 4.5f;
        this.criticals = 2;
        this.bv = 89;
        this.cost = 120000;
        this.shortAV = 12;
        this.maxRange = RANGE_SHORT;
    }
}
