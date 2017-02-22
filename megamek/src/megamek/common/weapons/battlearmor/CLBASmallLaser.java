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
import megamek.common.TechProgression;
import megamek.common.weapons.LaserWeapon;


/**
 * @author Andrew Hunter
 */
public class CLBASmallLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -6475366872597851742L;

    public CLBASmallLaser() {
        super();
        name = "Small Laser";
        setInternalName("CLBASmall Laser");
        addLookupName("CL BA Small Laser");
        addLookupName("CLBASmallLaser");
        heat = 1;
        damage = 3;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 4;
        tonnage = 0.2f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        bv = 9;
        cost = 11250;
        atClass = CLASS_POINT_DEFENSE;
        introDate = 2860;
        techLevel.put(2860, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(2868, TechConstants.T_CLAN_ADVANCED);
        techLevel.put(2870, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X ,RATING_C ,RATING_B ,RATING_B};
        techRating = RATING_E;
        rulesRefs = "258, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        techProgression.setClanProgression(2860, 2868, 2870);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_X, RATING_C, RATING_B, RATING_B });
    }
}
