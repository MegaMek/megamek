/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 12, 2004
 *
 */
package megamek.common.weapons;

/**
 * @author Andrew Hunter
 *
 */
public class CLHeavyLargeLaser extends LaserWeapon {
	/**
	 * 
	 */
	public CLHeavyLargeLaser() {
		super();
		this.name = "Heavy Large Laser";
        this.setInternalName("CLHeavyLargeLaser");
        this.addLookupName("Clan Large Heavy Laser");
        this.heat = 18;
        this.damage = 16;
        this.toHitModifier = 1;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.tonnage = 4.0f;
        this.criticals = 3;
        this.bv = 243;
	}
}
