/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.missiles;

import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.MissileWeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class MissileWeapon extends AmmoWeapon {

    private static final long serialVersionUID = -2759022204865126991L;

    /**
     *
     */
    public MissileWeapon() {
        super();
        damage = DAMAGE_BY_CLUSTERTABLE;
        flags = flags.or(F_MEK_WEAPON).or(F_TANK_WEAPON)
              .or(F_AERO_WEAPON).or(F_MISSILE);
        infDamageClass = WEAPON_CLUSTER_MISSILE;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        return new MissileWeaponHandler(toHit, waa, game, manager);
    }

    @Override
    public double getBattleForceDamage(int range) {
        return getBattleForceDamage(range, null);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (range > getLongRange()) {
            return 0;
        }
        int clusterRoll = 7;
        if (fcs != null && fcs.getType() instanceof MiscType) {
            if (fcs.getType().hasFlag(MiscType.F_ARTEMIS)) {
                clusterRoll = 9;
            } else if (fcs.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                clusterRoll = 8;
            } else if (fcs.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                clusterRoll = 11;
            }
        }
        double damage = Compute.calculateClusterHitTableAmount(clusterRoll, getRackSize());
        if ((range == AlphaStrikeElement.SHORT_RANGE) && (getMinimumRange() > 0)) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }

    @Override
    public double getBattleForceDamage(int range, int baSquadSize) {
        if (range > getLongRange()) {
            return 0;
        }
        double damage = Compute.calculateClusterHitTableAmount(7, getRackSize() * baSquadSize);
        if ((range == AlphaStrikeElement.SHORT_RANGE) && (getMinimumRange() > 0)) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }

}
