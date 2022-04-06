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
public class InfantryShotgunCeresCrowdbusterWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryShotgunCeresCrowdbusterWeapon() {
		super();

		name = "Ceres Arms Crowdbuster";
		setInternalName(name);
		addLookupName("InfantryCeresCrowdbuster");
		ammoType = AmmoType.T_INFANTRY;
		cost = 150;
		bv = 0.10;
		tonnage = .001;
		flags = flags.or(F_NO_FIRES).or(F_INF_NONPENETRATING).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.11;
		infantryRange = 0;
		ammoWeight = 0.0003;
		shots = 15;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3028, 3030, 3058, DATE_NONE, DATE_NONE)
				.setISApproximate(true, false, false, false, false)
				.setPrototypeFactions(F_CC)
				.setProductionFactions(F_CC).setTechRating(RATING_D)
				.setAvailability(RATING_X, RATING_F, RATING_D, RATING_C);

	}
}
