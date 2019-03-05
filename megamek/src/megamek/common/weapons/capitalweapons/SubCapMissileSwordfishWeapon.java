/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons.capitalweapons;

import megamek.common.AmmoType;

/**
 * @author Jay Lawson
 */
public class SubCapMissileSwordfishWeapon extends SubCapMissileWeapon {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3827228773281489872L;

	/**
	 * 
	 */
	public SubCapMissileSwordfishWeapon() {
		super();
		this.name = "Sub-Capital Missile Launcher (Swordfish)";
		this.setInternalName(this.name);
		this.addLookupName("Swordfish");
		this.shortName = "Swordfish";
		this.heat = 15;
		this.damage = 4;
		this.ammoType = AmmoType.T_SWORDFISH;
		this.shortRange = 7;
		this.mediumRange = 14;
		this.longRange = 21;
		this.extremeRange = 28;
		this.tonnage = 140.0;
		this.bv = 317;
		this.cost = 110000;
		this.flags = flags.or(F_MISSILE);
		this.atClass = CLASS_CAPITAL_MISSILE;
		this.shortAV = 4;
        this.missileArmor = 40;
		this.maxRange = RANGE_SHORT;
        rulesRefs = "345,TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
            .setISAdvancement(3066, 3072, 3145, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false, false, false)
            .setClanAdvancement(DATE_NONE,DATE_NONE,3073,DATE_NONE,DATE_NONE)
            .setISApproximate(true, false, false, false, false)
            .setPrototypeFactions(F_WB)
            .setProductionFactions(F_WB);
		
	}
}
