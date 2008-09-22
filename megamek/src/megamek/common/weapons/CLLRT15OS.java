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
public class CLLRT15OS extends LRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 2935323332234777496L;

    /**
     * 
     */
    public CLLRT15OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRT 15 (OS)";
        this.setInternalName("CLLRTorpedo15 (OS)");
        this.addLookupName("Clan OS LRT-15");
        this.addLookupName("Clan LRT 15 (OS)");
        this.heat = 5;
        this.rackSize = 15;
        this.minimumRange = WEAPON_NA;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 4.0f;
        this.criticals = 2;
        this.bv = 33;
        this.flags |= F_ONESHOT;
        this.cost = 175000;
    }
}
