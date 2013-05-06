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
public class ISLRM5OS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3915337270241715850L;

    /**
     *
     */
    public ISLRM5OS() {
        super();
        techLevel.put(3071,TechConstants.T_INTRO_BOXSET);
        name = "LRM 5 (OS)";
        setInternalName(name);
        addLookupName("IS OS LRM-5");
        addLookupName("ISLRM5 (OS)");
        addLookupName("IS LRM 5 (OS)");
        heat = 2;
        rackSize = 5;
        minimumRange = 6;
        tonnage = 2.5f;
        criticals = 1;
        bv = 9;
        flags = flags.or(F_ONESHOT);
        cost = 15000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        maxRange = RANGE_LONG;
        this.introDate = 2676;
        availRating = new int[]{RATING_C,RATING_C,RATING_C};
        techRating = RATING_C;
    }
}
