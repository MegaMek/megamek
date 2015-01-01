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
public class ISMML9 extends MMLWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -6856580158397507743L;

    /**
     * 
     */
    public ISMML9() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "MML 9";
        this.setInternalName("ISMML9");
        this.addLookupName("IS MML-9");
        this.heat = 5;
        this.rackSize = 9;
        this.tonnage = 6f;
        this.criticals = 5;
        this.bv = 86;
        this.cost = 225000;
        this.shortAV = 5;
        this.medAV = 5;
        this.longAV = 5;
        this.maxRange = RANGE_LONG;
    }
}
