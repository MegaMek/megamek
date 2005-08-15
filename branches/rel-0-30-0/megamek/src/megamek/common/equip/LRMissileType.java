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

public class LRMissileType extends MissileType  {

    public LRMissileType( int tech, int size, Vector valid_ammo ) {    
    super(size, valid_ammo);
    this.techLevel = tech;
    
    if(tech == TechConstants.T_IS_LEVEL_1) {
        switch(size) {
        case 5:
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 45;
        break;
        case 10:
        this.tonnage = 5.0f;
        this.criticals = 2;
        this.bv = 90;
        break;
        case 15:
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.bv = 126;
        break;
        case 20:
        this.tonnage = 10.0f;
        this.criticals = 5;
        this.bv = 181;
        break;
        }
    } else {
        // CLAN
        switch(size) {
        case 5:
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 55;
        break;
        case 10:
        this.tonnage = 2.5f;
        this.criticals = 1;
        this.bv = 109;
        break;
        case 15:
        this.tonnage = 3.5f;
        this.criticals = 2;
        this.bv = 164;
        break;
        case 20:
        this.tonnage = 5.0f;
        this.criticals = 4;
        this.bv = 220;
        break;
        }       
    }

    }

    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }
}
