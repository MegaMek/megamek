/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.capitalWeapons;

import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.common.game.Game;
import megamek.common.equipment.Mounted;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.handlers.artillery.ArtilleryWeaponIndirectFireHandler;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.capitalMissile.CapitalMissileBearingsOnlyHandler;
import megamek.common.weapons.handlers.capitalMissile.CapitalMissileHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Jay Lawson
 * @since Sept 2, 2004
 */
public abstract class CapitalMissileWeapon extends AmmoWeapon {
    private static final long serialVersionUID = 9186993166684654767L;

    public CapitalMissileWeapon() {
        super();
        this.atClass = CLASS_CAPITAL_MISSILE;
        this.capital = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.game.Game)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        Mounted<?> weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());
        Entity attacker = game.getEntity(waa.getEntityId());
        int rangeToTarget = attacker.getPosition().distance(waa.getTarget(game).getPosition());
        // Capital missiles work like artillery for surface to surface fire
        if (Compute.isGroundToGround(attacker, waa.getTarget(game))) {
            return new ArtilleryWeaponIndirectFireHandler(toHit, waa, game, manager);
        }
        if (weapon.isInBearingsOnlyMode()
              && (rangeToTarget >= RangeType.RANGE_BEARINGS_ONLY_MINIMUM)) {
            return new CapitalMissileBearingsOnlyHandler(toHit, waa, game, manager);
        }
        return new CapitalMissileHandler(toHit, waa, game, manager);
    }

    @Override
    public int getBattleForceClass() {
        return BFCLASS_CAPITAL_MISSILE;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        return damage;
    }
}
