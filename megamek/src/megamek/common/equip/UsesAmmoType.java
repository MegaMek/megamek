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

public abstract class UsesAmmoType extends WeaponType {

    protected Vector valid_ammo = null;
    protected int    tech_level = 0;
        
    public UsesAmmoType (Vector valid_ammo) {
    this.valid_ammo = valid_ammo;
    };

    // is fed to the lower classes
    public EquipmentState getNewState(Mounted location) {
    return new UsesAmmoState(location, this);
    }

    public Vector getValidAmmo () {
    return valid_ammo;
    }

    // By default, get all paramters from the ammunition.

    public int getHeat(EquipmentState state) {
    UsesAmmoState amst = (UsesAmmoState) state;
    AmmoType at = amst.getAmmoBin().getAmmoType();
    return at.getHeat();
    }

    public int getShotDamage(EquipmentState state, Entity en, Targetable targ) {
    UsesAmmoState amst = (UsesAmmoState) state;
    AmmoType at = amst.getAmmoBin().getAmmoType();
    return at.getShotDamage(en, targ);
    }


    public RangeType getRange(EquipmentState state) {
    UsesAmmoState amst = (UsesAmmoState) state;
    AmmoType at = amst.getAmmoBin().getAmmoType();
    return at.getRange();
    }


    public void resolveAttack(Game game, EquipmentState state, WeaponResult wr) {
    UsesAmmoState amst = (UsesAmmoState) state;
    AmmoType at = amst.getAmmoBin().getAmmoType();
    at.resolveAttack(game, wr, this, state);
    }

    public int getFireTN() {
/* TODO: implement me
    UsesAmmoState amst = (UsesAmmoState) state;
    AmmoType at = amst.getAmmoBin().getAmmoType();
    at.getFireTN();
*/
        return TargetRoll.IMPOSSIBLE;
    }
}

