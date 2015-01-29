/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISExtendedLRM5 extends ExtendedLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6153832907941260136L;

    /**
     *
     */
    public ISExtendedLRM5() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Extended LRM 5";
        setInternalName(name);
        addLookupName("IS Extended LRM-5");
        addLookupName("ISExtendedLRM5");
        addLookupName("IS Extended LRM 5");
        addLookupName("ELRM-5 (THB)");
        heat = 3;
        rackSize = 5;
        tonnage = 6.0f;
        criticals = 1;
        bv = 67;
        cost = 60000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        extAV = 3;
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3054;
        techLevel.put(3054, techLevel.get(3071));
        techLevel.put(3080, TechConstants.T_IS_TW_NON_BOX);
    }
}
