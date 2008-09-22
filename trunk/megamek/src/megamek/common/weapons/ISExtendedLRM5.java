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
public class ISExtendedLRM5 extends ExtendedLRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -6153832907941260136L;

    /**
     * 
     */
    public ISExtendedLRM5() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "ExtendedLRM 5";
        this.setInternalName(this.name);
        this.addLookupName("IS ExtendedLRM-5");
        this.addLookupName("ISExtendedLRM5");
        this.addLookupName("IS ExtendedLRM 5");
        this.addLookupName("ELRM-5 (THB)");
        this.heat = 3;
        this.rackSize = 5;
        this.minimumRange = 10;
        this.shortRange = 12;
        this.mediumRange = 22;
        this.longRange = 38;
        this.extremeRange = 44;
        this.tonnage = 6.0f;
        this.criticals = 1;
        this.bv = 59;
        this.cost = 110000;
    }
}
