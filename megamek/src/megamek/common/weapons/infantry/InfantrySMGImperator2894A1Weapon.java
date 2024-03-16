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
public class InfantrySMGImperator2894A1Weapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySMGImperator2894A1Weapon() {
		super();

		name = "SMG (Imperator 2894A1)";
		setInternalName(name);
		addLookupName("InfantryImperator2894A1");
		addLookupName("Imperator 2894A1 SMG");
		ammoType = AmmoType.T_INFANTRY;
		cost = 100;
		bv = 0.20;
		tonnage = .004;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.22;
		infantryRange = 0;
		ammoWeight = 0.00038;
		ammoCost = 5;
		shots = 50;
		bursts = 5;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2838, 2842, 2900, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_FW)
		        .setProductionFactions(F_FW).setTechRating(RATING_C)
		        .setAvailability(RATING_X, RATING_C, RATING_B, RATING_C);

	}
}
