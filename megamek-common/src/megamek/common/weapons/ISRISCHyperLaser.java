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
 * Created on Sep 12, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.EquipmentType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISRISCHyperLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 4467522144065588079L;

    /**
     *
     */
    public ISRISCHyperLaser() {
        super();
        techLevel.put(3134, TechConstants.T_IS_EXPERIMENTAL);
        name = "RISC Hyper Laser";
        setInternalName("ISRISCHyperLaser");
        heat = 24;
        damage = 20;
        toHitModifier = 0;
        shortRange = 8;
        mediumRange = 15;
        longRange = 25;
        extremeRange = 30;
        tonnage = 8.0f;
        criticals = 6;
        bv = 596;
        cost = 750000;
        shortAV = 20;
        medAV = 20;
        maxRange = RANGE_EXT;
        explosionDamage = 10;
        explosive = true;
        availRating = new int[] { EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_X, RATING_F };
        introDate = 3134;
        techRating = RATING_F;

    }
}
