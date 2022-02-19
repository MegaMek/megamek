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
package megamek.common.weapons.lrms;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM15 extends StreakLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5847309314576444364L;

    /**
     *
     */
    public CLStreakLRM15() {
        super();
        name = "Streak LRM 15";
        setInternalName("CLStreakLRM15");
        addLookupName("Clan Streak LRM-15");
        addLookupName("Clan Streak LRM 15");
        heat = 5;
        rackSize = 15;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 7.0;
        criticals = 3;
        bv = 259;
        cost = 400000;
        shortAV = 15;
        medAV = 15;
        longAV = 15;
        maxRange = RANGE_LONG;
        rulesRefs = "327,TO";
        //Tech Advancement moved to StreakLRMWeapon.java
    }
}
