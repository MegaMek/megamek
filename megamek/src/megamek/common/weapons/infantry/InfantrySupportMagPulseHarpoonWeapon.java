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
 * TODO
 * Any vehicular unit (including battle armor, ProtoMechs, Combat Vehicles and
 * BattleMechs) successfully struck by a shot from a MagPulse harpoon gun will
 * suffer electronic interference sufficient to cause a -1 roll modifier
 * for all Gunnery and Sensor Operations Skill Checks by its pilot for 10
 * seconds (1 Total Warfare combat turn), in addition to any physical damage the
 * weapon delivers. These effects are noncumulative, and are not enhanced by
 * multiple harpoon hits at the same time.
 *
 * @author Ben Grills
 * @since Sep 7, 2005
 */
public class InfantrySupportMagPulseHarpoonWeapon extends InfantryWeapon {
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportMagPulseHarpoonWeapon() {
		super();

		name = "Mag-Pulse Harpoon Gun";
		setInternalName(name);
		addLookupName("InfantryMagpulseHarpoonGun");
		addLookupName("MagpulseHarpoonGun");
		ammoType = AmmoType.T_INFANTRY;
		cost = 12000;
		bv = 1.47;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE).or(F_INF_SUPPORT);
		infantryDamage = 0.37;
		infantryRange = 3;
		crew = 2;
		ammoWeight = 0.022;
		ammoCost = 1000;
		shots = 1;
		tonnage = .095;
		rulesRefs = "176, AToW-C";
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3070, 3079, 3100, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_FW)
		        .setProductionFactions(F_FW).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);
	}
}
