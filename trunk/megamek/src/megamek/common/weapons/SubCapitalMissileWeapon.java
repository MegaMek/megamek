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

/**
 * @author Jay Lawson
 */
public abstract class SubCapitalMissileWeapon extends AmmoWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 9186993166684654767L;

    public SubCapitalMissileWeapon() {
        super();
        this.atClass = CLASS_CAPITAL_MISSILE;
        this.capital = true;
        this.subCapital = true;
    }
}