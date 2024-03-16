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
public class InfantryRifleThunderstrokeIIWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleThunderstrokeIIWeapon() {
		super();

		name = "Gauss Rifle (Thunderstroke II)";
		setInternalName(name);
		addLookupName("InfantryTStroke2");
		addLookupName("InfantryThunderstroke2");
		addLookupName("InfantryThunderStrokeII");
		addLookupName("Thunderstroke II");
		ammoType = AmmoType.T_INFANTRY;
		cost = 3500;
		bv = 2.71;
		tonnage = .0065;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.53;
		infantryRange = 2;
		ammoWeight = 0.00042;
		ammoCost = 10;
		shots = 20;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3058, 3062, 3090, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_FS)
		        .setProductionFactions(F_FS).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D);

	}
}
