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
public class InfantrySupportClanSemiPortablePulseLaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportClanSemiPortablePulseLaserWeapon() {
		super();

		name = "Support Pulse Laser (Semi-Portable)";
		setInternalName(name);
		addLookupName("InfantryClanSemiPortablePulseLaser");
		addLookupName("InfantryClanMicroPulseLaser");
		addLookupName("Infantry Clan Semi-Portable Pulse Laser");
		ammoType = AmmoType.T_INFANTRY;
		cost = 12500;
		bv = 2.81;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_PULSE).or(F_INF_SUPPORT);
		infantryDamage = 0.55;
		infantryRange = 2;
		crew = 2;
		ammoWeight = 0.003;
		shots = 25;
		bursts = 2;
		rulesRefs = "273, TM";
		tonnage = .040;
		techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3056, 3059, DATE_NONE, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
		        .setProductionFactions(F_CSJ).setTechRating(RATING_F)
		        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D);

	}
}
