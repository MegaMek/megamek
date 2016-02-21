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
public class ISExtendedLRM10 extends ExtendedLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 8831960393355550709L;

    /**
     *
     */
    public ISExtendedLRM10() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Extended LRM 10";
        setInternalName(name);
        addLookupName("IS Extended LRM-10");
        addLookupName("ISExtendedLRM10");
        addLookupName("IS Extended LRM 10");
        addLookupName("ELRM-10 (THB)");
        heat = 6;
        rackSize = 10;
        tonnage = 8.0f;
        criticals = 4;
        bv = 133;
        cost = 200000;
        shortAV = 6;
        medAV = 6;
        longAV = 6;
        extAV = 6;
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3054;
        techLevel.put(3054, techLevel.get(3071));
        techLevel.put(3080, TechConstants.T_IS_TW_NON_BOX);
    }
}
