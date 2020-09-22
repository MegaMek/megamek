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
public class InfantryRifleClanMauserIICIASWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantryRifleClanMauserIICIASWeapon() {
		super();

		name = "Laser Rifle (Mauser IIC IAS)";
		setInternalName(name);
		addLookupName("InfantryClanMauserIICIAS");
		addLookupName("Infantry Clan Mauser IIC");
		ammoType = AmmoType.T_INFANTRY;
		tonnage = 0.0012;
		cost = 18000;
		bv = 9.82;
		tonnage = .012;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_INF_ENCUMBER).or(F_INF_BURST);
		/*Errata
		 * https://bg.battletech.com/forums/index.php?topic=60038.msg1377699#msg1377699
		 * No Primary Infantry Weapon may have a Damage Value greater than 0.60. 
		 * If the weapon selected has a Damage Value greater than 0.60, then reduce it's Damage Value to 0.60 
		 * when determining final damage values (pg.152). Platoons that have their primary weapon damage reduced 
		 * in this way automatically gain the Heavy Burst Weapon special feature
		*/
		infantryDamage = .6; //was 1.37
		infantryRange = 3;
		ammoWeight = 0.0003;
		shots = 6;
		rulesRefs = "273,TM";
		techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3013, 3015, DATE_NONE, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
		        .setProductionFactions(F_CHH).setTechRating(RATING_F)
		        .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D);

	}
}
