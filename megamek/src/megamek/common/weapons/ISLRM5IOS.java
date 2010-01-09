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
public class ISLRM5IOS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3915337270241715850L;

    /**
     *
     */
    public ISLRM5IOS() {
        super();
        techLevel = TechConstants.T_INTRO_BOXSET;
        name = "LRM 5 (IOS)";
        setInternalName(name);
        addLookupName("IS IOS LRM-5");
        addLookupName("ISLRM5 (IOS)");
        addLookupName("IS LRM 5 (IOS)");
        heat = 2;
        rackSize = 5;
        minimumRange = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 2.0f;
        criticals = 1;
        bv = 9;
        flags = flags.or(F_ONESHOT);
        cost = 30000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        maxRange = RANGE_LONG;
    }
}
