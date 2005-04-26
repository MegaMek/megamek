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

/* Yet another marker class, until energy specific things happen */

public class PulseLaserType extends LaserType {

//     public static final int MICRO = 20;
//     public static final int SMALL = 21;
//     public static final int MED   = 22;
//     public static final int LARGE = 23;

    public PulseLaserType (int tech, int size) {
    super(tech,size);
    this.techType = tech;

    if (tech == TechConstants.T_IS_LEVEL_2) {
        switch(size) {
        case SMALL:
        this.heat = 2;
        this.damage = 3;
        this.range = new RangeType(1,2,3);
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 12;
        break;
        case MED:
        this.heat = 4;
        this.damage = 6;
        this.range = new RangeType(2,4,6);
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 48;
        break;
        case LARGE:
        this.heat = 10;
        this.damage = 9;
        this.range = new RangeType(3,7,10);
        this.tonnage = 7.0f;
        this.criticals = 2;
        this.bv = 119;
        break;
        }
    } else {
        // CLAN
        switch(size) {
        case MICRO:
        this.heat = 1;
        this.damage = 3;
        this.range = new RangeType(1,2,3);
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 12;
        break;
        case SMALL:
        this.heat = 2;
        this.damage = 3;
        this.range = new RangeType(2,4,6);
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 24;
        break;
        case MED:
        this.heat = 4;
        this.damage = 7;
        this.range = new RangeType(4,8,12);
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 111;
        break;
        case LARGE:
        this.heat = 10;
        this.damage = 10;
        this.range = new RangeType(6,14,20);
        this.tonnage = 6.0f;
        this.criticals = 2;
        this.bv = 265;
        break;
            }
    }
    }

    // Micro pulse lasers cannot start fire
    public int getFireTN() {
    if (size != MICRO)
        return 7;
    else
        return TargetRoll.IMPOSSIBLE;
    }

    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) {
    TargetRoll tr = super.getModifiersFor(loc,en,targ);
    tr.addModifier(-2, "pulse laser");
    return tr;
    }
    
}


