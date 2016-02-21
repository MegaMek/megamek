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
 * @author Sebastian Brocks, edited by Greg
 */
public class CLIATM6 extends CLIATMWeapon {

    /**
     * I think i can just assign 1? I don't think SVUIDs conflict with those from other classes
     */
    private static final long serialVersionUID = 1L;

    /**
     * TODO: Changeme
     */
    public CLIATM6() {
        super();
        techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        this.name = "iATM 6";
        this.setInternalName("CLiATM6");
        this.addLookupName("Clan iATM-6");
        this.heat = 4;
        this.rackSize = 6;
        this.minimumRange = 4;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 3.5f;
        this.criticals = 3;
        this.bv = 165; // Ammo BV is 39
        this.cost = 250000;
        this.shortAV = 12;
        this.medAV = 12;
        this.maxRange = RANGE_MED;
        introDate = 3070;
        availRating = new int[]{RATING_X,RATING_X,RATING_F};
        techRating = RATING_F;
    }
}
