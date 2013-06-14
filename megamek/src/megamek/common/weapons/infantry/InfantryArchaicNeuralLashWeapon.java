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
public class InfantryArchaicNeuralLashWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryArchaicNeuralLashWeapon() {
        super();
        techLevel.put(3071,TechConstants.T_ALLOWED_ALL);
        name = "Neural Lash";
        setInternalName(name);
        addLookupName("InfantryNeuralLash");
        ammoType = AmmoType.T_NA;
        cost = 750;
        bv = 0.08;
        flags = flags.or(F_NO_FIRES).or(F_INF_NONPENETRATING).or(F_INF_POINT_BLANK).or(F_INF_ARCHAIC);
        infantryDamage = 0.09;
        infantryRange = 0;
        introDate = 2200;
        techLevel.put(2200,techLevel.get(3071));
        availRating = new int[]{RATING_E,RATING_E,RATING_D};
        techRating = RATING_D;
    }
}
