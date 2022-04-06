/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Ben Grills
 * @since Sep 7, 2005
 */
public class InfantryGrenadeInfernoWeapon extends InfantryWeapon {
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryGrenadeInfernoWeapon() {
        super();

        name = "Grenade (Inferno)";
        // I can find no reference to a Thrown Inferno Grenade. Moving these to Unofficial.
        // Hammer Feb 2017

        setInternalName("InfantryGrenadeInferno");
        addLookupName(name);
        addLookupName("InfantryInfernoGrenade");
        addLookupName("Inferno Grenades");
        ammoType = AmmoType.T_NA;
        cost = 16;
        bv = 0.17;
        tonnage = .0006;
        flags = flags.or(F_INFERNO).or(F_BALLISTIC).or(F_INF_SUPPORT);
        String[] modeStrings = { "Damage", "Heat" };
        setModes(modeStrings);
        infantryDamage = 0.19;
        infantryRange = 0;
        // very hackish - using some data from Inferno Fuel.
        rulesRefs = "272, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_C, RATING_B);
    }
}
