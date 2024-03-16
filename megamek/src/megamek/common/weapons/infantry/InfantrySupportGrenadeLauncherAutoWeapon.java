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
public class InfantrySupportGrenadeLauncherAutoWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportGrenadeLauncherAutoWeapon() {
		super();

		name = "Grenade Launcher (Auto)";
		setInternalName("InfantryAutoGL");
		addLookupName(name);
		addLookupName("Infantry Auto Grenade Launcher");
		ammoType = AmmoType.T_INFANTRY;
		cost = 975;
		bv = 4.55;
		tonnage = .012;
		flags = flags.or(F_NO_FIRES).or(F_BALLISTIC).or(F_INF_SUPPORT);
		infantryDamage = 1.49;
		infantryRange = 1;
		crew = 1;
		ammoWeight = 0.009;
		ammoCost = 160;
		shots = 20;
		bursts = 4;
		rulesRefs = " 273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2100, 2100, 2100, DATE_NONE, DATE_NONE)
				.setISApproximate(false, false, false, false, false)
				.setClanAdvancement(2100, 2100, 2100, DATE_NONE, DATE_NONE)
				.setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
				.setAvailability(RATING_B, RATING_B, RATING_B, RATING_B);

	}
}
