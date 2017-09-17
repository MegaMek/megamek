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
public class InfantrySupportSRMStandardWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportSRMStandardWeapon() {
		super();

		name = "SRM Launcher (Std, Two-Shot)";
		setInternalName(name);
		addLookupName("InfantryStandardSRM");
		addLookupName("Infantry2ShotSRM");
		addLookupName("Infantry Two-Shot SRM Launcher");
		ammoType = AmmoType.T_NA;
		cost = 1500;
		bv = 5.83;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE).or(F_INF_ENCUMBER).or(F_INF_SUPPORT);
		infantryDamage = 1.14;
		infantryRange = 2;
		crew = 1;
		tonnage = .030;
		rulesRefs = "273,TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
		        .setProductionFactions(F_TH).setTechRating(RATING_C)
		        .setAvailability(RATING_C, RATING_C, RATING_D, RATING_C);

	}
}
