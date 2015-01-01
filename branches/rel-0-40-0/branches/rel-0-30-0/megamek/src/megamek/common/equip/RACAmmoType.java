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

public class RACAmmoType extends AmmoType {
    public RACAmmoType(int size) {
        this.techLevel = TechConstants.T_IS_LEVEL_2;
    this.rackSize = size;
    this.damagePerShot = this.rackSize;

    switch(size) {
    case 2:
        this.heat = 1;
        this.range = new RangeType(6, 12, 18);
        this.shots = 45;
        this.bv = 15;
        break;
    case 5: 
        this.heat = 1;
        this.range = new RangeType(5, 10, 15);
        this.shots = 20;
        this.bv = 31;
        break;
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
