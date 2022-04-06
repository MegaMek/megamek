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
public class InfantrySupportPortablePlasmaWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -5741978934100309295L;

	public InfantrySupportPortablePlasmaWeapon() {
		super();

		name = "Plasma Rifle (Man-Portable)";
		setInternalName(name);
		addLookupName("InfantryPlasmaRifle");
		addLookupName("InfantryPlasmaPortable");
		addLookupName("InfantryMPPR");
		addLookupName("Portable Plasma Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 7500;
		bv = 8.08;
		flags = flags.or(F_DIRECT_FIRE).or(F_PLASMA).or(F_BALLISTIC).or(F_INF_SUPPORT).or(F_INF_ENCUMBER);
		String[] modeStrings = { "Damage", "Heat" };
		setModes(modeStrings);
		infantryDamage = 1.58;
		infantryRange = 2;
		crew = 1;
		ammoWeight = 0.018;
		ammoCost = 10;
		shots = 10;
		rulesRefs = "273, TM";
		tonnage = .030;
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3063, 3065, 3075, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_CC)
		        .setProductionFactions(F_CC).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_C);

	}
}
