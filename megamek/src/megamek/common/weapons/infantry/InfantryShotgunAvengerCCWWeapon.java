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
public class InfantryShotgunAvengerCCWWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryShotgunAvengerCCWWeapon() {
		super();

		name = "Shotgun (Avenger CCW)";
		setInternalName(name);
		addLookupName("InfantryAvengerCCW");
		addLookupName("Avenger Crowd Control Weapon");
		ammoType = AmmoType.T_INFANTRY;
		cost = 345;
		bv = 0.30;
		tonnage = .0055;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
		infantryDamage = 0.33;
		infantryRange = 0;
		ammoWeight = 0.0004;
		ammoCost = 4;
		shots = 15;
		bursts = 5;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3019, 3020, DATE_NONE, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSV)
		        .setProductionFactions(F_CSV).setTechRating(RATING_C)
		        .setAvailability(RATING_X, RATING_E, RATING_C, RATING_D);

	}
}
