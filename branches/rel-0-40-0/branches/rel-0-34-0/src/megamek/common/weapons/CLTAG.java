/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLTAG extends TAGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 7446980554102548125L;

    public CLTAG() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Clan TAG";
        this.setInternalName("CLTAG");
        this.addLookupName("Clan TAG");
        this.tonnage = 1;
        this.criticals = 1;
        this.hittable = true;
        this.spreadable = false;
        this.heat = 0;
        this.damage = 0;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.bv = 0;
        this.cost = 50000;
    }
}
