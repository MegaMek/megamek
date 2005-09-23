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
 * 
 */
public class CLLongTom extends ArtilleryWeapon {

    /**
     * 
     */
    public CLLongTom() {
        super();
        this.techLevel = TechConstants.T_CLAN_LEVEL_2;
        this.name = "Long Tom";
        this.setInternalName("CLLongTom");
        this.addLookupName("CLLongTomArtillery");
        this.addLookupName("Clan Long Tom");
        this.heat = 20;
        this.rackSize = 20;
        this.ammoType = AmmoType.T_LONG_TOM;
        this.shortRange = 1;  //
        this.mediumRange = 2;
        this.longRange = 20;
        this.extremeRange = 20; // No extreme range.
        this.tonnage = 30f;
        this.criticals = 30;
        this.bv = 171;
        this.cost = 450000;
    }

}
