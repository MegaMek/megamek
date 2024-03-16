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
public class InfantrySupportClanBearhunterAutocannonWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportClanBearhunterAutocannonWeapon() {
        super();

        name = "Autocannon (Bearhunter Superheavy)";
        setInternalName(name);
        addLookupName("InfantryBearhunter");
        addLookupName("InfantryBearhunterAutocannon");
        addLookupName("Infantry Bearhunter Super-Heavy Autocannon");
        ammoType = AmmoType.T_INFANTRY;
        cost = 3000;
        bv = 2.13;
        tonnage = 0.040;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_BURST).or(F_INF_SUPPORT);
        infantryDamage = 2.33;
        infantryRange = 0;
        crew = 2;
        ammoWeight = 0.009;
        ammoCost = 200;
        shots = 180;
        bursts = 6;
        rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_CLAN)
		        .setClanAdvancement(3059, 3062, DATE_NONE, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
		        .setProductionFactions(F_CHH).setTechRating(RATING_D)
		        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D);

    }
}
