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
public class InfantryRiflePulseLaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRiflePulseLaserWeapon() {
		super();

		name = "Pulse Laser Rifle (Inner Sphere)";
		setInternalName(name);
		addLookupName("InfantryISPulseLaser");
		addLookupName("IS Pulse Laser Rifle");
		ammoType = AmmoType.T_NA;
		cost = 1000;
		bv = 0.76;
		tonnage = .005;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
		infantryDamage = 0.25;
		infantryRange = 1;
		ammoWeight = 0.0003;
		shots = 6;
		bursts = 1;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2678, 2680, 2700, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(2678, 2680, 2700, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
		        .setProductionFactions(F_TH).setTechRating(RATING_D)
		        .setAvailability(RATING_D, RATING_E, RATING_C, RATING_C);

	}
}
