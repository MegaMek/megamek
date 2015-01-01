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
public class ISSRT4 extends SRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -5648326444418700888L;

    /**
     * 
     */
    public ISSRT4() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "SRT 4";
        this.setInternalName(this.name);
        this.addLookupName("IS SRT-4");
        this.addLookupName("ISSRT4");
        this.addLookupName("IS SRT 4");
        this.heat = 3;
        this.rackSize = 4;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.waterExtremeRange = 12;
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 39;
        this.cost = 60000;
    }
}
