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
public class ISSRM4 extends SRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 4338199179135810932L;

    /**
     * 
     */
    public ISSRM4() {
        super();
        this.techLevel.put(3071, TechConstants.T_INTRO_BOXSET);
        this.name = "SRM 4";
        this.setInternalName(this.name);
        this.addLookupName("IS SRM-4");
        this.addLookupName("ISSRM4");
        this.addLookupName("IS SRM 4");
        this.heat = 3;
        this.rackSize = 4;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 39;
        this.cost = 60000;
        this.shortAV = 4;
        this.maxRange = RANGE_SHORT;
        introDate = 2370;
        techLevel.put(2370, techLevel.get(3071));
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        techRating = RATING_C;
    }
}
