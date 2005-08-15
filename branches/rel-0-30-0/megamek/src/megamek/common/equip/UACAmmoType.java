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

public class UACAmmoType extends AmmoType {

    public UACAmmoType(int size, int tech) {
        this.techLevel = tech;
        this.rackSize = size;
        this.damagePerShot = this.rackSize;
        
        if ( tech == TechConstants.T_IS_LEVEL_2) {
            switch(size) {
            case 2:
                this.heat = 1;
                this.range = new RangeType(3, 8, 17, 25);
                this.shots = 45;
                this.bv = 7;
                break;
            case 5: 
                this.heat = 1;
                this.range = new RangeType(2, 6, 13, 20);
                this.shots = 20;
                this.bv = 14;
                break;
            case 10:
                this.heat = 4;
                this.range = new RangeType(6, 12, 18);
                this.shots = 10;
                this.bv = 29;
                break;
            case 20:
                this.heat = 8;
                this.range = new RangeType(3, 7, 10);
                this.shots = 5;
                this.bv = 32;
                break;
            }
        } else {
            // CLAN
            switch(size) {
            case 2:
                this.heat = 1;
                this.range = new RangeType(2, 9, 18, 27);
                this.shots = 45;
                this.bv = 6;
                break;
            case 5: 
                this.heat = 1;
                this.range = new RangeType(7, 14, 21);
                this.shots = 20;
                this.bv = 15;
                break;
            case 10:
                this.heat = 3;
                this.range = new RangeType(6, 12, 18);
                this.shots = 10;
                this.bv = 26;
                break;
            case 20:
                this.heat = 7;
                this.range = new RangeType(4, 8, 12);
                this.shots = 5;
                this.bv = 35;
                break;
            }
        }
    }
    
    protected HitData resolveACAttack(IGame game, 
            WeaponResult wr, 
            UsesAmmoType weap, 
            UsesAmmoState weap_state) {
        return null;
    }
    
    // AC's do damage to a single location
    public void resolveAttack(IGame game, 
            WeaponResult wr, 
            UsesAmmoType weap, 
            UsesAmmoState weap_state) {
        resolveACAttack(game, wr, weap, weap_state);
    }
    
}
