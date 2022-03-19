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
public class InfantrySMGGuntherMP20Weapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySMGGuntherMP20Weapon() {
		super();

		name = "SMG (Gunther MP-20)";
		setInternalName(name);
		addLookupName("InfantryGuntherMP20");
		addLookupName("Gunther MP-20");
		ammoType = AmmoType.T_INFANTRY;
		cost = 125;
		bv = 0.30;
		tonnage = .0025;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.33;
		infantryRange = 0;
		ammoWeight = 0.00034;
		ammoCost = 5;
		shots = 30;
		bursts = 6;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3005, 3007, 3025, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_LC)
		        .setProductionFactions(F_LC).setTechRating(RATING_C)
		        .setAvailability(RATING_X, RATING_E, RATING_C, RATING_C);

	}
}
