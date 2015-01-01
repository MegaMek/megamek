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
public class ISExtendedLRM20 extends ExtendedLRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2230366483054553162L;

    /**
     * 
     */
    public ISExtendedLRM20() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "ExtendedLRM 20";
        this.setInternalName(this.name);
        this.addLookupName("IS ExtendedLRM-20");
        this.addLookupName("ISExtendedLRM20");
        this.addLookupName("IS ExtendedLRM 20");
        this.addLookupName("ELRM-20 (THB)");
        this.heat = 10;
        this.rackSize = 20;
        this.minimumRange = 10;
        this.shortRange = 12;
        this.mediumRange = 22;
        this.longRange = 38;
        this.extremeRange = 44;
        this.tonnage = 18.0f;
        this.criticals = 8;
        this.bv = 236;
        this.cost = 450000;
    }
}
