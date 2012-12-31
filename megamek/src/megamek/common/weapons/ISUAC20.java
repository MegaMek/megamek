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
 * Created on Oct 2, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISUAC20 extends UACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -8297688910484314546L;

    /**
     *
     */
    public ISUAC20() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "Ultra AC/20";
        setInternalName("ISUltraAC20");
        addLookupName("IS Ultra AC/20");
        heat = 8;
        damage = 20;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 7;
        longRange = 10;
        extremeRange = 14;
        tonnage = 15.0f;
        criticals = 10;
        bv = 281;
        cost = 480000;
        flags = flags.or(F_SPLITABLE);
        shortAV = 30;
        medAV = 30;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        introDate = 3060;
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
        techRating = RATING_E;
    }
}
