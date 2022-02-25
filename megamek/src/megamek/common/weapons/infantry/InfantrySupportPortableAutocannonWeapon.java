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
public class InfantrySupportPortableAutocannonWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportPortableAutocannonWeapon() {
		super();

		name = "Autocannon (Semi-Portable)";
		setInternalName(name);
		addLookupName("InfantryPortableAutocannon");
		addLookupName("InfantrySemiPortableAutocannon");
		addLookupName("Infantry Semi Portable Autocannon");
		ammoType = AmmoType.T_INFANTRY;
		cost = 2000;
		bv = 2.35;
        tonnage = .025;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
		infantryDamage = 0.77;
		infantryRange = 1;
		crew = 2;
		ammoWeight = 0.008;
		ammoCost = 150;
		shots = 200;
		bursts = 8;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2100, 2255, 2300, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(2100, 2255, 2300, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setProductionFactions(F_TA)
		        .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_C);

	}
}
