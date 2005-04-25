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

/* This class defines the LRM type */

public class FixedLRMType extends LRMissileType {

    protected transient AmmoType def_ammo_type = null;
    protected String def_ammo_name;
    protected int starting_shots;

    public FixedLRMType( int tech, int size, Vector valid_ammo, int shots, 
			 AmmoType ammo_type ) {    
	super(tech, size, valid_ammo);
	this.def_ammo_type = ammo_type;
	this.def_ammo_name = ammo_type.getInternalName();
	this.starting_shots = shots;
    }

    public EquipmentState getNewState(Mounted location) {
	return new UsesFixedAmmoState(location, this, 
				      def_ammo_type, starting_shots);
    }

    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
}


