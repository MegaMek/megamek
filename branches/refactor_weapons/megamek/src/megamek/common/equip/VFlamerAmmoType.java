/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

public class VFlamerAmmoType extends AmmoType {
    // Overload the constructor 
    public VFlamerAmmoType(int tech) {
	this.techType = tech;
	this.damagePerShot = 2;
	this.heat = 3;
	this.range = new RangeType(1,2,3);
	this.shots = 20;
	this.bv = 1;
    }

    // Flamers ignite on 4+
    public int getFireTN() {
	return 4;
    }
}
