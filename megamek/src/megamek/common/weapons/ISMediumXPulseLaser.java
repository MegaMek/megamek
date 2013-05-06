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
 * Created on Sep 8, 2005
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISMediumXPulseLaser extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -6576828912486084151L;

    /**
     *
     */
    public ISMediumXPulseLaser() {
        super();
        techLevel.put(3071,TechConstants.T_IS_EXPERIMENTAL);
        name = "Medium X-Pulse Laser";
        setInternalName("ISMediumXPulseLaser");
        addLookupName("IS X-Pulse Med Laser");
        addLookupName("IS Medium X-Pulse Laser");
        heat = 6;
        damage = 6;
        toHitModifier = -2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 3;
        waterLongRange = 4;
        waterExtremeRange = 6;
        maxRange = RANGE_SHORT;
        shortAV = 6;
        tonnage = 2.0f;
        criticals = 1;
        bv = 71;
        cost = 110000;
        techRating = RATING_E;
        availRating = new int[]{RATING_X, RATING_X, RATING_E};
        introDate = 3057;
        techLevel.put(3057,techLevel.get(3071));
    }
}
