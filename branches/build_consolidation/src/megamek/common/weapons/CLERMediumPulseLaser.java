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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLERMediumPulseLaser extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 7816191920104768204L;

    /**
     *
     */
    public CLERMediumPulseLaser() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        name = "ER Medium Pulse Laser";
        setInternalName("CLERMediumPulseLaser");
        addLookupName("Clan ER Pulse Med Laser");
        addLookupName("Clan ER Medium Pulse Laser");
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
        tonnage = 2.0f;
        criticals = 2;
        bv = 117;
        cost = 150000;
        techRating = RATING_F;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3057;
        techLevel.put(3057, techLevel.get(3071));
        techLevel.put(3082, TechConstants.T_CLAN_ADVANCED);
    }
}
