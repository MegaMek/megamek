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

import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class CLPulseLaserSmall extends PulseLaserWeapon {
    private static final long serialVersionUID = -3257397139779601796L;

    public CLPulseLaserSmall() {
        super();
        name = "Small Pulse Laser";
        setInternalName("CLSmallPulseLaser");
        addLookupName("Clan Pulse Small Laser");
        addLookupName("Clan Small Pulse Laser");
        sortingName = "Laser Pulse B";
        heat = 2;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        toHitModifier = -2;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 4;
        waterExtremeRange = 4;
        tonnage = 1.0;
        criticals = 1;
        bv = 24;
        cost = 16000;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        flags = flags.or(F_BURST_FIRE);
        rulesRefs = "226, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2825, 2829, 2831, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CJF,F_CGB)
                .setProductionFactions(F_CGB);
    }

    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }
}
