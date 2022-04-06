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
public class InfantrySupportRecoillessRifleLightInfernoWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportRecoillessRifleLightInfernoWeapon() {
		super();

		name = "Recoilless Rifle (Light) - Inferno";
		setInternalName("InfantryLRRInferno");
		addLookupName(name);
		addLookupName("InfantryInfernoLRR");
		addLookupName("InfantryLightRecoillessRifleInferno");
		addLookupName("Infantry Inferno Light Recoilless Rifle");
		ammoType = AmmoType.T_INFANTRY;
		cost = 300;
		bv = 0.97;
		tonnage = .008;
		flags = flags.or(F_INFERNO).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_ENCUMBER).or(F_INF_SUPPORT);
		String[] modeStrings = { "Damage", "Heat" };
		setModes(modeStrings);
		infantryDamage = 0.19;
		infantryRange = 2;
		crew = 1;
		ammoWeight = 0.0006;
		ammoCost = 20;
		shots = 1;
		rulesRefs = " 273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
		        .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A);

	}
}
