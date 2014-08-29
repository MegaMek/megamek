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
 * @author Sebastian Brocks, modified by Greg
 */
public class CLIATM3 extends CLIATMWeapon {

    /**
     * I think i can just assign 1? I don't think SVUIDs conflict with those from other classes
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    public CLIATM3() {
        super();
        techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        this.name = "iATM 3";
        this.setInternalName("CLiATM3");
        this.addLookupName("Clan iATM-3");
        this.heat = 2;
        this.rackSize = 3;
        this.minimumRange = 4;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 1.5f;
        this.criticals = 2;
        this.bv = 83; // Ammo BV is 21
        this.cost = 100000;
        this.shortAV = 6; // Seems to be for aero
        this.medAV = 6; // Seems to be for aero
        this.maxRange = RANGE_MED;
        introDate = 3070;
        availRating = new int[]{RATING_X,RATING_X,RATING_F};
        techRating = RATING_F;
    }
}
