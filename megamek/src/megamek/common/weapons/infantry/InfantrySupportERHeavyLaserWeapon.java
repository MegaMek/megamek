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
public class InfantrySupportERHeavyLaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportERHeavyLaserWeapon() {
		super();

		name = "Support Laser (ER Heavy, IS)";
		setInternalName(name);
		addLookupName("InfantryERHeavyLaser");
		addLookupName("InfantryERMediumLaser");
		addLookupName("Infantry ER Heavy Laser");
		ammoType = AmmoType.T_INFANTRY;
		cost = 80000;
		bv = 13.88;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_LASER).or(F_INF_SUPPORT);
		infantryDamage = 1.05;
		infantryRange = 6;
		crew = 4;
		ammoWeight = 0.003;
		shots = 7;
		damage = 1;
		minimumRange = 0;
		shortRange = 6;
		mediumRange = 12;
		longRange = 18;
		extremeRange = 24;
		rulesRefs = "273, TM";
		tonnage = .250;
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3054, 3060, 3075, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_FW)
		        .setProductionFactions(F_FW).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);

	}
}
