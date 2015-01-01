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
public class ISLRM3OS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 435741447089925036L;

    /**
     *
     */
    public ISLRM3OS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "LRM 3 (OS)";
        setInternalName(name);
        addLookupName("ISLRM3OS");
        rackSize = 3;
        minimumRange = 6;
        bv = 6;
        flags = flags.or(F_ONESHOT);
        introDate = 3057;
        techLevel.put(3057, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_E;
    }
}
