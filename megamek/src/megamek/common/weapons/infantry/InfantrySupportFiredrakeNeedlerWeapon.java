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
public class InfantrySupportFiredrakeNeedlerWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportFiredrakeNeedlerWeapon() {
		super();

		name = "Needler, Support (Firedrake)";
		setInternalName(name);
		addLookupName("InfantryFiredrake");
		addLookupName("InfantrySupportNeedler");
		addLookupName("Infantry Firedrake Needler");
		ammoType = AmmoType.T_INFANTRY;
		cost = 500;
		bv = 6.13;
		flags = flags.or(F_INCENDIARY_NEEDLES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
		String[] modeStrings = { "Damage", "Heat" };
		setModes(modeStrings);
		infantryDamage = 1.2;
		infantryRange = 1;
		crew = 2;
		ammoWeight = 0.003;
		ammoCost = 10;
		shots = 30;
		bursts = 3;
		tonnage = .025;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3058, 3061, 3075, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_LC)
		        .setProductionFactions(F_LC).setTechRating(RATING_D)
		        .setAvailability(RATING_X, RATING_X, RATING_C, RATING_C);

	}
}
