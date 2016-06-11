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
package megamek.common.weapons.battlearmor;

import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.weapons.PulseLaserWeapon;

/**
 * @author Jay Lawson
 */
public class ISBASmallPulseLaser extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 2977404162226570144L;

    /**
     *
     */
    public ISBASmallPulseLaser() {
        super();
        name = "Small Pulse Laser";
        setInternalName("ISBASmallPulseLaser");
        addLookupName("IS BA Small Pulse Laser");
        addLookupName("ISBASmall Pulse Laser");
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
        tonnage = 0.4f;
        criticals = 1;
        bv = 12;
        cost = 16000;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        flags = flags.or(F_BURST_FIRE).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 3052;
        techLevel.put(3052, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3060, TechConstants.T_IS_ADVANCED);
        techLevel.put(3062, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_D ,RATING_C};
        techRating = RATING_E;
        rulesRefs = "258, TM";
    }

}
