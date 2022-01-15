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
public class InfantryRifleMagnaLaserWeapon extends InfantryWeapon {
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleMagnaLaserWeapon() {
		super();

		name = "Laser Rifle (Magna)";
		setInternalName(name);
		addLookupName("InfantryMagnaLaser");
		addLookupName("Magna Laser Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 1500;
		bv = 1.33;
		tonnage = .006;
		infantryRange = 2;
		ammoWeight = 0.0003;
		shots = 6;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
		infantryDamage = 0.26;
		rulesRefs = "273,TM";
		techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(2460, 2465, 2500, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(2460, 2465, 2500, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_DC)
		        .setProductionFactions(F_DC)
                .setTechRating(RATING_D)
		        .setAvailability(RATING_C, RATING_C, RATING_D, RATING_D);
	}
}
