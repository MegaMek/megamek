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
public class InfantryPistolHoldoutNeedlerPistolWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryPistolHoldoutNeedlerPistolWeapon() {
		super();

		name = "Needler Pistol (Hold-Out)";
		setInternalName(name);
		addLookupName("InfantryHoldoutNeedlerpistol");
		addLookupName("Holdout Needler Pistol");
		ammoType = AmmoType.T_INFANTRY;
		cost = 20;
		bv = 0.10;
		tonnage = .00005;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_INF_NONPENETRATING).or(F_BALLISTIC);
		infantryDamage = 0.11;
		infantryRange = 0;
		ammoWeight = 0.00003;
		shots = 5;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2510, 2515, 2570, DATE_NONE, DATE_NONE)
				.setISApproximate(true, false, false, false, false)
				.setClanAdvancement(2510, 2515, 2570, DATE_NONE, DATE_NONE)
				.setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
				.setProductionFactions(F_TH).setTechRating(RATING_D)
				.setAvailability(RATING_C, RATING_B, RATING_B, RATING_A);

	}
}
