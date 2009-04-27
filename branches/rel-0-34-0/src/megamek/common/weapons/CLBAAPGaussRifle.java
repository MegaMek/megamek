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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLBAAPGaussRifle extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 3055904827702262063L;

    /**
     *
     */
    public CLBAAPGaussRifle() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "AP Gauss Rifle";
        setInternalName("CLBAAPGaussRifle");
        heat = 1;
        damage = 3;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 0.5f;
        criticals = 1;
        bv = 21;
        cost = 8500;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        explosionDamage = 3;
        flags |= F_BA_WEAPON;
    }
}
