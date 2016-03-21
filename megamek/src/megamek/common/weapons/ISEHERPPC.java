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
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISEHERPPC extends PPCWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 7175778897598535734L;

    /**
     *
     */
    public ISEHERPPC() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Enhanced ER PPC";
        setInternalName("ISEHERPPC");
        addLookupName("IS EH ER PPC");
        heat = 15;
        damage = 12;
        shortRange = 7;
        mediumRange = 14;
        longRange = 23;
        extremeRange = 28;
        waterShortRange = 4;
        waterMediumRange = 10;
        waterLongRange = 16;
        waterExtremeRange = 20;
        tonnage = 7.0f;
        criticals = 3;
        bv = 329;
        cost = 300000;
        shortAV = 12;
        medAV = 12;
        longAV = 12;
        maxRange = RANGE_LONG;
        //This is the Clan Wolverine PPC mentioned in Blake Documents
        introDate = 2823;
        extinctDate = 2828;
        techLevel.put(2828, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_F, RATING_X };
        techRating = RATING_F;
    }
}
