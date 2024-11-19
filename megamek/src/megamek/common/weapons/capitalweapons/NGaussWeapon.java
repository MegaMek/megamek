/*
 * Copyright (c) 2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.capitalweapons;

import megamek.common.Mounted;
import megamek.common.weapons.gaussrifles.GaussWeapon;

import java.io.Serial;

/**
 * Naval Gauss Weapon superclass
 * @author Jay Lawson
 */
public abstract class NGaussWeapon extends GaussWeapon {
    @Serial
    private static final long serialVersionUID = -2800123131421584210L;

    public NGaussWeapon() {
        super();
        atClass = CLASS_CAPITAL_GAUSS;
        capital = true;
        flags = flags.andNot(F_PROTO_WEAPON).andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON);
    }

    @Override
    public int getBattleForceClass() {
        return BFCLASS_CAPITAL;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        return damage;
    }
}
