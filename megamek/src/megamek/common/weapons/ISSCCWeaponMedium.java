/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class ISSCCWeaponMedium extends SubCapitalCannonWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     *
     */
    public ISSCCWeaponMedium() {
        super();
        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "Medium Sub-Capital Cannon";
        setInternalName(name);
        addLookupName("MediumSCC");
        heat = 30;
        damage = 5;
        rackSize = 5;
        shortRange = 11;
        mediumRange = 22;
        longRange = 33;
        extremeRange = 44;
        tonnage = 500.0f;
        bv = 708;
        cost = 780000;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        introDate = 3068;
        techLevel.put(3068, techLevel.get(3071));
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        techRating = RATING_E;
    }
}
