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
public class InfantryRifleClanMauserIICIASInfernoWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleClanMauserIICIASInfernoWeapon() {
		super();

		name = "Laser Rifle (Mauser IIC IAS) (Inferno Grenades)";
		setInternalName(name);
		addLookupName("InfantryClanMauserIICIASInferno");
		addLookupName("Infantry Clan Mauser IIC Inferno");
		ammoType = AmmoType.T_INFANTRY;
		tonnage = 0.0012;
		cost = 18000;
		bv = 6.45;
		tonnage = .012;
		flags = flags.or(F_INFERNO).or(F_INF_BURST).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_INF_ENCUMBER);
		infantryDamage = 0.90;
		infantryRange = 3;
		ammoWeight = 0.0003;
		shots = 6;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3013, 3015, DATE_NONE, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
		        .setProductionFactions(F_CHH).setTechRating(RATING_F)
		        .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D);

	}
}
