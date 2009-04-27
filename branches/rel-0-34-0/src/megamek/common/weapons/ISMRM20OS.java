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
public class ISMRM20OS extends MRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2738014475152659505L;

    /**
     * 
     */
    public ISMRM20OS() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "MRM 20 (OS)";
        this.setInternalName(this.name);
        this.addLookupName("OS MRM-20");
        this.addLookupName("ISMRM20 (OS)");
        this.addLookupName("IS MRM 20 (OS)");
        this.heat = 6;
        this.rackSize = 20;
        this.shortRange = 3;
        this.mediumRange = 8;
        this.longRange = 15;
        this.extremeRange = 22;
        this.tonnage = 7.5f;
        this.criticals = 3;
        this.bv = 22;
        this.flags |= F_ONESHOT;
        this.cost = 125000;
        this.shortAV = 12;
        this.medAV = 12;
        this.maxRange = RANGE_MED;
    }
}
