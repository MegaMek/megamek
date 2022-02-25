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
public class InfantrySupportClanERLaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportClanERLaserWeapon() {
		super();

		name = "Support Laser (ER, Clan)";
		setInternalName(name);
		addLookupName("InfantryClanERLaser");
		addLookupName("InfantryClanERSmallLaser");
		addLookupName("Infantry Clan ER Laser");
		ammoType = AmmoType.T_INFANTRY;
		cost = 11250;
		bv = 10.27;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_LASER).or(F_INF_SUPPORT);
		infantryDamage = 1.05;
		infantryRange = 4;
		crew = 2;
		ammoWeight = 0.003;
		shots = 10;
		tonnage = .100;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(2824, 2825, DATE_NONE, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
		        .setProductionFactions(F_CHH).setTechRating(RATING_F)
		        .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C);

	}
}
