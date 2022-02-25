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

/**
 * @author Ben Grills
 */
public class InfantryPistolCoventryHandrocketGyrojetPistolWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryPistolCoventryHandrocketGyrojetPistolWeapon() {
        super();

        name = "Gyrojet Pistol (Coventry Handrocket)";
        setInternalName(name);
        addLookupName("InfantryCoventryHandrocketGyrojetpistol");
        addLookupName("Coventry Handrocket");
        ammoType = AmmoType.T_INFANTRY;
        cost = 250;
        bv = 0.43;
        tonnage = .0031;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.14;
        infantryRange = 1;
        ammoWeight = 0.00024;
        ammoCost = 3;
        shots = 5;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3050, 3052, 3090, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC, F_FS).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_C);

    }
}
