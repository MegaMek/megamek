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

import java.util.Vector;

/* This class defines Streak missile type.  Abstract, and extended by 
   specific missile types */

public abstract class StreakMissileType extends MissileType {

    // Supply input to the parent class' contructor.
    public StreakMissileType( int size, Vector valid_ammo ) {    
	super(size, valid_ammo);
    }

    // Always hits with all missiles
    public int missilesHit() {
	return size;
    }

}
