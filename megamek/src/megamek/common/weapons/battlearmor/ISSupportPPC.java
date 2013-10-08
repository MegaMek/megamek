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
import megamek.common.weapons.PPCWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISSupportPPC extends PPCWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -993141316216102914L;

    /**
     *
     */
    public ISSupportPPC() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Support PPC";
        setInternalName(name);
        addLookupName("ISSupportPPC");
        damage = 2;
        ammoType = AmmoType.T_NA;
        shortRange = 2;
        mediumRange = 5;
        longRange = 7;
        extremeRange = 10;
        tonnage = 0.25f;
        criticals = 2;
        flags = flags.or(F_BA_WEAPON);
        bv = 12;
        setModes(new String[] { "Field Inhibitor ON", "Field Inhibitor OFF" });
        cost = 14000;
        introDate = 2436;
        techLevel.put(2436, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_D };
        techRating = RATING_D;
    }
}
