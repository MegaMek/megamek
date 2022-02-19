/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Ben Grills
 * @since Sep 7, 2005
 */
public class InfantrySupportVintageMiniGunWeapon extends InfantryWeapon {
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportVintageMiniGunWeapon() {
		super();

		name = "Vintage MiniGun";
		setInternalName(name);
		addLookupName("InfantryVintageMiniGun");
		addLookupName("VintageMinGun");
		ammoType = AmmoType.T_INFANTRY;
		cost = 50000;
		tonnage = .021;
		bv = 0.0;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
		infantryDamage = 0.81;
		infantryRange = 2;
		crew = 2;
		ammoWeight = 0.012;
		ammoCost = 600;
		shots = 500;
		bursts = 10;
		rulesRefs = "195, AToW-C";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
		        .setAvailability(RATING_E, RATING_E, RATING_E, RATING_F);
	}
}
