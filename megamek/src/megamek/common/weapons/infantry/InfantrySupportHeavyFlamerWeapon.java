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
 * @author Sebastian Brocks
 */
public class InfantrySupportHeavyFlamerWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -5741978934100309295L;

	public InfantrySupportHeavyFlamerWeapon() {
		super();

		name = "Flamer (Heavy)";
		setInternalName(name);
		addLookupName("InfantryHeavyFlamer");
		ammoType = AmmoType.T_INFANTRY;
		// Flamer (Heavy), TM p. 300
		cost = 200;
		bv = 0.72;
		tonnage = .025;
		flags = flags.or(F_DIRECT_FIRE).or(F_FLAMER).or(F_ENERGY).or(F_INF_SUPPORT);
		infantryDamage = 0.79;
		infantryRange = 0;
		crew = 2;
		ammoWeight = 0.0029;
		ammoCost = 200;
		shots = 3;
		rulesRefs = " 273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2100, 2100, 2100, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(2100, 2100, 2100, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
		        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B);

	}
}
