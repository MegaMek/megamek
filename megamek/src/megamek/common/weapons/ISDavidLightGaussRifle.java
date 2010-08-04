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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISDavidLightGaussRifle extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -4247046315958528324L;

    /**
     *
     */
    public ISDavidLightGaussRifle() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "David Light Gauss Rifle";
        setInternalName(name);
        addLookupName("ISDavidLightGaussRifle");
        damage = 1;
        ammoType = AmmoType.T_NA;
        shortRange = 3;
        mediumRange = 5;
        longRange = 8;
        extremeRange = 10;
        bv = 7;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        cost = 22500;
    }
}
