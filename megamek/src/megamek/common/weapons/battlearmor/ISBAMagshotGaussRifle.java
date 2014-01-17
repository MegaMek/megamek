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
package megamek.common.weapons.battlearmor;

import megamek.common.TechConstants;
import megamek.common.weapons.Weapon;

/**
 * @author Sebastian Brocks
 */
public class ISBAMagshotGaussRifle extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 870812653880382979L;

    /**
     *
     */
    public ISBAMagshotGaussRifle() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "BA Magshot";
        setInternalName("ISBAMagshotGR");
        damage = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 0.175f;
        criticals = 3;
        bv = 15;
        cost = 10500;
        flags = flags.or(F_NO_FIRES).or(F_BALLISTIC).or(F_DIRECT_FIRE)
                .or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 3059;
        techLevel.put(3059, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_E;
    }
}
