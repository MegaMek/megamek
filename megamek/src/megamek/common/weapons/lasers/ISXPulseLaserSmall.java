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
package megamek.common.weapons.lasers;

import megamek.common.WeaponType;
import megamek.common.weapons.PulseLaserWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISXPulseLaserSmall extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 5322977585378755226L;

    /**
     *
     */
    public ISXPulseLaserSmall() {
        super();
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
        waterLongRange = 3;
        waterExtremeRange = 4;
        tonnage = 1.0f;
        criticals = 1;
        bv = 21;
        maxRange = RANGE_SHORT;
        shortAV = 3;
        cost = 31000;
        flags = flags.or(F_BURST_FIRE);
        rulesRefs = "321,TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(3057, 3078, 3082, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_LC,F_FS)
            .setProductionFactions(F_LC);
    }
}
