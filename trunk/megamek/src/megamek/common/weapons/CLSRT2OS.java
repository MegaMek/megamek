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
public class CLSRT2OS extends SRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 4523859966917171130L;

    /**
     * 
     */
    public CLSRT2OS() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "SRT 2 (OS)";
        this.setInternalName("CLSRT2 (OS)");
        this.addLookupName("Clan OS SRT-2");
        this.addLookupName("Clan SRT 2 (OS)");
        this.heat = 2;
        this.rackSize = 2;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.waterExtremeRange = 12;
        this.tonnage = 1.5f;
        this.criticals = 1;
        this.bv = 4;
        this.flags |= F_NO_FIRES | F_ONESHOT;
        this.cost = 10000;
    }
}
