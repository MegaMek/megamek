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
public class InfantrySupportMGPortableWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = 3434311797513896108L;

	public InfantrySupportMGPortableWeapon() {
		super();

		name = "Machine Gun (Portable)";
		setInternalName(name);
		addLookupName("InfantryMGPortable");
		addLookupName("Infantry MG Portable");
		addLookupName("InfantryMG");
		ammoType = AmmoType.T_INFANTRY;
		cost = 1000;
		bv = 1.99;
		tonnage = .0115;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_BURST).or(F_INF_SUPPORT)
		        .or(F_INF_ENCUMBER);
		infantryDamage = 0.65;
		infantryRange = 1;
		ammoWeight = 0.003;
		ammoCost = 10;
		shots = 75;
		bursts = 5;
		damage = 1;
		minimumRange = 0;
		shortRange = 1;
		mediumRange = 2;
		longRange = 3;
		extremeRange = 4;
		rulesRefs = " 273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
		        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B);

	}
}
