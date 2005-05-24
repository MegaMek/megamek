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

public class ArmorPiercingACAmmoType extends ACAmmoType {
    // Overload the constructor 
    public ArmorPiercingACAmmoType(int size) {
        super(size);
        this.techType = TechConstants.T_IS_LEVEL_2;
        
        // AP rounds have half the ammo per ton
        this.shots = this.shots / 2;
    }
    
    public TargetRoll getModifiersFor(IGame game, Entity en, Targetable targ) {
        TargetRoll tr = super.getModifiersFor(game,en,targ);
        tr.addModifier(1, "AP rounds");
        return tr;
    }
    
    public void resolveAttack(IGame game, 
            WeaponResult wr, 
            UsesAmmoType weap, 
            UsesAmmoState weap_state) {
        
        HitData hd = resolveACAttack(game,wr,weap, weap_state);
        
        // Actually add a critical if there is armor left in the location hit???
        //  ##########
    }
    
}

