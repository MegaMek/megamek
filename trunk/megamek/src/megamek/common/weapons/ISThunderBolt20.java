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

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISThunderBolt20 extends ThunderBoltWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6976091682813292840L;

    /**
     *
     */
    public ISThunderBolt20() {
        super();
        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "Thunderbolt 20";
        setInternalName(name);
        addLookupName("IS Thunderbolt-20");
        addLookupName("ISThunderbolt20");
        addLookupName("ISTBolt20");
        addLookupName("IS Thunderbolt 20");
        ammoType = AmmoType.T_TBOLT_20;
        heat = 8;
        minimumRange = 5;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        shortAV = 20;
        medAV = 20;
        maxRange = RANGE_MED;
        tonnage = 15.0f;
        criticals = 5;
        bv = 305;
        cost = 450000;
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3072;
        techLevel.put(3072, techLevel.get(3071));
        techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);
    }
}
