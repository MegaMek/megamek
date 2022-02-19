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

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class CLPulseLaserLarge extends PulseLaserWeapon {
    private static final long serialVersionUID = 608317914802476438L;

    public CLPulseLaserLarge() {
        super();
        name = "Large Pulse Laser";
        setInternalName("CLLargePulseLaser");
        addLookupName("Clan Pulse Large Laser");
        addLookupName("Clan Large Pulse Laser");
        sortingName = "Laser Pulse D";
        heat = 10;
        damage = 10;
        toHitModifier = -2;
        shortRange = 6;
        mediumRange = 14;
        longRange = 20;
        extremeRange = 28;
        waterShortRange = 4;
        waterMediumRange = 10;
        waterLongRange = 14;
        waterExtremeRange = 20;
        tonnage = 6.0;
        criticals = 2;
        bv = 265;
        cost = 175000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_LONG;
        rulesRefs = "226, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2831, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
    }
}
