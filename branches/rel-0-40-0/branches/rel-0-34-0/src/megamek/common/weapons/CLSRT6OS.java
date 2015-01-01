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
public class CLSRT6OS extends SRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -4262996818773684373L;

    /**
     * 
     */
    public CLSRT6OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "SRT 6 (OS)";
        this.setInternalName("CLSRT6 (OS)");
        this.addLookupName("Clan OS SRT-6");
        this.addLookupName("Clan SRT 6 (OS)");
        this.heat = 4;
        this.rackSize = 6;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.waterExtremeRange = 12;
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 12;
        this.flags |= F_NO_FIRES | F_ONESHOT;
        this.cost = 80000;
    }
}
