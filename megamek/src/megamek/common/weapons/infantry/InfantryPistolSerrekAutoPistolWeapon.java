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
public class InfantryPistolSerrekAutoPistolWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryPistolSerrekAutoPistolWeapon() {
		super();

		name = "Auto-Pistol (Serrek 7875D)";
		setInternalName(name);
		addLookupName("InfantrySerrekAutopistol");
		addLookupName("Serrek 7875D AutoPistol");
		ammoType = AmmoType.T_INFANTRY;
		cost = 185;
		bv = 0.15;
        tonnage = .0004;
        ammoWeight = 0.00016;
		ammoCost = 3;
        shots = 16;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.16;
		infantryRange = 0;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2805, 2810, 2900, DATE_NONE, DATE_NONE)
				.setISApproximate(true, false, false, false, false)
				.setPrototypeFactions(F_FS)
				.setProductionFactions(F_FS).setTechRating(RATING_C)
				.setAvailability(RATING_X, RATING_C, RATING_B, RATING_B);

	}
}
