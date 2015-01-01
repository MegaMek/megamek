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
public class ISLRM3 extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 435741447089925036L;

    /**
     * 
     */
    public ISLRM3() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "LRM 3";
        this.setInternalName(this.name);
        this.addLookupName("IS LRM-3");
        this.addLookupName("ISLRM3");
        this.addLookupName("IS LRM 3");
        this.rackSize = 3;
        this.minimumRange = 6;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.bv = 29;
    }
}
