/*
 * Copyright (c) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.missiles;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.lrms.LRMWeapon;
import megamek.common.weapons.srms.SRMWeapon;
import megamek.server.totalwarfare.TWGameManager;

import java.io.Serial;

/**
 * @author Sebastian Brocks
 */
public abstract class MMLWeapon extends MissileWeapon {
    @Serial
    private static final long serialVersionUID = 110779423352325731L;

    public MMLWeapon() {
        super();
        ammoType = AmmoType.T_MML;
        atClass = CLASS_MML;
        flags = flags.or(F_ARTEMIS_COMPATIBLE);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager) {
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId()).getLinked().getType();
        if (atype.hasFlag(AmmoType.F_MML_LRM)) {
            return LRMWeapon.getLRMHandler(toHit, waa, game, manager);
        } else {
            return SRMWeapon.getSRMHandler(toHit, waa, game, manager);
        }
    }

    @Override
    public int getBattleForceClass() {
        return BFCLASS_MML;
    }

    @Override
    public boolean hasIndirectFire() {
        return true;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Indirect Fire
        if (gameOptions.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }
}
