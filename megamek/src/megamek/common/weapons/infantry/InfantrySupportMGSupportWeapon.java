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
public class InfantrySupportMGSupportWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportMGSupportWeapon() {
		super();

		name = "Machine Gun (Support)";
		setInternalName(name);
		addLookupName("InfantryHMG");
		addLookupName("InfantrySupportMG");
		addLookupName("InfantrySupportMachineGun");
		ammoType = AmmoType.T_INFANTRY;
		cost = 1750;
		bv = 4.80;
		tonnage = .044;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_BURST).or(F_INF_SUPPORT);
		infantryDamage = 0.94;
		crew = 2;
		infantryRange = 2;
		ammoWeight = 0.005;
		ammoCost = 50;
		shots = 100;
		bursts = 5;
		damage = 1;
		minimumRange = 0;
		shortRange = 2;
		mediumRange = 4;
		longRange = 6;
		extremeRange = 8;
		rulesRefs = " 273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
		        .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C);

	}
}
