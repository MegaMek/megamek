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
public class ISStreakSRM4OS extends StreakSRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -8651111887714823028L;

    /**
     * 
     */
    public ISStreakSRM4OS() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Streak SRM 4 (OS)";
        this.setInternalName("ISStreakSRM4OS");
        this.addLookupName("ISStreakSRM4 (OS)"); // mtf
        this.addLookupName("IS Streak SRM 4 (OS)"); // tdb
        this.addLookupName("OS Streak SRM-4"); // mep
        this.heat = 3;
        this.rackSize = 4;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 2.5f;
        this.criticals = 1;
        this.flags |= F_ONESHOT | F_NO_FIRES;
        this.bv = 12;
        this.cost = 60000;
        this.shortAV = 8;
        this.maxRange = RANGE_SHORT;
    }
}
