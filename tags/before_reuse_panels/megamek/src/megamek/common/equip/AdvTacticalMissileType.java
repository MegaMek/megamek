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

/**
 * This class defines the ATM weapon systems.
 */

public class AdvTacticalMissileType extends MissileType {

    public AdvTacticalMissileType( int size, Vector valid_ammo ) {    
	super(size, valid_ammo);
	// Clan only

	switch(size) {
	case 3:
	    this.tonnage = 1.5f;
	    this.criticals = 2;
	    this.bv = 53;
	    break;
	case 6:
	    this.tonnage = 3.5f;
	    this.criticals = 3;
	    this.bv = 105;
	    break;
	case 9:
	    this.tonnage = 5.0f;
	    this.criticals = 4;
	    this.bv = 147;
	    break;
	case 12:
	    this.tonnage = 7.0f;
	    this.criticals = 5;
	    this.bv = 212;
	    break;
	}
    }

    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }

}
