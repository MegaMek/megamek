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
public class InfantrySupportMk2PortableAAWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportMk2PortableAAWeapon() {
		super();

		name = "AA Weapon (Mk. 2, Man-Portable)";
		setInternalName(name);
		addLookupName("InfantryMk2PortableAA");
		addLookupName("Infantry Mk 2 Man-Portable AA Weapon");
		ammoType = AmmoType.T_INFANTRY;
		cost = 3500;
		bv = 4.14;
		tonnage = .035;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE).or(F_INF_AA).or(F_INF_SUPPORT);
		infantryDamage = 0.81;
		infantryRange = 2;
		ammoWeight = 0.014;
		ammoCost = 1000;
		shots = 4;
		crew = 2;
		damage = 1;
		minimumRange = 0;
		shortRange = 2;
		mediumRange = 4;
		longRange = 6;
		extremeRange = 8;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2498, 2510, 2590, 2790, 3056)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(2498, 2510, 2590, DATE_NONE, 3056)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
		        .setProductionFactions(F_TH).setReintroductionFactions(F_FW).setTechRating(RATING_D)
		        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_D);

	}
}
