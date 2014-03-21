/**
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISSmallLaserPrimitive extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -7637928016342153078L;

    public ISSmallLaserPrimitive() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Small Laser Prototype";
        setInternalName(name);
        addLookupName("ISSmall Laser Prototype");
        addLookupName("ISSmallLaserPrototype");
        heat = 2;
        damage = 3;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 4;
        tonnage = 0.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 9;
        cost = 11250;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
    }
}
