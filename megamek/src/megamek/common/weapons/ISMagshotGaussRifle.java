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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISMagshotGaussRifle extends GaussWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 651029127510862887L;

    /**
     *
     */
    public ISMagshotGaussRifle() {
        super();
        techLevel = TechConstants.T_IS_ADVANCED;
        name = "Magshot";
        setInternalName("ISMagshotGR");
        heat = 1;
        damage = 2;
        ammoType = AmmoType.T_MAGSHOT;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 0.5f;
        criticals = 2;
        bv = 15;
        cost = 8500;
        explosionDamage = 3;
    }
}
