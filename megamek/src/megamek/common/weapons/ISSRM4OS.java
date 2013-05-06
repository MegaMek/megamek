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
public class ISSRM4OS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6674141690237272868L;

    /**
     *
     */
    public ISSRM4OS() {
        super();
        techLevel.put(3071,TechConstants.T_INTRO_BOXSET);
        name = "SRM 4 (OS)";
        setInternalName("ISSRM4OS");
        addLookupName("ISSRM4 (OS)"); // mtf
        addLookupName("IS SRM 4 (OS)"); // tdb
        addLookupName("OS SRM-4"); // mep
        heat = 3;
        rackSize = 4;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 2.5f;
        criticals = 1;
        bv = 8;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT);
        cost = 30000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        this.introDate = 2676;
        this.extinctDate = 2800;
        this.reintroDate = 3030;
        availRating = new int[]{RATING_C,RATING_C,RATING_C};
        techRating = RATING_C;
    }
}
