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

public class UsesAmmoState extends EquipmentState {
    public UsesAmmoState (Mounted location, UsesAmmoType type) {
	super(location, type);
    }
    
    protected AmmoBin ammo;

    public AmmoBin getAmmoBin() {
	return ammo;
    }

    public void setAmmoBin(AmmoBin ammo) {
	this.ammo = ammo;
    }
}

