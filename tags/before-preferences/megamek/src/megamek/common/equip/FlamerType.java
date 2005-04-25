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

/* Yet another marker class, until energy specific things happen */

public class FlamerType extends EnergyType implements BattleArmorWeapon {

    protected static final String[] FLAMER_MODES = { "Damage", "Heat" };

    public FlamerType (int tech) {
	this.heat = 3;
	this.damage = 2;
	this.range = new RangeType(1,2,3);
	this.criticals = 1;
	this.bv = 6;
	this.setModes(FLAMER_MODES);
	this.techType = tech;

	if (tech == TechConstants.T_IS_LEVEL_1) {
	    this.tonnage = 1.0f;
	} else {
	    this.tonnage = 0.5f;	    
	}
    }


    public int getFireTN() {
	return 4;
    }


    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }

    public void resolveBattleArmorAttack(WeaponResult wr, int num_units) {}

}
