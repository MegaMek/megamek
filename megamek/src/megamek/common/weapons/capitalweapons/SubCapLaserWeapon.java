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

import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.lasers.EnergyWeapon;

/**
 * @author Jay Lawson
 * @since Sep 2, 2004
 */
public abstract class SubCapLaserWeapon extends EnergyWeapon {
    private static final long serialVersionUID = -4293264735637352953L;

    public SubCapLaserWeapon() {
        super();
        atClass = CLASS_CAPITAL_LASER;
        capital = true;
        subCapital = true;
        flags = flags.or(F_DIRECT_FIRE).or(F_ENERGY).andNot(F_PROTO_WEAPON);
    }

    @Override
    public int getBattleForceClass() {
        return BFCLASS_SUBCAPITAL;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted linked) {
        int maxRange = shortAV <= 1 ? AlphaStrikeElement.LONG_RANGE : AlphaStrikeElement.MEDIUM_RANGE;
        return (range <= maxRange) ? shortAV : 0;
    }
}
