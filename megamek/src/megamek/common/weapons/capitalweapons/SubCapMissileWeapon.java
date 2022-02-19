/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.capitalweapons;

/**
 * @author Jay Lawson
 * @since Sep 2, 2004
 */
public abstract class SubCapMissileWeapon extends CapitalMissileWeapon {
    private static final long serialVersionUID = 9186993166684654767L;

    public SubCapMissileWeapon() {
        super();
        atClass = CLASS_CAPITAL_MISSILE;
        capital = true;
        subCapital = true;
        flags = flags.or(F_AERO_WEAPON).or(F_MISSILE).andNot(F_PROTO_WEAPON);
    }
}
