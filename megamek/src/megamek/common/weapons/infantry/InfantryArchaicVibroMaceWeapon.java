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
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Ben Grills
 */
public class InfantryArchaicVibroMaceWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryArchaicVibroMaceWeapon() {
        super();

        name = "Club (Vibro-Mace)";
        setInternalName(name);
        addLookupName("InfantryVibroMace");
        addLookupName("IS Vibro Mace");
        ammoType = AmmoType.T_NA;
        cost = 300;
        bv = 0.24;
        flags = flags.or(F_NO_FIRES).or(F_INF_POINT_BLANK).or(F_INF_ARCHAIC);
        infantryDamage = 0.26;
        infantryRange = 0;
        introDate = 2995;
        techLevel.put(2995, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3045, TechConstants.T_IS_ADVANCED);
        techLevel.put(3100, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X,RATING_F ,RATING_E ,RATING_E};
        techRating = RATING_E;
        rulesRefs = "272, TM";
    }
}
