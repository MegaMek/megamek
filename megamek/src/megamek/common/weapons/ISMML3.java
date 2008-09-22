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
public class ISMML3 extends MMLWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -9170270710231973218L;

    /**
     * 
     */
    public ISMML3() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "MML 3";
        this.setInternalName("ISMML3");
        this.addLookupName("IS MML-3");
        this.heat = 2;
        this.rackSize = 3;
        this.tonnage = 1.5f;
        this.criticals = 2;
        this.bv = 29;
        this.cost = 50000;
        this.shortAV = 2;
        this.medAV = 2;
        this.longAV = 2;
        this.maxRange = RANGE_LONG;
    }
}
