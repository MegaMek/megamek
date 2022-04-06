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
public class InfantryPistolSpitballGasPistolWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryPistolSpitballGasPistolWeapon() {
		super();

		name = "Pistol (Spitball Gas Weapon)";
		setInternalName(name);
		addLookupName("InfantrySpitballGaspistol");
		addLookupName("Spitball Gas Weapon");
		ammoType = AmmoType.T_INFANTRY;
		cost = 6;
		bv = 0;
		tonnage = .001;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.0;
		infantryRange = 0;
		ammoWeight = 0.00004;
		ammoCost = 2;
		shots = 25;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3056, 3059, 3100, DATE_NONE, DATE_NONE)
				.setISApproximate(true, false, false, false, false)
				.setPrototypeFactions(F_LC)
				.setProductionFactions(F_LC).setTechRating(RATING_C)
				.setAvailability(RATING_X, RATING_X, RATING_C, RATING_C);

	}
}
