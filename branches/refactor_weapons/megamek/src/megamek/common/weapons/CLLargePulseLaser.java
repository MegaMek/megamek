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
public class CLLargePulseLaser extends LaserWeapon {
	/**
	 * 
	 */
	public CLLargePulseLaser() {
		super();
		this.name = "Large Pulse Laser";
        this.setInternalName("CLLargePulseLaser");
        this.addLookupName("Clan Pulse Large Laser");
        this.addLookupName("Clan Large Pulse Laser");
        this.heat = 10;
        this.damage = 10;
        this.toHitModifier = -2;
        this.shortRange = 6;
        this.mediumRange = 14;
        this.longRange = 20;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 14;
        this.tonnage = 6.0f;
        this.criticals = 2;
        this.bv = 265;
	}
}
