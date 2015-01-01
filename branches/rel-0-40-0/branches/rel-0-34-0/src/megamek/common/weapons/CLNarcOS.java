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
public class CLNarcOS extends NarcWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5553288957570246232L;

    /**
     * 
     */
    public CLNarcOS() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Narc (OS)";
        this.setInternalName("CLNarcBeacon (OS)");
        this.addLookupName("Clan OS Narc Beacon");
        this.addLookupName("Clan Narc Missile Beacon (OS)");
        this.heat = 0;
        this.rackSize = 1;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.tonnage = 2.5f;
        this.criticals = 1;
        this.flags |= F_ONESHOT;
        this.bv = 6;
        this.cost = 100000;
    }
}
