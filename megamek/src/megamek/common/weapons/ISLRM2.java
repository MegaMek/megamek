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
public class ISLRM2 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -7559909598872310558L;

    /**
     *
     */
    public ISLRM2() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "LRM 2";
        setInternalName(name);
        addLookupName("IS LRM-2");
        addLookupName("ISLRM2");
        addLookupName("IS LRM 2");
        rackSize = 2;
        minimumRange = 6;
        bv = 20;
        introDate = 3057;
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
        techRating = RATING_E;
    }
}
