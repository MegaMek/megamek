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

public class AutoCannonType extends BallisticType {

    private int size;

    public AutoCannonType( int size, Vector valid_ammo ) {    
	super(valid_ammo);
	this.tech_level = TechConstants.T_IS_LEVEL_1;
	this.size = size;
	this.flags |= F_DIRECT_FIRE;

	switch(size) {
	case 2:
	    this.tonnage = 6.0f;
	    this.criticals = 1;
	    this.bv = 37;
	    break;
	case 5:
	    this.tonnage = 8.0f;
	    this.criticals = 4;
	    this.bv = 70;
	    break;
	case 10:
	    this.tonnage = 12.0f;
	    this.criticals = 7;
	    this.bv = 124;
	    break;
	case 20:
	    this.tonnage = 14.0f;
	    this.criticals = 10;
	    this.bv = 178;
	    break;
	}

    }
    
    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }

}
