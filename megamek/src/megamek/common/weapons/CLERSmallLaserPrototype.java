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
 * @author Andrew Hunter
 */
public class CLERSmallLaserPrototype extends LaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3560080195438539835L;

    /**
     *
     */
    public CLERSmallLaserPrototype() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "ER Small Laser (CP)";
        setInternalName("CLERSmallLaserPrototype");
        heat = 2;
        damage = 3;
        shortRange = 2;
        mediumRange = 4;
        longRange = 5;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 3;
        waterExtremeRange = 4;
        tonnage = 0.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 17;
        cost = 11250;
        shortAV = 3;
        maxRange = RANGE_SHORT;
    }
}
