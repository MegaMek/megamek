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
public class InfantryRifleVSPLaserWeapon extends InfantryWeapon {
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleVSPLaserWeapon() {
		super();

		name = "Variable-Pulse Laser Rifle";
		setInternalName(name);
		addLookupName("InfantryVSPRifle");
		addLookupName("VariablePulseLaserRifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 4500;
		tonnage = 0.006;
		bv = 0.0;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
		infantryDamage = 0.33;
		infantryRange = 1;
		ammoWeight = 0.0003;
		shots = 6;
		bursts = 1;
		rulesRefs = "195, AToW-C";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3075, 3077, 3085, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setPrototypeFactions(F_FS)
		        .setProductionFactions(F_FS).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);
	}
}
