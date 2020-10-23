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
public class InfantryRifleMauser960LaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleMauser960LaserWeapon() {
		super();

		name = "Laser Rifle (Mauser 960)";
		setInternalName(name);
		addLookupName("InfantryMauser960");
		addLookupName("Mauser 960 Assault System");
		ammoType = AmmoType.T_NA;
		cost = 8000;
		bv = 4.75;
		tonnage = .0108;
		ammoWeight = 0.0003;
		shots = 15;
		bursts = 2;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
		infantryDamage = 0.93;
		infantryRange = 2;
		rulesRefs = "273,TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2698, 2700, 2710, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(2698, 2700, 2710, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
		        .setProductionFactions(F_TH).setTechRating(RATING_E)
		        .setAvailability(RATING_C, RATING_F, RATING_D, RATING_E);

	}
}
