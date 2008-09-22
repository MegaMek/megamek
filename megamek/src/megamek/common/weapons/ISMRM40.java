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
public class ISMRM40 extends MRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -6020264492733884246L;

    /**
     * 
     */
    public ISMRM40() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "MRM 40";
        this.setInternalName(this.name);
        this.addLookupName("MRM-40");
        this.addLookupName("ISMRM40");
        this.addLookupName("IS MRM 40");
        this.heat = 12;
        this.rackSize = 40;
        this.shortRange = 3;
        this.mediumRange = 8;
        this.longRange = 15;
        this.extremeRange = 22;
        this.tonnage = 12.0f;
        this.criticals = 7;
        this.bv = 224;
        this.cost = 350000;
        this.shortAV = 24;
        this.medAV = 24;
        this.maxRange = RANGE_MED;
    }
}
