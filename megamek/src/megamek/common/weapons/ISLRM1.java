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
public class ISLRM1 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5976936994611000430L;

    /**
     *
     */
    public ISLRM1() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "LRM 1";
        setInternalName(name);
        addLookupName("IS LRM-1");
        addLookupName("ISLRM1");
        addLookupName("IS LRM 1");
        rackSize = 1;
        minimumRange = 6;
        bv = 14;
        introDate = 3057;
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
        techRating = RATING_E;
    }
}
