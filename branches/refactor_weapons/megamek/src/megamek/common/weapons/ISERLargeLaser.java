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
public class ISERLargeLaser extends LaserWeapon {
	
	public ISERLargeLaser() {
		super();
		this.name = "ER Large Laser";
        this.setInternalName("ISERLargeLaser");
        this.addLookupName("IS ER Large Laser");
        this.heat = 12;
        this.damage = 8;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 19;
        this.waterShortRange = 3;
        this.waterMediumRange = 9;
        this.waterLongRange = 12;
        this.tonnage = 5.0f;
        this.criticals = 2;
        this.bv = 163;
	}
}
