/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.autocannons;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.HVACWeaponHandler;
import megamek.common.weapons.RapidfireHVACWeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

import java.io.Serial;

/**
 * @author Jason Tighe
 */
public abstract class HVACWeapon extends ACWeapon {
    @Serial
    private static final long serialVersionUID = 4958849713169213573L;

    public HVACWeapon() {
        super();
        ammoType = AmmoType.T_HYPER_VELOCITY;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager) {
        Mounted<?> weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());
        if (weapon.curMode().equals("Rapid")) {
            return new RapidfireHVACWeaponHandler(toHit, waa, game, manager);
        } else {
            return new HVACWeaponHandler(toHit, waa, game, manager);
        }
    }

    @Override
    public int getBattleForceClass() {
        return BFCLASS_STANDARD;
    }
}
