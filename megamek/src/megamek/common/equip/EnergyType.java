/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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

/* Yet another marker class, until energy specific things happen */

public abstract class EnergyType extends WeaponType {

    // Used to get the amount of heat. 
    public int getHeat(EquipmentState state) {
        return heat;
    }

    // Takes entity and target.  This will basically be used for variable
    // damage weapons (HGauss). 
    public int getShotDamage(EquipmentState state, Entity en, Targetable targ) {
        return damage;
    }

    // Since the range is variable based on ammo, pass the state in.
    public RangeType getRange(EquipmentState state) {
        return range;
    }
    
}
