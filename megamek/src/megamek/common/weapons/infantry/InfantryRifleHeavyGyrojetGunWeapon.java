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
public class InfantryRifleHeavyGyrojetGunWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleHeavyGyrojetGunWeapon() {
		super();

		name = "Gyrojet Gun (Heavy)";
		setInternalName(name);
		addLookupName("InfantryHeavyGyrojetGun");
		addLookupName("Heavy Gyrojet Gun");
		ammoType = AmmoType.T_INFANTRY;
		cost = 2500;
		bv = 1.07;
		tonnage = .010;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.21;
		infantryRange = 2;
		ammoWeight = 0.001;
		ammoCost = 250;
		shots = 5;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2585, 2590, 2600, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(2585, 2590, 2600, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
		        .setProductionFactions(F_LC).setTechRating(RATING_D)
		        .setAvailability(RATING_C, RATING_D, RATING_C, RATING_C);

	}
}
