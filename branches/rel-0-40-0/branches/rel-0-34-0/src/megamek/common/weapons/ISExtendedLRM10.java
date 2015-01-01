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
public class ISExtendedLRM10 extends ExtendedLRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 8831960393355550709L;

    /**
     * 
     */
    public ISExtendedLRM10() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "ExtendedLRM 10";
        this.setInternalName(this.name);
        this.addLookupName("IS ExtendedLRM-10");
        this.addLookupName("ISExtendedLRM10");
        this.addLookupName("IS ExtendedLRM 10");
        this.addLookupName("ELRM-10 (THB)");
        this.heat = 6;
        this.rackSize = 10;
        this.minimumRange = 10;
        this.shortRange = 12;
        this.mediumRange = 22;
        this.longRange = 38;
        this.extremeRange = 44;
        this.tonnage = 8.0f;
        this.criticals = 4;
        this.bv = 117;
        this.cost = 225000;
    }
}
