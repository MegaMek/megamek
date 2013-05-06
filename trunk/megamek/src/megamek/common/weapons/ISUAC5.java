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
 * Created on Oct 1, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.EquipmentType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISUAC5 extends UACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -6307637324918648850L;

    /**
     *
     */
    public ISUAC5() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "Ultra AC/5";
        setInternalName("ISUltraAC5");
        addLookupName("IS Ultra AC/5");
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 13;
        longRange = 20;
        extremeRange = 26;
        tonnage = 9.0f;
        criticals = 5;
        bv = 112;
        cost = 200000;
        shortAV = 7;
        medAV = 7;
        longAV = 7;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
        availRating = new int[]{EquipmentType.RATING_D, EquipmentType.RATING_F,EquipmentType.RATING_D};
        introDate = 2640;
        techLevel.put(2640,techLevel.get(3071));
        extinctDate = 2915;
        reintroDate = 3035;
    }
}
