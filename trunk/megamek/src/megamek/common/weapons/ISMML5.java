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
public class ISMML5 extends MMLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -546200914895806968L;

    /**
     *
     */
    public ISMML5() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "MML 5";
        setInternalName("ISMML5");
        addLookupName("IS MML-5");
        heat = 3;
        rackSize = 5;
        tonnage = 3f;
        criticals = 3;
        bv = 45;
        cost = 75000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        maxRange = RANGE_LONG;
        introDate = 3068;
        techLevel.put(3068,techLevel.get(3071));
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
        techRating = RATING_E;
    }
}
