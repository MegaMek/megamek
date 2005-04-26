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

public class PrecisionACAmmoType extends ACAmmoType {
    // Overload the constructor 
    public PrecisionACAmmoType(int size) {
    super(size);
    this.techType = TechConstants.T_IS_LEVEL_2;
    
    // Precision rounds have half the ammo per ton
    this.shots = this.shots / 2;
    }

    public TargetRoll getModifiersFor(Game game, Entity en, Targetable targ) {
/* TODO: implement me.
    TargetRoll tr = super.getModifiersFor(loc,en,targ);

    // Need the targets movement.  If movement modifer is +2 or better,
    // add a -2 from precise ammo.  If it's +1, return a -1 modifier
    ToHitData thTemp = Compute.getTargetMovementModifier(game, 
                                 target.getTargetId());
    int nAdjust = Math.min(2, thTemp.getValue());
    
    if (nAdjust > 0) {
        tr.addModifier(-nAdjust,"Precision Ammo");
    }
    return super_tr;
*/
        return null;
    }


    public void resolveAttack(Game game, 
                  WeaponResult wr, 
                  UsesAmmoType weap, 
                  UsesAmmoState weap_state) {

/* TODO: implement me.
    HitData hd = resolveACAttack(game,wr,weap, weap_sate);
*/  
    // Actually add a critical if there is armor left in the location hit???
//  ##########
    }
    
}
