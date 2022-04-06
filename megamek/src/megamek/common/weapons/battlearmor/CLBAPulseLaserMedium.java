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

import megamek.common.weapons.lasers.PulseLaserWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class CLBAPulseLaserMedium extends PulseLaserWeapon {
    private static final long serialVersionUID = -5538336797804604495L;

    public CLBAPulseLaserMedium() {
        super();
        name = "Medium Pulse Laser";
        setInternalName("CLBAMediumPulseLaser");
        addLookupName("Clan BA Pulse Med Laser");
        addLookupName("Clan BA Medium Pulse Laser");
        sortingName = "Laser Pulse C";
        heat = 4;
        damage = 7;
        toHitModifier = -2;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        waterShortRange = 3;
        waterMediumRange = 5;
        waterLongRange = 8;
        waterExtremeRange = 10;
        tonnage = .8;
        criticals = 3;
        bv = 111;
        cost = 60000;
        shortAV = 7;
        medAV = 7;
        maxRange = RANGE_MED;
        flags = flags.or(F_BURST_FIRE).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "258, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setClanAdvancement(2870, 2872, 2880, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false)
                .setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
    }
}
