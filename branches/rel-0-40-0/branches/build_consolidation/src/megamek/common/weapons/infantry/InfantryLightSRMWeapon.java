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
 * @author Sebastian Brocks
 */
public class InfantryLightSRMWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5311681183178942222L;

    public InfantryLightSRMWeapon() {
        super();
        techLevel.put(3071,TechConstants.T_ALLOWED_ALL);
        name = "Light SRM Launcher";
        setInternalName(name);
        addLookupName("InfantrySRMLight");
        addLookupName("InfantrySRM");
        ammoType = AmmoType.T_SRM;
        cost = 1500;
        bv = 2.91;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE).or(F_INF_SUPPORT);
        infantryDamage = 0.57;
        infantryRange = 2;
        introDate = 2370;
        techLevel.put(2370,techLevel.get(3071));
        availRating = new int[]{RATING_C,RATING_C,RATING_D};
        techRating = RATING_C;
    }
}