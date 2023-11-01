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
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

/**
 * @author Ben Grills
 */
public class InfantrySupportMortarHeavyInfernoWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportMortarHeavyInfernoWeapon() {
		super();

		name = "Mortar (Heavy) - Inferno";
		setInternalName("InfantryHeavyMortarInferno");
		addLookupName(name);
		addLookupName("Infantry Heavy Inferno Mortar");
		ammoType = AmmoType.T_INFANTRY;
		cost = 5000;
		bv = 2.44;
		tonnage = .220;
		flags = flags.or(F_INFERNO).or(F_BALLISTIC).or(F_INF_SUPPORT);
		String[] modeStrings = { "Damage", "Heat" };
		setModes(modeStrings);
		infantryDamage = 0.34;
		ammoCost = 32;
 		infantryRange = 3;
		crew = 3;
		ammoWeight = 0.004;
		shots = 1;
		rulesRefs = " 273, TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setTechRating(RATING_B)
		        .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C);

	}

	@Override
	public void adaptToGameOptions(GameOptions gOp) {
		super.adaptToGameOptions(gOp);

		// Indirect Fire
		if (gOp.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
			addMode(MODE_MISSILE_INDIRECT);
			addMode(MODE_INDIRECT_HEAT);
		} else {
			removeMode(MODE_MISSILE_INDIRECT);
			removeMode(MODE_INDIRECT_HEAT);
		}
	}
}
