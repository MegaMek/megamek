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
public class ISSRM2OS extends SRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -6918950640293828718L;

    /**
     * 
     */
    public ISSRM2OS() {
        super();
        this.techLevel = TechConstants.T_INTRO_BOXSET;
        this.name = "SRM 2 (OS)";
        this.setInternalName("ISSRM2OS");
        this.addLookupName("ISSRM2 (OS)"); // mtf
        this.addLookupName("IS SRM 2 (OS)"); // tdb
        this.addLookupName("OS SRM-2"); // mep
        this.heat = 2;
        this.rackSize = 2;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 1.5f;
        this.criticals = 1;
        this.bv = 4;
        this.flags |= F_NO_FIRES | F_ONESHOT;
        this.cost = 10000;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
    }
}
