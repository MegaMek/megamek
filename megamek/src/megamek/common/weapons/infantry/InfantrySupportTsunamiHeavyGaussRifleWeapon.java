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
public class InfantrySupportTsunamiHeavyGaussRifleWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportTsunamiHeavyGaussRifleWeapon() {
		super();

		name = "Gauss Rifle (Tsunami Heavy)";
		setInternalName(name);
		addLookupName("InfantryTsunamiHeavyGaussRifle");
		addLookupName("InfantryTsunamiGauss");
		addLookupName("Infantry Tsunami Heavy Gauss Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 5500;
		bv = 3.22;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_ENCUMBER).or(F_INF_SUPPORT);
		infantryDamage = 0.63;
		infantryRange = 2;
		crew = 1;
		tonnage = .0125;
		ammoWeight = 0.0045;
		ammoCost = 5;
		shots = 5;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3053, 3056, 3068, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_DC)
		        .setProductionFactions(F_DC).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_E);

	}
}
