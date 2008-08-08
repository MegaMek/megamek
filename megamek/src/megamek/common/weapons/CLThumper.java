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
public class CLThumper extends ArtilleryWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 1816815968973747103L;

    /**
     * 
     */
    public CLThumper() {
        super();
        this.techLevel = TechConstants.T_CLAN_LEVEL_2;
        this.name = "Thumper";
        this.setInternalName("CLThumper");
        this.addLookupName("CLThumperArtillery");
        this.addLookupName("Clan Thumper");
        this.heat = 5;
        this.rackSize = 15;
        this.ammoType = AmmoType.T_THUMPER;
        this.shortRange = 1; //
        this.mediumRange = 2;
        this.longRange = 21;
        this.extremeRange = 21; // No extreme range.
        this.tonnage = 15f;
        this.criticals = 15;
        this.bv = 40;
        this.cost = 187500;
    }

}
