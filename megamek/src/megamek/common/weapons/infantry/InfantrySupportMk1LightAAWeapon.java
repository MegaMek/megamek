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
public class InfantrySupportMk1LightAAWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportMk1LightAAWeapon() {
		super();

		name = "AA Weapon (Mk. 1, Light)";
		setInternalName(name);
		addLookupName("InfantryMk1LightAA");
		addLookupName("Infantry Mk 1 Light AA Weapon");
		ammoType = AmmoType.T_NA;
		cost = 1000;
		bv = 0.70;
		tonnage = .005;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE).or(F_INF_AA).or(F_INF_SUPPORT);
		infantryDamage = 0.23;
		infantryRange = 1;
		crew = 1;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2490, 2500, 2590, 2790, 3056)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(2490, 2500, 2590, DATE_NONE, 3056)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
		        .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC).setTechRating(RATING_D)
		        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_D);

	}
}
