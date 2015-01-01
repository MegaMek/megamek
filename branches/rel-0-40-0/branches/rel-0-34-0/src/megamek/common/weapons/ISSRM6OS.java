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
public class ISSRM6OS extends SRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -1926715836221080572L;

    /**
     * 
     */
    public ISSRM6OS() {
        super();
        this.techLevel = TechConstants.T_INTRO_BOXSET;
        this.name = "SRM 6 (OS)";
        this.setInternalName("ISSRM6OS");
        this.addLookupName("ISSRM6 (OS)"); // mtf
        this.addLookupName("IS SRM 6 (OS)"); // tdb
        this.addLookupName("OS SRM-6"); // mep
        this.heat = 4;
        this.rackSize = 6;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 3.5f;
        this.criticals = 2;
        this.bv = 12;
        this.flags |= F_NO_FIRES | F_ONESHOT;
        this.cost = 80000;
        this.shortAV = 8;
        this.maxRange = RANGE_SHORT;
    }
}
