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
public class InfantryRifleImperatorAX22AssaultWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleImperatorAX22AssaultWeapon() {
		super();

		name = "Rifle (Imperator AX-22 Assault)";
		setInternalName(name);
		addLookupName("InfantryImperatorAX22");
		addLookupName("Imperator AX-22 Assault Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 200;
		bv = 1.59;
		tonnage = .0035;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.52;
		infantryRange = 1;
		ammoWeight = 0.00024;
		ammoCost = 3;
		shots = 15;
		bursts = 1;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3052, 3060, 3085, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_FW)
		        .setProductionFactions(F_FW).setTechRating(RATING_C)
		        .setAvailability(RATING_X, RATING_X, RATING_B, RATING_B);

	}
}
