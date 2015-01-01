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
public class ISMRM20IOS extends MRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2738014475152659505L;

    /**
     *
     */
    public ISMRM20IOS() {
        super();
        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "MRM 20 (I-OS)";
        setInternalName(name);
        addLookupName("IOS MRM-20");
        addLookupName("ISMRM20 (IOS)");
        addLookupName("IS MRM 20 (IOS)");
        heat = 6;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 8;
        longRange = 15;
        extremeRange = 16;
        tonnage = 6.5f;
        criticals = 3;
        bv = 22;
        flags = flags.or(F_ONESHOT);
        cost = 100000;
        shortAV = 12;
        medAV = 12;
        maxRange = RANGE_MED;
        techRating = RATING_B;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3058;
        techLevel.put(3058, techLevel.get(3071));
        techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);
    }
}
