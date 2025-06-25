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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IBomber;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.WeaponTypeFlag;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.totalwarfare.TWGameManager;

import java.io.Serial;

/**
 * @author Jay Lawson
 */
public class DiveBombAttack extends Weapon {
    @Serial
    private static final long serialVersionUID = -179884141546884171L;

    public DiveBombAttack() {
        name = "Dive Bomb";
        setInternalName(IBomber.DIVE_BOMB_ATTACK);
        heat = 0;
        damage = DAMAGE_SPECIAL;
        ammoType = AmmoType.AmmoTypeEnum.NA;
        minimumRange = WEAPON_NA;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        maxRange = 0;
        tonnage = 0.0;
        criticals = 0;
        bv = 0;
        cost = 0;
        flags = flags.or(F_DIVE_BOMB).or(F_SOLO_ATTACK).or(WeaponTypeFlag.INTERNAL_REPRESENTATION);
        hittable = false;
        techAdvancement.setAvailability(AvailabilityValue.B,
              AvailabilityValue.B,
              AvailabilityValue.B,
              AvailabilityValue.B);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        return new BombAttackHandler(toHit, waa, game, manager);
    }
}
