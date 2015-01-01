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
public class ISNarcOS extends NarcWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 8610275030183400408L;

    /**
     * 
     */
    public ISNarcOS() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Narc (OS)";
        this.setInternalName("ISNarcBeacon (OS)");
        this.addLookupName("IS OS Narc Beacon");
        this.addLookupName("IS Narc Missile Beacon (OS)");
        this.heat = 0;
        this.rackSize = 1;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 3.5f;
        this.criticals = 2;
        this.flags |= F_ONESHOT;
        this.bv = 6;
        this.cost = 100000;
    }
}
