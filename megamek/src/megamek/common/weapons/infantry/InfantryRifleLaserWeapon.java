/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Sebastian Brocks
 * @since Sep 7, 2005
 */
public class InfantryRifleLaserWeapon extends InfantryWeapon {
	private static final long serialVersionUID = -9065123199493897216L;

	public InfantryRifleLaserWeapon() {
		super();

		name = "Laser Rifle";
		setInternalName(name);
		addLookupName("InfantryLaserRifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 1250;
		bv = 1.43;
		tonnage = .005;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
		infantryDamage = 0.28;
		infantryRange = 2;
		ammoWeight = 0.0003;
		shots = 6;
		rulesRefs = " 273,TM";
		techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(2100, 2230, 2300, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(2100, 2230, 2300, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false)
                .setProductionFactions(F_TA)
		        .setTechRating(RATING_D).setAvailability(RATING_C, RATING_B, RATING_B, RATING_B);
	}
}
