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
public class ISLRM15 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 603060073432118270L;

    /**
     *
     */
    public ISLRM15() {
        super();
        techLevel = TechConstants.T_INTRO_BOXSET;
        name = "LRM 15";
        setInternalName(name);
        addLookupName("IS LRM-15");
        addLookupName("ISLRM15");
        addLookupName("IS LRM 15");
        heat = 5;
        rackSize = 15;
        minimumRange = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 7.0f;
        criticals = 3;
        bv = 136;
        cost = 175000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        maxRange = RANGE_LONG;
    }
}
