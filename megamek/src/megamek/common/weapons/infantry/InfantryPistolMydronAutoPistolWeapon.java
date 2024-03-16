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
public class InfantryPistolMydronAutoPistolWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryPistolMydronAutoPistolWeapon() {
		super();

		name = "Auto-Pistol (Mydron)";
		setInternalName(name);
		addLookupName("InfantryMydronAutopistol");
		addLookupName("Mydron Auto Pistol");
		ammoType = AmmoType.T_INFANTRY;
		cost = 100;
		bv = 0.13;
		tonnage = .0015;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.14;
		infantryRange = 0;
		ammoWeight = 0.00014;
		ammoCost = 4;
		shots = 20;
		bursts = 4;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2607, 2612, 2700, DATE_NONE, DATE_NONE)
				.setISApproximate(true, false, false, false, false)
				.setClanAdvancement(2607, 2612, 2700, DATE_NONE, DATE_NONE)
				.setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
				.setProductionFactions(F_FS).setTechRating(RATING_C)
				.setAvailability(RATING_C, RATING_B, RATING_B, RATING_B);

	}
}
