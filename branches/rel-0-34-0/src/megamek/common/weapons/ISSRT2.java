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
public class ISSRT2 extends SRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 4156966181240837624L;

    /**
     * 
     */
    public ISSRT2() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "SRT 2";
        this.setInternalName(this.name);
        this.addLookupName("IS SRT-2");
        this.addLookupName("ISSRT2");
        this.addLookupName("IS SRT 2");
        this.heat = 2;
        this.rackSize = 2;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.waterExtremeRange = 12;
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 21;
        this.flags |= F_NO_FIRES;
        this.cost = 10000;
    }
}
