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

/**
 * @author Sebastian Brocks
 */
public class InfantryTWFlamerWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -5741978934100309295L;

	public InfantryTWFlamerWeapon() {
		super();
		// Range 1, reduced damage flamer for TW Platoon support

		name = "Total Warfare Flamer";
		setInternalName(name);
		addLookupName("InfantryTWFlamer");
		addLookupName("InfantryTWPortableFlamer");
		cost = 100;
		bv = 0.36;
		flags = flags.or(F_DIRECT_FIRE).or(F_FLAMER).or(F_ENERGY).or(F_INF_ENCUMBER).or(F_INF_SUPPORT);
		infantryDamage = 0.35;
		infantryRange = 1;
		crew = 1;
		// This Weapon has been replaced by the Flamer Pistol. Moving to
		// Unofficial.
		techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2100, 2100, 2100, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(2100, 2100, 2100, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
		        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B);

	}
}
