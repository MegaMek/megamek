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
 * Created on Oct 15, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISLB20XAC extends LBXACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 5809856400438327826L;

    /**
     *
     */
    public ISLB20XAC() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "LB 20-X AC";
        setInternalName("ISLBXAC20");
        addLookupName("IS LB 20-X AC");
        heat = 6;
        damage = 20;
        rackSize = 20;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 14.0f;
        criticals = 11;
        bv = 237;
        cost = 600000;
        shortAV = 20;
        medAV = 20;
        maxRange = RANGE_MED;
        introDate = 3058;
        techLevel.put(3058, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_E;
    }
}
