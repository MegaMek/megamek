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
public class CLLB5XACPrototype extends CLLBXACPrototypeWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8003492051812171922L;

    /**
     *
     */
    public CLLB5XACPrototype() {
        super();
        name = "LB 5-X AC (CP)";
        techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        setInternalName("CLLBXAC5Prototype");
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 3;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 8.0f;
        criticals = 6;
        bv = 83;
        cost = 250000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        maxRange = RANGE_LONG;
        techRating = RATING_F;
        availRating = new int[] { RATING_X, RATING_F, RATING_X };
        introDate = 2820;
        extinctDate = 2826;    
        techLevel.put(2820, techLevel.get(3071));
    }
}
