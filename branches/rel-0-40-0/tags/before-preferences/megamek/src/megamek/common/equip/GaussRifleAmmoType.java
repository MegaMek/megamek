/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.equip;

import megamek.common.*;

public class GaussRifleAmmoType extends AmmoType {
    /** The type of Gauss Rifle that uses this ammo. */
    protected int type;

    public GaussRifleAmmoType(int tech, int type) {
	this.techType = tech;
	this.type = type;

	if (tech == TechConstants.T_IS_LEVEL_2) {
	    switch(type) {
	    case GaussRifleType.LIGHT:
		this.damagePerShot = 8;
//		this.heat = 1;
		this.shots = 16;
		this.range = new RangeType (3,8,17,25);
		this.bv = 20;
		break;
	    case GaussRifleType.NORMAL:
		this.damagePerShot = 15;
//		this.heat = 1;
		this.shots = 8;
		this.range = new RangeType (2,7,15,22);
		this.bv = 37;
		break;
		// While the constants are here, the special rules (PSR for
		// shooting and moving) is handled in a subclass, along
		// with the damage returning routines
	    case GaussRifleType.HEAVY:
		this.damagePerShot = 25;
		this.shots = 4;
		this.range = new RangeType(4,6,13,20);
		this.bv = 43;
//		this.heat = 2;
	    }
	} else { 
	    // Clan
	    this.damagePerShot = 15;
//	    this.heat = 1;
	    this.shots = 8;
	    this.range = new RangeType(2,7,15,22);
	    this.bv = 33;
	}
    }

    // No gauss rifle can start fires.
    public int getFireTN() {
	return TargetRoll.IMPOSSIBLE;
    }
    
    public boolean isExplosive() {
	return false;
    }
}
