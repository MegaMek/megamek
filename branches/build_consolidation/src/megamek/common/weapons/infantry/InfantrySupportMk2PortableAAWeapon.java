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
public class InfantrySupportMk2PortableAAWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportMk2PortableAAWeapon() {
        super();
        techLevel.put(3071,TechConstants.T_ALLOWED_ALL);
        name = "Infantry Mk 2 Man-Portable AA Weapon";
        setInternalName(name);
        addLookupName("InfantryMk2PortableAA");
        ammoType = AmmoType.T_NA;
        cost = 3500;
        bv = 4.14;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE).or(F_INF_AA).or(F_INF_SUPPORT);
        infantryDamage = 0.81;
        infantryRange = 2;
        crew = 2;
        damage = 1;
        minimumRange = 0;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        introDate = 2510;
        techLevel.put(2510,techLevel.get(3071));
        extinctDate = 2790;
        reintroDate = 3058;
        availRating = new int[]{RATING_E,RATING_F,RATING_D};
        techRating = RATING_D;
    }
}