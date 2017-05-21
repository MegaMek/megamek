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
public class InfantrySupportDragonsbaneDisposablePulseLaserWeapon extends InfantryWeapon {

	/**
	 *
	 */
	private static final long serialVersionUID = -3164871600230559641L;

	public InfantrySupportDragonsbaneDisposablePulseLaserWeapon() {
		super();

		name = "Pulse Laser (Dragonsbane Disposable)";
		setInternalName(name);
		addLookupName("InfantryDragonsbane");
		addLookupName("InfantryDragonsbanePulseLaser");
		addLookupName("Infantry Dragonsbane Disposable Pulse Laser");
		ammoType = AmmoType.T_NA;
		cost = 5000;
		bv = 5.08;
		flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY).or(F_PULSE).or(F_INF_ENCUMBER).or(F_INF_SUPPORT);
		infantryDamage = 0.49;
		infantryRange = 3;
		crew = 1;
		rulesRefs = "273, TM";
		tonnage = .007;
		techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3054, 3058, 3068, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setPrototypeFactions(F_DC)
		        .setProductionFactions(F_DC).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_F);

	}
}
