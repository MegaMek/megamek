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
public class InfantrySupportHellboreAssaultLaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportHellboreAssaultLaserWeapon() {
		super();

		name = "Laser (Hellbore Assault)";
		setInternalName(name);
		addLookupName("InfantryHellbore");
		addLookupName("InfantryHellboreAssaultLaser");
		addLookupName("Infantry Hellbore Assault Laser");
		ammoType = AmmoType.T_INFANTRY;
		cost = 2500;
		bv = 3.22;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_INF_ENCUMBER).or(F_ENERGY).or(F_LASER).or(F_INF_SUPPORT);
		infantryDamage = 0.63;
		infantryRange = 2;
		crew = 1;
		tonnage = .020;
		ammoWeight = 0.003;
		shots = 15;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3054, 3058, 3062, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_CS)
		        .setProductionFactions(F_CS).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);

	}
}
