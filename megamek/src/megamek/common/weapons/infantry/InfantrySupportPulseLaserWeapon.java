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
public class InfantrySupportPulseLaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportPulseLaserWeapon() {
		super();

		name = "Support Pulse Laser";
		setInternalName(name);
		addLookupName("InfantrySupportPulseLaser");
		addLookupName("Infantry Support Pulse Laser");
		addLookupName("InfantrySmallPulseLaser");
		ammoType = AmmoType.T_INFANTRY;
		cost = 16000;
		bv = 5.81;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_PULSE).or(F_INF_BURST).or(F_INF_SUPPORT);
		infantryDamage = 0.81;
		infantryRange = 3;
		crew = 2;
		ammoWeight = 0.003;
		shots = 21;
		bursts = 4;
		rulesRefs = "273, TM";
		tonnage = .150;
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2605, 2610, 2650, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(2605, 2610, 2650, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
		        .setProductionFactions(F_TH).setTechRating(RATING_E)
		        .setAvailability(RATING_D, RATING_E, RATING_D, RATING_C);

	}
}
