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

import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Jason Tighe
 */
public class ISSmallVariableSpeedPulseLaser extends VariableSpeedPulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 2676144961105838316L;

    /**
     *
     */
    public ISSmallVariableSpeedPulseLaser() {
        super();
        techLevel = TechConstants.T_IS_ADVANCED;
        name = "Small VSP";
        setInternalName("ISSmallVSPLaser");
        setInternalName("ISSVSPL");
        setInternalName("ISSmallVariableSpeedLaser");
        heat = 3;
        damage = WeaponType.DAMAGE_VARIABLE;
        toHitModifier = -4;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 3;
        waterExtremeRange = 4;
        damageShort = 5;
        damageMedium = 4;
        damageLong = 3;
        tonnage = 2.0f;
        criticals = 1;
        bv = 22;
        cost = 60000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
    }

}
