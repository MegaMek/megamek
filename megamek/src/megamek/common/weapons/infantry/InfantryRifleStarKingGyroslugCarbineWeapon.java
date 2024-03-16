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
public class InfantryRifleStarKingGyroslugCarbineWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleStarKingGyroslugCarbineWeapon() {
		super();

		name = "Gyroslug Carbine (Star King)";
		setInternalName(name);
		addLookupName("InfantryStarKing");
		addLookupName("Star King Gyroslug Carbine");
		ammoType = AmmoType.T_INFANTRY;
		cost = 950;
		bv = 1.07;
		tonnage = .008;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.35;
		infantryRange = 1;
		ammoWeight = 0.0023;
		ammoCost = 15;
		shots = 20;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2775, 2789, 2825, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_LC)
		        .setProductionFactions(F_LC).setTechRating(RATING_D)
		        .setAvailability(RATING_X, RATING_C, RATING_D, RATING_C);

	}
}
