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

public class UsesFixedAmmoState extends UsesAmmoState implements AmmoBin {

    protected transient AmmoType ammo_type = null;
    protected String ammo_name;
    protected int shots;

    public UsesFixedAmmoState (Mounted location, UsesAmmoType type,
                   AmmoType ammo_type, int shots) {
    super(location, type);
    this.ammo_type = ammo_type;
    this.ammo_name = ammo_type.getInternalName();
    this.shots = shots;
    }


    // For this special class, it is its own Ammo Bin
    public AmmoBin getAmmoBin() {
    return this;
    }

    public void depleteAmmo() {
    shots--;
    }
    public int shotsLeft() {
    return shots;
    }
   
    public AmmoType getAmmoType() {
    if(ammo_type == null) {
        // Look up the appropraite thing 
//      #######################
        }
    return ammo_type;
    }
}
