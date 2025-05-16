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
public class InfantrySupportHeavyGrenadeLauncherWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportHeavyGrenadeLauncherWeapon() {
		super();

		name = "Grenade Launcher (Heavy)";
		setInternalName("InfantryHeavyGrenadeLauncher");
		addLookupName(name);
		addLookupName("Infantry Heavy Grenade Launcher");
		ammoType = AmmoType.T_INFANTRY;
		cost = 1500;
		bv = 5.38;
		flags = flags.or(F_NO_FIRES).or(F_BALLISTIC).or(F_INF_ENCUMBER).or(F_INF_SUPPORT);
		infantryDamage = 1.76;
		infantryRange = 1;
		crew = 1;
		ammoWeight = 0.012;
		ammoCost = 320;
		shots = 20;
		bursts = 4;
		tonnage = .018;
		rulesRefs = "273, TM";
		techAdvancement.setTechBase(TechBase.IS).setISAdvancement(3049, 3050, 3057, DATE_NONE, DATE_NONE)
				.setISApproximate(true, false, false, false, false)
				.setPrototypeFactions(Faction.FS, Faction.LC)
				.setProductionFactions(Faction.FS, Faction.LC).setTechRating(TechRating.C)
				.setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C);

	}
}
