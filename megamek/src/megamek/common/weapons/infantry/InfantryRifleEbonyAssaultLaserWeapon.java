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
public class InfantryRifleEbonyAssaultLaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleEbonyAssaultLaserWeapon() {
		super();

		name = "Laser Rifle (Ebony Assault)";
		setInternalName(name);
		addLookupName("InfantryEbonyAssaultLaser");
		addLookupName("Infantry Ebony Assault Laser Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 8500;
		bv = 1.07;
		tonnage = .010;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
		infantryDamage = 0.21;
		infantryRange = 2;
		ammoWeight = 0.0003;
		shots = 3;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3056, 3063, DATE_NONE, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_MC)
		        .setProductionFactions(F_MC).setTechRating(RATING_F)
		        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);

	}
}
