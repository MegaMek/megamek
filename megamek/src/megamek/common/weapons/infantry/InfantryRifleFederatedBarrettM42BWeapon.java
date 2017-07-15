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
public class InfantryRifleFederatedBarrettM42BWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleFederatedBarrettM42BWeapon() {
        super();

        name = "Rifle (Federated-Barrett M42B)";
        setInternalName(name);
        addLookupName("InfantryFederatedBarrettM42B");
        addLookupName("Federated Barrett M42B");
        ammoType = AmmoType.T_NA;
        cost = 1385;
        bv = 3.12;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_BURST);
        infantryDamage = 1.02;
        infantryRange = 1;
        rulesRefs = "273,TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3060, 3064, 3095, DATE_NONE, DATE_NONE)
        .setISApproximate(true, false, false, false, false)
        .setPrototypeFactions(F_FS)
        .setProductionFactions(F_FS).setTechRating(RATING_C)
        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_C);
    }
}
