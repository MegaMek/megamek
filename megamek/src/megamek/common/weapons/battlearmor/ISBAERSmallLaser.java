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

import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import megamek.common.weapons.LaserWeapon;

/**
 * @author Andrew Hunter
 */
public class ISBAERSmallLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -4997798107691083605L;

    /**
     *
     */
    public ISBAERSmallLaser() {
        super();
         name = "ER Small Laser";
        setInternalName("ISBAERSmallLaser");
        addLookupName("IS BA ER Small Laser");
        damage = 3;
        shortRange = 2;
        mediumRange = 4;
        longRange = 5;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 3;
        waterExtremeRange = 4;
        tonnage = 0.35f;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        bv = 17;
        cost = 11250;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        introDate = 3050;
        techLevel.put(3050, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3058, TechConstants.T_IS_ADVANCED);
        techLevel.put(3062, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_D ,RATING_C};
        techRating = RATING_E;
        rulesRefs = "258, TM";

    }
}
