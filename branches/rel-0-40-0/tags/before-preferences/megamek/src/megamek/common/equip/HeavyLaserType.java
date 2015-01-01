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

public class HeavyLaserType extends LaserType {

//     // All heavy lasers are clan weaons.
//     public static final int SMALL = 31;
//     public static final int MED   = 32;
//     public static final int LARGE = 33;

    public HeavyLaserType (int size) {
	super(TechConstants.T_CLAN_LEVEL_2, size);
	
	// Change the settings now
	switch(size) {
	case SMALL:
	    this.heat = 3;
	    this.damage = 6;
	    this.tonnage = 0.5f;
	    this.criticals = 1;
	    this.bv = 15;
	    this.range = new RangeType(1,2,3);
	    break;
	case MED:
	    this.heat = 7;
	    this.damage = 10;
	    this.tonnage = 1.0f;
	    this.criticals = 2;
	    this.bv = 76;
	    this.range = new RangeType(3,6,9);
	    break;
	case LARGE:
	    this.heat = 18;
	    this.damage = 16;
	    this.tonnage = 4.0f;
	    this.criticals = 3;
	    this.bv = 243;
	    this.range = new RangeType(5,10,15);
	    break;
	}
	
    }

    public int getFireTN() {
	return 7;
    }

    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }
}
