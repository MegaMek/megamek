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
public class CLStreakLRM5OS extends StreakLRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 540083231235504476L;

    /**
     * 
     */
    public CLStreakLRM5OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        this.name = "Streak LRM 5 (OS)";
        this.setInternalName("CLOSStreakLRM5");
        this.addLookupName("Clan Streak LRM-5 (OS)");
        this.addLookupName("Clan Streak LRM 5 (OS)");
        this.addLookupName("CLStreakLRM5 (OS)");
        this.heat = 2;
        this.rackSize = 5;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 2.5f;
        this.criticals = 1;
        this.bv = 87;
        this.flags |= F_ONESHOT;
        this.cost = 75000;
    }
}
