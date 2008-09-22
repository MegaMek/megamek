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
public class ISMML5 extends MMLWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -546200914895806968L;

    /**
     * 
     */
    public ISMML5() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "MML 5";
        this.setInternalName("ISMML5");
        this.addLookupName("IS MML-5");
        this.heat = 3;
        this.rackSize = 5;
        this.tonnage = 3f;
        this.criticals = 3;
        this.bv = 45;
        this.cost = 90000;
        this.shortAV = 3;
        this.medAV = 3;
        this.longAV = 3;
        this.maxRange = RANGE_LONG;
    }
}
