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
package megamek.common.weapons.battlearmor;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.WeaponType;
import megamek.common.weapons.lasers.VariableSpeedPulseLaserWeapon;

/**
 * @author Jason Tighe
 * @since Sep 12, 2004
 */
public class ISBALaserVSPSmall extends VariableSpeedPulseLaserWeapon {
    private static final long serialVersionUID = 2676144961105838316L;

    public ISBALaserVSPSmall() {
        super();
        name = "Small VSP Laser";
        setInternalName("ISBASmallVSPLaser");
        addLookupName("ISBASVSPL");
        addLookupName("ISBASmallVariableSpeedLaser");
        addLookupName("ISBASmallVSP");
        sortingName = "Laser VSP B";
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
        tonnage = .5;
        criticals = 2;
        bv = 22;
        cost = 60000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON)
                .andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        // Tech Progression Missing in IO. Confirmed with Herb uses the same as the Mech Weapon.
        rulesRefs = "321, TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3070, 3072, 3080, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_FW,F_WB)
                .setProductionFactions(F_FW,F_WB);
    }

    @Override
    public double getBattleForceDamage(int range) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.575;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.315;
        } else {
            return 0;
        }
    }

}
