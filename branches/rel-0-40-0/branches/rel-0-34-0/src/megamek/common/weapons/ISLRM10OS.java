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
public class ISLRM10OS extends LRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2792101005477263443L;

    /**
     * 
     */
    public ISLRM10OS() {
        super();
        this.techLevel = TechConstants.T_INTRO_BOXSET;
        this.name = "LRM 10 (OS)";
        this.setInternalName(this.name);
        this.addLookupName("IS OS LRM-10");
        this.addLookupName("ISLRM10 (OS)");
        this.addLookupName("IS LRM 10 (OS)");
        this.heat = 4;
        this.rackSize = 10;
        this.minimumRange = 6;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 5.5f;
        this.criticals = 2;
        this.bv = 18;
        this.flags |= F_ONESHOT;
        this.cost = 100000;
        this.shortAV = 6;
        this.medAV = 6;
        this.longAV = 6;
        this.maxRange = RANGE_LONG;
    }
}
