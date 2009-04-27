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
public class ISMRM10OS extends MRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 1980690855716987710L;

    /**
     * 
     */
    public ISMRM10OS() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "MRM 10 (OS)";
        this.setInternalName(this.name);
        this.addLookupName("OS MRM-10");
        this.addLookupName("ISMRM10 (OS)");
        this.addLookupName("IS MRM 10 (OS)");
        this.heat = 4;
        this.rackSize = 10;
        this.shortRange = 3;
        this.mediumRange = 8;
        this.longRange = 15;
        this.extremeRange = 22;
        this.tonnage = 3.5f;
        this.criticals = 2;
        this.bv = 11;
        this.flags |= F_ONESHOT;
        this.cost = 50000;
        this.shortAV = 6;
        this.medAV = 6;
        this.maxRange = RANGE_MED;
    }
}
