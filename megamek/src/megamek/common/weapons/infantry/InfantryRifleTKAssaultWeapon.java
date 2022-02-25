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
public class InfantryRifleTKAssaultWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleTKAssaultWeapon() {
		super();

		name = "Rifle (TK Assault)";
		setInternalName(name);
		addLookupName("InfantryTKAssaultRifle");
		addLookupName("TK Assault Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 150;
		bv = 1.34;
		tonnage = .0055;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.44;
		infantryRange = 1;
		ammoWeight = 0.00032;
		ammoCost = 3;
		shots = 20;
		bursts = 2;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2865, 2870, 2925, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_LC)
		        .setProductionFactions(F_LC).setTechRating(RATING_C)
		        .setAvailability(RATING_X, RATING_C, RATING_A, RATING_B);

	}
}
