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
public class InfantrySupportLaserUltraHeavyWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportLaserUltraHeavyWeapon() {
		super();

		name = "Support Laser (Ultra-Heavy)";
		setInternalName(name);
		addLookupName("InfantryUltraHeavyLaser");
		addLookupName("InfantryHeavyMediumLaser");
		addLookupName("Infantry Ultra-Heavy Support Laser");
		ammoType = AmmoType.T_INFANTRY;
		cost = 100000;
		bv = 20.31;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_LASER).or(F_INF_SUPPORT);
		infantryDamage = 1.72;
		infantryRange = 5;
		crew = 4;
		ammoWeight = 0.003;
		shots = 5;
		rulesRefs = "273, TM";
		tonnage = .250;
		techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3055, 3062, DATE_NONE, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CBR)
		        .setProductionFactions(F_CSA).setTechRating(RATING_F)
		        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D);

	}
}
