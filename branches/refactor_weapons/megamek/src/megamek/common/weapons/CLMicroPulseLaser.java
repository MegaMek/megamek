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
public class CLMicroPulseLaser extends LaserWeapon {
	/**
	 * 
	 */
	public CLMicroPulseLaser() {
		super();
		this.name = "Micro Pulse Laser";
        this.setInternalName("CLMicroPulseLaser");
        this.addLookupName("Clan Micro Pulse Laser");
        this.heat = 1;
        this.damage = 3;
        this.toHitModifier = -2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 2;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.flags |=  F_NO_FIRES;
        this.bv = 12;
	}
}
