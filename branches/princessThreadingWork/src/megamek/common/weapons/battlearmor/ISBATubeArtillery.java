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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.weapons.ArtilleryWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISBATubeArtillery extends ArtilleryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2803991494958411097L;

    /**
     *
     */
    public ISBATubeArtillery() {
        super();
        techLevel.put(3075, TechConstants.T_IS_ADVANCED);
        name = "BA Tube Artillery";
        setInternalName("ISBATubeArtillery");
        rackSize = 3;
        ammoType = AmmoType.T_BA_TUBE;
        shortRange = 2;
        mediumRange = 2;
        longRange = 2;
        extremeRange = 2; // No extreme range.
        tonnage = 0.5f;
        criticals = 4;
        bv = 27;
        cost = 200000;
        techRating = RATING_B;
        introDate = 3075;
        flags = flags.or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON);
        availRating = new int[] { RATING_C, RATING_C, RATING_C };

    }

}
