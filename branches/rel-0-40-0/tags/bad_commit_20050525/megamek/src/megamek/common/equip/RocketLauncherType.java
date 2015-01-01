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

/* This class is for Rocket Launchers, which are always one shot */

public class RocketLauncherType extends MissileType {

    protected transient AmmoType def_ammo_type = null;
    protected String def_ammo_name;

    public EquipmentState getNewState(Mounted location) {
    return new UsesFixedAmmoState(location, this, 
                      def_ammo_type, 1);
    }


    public RocketLauncherType( int size, Vector valid_ammo, 
                   AmmoType ammo_type ) {    
    super(size, valid_ammo);
    this.def_ammo_type = ammo_type;
    this.def_ammo_name = ammo_type.getInternalName();
    this.techType = TechConstants.T_IS_LEVEL_2;

    switch (size) {
    case 10:
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 18;
        break;
    case 15:
        this.tonnage = 1.0f;
        this.criticals = 2;
        this.bv = 23;
        break;
    case 20:
        this.tonnage = 1.5f;
        this.criticals = 3;
        this.bv = 24;
        break;
    }
    }


    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ){ return null; }
    public void resolveAttack( WeaponResult wr) {}
    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }

}
