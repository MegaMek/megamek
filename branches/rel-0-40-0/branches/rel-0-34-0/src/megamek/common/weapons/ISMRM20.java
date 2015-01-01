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
public class ISMRM20 extends MRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 6320263755562771620L;

    /**
     * 
     */
    public ISMRM20() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "MRM 20";
        this.setInternalName(this.name);
        this.addLookupName("MRM-20");
        this.addLookupName("ISMRM20");
        this.addLookupName("IS MRM 20");
        this.heat = 6;
        this.rackSize = 20;
        this.shortRange = 3;
        this.mediumRange = 8;
        this.longRange = 15;
        this.extremeRange = 22;
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.bv = 112;
        this.cost = 125000;       
        this.shortAV = 12;
        this.medAV = 12;
        this.maxRange = RANGE_MED;
    }
}
