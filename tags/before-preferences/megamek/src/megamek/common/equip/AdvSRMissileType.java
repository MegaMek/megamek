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
import java.util.Vector;

/* This class defines the Advanced SRM type */

public class AdvSRMissileType extends FixedSRMType {

    public AdvSRMissileType( int size, Vector valid_ammo, int shots) {    
        // TODO : replace the "null" with the default ammo
	super(TechConstants.T_CLAN_LEVEL_2, size, valid_ammo, shots, null);
    }
    // Adv SRM's are like normal, except they always hit in even numbers

    public int BAmissilesHit(int troops) {
	int hits = super.BAmissilesHit(troops);

	// Adv Missiles are like normal, but always hit in pairs, rounded up
	hits += hits % 2;
	return hits;
    }

}
