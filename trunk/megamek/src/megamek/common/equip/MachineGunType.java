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

/* This class is used for Machine guns moutned on vehicles or mechs.  
   Battle Armor use their own class */

public class MachineGunType extends BallisticType {

    public static final int LIGHT  = 1;
    public static final int NORMAL = 2;
    public static final int HEAVY  = 3;

    // Assume normal size
    public MachineGunType( int tech, Vector valid_ammo) {
	this(tech, NORMAL, valid_ammo);
    }

    public MachineGunType( int tech, int size, Vector valid_ammo ) {    
	super(valid_ammo);

	this.techType = tech;
	this.criticals = 1;

	if (tech == TechConstants.T_IS_LEVEL_1 ) {
	    this.bv = 5;
	    this.tonnage = 0.5f;	    
	} else {
	    // Clan's come in light/normal/heavy 
	    switch(size) {
	    case LIGHT:
	    case NORMAL:
		this.tonnage = 0.25f;
		this.bv = 5;
		break;
	    case HEAVY:
		this.tonnage = 0.5f;
		this.bv = 6;
		break;
            }
        }
    }

    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }

}
