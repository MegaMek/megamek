/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons.missiles;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.RLHandler;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class RLWeapon extends MissileWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1718301014226423896L;

    /**
     *
     */
    public RLWeapon() {
        super();
        ammoType = AmmoType.T_ROCKET_LAUNCHER;
        flags = flags.or(F_ONESHOT);
        toHitModifier = 1;
        atClass = CLASS_ROCKET_LAUNCHER;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        return new RLHandler(toHit, waa, game, manager);
    }
    
    @Override
    public boolean hasIndirectFire() {
        return true;
    }
    
    @Override
    public boolean isAlphaStrikeIndirectFire() {
        return false;
    }
}
