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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Jason Tighe
 */
public class CLImprovedHeavyMediumLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 4467522144065588079L;

    /**
     *
     */
    public CLImprovedHeavyMediumLaser() {
        super();
        techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        name = "Improved Heavy Medium Laser";
        setInternalName("CLImprovedMediumHeavyLaser");
        addLookupName("Clan Improved Heavy Medium Laser");
        addLookupName("CLImprovedHeavyMediumLaser");
        heat = 7;
        damage = 10;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 5;
        waterLongRange = 8;
        waterExtremeRange = 10;
        tonnage = 1.0f;
        criticals = 2;
        bv = 93;
        cost = 150000;
        shortAV = 10;
        maxRange = RANGE_SHORT;
        explosionDamage = 5;
        explosive = true;
    }
}
