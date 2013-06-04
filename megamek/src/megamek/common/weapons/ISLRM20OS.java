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
public class ISLRM20OS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3960681625679721032L;

    /**
     *
     */
    public ISLRM20OS() {
        super();
        techLevel.put(3071, TechConstants.T_INTRO_BOXSET);
        name = "LRM 20 (OS)";
        setInternalName(name);
        addLookupName("IS OS LRM-20");
        addLookupName("ISLRM20 (OS)");
        addLookupName("IS LRM 20 (OS)");
        heat = 6;
        rackSize = 20;
        minimumRange = 6;
        tonnage = 10.5f;
        criticals = 5;
        bv = 36;
        flags = flags.or(F_ONESHOT);
        cost = 125000;
        shortAV = 12;
        medAV = 12;
        longAV = 12;
        maxRange = RANGE_LONG;
        introDate = 2676;
        techLevel.put(2676, techLevel.get(3071));
        this.extinctDate = 2800;
        this.reintroDate = 3030;
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        techRating = RATING_C;
    }
}
