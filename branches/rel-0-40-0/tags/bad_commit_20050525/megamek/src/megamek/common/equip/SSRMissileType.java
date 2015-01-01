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

/* This class defines Streak SRM missiles */

public class SSRMissileType extends StreakMissileType {
   
    public SSRMissileType( int tech, int size, Vector valid_ammo ) {    
    super(size, valid_ammo);
    this.techType = tech;

    if (tech == TechConstants.T_IS_LEVEL_2 ) {
        switch(size) {
        case 2:
        this.tonnage = 1.5f;
        this.criticals = 1;
        this.bv = 30;
        break;
        case 4:
        this.tonnage = 3.0f;
        this.criticals = 1;
        this.bv = 59;
        break;
        case 6:
        this.tonnage = 4.5f;
        this.criticals = 2;
        this.bv = 89;
        break;
        }
    } else { // CLAN 

        switch (size) {
        case 2: 
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 40;
        break;
        case 4:
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 79;
        break;
        case 6:
        this.tonnage = 3.0f;
        this.criticals = 2;
        this.bv = 119;
        break;
        }
    }
    }

    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}

}
