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

/* This class is used for Light Gauss and Gauss rifles ... heavys get their
   own class */

public class GaussRifleType extends BallisticType {

    public static final int LIGHT = 1;
    public static final int NORMAL = 2;
    public static final int HEAVY = 3;

    protected int explosive_damage;

    // Assume normal size
    public GaussRifleType (int tech, Vector valid_ammo) {
    this(tech, NORMAL, valid_ammo);
    }

    public GaussRifleType( int tech, int type, Vector valid_ammo ) {    
    super(valid_ammo);
    this.flags = F_DIRECT_FIRE;
    this.techType = tech;

    if (tech == TechConstants.T_IS_LEVEL_2 ) {
        switch(type) {
        case LIGHT:
        this.tonnage = 12.0f;
        this.criticals = 5;
        this.bv = 159;
        explosive_damage =16;
        break;
        case NORMAL:
        this.tonnage = 15.0f;
        this.criticals = 7;
        this.bv = 321;
        explosive_damage = 20;
        break;
        case HEAVY:
        this.tonnage = 18.0f;
        this.criticals = 11;
        this.bv = 346;
        explosive_damage = 25;
        }
    } else { // Clan ... only normal
        this.tonnage = 12.0f;
        this.criticals = 6;
        this.bv = 321;
        explosive_damage = 20;
    }
    }

    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
    
    public boolean isExplosive() {
    return true;
    }

    public void doCriticalDamage(EquipmentState state) {
/* TODO: implement me
    super(state);
    // Explode
*/
    }

}
