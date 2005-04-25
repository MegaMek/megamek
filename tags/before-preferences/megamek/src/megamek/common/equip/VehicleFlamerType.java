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

/* Vehicle flamers */

public class VehicleFlamerType extends BallisticType {

    public VehicleFlamerType(int tech, Vector valid_ammo ) {    
	super(valid_ammo);
	this.techType = tech;
	this.heat = 3;
	this.damage = 2;
	
	String[] modes = { "Damage", "Heat" };
	this.setModes(modes);
	this.range = new RangeType(1, 2, 3);

	this.tonnage = 0.5f;
	this.criticals = 1;
	this.bv = 5;
    }
    
    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
}
