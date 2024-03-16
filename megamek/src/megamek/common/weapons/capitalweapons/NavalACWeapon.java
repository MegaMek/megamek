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

import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.AmmoWeapon;

/**
 * @author Jay Lawson
 */
public abstract class NavalACWeapon extends AmmoWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -4293264735637352953L;

    public NavalACWeapon() {
        super();
        ammoType = AmmoType.T_NAC;
        atClass = CLASS_CAPITAL_AC;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC);
        capital = true;
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_CAPITAL;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted linked) {
        int maxRange = rackSize < 35 ? AlphaStrikeElement.LONG_RANGE : AlphaStrikeElement.MEDIUM_RANGE;
        return (range <= maxRange) ? rackSize : 0;
    }
}