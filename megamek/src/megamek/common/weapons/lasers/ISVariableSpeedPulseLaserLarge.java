/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.lasers;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.SimpleTechLevel;
import megamek.common.WeaponType;

/**
 * @author Jason Tighe
 * @since Sep 12, 2004
 */
public class ISVariableSpeedPulseLaserLarge extends VariableSpeedPulseLaserWeapon {
    private static final long serialVersionUID = 2676144961105838316L;

    public ISVariableSpeedPulseLaserLarge() {
        super();
        name = "Large VSP Laser";
        setInternalName("ISLargeVSPLaser");
        addLookupName("ISLVSPL");
        addLookupName("ISLargeVariableSpeedLaser");
        addLookupName("ISLargeVSP");
        sortingName = "Laser VSP D";
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
        tonnage = 9.0;
        criticals = 4;
        bv = 123;
        cost = 465000;
        shortAV = 10;
        medAV = 7;
        maxRange = RANGE_MED;
        rulesRefs = "321, TO";
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3070, 3072, 3080).setPrototypeFactions(F_FW,F_WB)
                .setProductionFactions(F_FW,F_WB).setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    public double getBattleForceDamage(int range) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 1.265;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.863;
        } else {
            return 0;
        }
    }

    public int getAlphaStrikeHeat() {
        return 14;
    }

}
