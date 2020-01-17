package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Ben Grills
 */

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

/**
 * @author Dave Nawton
 */
 
public class InfantryPistolSMGGHTSpec7aWeapon extends InfantryWeapon {

	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryPistolSMGGHTSpec7aWeapon() {
		super();

		name = "Machine Pistol (H-T Spec-7a)";
		setInternalName(name);
		ammoType = AmmoType.T_NA;
		cost = 190;
		bv = 0.23;
		tonnage = .0012;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.13;
		infantryRange = 0;
		rulesRefs = "176,HBHK";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(DATE_NONE, DATE_NONE, 2950, DATE_NONE, DATE_NONE)
				.setISApproximate(true, false, false, false, false)
				.setPrototypeFactions(F_DC)
				.setProductionFactions(F_DC).setTechRating(RATING_D)
				.setAvailability(RATING_X, RATING_X, RATING_D, RATING_C);

	}
}
