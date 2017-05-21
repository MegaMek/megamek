/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 2, 2004
 *
 */
package megamek.common.weapons.battlearmor;

import megamek.common.TechAdvancement;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Jay Lawson
 */
public class ISBAMediumLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 2178224725694704541L;

    public ISBAMediumLaser() {
        super();
        name = "Medium Laser";
        setInternalName("ISBAMediumLaser");
        addLookupName("IS BA Medium Laser");
        damage = 5;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 4;
        waterLongRange = 6;
        waterExtremeRange = 8;
        tonnage = 0.5;
        criticals = 3;
        bv = 46;
        cost = 40000;
        shortAV = 5;
        flags = flags.or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        maxRange = RANGE_SHORT;
        rulesRefs = "258, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(3045, 3050, 3050);
        techAdvancement.setIntroLevel(true);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_B, RATING_B });
    }
}
