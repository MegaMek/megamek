/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;

/**
 * @author Sebastian Brocks
 */
public abstract class InfantryWeapon extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -4437093890717853422L;

    public InfantryWeapon() {
        super();
        damage = DAMAGE_VARIABLE;
        flags = flags.or(F_INFANTRY);
        ammoType = AmmoType.T_NA;
        heat = 0;
        tonnage = 0.0f;
        criticals = 0;
    }
}
