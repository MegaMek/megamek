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
public class InfantrySupportRecoillessRifleMediumWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportRecoillessRifleMediumWeapon() {
		super();

		name = "Recoilless Rifle (Medium)";
		setInternalName("InfantryMRR");
		addLookupName(name);
		addLookupName("InfantryMediumRecoillessRifle");
		addLookupName("Infantry Medium Recoilless Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 2000;
		bv = 2.71;
		tonnage = .032;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
		infantryDamage = 0.53;
		infantryRange = 2;
		crew = 2;
		ammoWeight = 0.002;
		ammoCost = 30;
		shots = 1;
		rulesRefs = " 273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
		        .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A);

	}
}
