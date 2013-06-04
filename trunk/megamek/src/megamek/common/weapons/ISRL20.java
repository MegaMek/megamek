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
public class ISRL20 extends RLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1220608344459915265L;

    /**
     *
     */
    public ISRL20() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Rocket Launcher 20";
        setInternalName("RL20");
        addLookupName("ISRocketLauncher20");
        addLookupName("RL 20");
        addLookupName("IS RLauncher-20");
        heat = 5;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 7;
        longRange = 12;
        extremeRange = 14;
        tonnage = 1.5f;
        criticals = 3;
        bv = 24;
        cost = 45000;
        shortAV = 12;
        medAV = 12;
        maxRange = RANGE_MED;
        introDate = 3064;
        techLevel.put(3064, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_B };
        techRating = RATING_B;
    }
}
