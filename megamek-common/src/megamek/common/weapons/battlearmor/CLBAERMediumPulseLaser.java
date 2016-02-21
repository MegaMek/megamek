/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
import megamek.common.weapons.PulseLaserWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLBAERMediumPulseLaser extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 7816191920104768204L;

    /**
     *
     */
    public CLBAERMediumPulseLaser() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        name = "ER Medium Pulse Laser";
        setInternalName("BACLERMediumPulseLaser");
        addLookupName("CLBAERMediumPulseLaser");
        addLookupName("BA Clan ER Pulse Med Laser");
        addLookupName("BA Clan ER Medium Pulse Laser");
        heat = 6;
        damage = 7;
        toHitModifier = -1;
        shortRange = 5;
        mediumRange = 9;
        longRange = 14;
        extremeRange = 18;
        waterShortRange = 3;
        waterMediumRange = 5;
        waterLongRange = 8;
        waterExtremeRange = 10;
        tonnage = .8f;
        criticals = 4;
        bv = 117;
        cost = 150000;
        techRating = RATING_F;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3059;
        techLevel.put(3059, techLevel.get(3071));
        techLevel.put(3082, TechConstants.T_CLAN_ADVANCED);
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
    }
}
