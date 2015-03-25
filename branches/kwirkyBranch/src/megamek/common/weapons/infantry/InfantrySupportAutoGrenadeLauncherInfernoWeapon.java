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
public class InfantrySupportAutoGrenadeLauncherInfernoWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportAutoGrenadeLauncherInfernoWeapon() {
        super();
        techLevel.put(3071,TechConstants.T_ALLOWED_ALL);
        name = "Grenade Launcher (Auto) - Inferno";
        setInternalName(name);
        addLookupName("InfantryAutoGLInferno");
        addLookupName("Infantry Inferno Auto Grenade Launcher");
        ammoType = AmmoType.T_NA;
        cost = 975;
        bv = 1.25;
        flags = flags.or(F_INFERNO).or(F_BALLISTIC).or(F_INF_SUPPORT);
        infantryDamage = 0.41;
        infantryRange = 1;
        crew = 1;
        introDate = 2100;
        techLevel.put(2100,techLevel.get(3071));
        availRating = new int[]{RATING_B,RATING_B,RATING_B};
        techRating = RATING_C;
    }
}