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
 * @author Sebastian Brocks
 */
public class InfantrySupportPortableFlamerWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -5741978934100309295L;

	public InfantrySupportPortableFlamerWeapon() {
		super();

		name = "Flamer (Man-Portable)";
		setInternalName(name);
		addLookupName("InfantryPortableFlamer");
		addLookupName("Portable Flamer");
		addLookupName("Flamer (man-portable)");
		addLookupName("Flamer (Man-Pack)");
		ammoType = AmmoType.T_INFANTRY;
		cost = 100;
		bv = 0.50;
        tonnage = .015;
		flags = flags.or(F_DIRECT_FIRE).or(F_FLAMER).or(F_ENERGY).or(F_INF_SUPPORT).or(F_INF_ENCUMBER);
		infantryDamage = 0.55;
		infantryRange = 0;
		crew = 1;
		ammoWeight = 0.0084;
		ammoCost = 2;
		shots = 12;
		rulesRefs = " 273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setTechRating(RATING_C)
		        .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A);

	}
}
