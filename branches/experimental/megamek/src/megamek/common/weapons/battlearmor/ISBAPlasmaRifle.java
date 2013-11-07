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
package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.weapons.Weapon;

/**
 * @author Sebastian Brocks
 */
public class ISBAPlasmaRifle extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 4885473724392214253L;

    /**
     *
     */
    public ISBAPlasmaRifle() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Plasma Rifle";
        setInternalName(name);
        addLookupName("ISBAPlasmaRifle");
        damage = 2;
        ammoType = AmmoType.T_NA;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        bv = 12;
        tonnage = 0.3f;
        criticals = 2;
        cost = 28000;
        flags = flags.or(F_BA_WEAPON).or(F_DIRECT_FIRE).or(F_ENERGY)
                .or(F_PLASMA);
        introDate = 3065;
        techLevel.put(3065, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_D };
        techRating = RATING_E;
    }
}
