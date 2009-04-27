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
public class ISLRM5 extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 1922843634155860893L;

    /**
     * 
     */
    public ISLRM5() {
        super();
        this.techLevel = TechConstants.T_INTRO_BOXSET;
        this.name = "LRM 5";
        this.setInternalName(this.name);
        this.addLookupName("IS LRM-5");
        this.addLookupName("ISLRM5");
        this.addLookupName("IS LRM 5");
        this.heat = 2;
        this.rackSize = 5;
        this.minimumRange = 6;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 45;
        this.cost = 30000;
        this.shortAV = 3;
        this.medAV = 3;
        this.longAV = 3;
        this.maxRange = RANGE_LONG;
    }
}
