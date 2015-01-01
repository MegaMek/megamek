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
public class ISLRM10 extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 4015441487276641235L;

    /**
     * 
     */
    public ISLRM10() {
        super();
        this.techLevel = TechConstants.T_INTRO_BOXSET;
        this.name = "LRM 10";
        this.setInternalName(this.name);
        this.addLookupName("IS LRM-10");
        this.addLookupName("ISLRM10");
        this.addLookupName("IS LRM 10");
        this.heat = 4;
        this.rackSize = 10;
        this.minimumRange = 6;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 5.0f;
        this.criticals = 2;
        this.bv = 90;
        this.cost = 100000;
        this.shortAV = 6;
        this.medAV = 6;
        this.longAV = 6;
        this.maxRange = RANGE_LONG;
    }
}
