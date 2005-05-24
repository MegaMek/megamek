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

/* This class defines the SRM type used by Vehicles and Mechs.  Battle Armor
   use a fixed ammo type*/

public class SRMissileType extends MissileType {

    public SRMissileType( int tech, int size, Vector valid_ammo ) {    
    super(size, valid_ammo);
    this.techType = tech;
    
    if (tech == TechConstants.T_IS_LEVEL_1 ) {
        switch(size) {
        case 2:
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 21;
        break;
        case 4:
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 39;
        break;
        case 6:
        this.tonnage = 3.0f;
        this.criticals = 2;
        this.bv = 59;
        break;
        }
    } else { // CLAN 

        switch (size) {
        case 2: 
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 21;
        break;
        case 4:
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 39;
        break;
        case 6:
        this.tonnage = 1.5f;
        this.criticals = 1;
        this.bv = 59;
        break;
        }
    }
    }
    

    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}

}
