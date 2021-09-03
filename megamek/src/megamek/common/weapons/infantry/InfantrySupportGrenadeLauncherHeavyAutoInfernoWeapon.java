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
public class InfantrySupportGrenadeLauncherHeavyAutoInfernoWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportGrenadeLauncherHeavyAutoInfernoWeapon() {
		super();

		name = "Grenade Launcher (Heavy Auto) w/Inferno";
		setInternalName("InfantryHeavyAutoGrenadeLauncherInferno");
		addLookupName(name);
		addLookupName("Infantry Inferno Heavy Auto Grenade Launcher");
		ammoType = AmmoType.T_INFANTRY;
		cost = 4500;
		bv = 2.93;
		tonnage = .020;
		flags = flags.or(F_INFERNO).or(F_BALLISTIC).or(F_INF_ENCUMBER).or(F_INF_SUPPORT);
		infantryDamage = 0.96;
		infantryRange = 1;
		crew = 1;
		ammoWeight = 0.012;
		ammoCost = 400;
		shots = 20;
		bursts = 3;
		tonnage = .020;
		rulesRefs = "273,TM";
		techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(2896, 2900, DATE_NONE, DATE_NONE, DATE_NONE)
				.setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF, F_CHH)
				.setProductionFactions(F_CSF).setTechRating(RATING_D)
				.setAvailability(RATING_X, RATING_X, RATING_D, RATING_D);

	}
}
