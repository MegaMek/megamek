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
public class ISSRM6 extends SRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2774209761562289905L;

    /**
     * 
     */
    public ISSRM6() {
        super();
        this.techLevel = TechConstants.T_INTRO_BOXSET;
        this.name = "SRM 6";
        this.setInternalName(this.name);
        this.addLookupName("IS SRM-6");
        this.addLookupName("ISSRM6");
        this.addLookupName("IS SRM 6");
        this.heat = 4;
        this.rackSize = 6;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 3.0f;
        this.criticals = 2;
        this.bv = 59;
        this.cost = 80000;
        this.shortAV = 8;
        this.maxRange = RANGE_SHORT;
    }
}
