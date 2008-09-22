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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLSniper extends ArtilleryWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -599648142688689572L;

    /**
     * 
     */
    public CLSniper() {
        super();
        this.techLevel = TechConstants.T_CLAN_ADVANCED;
        this.name = "Sniper";
        this.setInternalName("CLSniper");
        this.addLookupName("CLSniperArtillery");
        this.addLookupName("Clan Sniper");
        this.heat = 10;
        this.rackSize = 20;
        this.ammoType = AmmoType.T_SNIPER;
        this.shortRange = 1; //
        this.mediumRange = 2;
        this.longRange = 18;
        this.extremeRange = 18; // No extreme range.
        this.tonnage = 20f;
        this.criticals = 20;
        this.bv = 86;
        this.cost = 300000;
    }

}
