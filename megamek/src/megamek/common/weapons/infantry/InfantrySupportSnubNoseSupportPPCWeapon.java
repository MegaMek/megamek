/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Ben Grills
 * @since Sep 7, 2005
 */
public class InfantrySupportSnubNoseSupportPPCWeapon extends InfantryWeapon {
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportSnubNoseSupportPPCWeapon() {
		super();

		name = "Support PPC (Snub-Nose)";
		setInternalName(name);
		addLookupName("InfantrySnubNoseSupportPPC");
		addLookupName("SnubNoseSupportPPC");
		ammoType = AmmoType.T_INFANTRY;
		cost = 60000;
		bv = 0.0;
        tonnage = 1.6;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_PPC).or(F_INF_SUPPORT);
		infantryDamage = 1.58;
		infantryRange = 5;
		crew = 3;
		ammoWeight = 0.025;
		shots = 150;
		rulesRefs = "176, AToW-C";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3075, 3082, 3090, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_DC)
		        .setProductionFactions(F_DC).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);
	}
}
