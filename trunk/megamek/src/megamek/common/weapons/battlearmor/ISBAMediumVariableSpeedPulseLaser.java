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
import megamek.common.weapons.VariableSpeedPulseLaserWeapon;

/**
 * @author Jason Tighe
 */
public class ISBAMediumVariableSpeedPulseLaser extends
        VariableSpeedPulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 2676144961105838316L;

    /**
     *
     */
    public ISBAMediumVariableSpeedPulseLaser() {
        super();
        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "BA Medium VSP Laser";
        setInternalName("ISBAMediumVSPLaser");
        addLookupName("ISBAMVSPL");
        addLookupName("ISBAMediumVariableSpeedLaser");
        addLookupName("ISBAMediumVSP");
        heat = 7;
        damage = DAMAGE_VARIABLE;
        toHitModifier = -4;
        shortRange = 2;
        mediumRange = 5;
        longRange = 9;
        extremeRange = 10;
        waterShortRange = 1;
        waterMediumRange = 3;
        waterLongRange = 6;
        waterExtremeRange = 6;
        damageShort = 9;
        damageMedium = 7;
        damageLong = 5;
        tonnage = .9f;
        criticals = 4;
        bv = 56;
        cost = 200000;
        shortAV = 7;
        maxRange = RANGE_SHORT;
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3072;
        techLevel.put(3072, techLevel.get(3071));
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
    }

}
