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
public class InfantrySupportRecoillessRifleHeavyInfernoWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportRecoillessRifleHeavyInfernoWeapon() {
		super();

		name = "Recoilless Rifle (Heavy) - Inferno";
		setInternalName("InfantryHRRInferno");
		addLookupName(name);
		addLookupName("InfantryInfernoHRR");
		addLookupName("InfantryHeavyRecoillessRifleInferno");
		addLookupName("Infantry Inferno Heavy Recoilless Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 3000;
		bv = 1.74;
		tonnage = .060;
		flags = flags.or(F_INFERNO).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
		String[] modeStrings = { "Damage", "Heat" };
		setModes(modeStrings);
		infantryDamage = 0.34;
		infantryRange = 2;
		crew = 3;
		ammoWeight = 0.004;
		ammoCost = 40;
		shots = 1;
		tonnage = .060;
		rulesRefs = " 273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
		        .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A);

	}
}
