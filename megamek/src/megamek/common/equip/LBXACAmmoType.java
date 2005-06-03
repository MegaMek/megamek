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

public class LBXACAmmoType extends AmmoType {

    public LBXACAmmoType(int tech, int size) {
    this.techLevel = tech;
    this.rackSize = size;
    this.damagePerShot = this.rackSize;
    
    if ( tech == TechConstants.T_IS_LEVEL_2) {
        switch(size) {
        case 2:
        this.heat = 1;
        this.range = new RangeType(4, 9, 18, 27);
        this.shots = 45;
        this.bv = 5;
        break;
        case 5: 
        this.heat = 1;
        this.range = new RangeType(3, 7, 14, 21);
        this.shots = 20;
        this.bv = 10;
        break;
        case 10:
        this.heat = 2;
        this.range = new RangeType(6, 12, 18);
        this.shots = 10;
        this.bv = 19;
        break;
        case 20:
        this.heat = 6;
        this.range = new RangeType(4, 8, 12);
        this.shots = 5;
        this.bv = 27;
        break;
        }
    } else {
        // CLAN
        switch(size) {
        case 2:
        this.heat = 1;
        this.range = new RangeType(4, 10, 20, 30);
        this.shots = 45;
        this.bv = 6;
        break;
        case 5: 
        this.heat = 1;
        this.range = new RangeType(3, 8, 15, 24);
        this.shots = 20;
        this.bv = 12;
        break;
        case 10:
        this.heat = 2;
        this.range = new RangeType(6, 12, 18);
        this.shots = 10;
        this.bv = 19;
        break;
        case 20:
        this.heat = 6;
        this.range = new RangeType(4, 8, 12);
        this.shots = 5;
        this.bv = 33;
        break;
        }
    }
    }

    
}
