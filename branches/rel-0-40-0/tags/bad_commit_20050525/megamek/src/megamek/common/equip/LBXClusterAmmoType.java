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

public class LBXClusterAmmoType extends LBXACAmmoType {
    // Use the normal constructor
    public LBXClusterAmmoType(int tech, int size) {
        super(tech,size);
    }
    
    // Overload the firing routines
    public void resolveAttack(IGame game, 
            WeaponResult wr, 
            UsesAmmoType weap, 
            UsesAmmoState weap_state) {
        
    }
    
    
    
    public TargetRoll getModifiersFor(IGame game, Entity en, Targetable targ) {
        TargetRoll tr = super.getModifiersFor(game,en,targ);
        // For example, if target is VTOL, we could change this modifier
        // easily, by checking the target entity type
        tr.addModifier(-1,"cluster ammo");
        return tr;
    }
    
}
