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
public class ISExtendedLRM15 extends ExtendedLRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -7039029686193601958L;

    /**
     * 
     */
    public ISExtendedLRM15() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "ExtendedLRM 15";
        this.setInternalName(this.name);
        this.addLookupName("IS ExtendedLRM-15");
        this.addLookupName("ISExtendedLRM15");
        this.addLookupName("IS ExtendedLRM 15");
        this.addLookupName("ELRM-15 (THB)");
        this.heat = 8;
        this.rackSize = 15;
        this.minimumRange = 10;
        this.shortRange = 12;
        this.mediumRange = 22;
        this.longRange = 38;
        this.extremeRange = 44;
        this.tonnage = 12.0f;
        this.criticals = 6;
        this.bv = 177;
        this.cost = 350000;
    }
}
