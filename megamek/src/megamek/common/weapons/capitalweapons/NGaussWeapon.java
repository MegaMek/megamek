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
package megamek.common.weapons.capitalweapons;

import megamek.common.Mounted;
import megamek.common.weapons.gaussrifles.GaussWeapon;

/**
 * @author Jay Lawson
 */
public abstract class NGaussWeapon extends GaussWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2800123131421584210L;

    public NGaussWeapon() {
        super();
        this.atClass = CLASS_CAPITAL_GAUSS;
        this.capital = true;
        flags = flags.andNot(F_PROTO_WEAPON);
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_CAPITAL;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted linked) {
        return damage;
    }
}
