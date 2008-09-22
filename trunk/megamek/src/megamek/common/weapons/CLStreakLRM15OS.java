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
public class CLStreakLRM15OS extends StreakLRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 7336450815633311159L;

    /**
     * 
     */
    public CLStreakLRM15OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        this.name = "Streak LRM 15 (OS)";
        this.setInternalName("CLOSStreakLRM15");
        this.addLookupName("Clan Streak LRM-15 (OS)");
        this.addLookupName("Clan Streak LRM 15 (OS)");
        this.addLookupName("CLStreakLRM15 (OS)");
        this.heat = 5;
        this.rackSize = 15;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 7.5f;
        this.criticals = 3;
        this.bv = 260;
        this.flags |= F_ONESHOT;
        this.cost = 400000;
    }
}
