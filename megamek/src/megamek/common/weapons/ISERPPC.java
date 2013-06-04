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

import megamek.common.EquipmentType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISERPPC extends PPCWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 7175778897598535734L;

    /**
     *
     */
    public ISERPPC() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "ER PPC";
        setInternalName("ISERPPC");
        addLookupName("IS ER PPC");
        heat = 15;
        damage = 10;
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
        bv = 229;
        cost = 300000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_LONG;
        // with a capacitor
        explosive = true;
        introDate = 2751;
        techLevel.put(2751, techLevel.get(3071));
        extinctDate = 2860;
        reintroDate = 3037;
        availRating = new int[] { RATING_E, RATING_F, RATING_D };
        techRating = RATING_E;
    }
}
