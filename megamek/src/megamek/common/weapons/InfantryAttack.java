/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.AmmoType;

/**
 * @author Sebastian Brocks
 */
public abstract class InfantryAttack extends Weapon {

    /**
     *
     */
    private static final long serialVersionUID = -8249141375380685926L;

    public InfantryAttack() {
        super();
        flags = flags.or(F_NO_FIRES).or(F_SOLO_ATTACK).or(F_INFANTRY_ATTACK);
        heat = 0;
        damage = DAMAGE_SPECIAL;
        ammoType = AmmoType.T_NA;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        tonnage = 0.0f;
        criticals = 0;
        bv = 0;
        cost = 0;
    }
}
