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
public class ISLargeVariableSpeedPulseLaser extends VariableSpeedPulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 2676144961105838316L;

    /**
     *
     */
    public ISLargeVariableSpeedPulseLaser() {
        super();
        techLevel = TechConstants.T_IS_ADVANCED;
        name = "Large VSP";
        setInternalName("ISLargeVSPLaser");
        setInternalName("ISLVSPL");
        setInternalName("ISLargeVariableSpeedLaser");
        heat = 10;
        damage = WeaponType.DAMAGE_VARIABLE;
        toHitModifier = -4;
        shortRange = 4;
        mediumRange = 8;
        longRange = 15;
        extremeRange = 16;
        waterShortRange = 2;
        waterMediumRange = 5;
        waterLongRange = 9;
        waterExtremeRange = 10;
        damageShort = 11;
        damageMedium = 9;
        damageLong = 7;
        tonnage = 9.0f;
        criticals = 4;
        bv = 123;
        cost = 465000;
        shortAV = 7;
        medAV = 10;
        maxRange = RANGE_MED;
    }

}
