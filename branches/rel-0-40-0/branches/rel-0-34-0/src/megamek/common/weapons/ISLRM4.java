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
public class ISLRM4 extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5929285415982964603L;

    /**
     * 
     */
    public ISLRM4() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "LRM 4";
        this.setInternalName(this.name);
        this.addLookupName("IS LRM-4");
        this.addLookupName("ISLRM4");
        this.addLookupName("IS LRM 4");
        this.rackSize = 4;
        this.minimumRange = 6;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.bv = 38;
    }
}
