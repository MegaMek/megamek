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
public class CLLRT20OS extends LRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 4540170151130434608L;

    /**
     * 
     */
    public CLLRT20OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRT 20 (OS)";
        this.setInternalName("CLLRTorpedo20 (OS)");
        this.addLookupName("Clan OS LRT-20");
        this.addLookupName("Clan LRT 20 (OS)");
        this.heat = 6;
        this.rackSize = 20;
        this.minimumRange = WEAPON_NA;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 5.5f;
        this.criticals = 4;
        this.bv = 44;
        this.flags |= F_ONESHOT;
        this.cost = 250000;
    }
}
