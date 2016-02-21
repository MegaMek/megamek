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
public class InfantryGrenadeInfernoWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryGrenadeInfernoWeapon() {
        super();
        techLevel.put(3071,TechConstants.T_ALLOWED_ALL);
        name = "Grenade (Inferno)";
        setInternalName(name);
        addLookupName("InfantryInfernoGrenade");
        addLookupName("Inferno Grenades");
        ammoType = AmmoType.T_NA;
        cost = 16;
        bv = 0.17;
        flags = flags.or(F_INFERNO).or(F_BALLISTIC).or(F_INF_SUPPORT);
        String[] modeStrings = { "Damage", "Heat" };
        setModes(modeStrings);
        infantryDamage = 0.19;
        infantryRange = 0;
        introDate = 2380;
        availRating = new int[]{RATING_A,RATING_A,RATING_A};
        techRating = RATING_C;
    }
}
