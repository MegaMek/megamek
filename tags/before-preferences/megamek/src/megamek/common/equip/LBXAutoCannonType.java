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

/* This class defines all of the ballistic weapon subtypes. */

public class LBXAutoCannonType extends BallisticType {

    private int size;

    public LBXAutoCannonType( int tech, int size, Vector valid_ammo ) {    
	super(valid_ammo);
	this.tech_level = tech;
	this.size = size;
	this.flags |= F_DIRECT_FIRE;

	if (tech == TechConstants.T_IS_LEVEL_2) {
	    switch(size) {
	    case 2:
		this.tonnage = 6.0f;
		this.criticals = 4;
		this.bv = 42;
		break;
	    case 5:
		this.tonnage = 8.0f;
		this.criticals = 5;
		this.bv = 83;
		break;
	    case 10:
		this.tonnage = 11.0f;
		this.criticals = 6;
		this.bv = 148;
		break;
	    case 20:
		this.tonnage = 14.0f;
		this.criticals = 11;
		this.bv = 237;
		break;
	    }
	} else {
	    // CLAN
	    switch(size) {
	    case 2:
		this.tonnage = 5.0f;
		this.criticals = 3;
		this.bv = 47;
		break;
	    case 5:
		this.tonnage = 7.0f;
		this.criticals = 4;
		this.bv = 93;
		break;
	    case 10:
		this.tonnage = 10.0f;
		this.criticals = 5;
		this.bv = 148;
		break;
	    case 20:
		this.tonnage = 12.0f;
		this.criticals = 9;
		this.bv = 237;
		break;
	    }
	}

    }
    
    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }

}
