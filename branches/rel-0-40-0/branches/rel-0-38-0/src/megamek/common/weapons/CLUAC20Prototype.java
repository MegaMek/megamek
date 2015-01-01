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
public class CLUAC20Prototype extends CLPrototypeUACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -8297688910484314546L;

    /**
     *
     */
    public CLUAC20Prototype() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        name = "Ultra AC/20 (CP)";
        setInternalName("CLUltraAC20Prototype");
        heat = 8;
        damage = 20;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 7;
        longRange = 10;
        extremeRange = 14;
        tonnage = 15.0f;
        criticals = 11;
        bv = 281;
        cost = 480000;
        shortAV = 30;
        medAV = 30;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        this.techRating = RATING_F;
        introDate = 2820;
        extinctDate = 2827;
        techLevel.put(2820, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_E, RATING_F };
    }
}
