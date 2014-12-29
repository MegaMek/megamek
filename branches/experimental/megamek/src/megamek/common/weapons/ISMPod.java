/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISMPod extends MPodWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 7462050177159480L;

    /**
     *
     */
    public ISMPod() {
        super();
        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "M-Pod";
        setInternalName("ISMPod");
        addLookupName("ISM-Pod");
        introDate = 3060;
        techRating = RATING_C;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techLevel.put(3060, TechConstants.T_IS_ADVANCED);
        techLevel.put(3064, TechConstants.T_IS_TW_NON_BOX);

    }
}
