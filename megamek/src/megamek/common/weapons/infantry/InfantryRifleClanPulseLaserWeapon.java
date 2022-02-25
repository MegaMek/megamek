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
public class InfantryRifleClanPulseLaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleClanPulseLaserWeapon() {
		super();

		name = "Pulse Laser Rifle [Clan]";
		setInternalName(name);
		addLookupName("InfantryClanPulseLaserRifle");
		addLookupName("Infantry Clan Pulse Laser Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 3000;
		bv = 1.69;
		tonnage = .005;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
		infantryDamage = 0.33;
		ammoWeight = 0.0003;
		shots = 8;
		bursts = 1;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(2833, 2835, DATE_NONE, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CGS)
		        .setProductionFactions(F_CGS).setTechRating(RATING_F)
		        .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C);

	}
}
