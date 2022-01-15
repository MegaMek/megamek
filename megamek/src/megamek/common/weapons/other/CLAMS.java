/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.other;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;
import megamek.common.weapons.AmmoWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 25, 2004
 */
public class CLAMS extends AmmoWeapon {
	private static final long serialVersionUID = 7447941274169853546L;

	public CLAMS() {
		super();
		name = "Anti-Missile System";
		setInternalName("CLAntiMissileSystem");
		addLookupName("Clan Anti-Missile Sys");
		addLookupName("Clan AMS");
		addLookupName("ClanAMS");
		addLookupName("ClAMS");
		heat = 1;
		rackSize = 2;
		damage = 2; // for manual operation
		minimumRange = 0; 
		shortRange = 1;
		mediumRange = 1;
		longRange = 1;
		extremeRange = 1;
		maxRange = RANGE_SHORT;
		shortAV = 3;
		ammoType = AmmoType.T_AMS;
		tonnage = 0.5;
		criticals = 1;
		bv = 32;
		flags = flags.or(F_AUTO_TARGET).or(F_AMS).or(F_BALLISTIC).or(F_MECH_WEAPON).or(F_AERO_WEAPON).or(F_TANK_WEAPON)
		        .or(F_PROTO_WEAPON);
		setModes(new String[] { "On", "Off" });
		setInstantModeSwitch(false);
		cost = 100000;
		atClass = CLASS_AMS;
		rulesRefs = "204, TM";
		techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
		        .setAvailability(RATING_X, RATING_F, RATING_D, RATING_C)
		        .setClanAdvancement(2824, 2831, 2835, DATE_NONE, DATE_NONE)
		        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSA)
		        .setProductionFactions(F_CSA).setStaticTechLevel(SimpleTechLevel.STANDARD);
	}
}
