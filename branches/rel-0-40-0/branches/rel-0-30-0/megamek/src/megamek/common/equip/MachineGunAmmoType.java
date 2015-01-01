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

public class MachineGunAmmoType extends AmmoType {
    
    public MachineGunAmmoType(int tech, boolean half_ton) {
        // Assume normal size
        this(tech, MachineGunType.NORMAL, half_ton);
    }
    
    public MachineGunAmmoType(int tech, int size, boolean half_ton) {
        this.techLevel = tech;
        this.bv = 1;
        
        if (tech == TechConstants.T_IS_LEVEL_1) {
            // Normal only
            this.damagePerShot = 2;
            this.range = new RangeType(1,2,3);
            this.shots = 200;
        } else {
            switch(size) {
            case MachineGunType.LIGHT:
                this.damagePerShot = 1;
            this.range = new RangeType(2,4,6);
            this.shots = 200;
            break;
            case MachineGunType.NORMAL:
                this.damagePerShot = 2;
            this.range = new RangeType(1,2,3);
            this.shots = 200;
            break;
            case MachineGunType.HEAVY:
                this.damagePerShot = 3;
            // No long range
            this.range = new RangeType(1,2,2);
            this.shots = 100;
            }    
        }
        // Handle half-ton lots
        if (half_ton) {
            this.shots /= 2;
        }
    }
    
    
    public void resolveAttack(IGame game, 
            WeaponResult wr, 
            UsesAmmoType weap, 
            UsesAmmoState weap_state) {
        //  #######################
    }
    
}
