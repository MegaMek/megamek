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

public class PPCType extends EnergyType {
    
    public PPCType (int tech, boolean is_ER) {
    
    if ( tech == TechConstants.T_IS_LEVEL_1 || 
             tech == TechConstants.T_IS_LEVEL_2 ) {
        this.damage = 10;
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.flags |= F_DIRECT_FIRE;
        this.techType = tech;
    
        if (!is_ER) {
        this.bv = 176;
        this.heat = 10;
        this.range = new RangeType(3, 6, 12, 18);

        } else {
        this.bv = 229;
        this.heat = 15;
        this.range = new RangeType(7,14,23);
        }
    } else {
        this.bv = 412;
        this.tonnage = 6.0f;
        this.criticals = 2;

        this.range = new RangeType(7,14,23);
        this.heat = 15;
        this.damage = 15;
    }
    }

    public int getFireTN() {
    return 7;
    }


    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}


}
