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
public class InfantrySupportOneShotMRMInfernoWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportOneShotMRMInfernoWeapon() {
		super();

		name = "MRM Launcher w/Inferno";
		setInternalName("InfantryOneShotMRMInferno");
		addLookupName(name);
		addLookupName("InfantryInfernoMRM");
		addLookupName("InfantryOneShotInfernoMRM");
		addLookupName("Infantry One-Shot MRM Inferno Launcher");
		ammoType = AmmoType.T_INFANTRY;
		cost = 2500;
		bv = 1.33;
		tonnage = .03;
		flags = flags.or(F_DIRECT_FIRE).or(F_INFERNO).or(F_MISSILE).or(F_INF_SUPPORT);
		String[] modeStrings = { "Damage", "Heat" };
		setModes(modeStrings);
		infantryDamage = 0.26;
		infantryRange = 2;
		crew = 1;
		ammoWeight = 0.005;
		ammoCost = 21;
		shots = 1;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3063, 3065, 3075, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_DC)
		        .setProductionFactions(F_DC).setTechRating(RATING_D)
		        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);

	}
}
