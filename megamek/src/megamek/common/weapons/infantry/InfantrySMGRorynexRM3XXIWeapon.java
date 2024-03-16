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
public class InfantrySMGRorynexRM3XXIWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySMGRorynexRM3XXIWeapon() {
		super();

		name = "SMG (Rorynex RM-3/XXI)";
		setInternalName(name);
		addLookupName("InfantryRorynexRM3XXI");
		addLookupName("Rorynex RM-3/XXI");
		ammoType = AmmoType.T_INFANTRY;
		cost = 80;
		bv = 0.18;
		tonnage = .003;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.20;
		infantryRange = 0;
		ammoWeight = 0.00076;
		ammoCost = 10;
		shots = 100;
		bursts = 6;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2655, 2660, 2663, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(2655, 2660, 2663, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
		        .setProductionFactions(F_TH).setTechRating(RATING_D)
		        .setAvailability(RATING_C, RATING_B, RATING_C, RATING_D);

	}
}
