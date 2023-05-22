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
public class InfantrySniperRifleRadiumLaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySniperRifleRadiumLaserWeapon() {
		super();

		name = "Sniper Rifle (Radium Sniper)";
		setInternalName(name);
		addLookupName("InfantryRadiumSniper");
		addLookupName("Radium Sniper Rifle");
		addLookupName("Rifle (Radium Sniper)");
		ammoType = AmmoType.T_INFANTRY;
		cost = 9500;
		bv = 2.58;
		tonnage = .012;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
		infantryDamage = 0.36;
		infantryRange = 3;
		ammoWeight = 0.00033;
		ammoCost = 650;
		shots = 5;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2582, 2583, DATE_NONE, 2607, 3062)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_TC)
		        .setProductionFactions(F_TC).setReintroductionFactions(F_TC).setTechRating(RATING_E)
		        .setAvailability(RATING_F, RATING_X, RATING_F, RATING_F);

	}
}
