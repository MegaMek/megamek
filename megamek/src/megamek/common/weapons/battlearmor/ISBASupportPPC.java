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
public class ISBASupportPPC extends PPCWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -993141316216102914L;

    /**
     *
     */
    public ISBASupportPPC() {
        super();
        name = "Support PPC";
        setInternalName("ISBASupportPPC");
        addLookupName("IS BA Support PPC");
        damage = 2;
        ammoType = AmmoType.T_NA;
        shortRange = 2;
        mediumRange = 5;
        longRange = 7;
        extremeRange = 10;
        tonnage = 0.25f;
        criticals = 2;
        flags = flags.or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);;
        bv = 12;
        setModes(new String[] { "Field Inhibitor ON", "Field Inhibitor OFF" });
        introDate = 3046;
        techLevel.put(3046, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3053, TechConstants.T_IS_ADVANCED);
        techLevel.put(3056, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X ,RATING_F ,RATING_D ,RATING_C};
        techRating = RATING_D;
        rulesRefs = "267, TM";
    }
}
