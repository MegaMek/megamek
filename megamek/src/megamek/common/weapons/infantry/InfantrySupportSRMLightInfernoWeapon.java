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
public class InfantrySupportSRMLightInfernoWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = 7788576728727248931L;

	public InfantrySupportSRMLightInfernoWeapon() {
		super();

		name = "SRM Launcher (Light) - Inferno";
		setInternalName("InfantrySRMLightInferno");
		addLookupName(name);
		addLookupName("Light SRM (Inferno)");
		sortingName = "SRM Launcher BI";
		ammoType = AmmoType.T_INFANTRY;
		cost = 1500;
		bv = 1.74;
		flags = flags.or(F_DIRECT_FIRE).or(F_INFERNO).or(F_MISSILE).or(F_INF_SUPPORT);
		infantryDamage = 0.34;
		infantryRange = 2;
		ammoWeight = 0.009;
		ammoCost = 300;
		shots = 2;
		String[] modeStrings = { "Damage", "Heat" };
		setModes(modeStrings);
		rulesRefs = "273,TM";
		tonnage = .010;
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
		        .setProductionFactions(F_TH).setTechRating(RATING_C)
		        .setAvailability(RATING_C, RATING_C, RATING_D, RATING_C);

	}
}
