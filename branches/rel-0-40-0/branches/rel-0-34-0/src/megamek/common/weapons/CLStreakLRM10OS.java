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
public class CLStreakLRM10OS extends StreakLRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 2692279526867532848L;

    /**
     * 
     */
    public CLStreakLRM10OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        this.name = "Streak LRM 10 (OS)";
        this.setInternalName("CLOSStreakLRM10");
        this.addLookupName("Clan Streak LRM-10 (OS)");
        this.addLookupName("Clan Streak LRM 10 (OS)");
        this.addLookupName("CLStreakLRM10 (OS)");
        this.heat = 4;
        this.rackSize = 10;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 5.5f;
        this.criticals = 2;
        this.bv = 173;
        this.flags |= F_ONESHOT;
        this.cost = 225000;
    }
}
