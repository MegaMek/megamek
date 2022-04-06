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
public class InfantrySupportWireGuidedMissileWeapon extends InfantryWeapon {
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportWireGuidedMissileWeapon() {
		super();

		name = "Wire-Guided Missile Launcher";
		setInternalName(name);
		addLookupName("InfantryWireGuidedMissileLauncher");
		addLookupName("WireGuidedMissileLauncher");
		ammoType = AmmoType.T_INFANTRY;
		cost = 800000;
		tonnage = 0.095;
		bv = 0.00;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE).or(F_INF_SUPPORT);
		infantryDamage = 1.08;
		infantryRange = 2;
		toHitModifier = -2;
		crew = 4;
		ammoWeight = 0.022;
		ammoCost = 2500;
		shots = 1;
		rulesRefs = "195, AToW-C";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
		        .setAvailability(RATING_E, RATING_F, RATING_X, RATING_X);
	}
}
