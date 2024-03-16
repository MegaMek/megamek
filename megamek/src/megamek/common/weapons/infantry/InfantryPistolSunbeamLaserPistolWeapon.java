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
public class InfantryPistolSunbeamLaserPistolWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryPistolSunbeamLaserPistolWeapon() {
		super();

		name = "Laser Pistol (Sunbeam)";
		setInternalName(name);
		addLookupName("InfantrySunbeamLaserpistol");
		addLookupName("Sunbeam Laser Pistol");
		ammoType = AmmoType.T_INFANTRY;
		cost = 750;
		bv = 0.86;
		tonnage = .001;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY);
		infantryDamage = 0.28;
		infantryRange = 1;
		ammoWeight = 0.0003;
		shots = 7;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3015, 3020, 3050, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
		        .setProductionFactions(F_FW).setTechRating(RATING_D)
		        .setAvailability(RATING_X, RATING_F, RATING_D, RATING_C);

	}
}
