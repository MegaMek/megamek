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
import megamek.common.*;
/**
 * @author Andrew Hunter
 *
 */
public class ISLargeLaser extends LaserWeapon {
	public ISLargeLaser() {
		super();
		name = "Large Laser";
	    setInternalName(this.name);
	    addLookupName("IS Large Laser");
	    addLookupName("ISLargeLaser");
	    heat = 8;
	    damage = 8;
	    shortRange = 5;
	    mediumRange = 10;
	    longRange = 15;
	    waterShortRange = 3;
	    waterMediumRange = 6;
	    waterLongRange = 9;
	    tonnage = 5.0f;
	    criticals = 2;
	    bv = 124;
	}
}
