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
public class InfantrySMGClanGaussWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySMGClanGaussWeapon() {
		super();

		name = "Gauss Submachinegun";
		setInternalName(name);
		addLookupName("InfantryGaussSMG");
		addLookupName("InfantryClanGaussSMG");
		ammoType = AmmoType.T_INFANTRY;
		cost = 2000;
		bv = 1.38;
		tonnage = .0045;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.45;
		infantryRange = 1;
		ammoWeight = 0.00052;
		ammoCost = 10;
		shots = 30;
		bursts = 10;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3051, 3055, 3060, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
		        .setProductionFactions(F_CSF).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D);

	}
}
