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
public class ISMML7 extends MMLWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2143795495566407588L;

    /**
     * 
     */
    public ISMML7() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "MML 7";
        this.setInternalName("ISMML7");
        this.addLookupName("IS MML-7");
        this.heat = 4;
        this.rackSize = 7;
        this.tonnage = 4.5f;
        this.criticals = 4;
        this.bv = 67;
        this.cost = 160000;
        this.shortAV = 4;
        this.medAV = 4;
        this.longAV = 4;
        this.maxRange = RANGE_LONG;
    }
}
