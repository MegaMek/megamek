/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 8, 2005
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 */
public class ISSmallXPulseLaser extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 5322977585378755226L;

    /**
     *
     */
    public ISSmallXPulseLaser() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Small X-Pulse Laser";
        setInternalName("ISSmallXPulseLaser");
        addLookupName("IS X-Pulse Small Laser");
        addLookupName("IS Small X-Pulse Laser");
        heat = 3;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        toHitModifier = -2;
        shortRange = 2;
        mediumRange = 4;
        longRange = 5;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 4;
        tonnage = 1.0f;
        criticals = 1;
        bv = 21;
        maxRange = RANGE_SHORT;
        shortAV = 3;
        cost = 31000;
        flags = flags.or(F_BURST_FIRE);
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3057;
        techLevel.put(3057, techLevel.get(3071));
        techLevel.put(3078, TechConstants.T_IS_ADVANCED);
    }
}
