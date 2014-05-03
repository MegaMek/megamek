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

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class CLUAC10Prototype extends CLPrototypeUACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 6937673199956551674L;

    /**
     *
     */
    public CLUAC10Prototype() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        name = "Ultra AC/10 (CP)";
        setInternalName("CLUltraAC10Prototype");
        heat = 4;
        damage = 10;
        rackSize = 10;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 13.0f;
        criticals = 8;
        bv = 210;
        cost = 320000;
        shortAV = 15;
        medAV = 15;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        this.techRating = RATING_F;
        introDate = 2820;
        extinctDate = 2827;
        techLevel.put(2820, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_E, RATING_F };
    }
}
