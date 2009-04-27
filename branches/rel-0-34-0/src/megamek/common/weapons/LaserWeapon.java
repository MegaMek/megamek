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
 * Created on Sep 2, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;

/**
 * @author Andrew Hunter
 */
public abstract class LaserWeapon extends EnergyWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -9210696480919833245L;

    public LaserWeapon() {
        super();
        flags |= F_LASER | F_DIRECT_FIRE | F_MECH_WEAPON | F_AERO_WEAPON |  F_TANK_WEAPON;
        ammoType = AmmoType.T_NA;

        atClass = CLASS_LASER;
    }
}
