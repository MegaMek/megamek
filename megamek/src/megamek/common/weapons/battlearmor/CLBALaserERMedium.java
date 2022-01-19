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

import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class CLBALaserERMedium extends LaserWeapon {
    private static final long serialVersionUID = -2063816167191977118L;

    public CLBALaserERMedium() {
        super();
        name = "ER Medium Laser";
        setInternalName("CLBAERMediumLaser");
        addLookupName("Clan BA ER Medium Laser");
        sortingName = "Laser ER C";
        heat = 5;
        damage = 7;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 7;
        waterLongRange = 10;
        waterExtremeRange = 14;
        tonnage = .8;
        criticals = 3;
        bv = 108;
        cost = 80000;
        shortAV = 7;
        medAV = 7;
        maxRange = RANGE_MED;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "258, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setClanAdvancement(2872, 2875, 2880, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false)
                .setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF);
    }
}
