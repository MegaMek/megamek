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
import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 */
public class CLMicroPulseLaser extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -3335298535182304490L;

    /**
     *
     */
    public CLMicroPulseLaser() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "Micro Pulse Laser";
        setInternalName("CLMicroPulseLaser");
        addLookupName("Clan Micro Pulse Laser");
        heat = 1;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        toHitModifier = -2;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 4;
        tonnage = 0.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_BURST_FIRE);
        bv = 12;
        cost = 12500;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        this.availRating = new int[] { EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D };
        introDate = 3060;
        techLevel.put(3060, techLevel.get(3071));
    }

}
