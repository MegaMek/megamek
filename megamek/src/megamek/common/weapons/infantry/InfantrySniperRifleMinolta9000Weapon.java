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
public class InfantrySniperRifleMinolta9000Weapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySniperRifleMinolta9000Weapon() {
		super();

		name = "Sniper Rifle (Minolta 9000 Advanced Sniper System)";
		setInternalName(name);
		addLookupName("InfantryMinolta9000");
		addLookupName("Minolta 9000 Advanced Sniper System");
		addLookupName("Rifle (Minolta 9000 Advanced Sniper System)");
		ammoType = AmmoType.T_INFANTRY;
		cost = 1000;
		bv = 1.79;
		tonnage = .006;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.35;
		infantryRange = 2;
		ammoWeight = 0.00012;
		ammoCost = 5;
		shots = 10;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3052, 3055, 3100, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_CC)
		        .setProductionFactions(F_CC).setTechRating(RATING_D)
		        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);

	}
}
