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
public class InfantryPistolMauserNeedlerPistolWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryPistolMauserNeedlerPistolWeapon() {
		super();

		name = "Needler Pistol (M&G Flechette)";
		setInternalName(name);
		addLookupName("InfantryMauserneedlerpistol");
		addLookupName("M&G Flechette Pistol");
		ammoType = AmmoType.T_INFANTRY;
		cost = 100;
		bv = 0.10;
		tonnage = .0005;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_INF_NONPENETRATING).or(F_BALLISTIC);
		infantryDamage = 0.11;
		infantryRange = 0;
		ammoWeight = 0.00017;
		ammoCost = 4;
		shots = 15;
		bursts = 3;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2350, 2360, 2400, DATE_NONE, DATE_NONE)
				.setISApproximate(true, false, false, false, false)
				.setClanAdvancement(2350, 2360, 2400, DATE_NONE, DATE_NONE)
				.setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
				.setProductionFactions(F_LC).setTechRating(RATING_D)
				.setAvailability(RATING_B, RATING_B, RATING_C, RATING_C);

	}
}
