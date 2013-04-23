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
public class CLUAC20 extends UACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 2630276807984380743L;

    /**
     *
     */
    public CLUAC20() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "Ultra AC/20";
        setInternalName("CLUltraAC20");
        addLookupName("Clan Ultra AC/20");
        heat = 7;
        damage = 20;
        rackSize = 20;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 12.0f;
        criticals = 8;
        bv = 335;
        cost = 480000;
        shortAV = 30;
        medAV = 30;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        techRating = RATING_F;
        introDate = 2825;
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
    }
}
