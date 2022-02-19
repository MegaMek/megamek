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
public class InfantryGrenadeMicroWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryGrenadeMicroWeapon() {
		super();

		name = "Grenade (Micro)";
		setInternalName(name);
		addLookupName("InfantryMicroGrenade");
		addLookupName("Micro Grenades");
		ammoType = AmmoType.T_NA;
		cost = 2;
		bv = 0.15;
		tonnage = .0002;
		flags = flags.or(F_NO_FIRES).or(F_BALLISTIC).or(F_INF_SUPPORT);
		infantryDamage = 0.16;
		infantryRange = 0;
		rulesRefs = "272, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
				.setISApproximate(false, false, false, false, false)
				.setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
				.setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
				.setAvailability(RATING_B, RATING_C, RATING_C, RATING_B);

	}
}
