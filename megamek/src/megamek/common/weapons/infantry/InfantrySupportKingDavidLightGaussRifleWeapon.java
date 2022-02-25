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
public class InfantrySupportKingDavidLightGaussRifleWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportKingDavidLightGaussRifleWeapon() {
		super();

		name = "Gauss Rifle, Light (King David)";
		setInternalName(name);
		addLookupName("InfantryKingDavid");
		addLookupName("InfantryKingDavidLightGaussRifle");
		addLookupName("Infantry King David Light Gauss Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 8000;
		bv = 4.87;
		tonnage = .03;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
		infantryDamage = 0.68;
		infantryRange = 3;
		crew = 2;
		tonnage = .030;
		ammoWeight = 0.016;
		ammoCost = 200;
		shots = 80;
		bursts = 16;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3060, 3063, 3075, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_FW)
		        .setProductionFactions(F_FW).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);

	}
}
