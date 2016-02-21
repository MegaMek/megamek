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
 * @author Sebastian Brocks
 */
public class ISLightPPC extends PPCWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 1376257996653555538L;

    /**
     *
     */
    public ISLightPPC() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Light PPC";
        setInternalName(name);
        addLookupName("ISLightPPC");
        addLookupName("ISLPPC");
        heat = 5;
        damage = 5;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        waterShortRange = 4;
        waterMediumRange = 7;
        waterLongRange = 10;
        waterExtremeRange = 14;
        setModes(new String[] { "Field Inhibitor ON", "Field Inhibitor OFF" });
        tonnage = 3.0f;
        criticals = 2;
        bv = 88;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        cost = 150000;
        // with a capacitor
        explosive = true;
        this.availRating = new int[] { EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E };
        introDate = 3067;
        techLevel.put(3067, techLevel.get(3071));
    }

}
