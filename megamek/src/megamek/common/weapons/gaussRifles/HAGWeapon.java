/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

/*
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons.gaussRifles;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.HAGWeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class HAGWeapon extends GaussWeapon {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -2890339452762009216L;

    public HAGWeapon() {
        super();
        damage = DAMAGE_BY_CLUSTER_TABLE;
        ammoType = AmmoType.AmmoTypeEnum.HAG;
        flags = flags.or(F_NO_AIM);
        atClass = CLASS_AC;
        infDamageClass = WEAPON_CLUSTER_BALLISTIC;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.game.Game)
     */
    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            return new HAGWeaponHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    @Override
    public double getBattleForceDamage(int range) {
        if (rackSize == 20) {
            if (range == AlphaStrikeElement.SHORT_RANGE) {
                return 1.328;
            } else if (range <= AlphaStrikeElement.LONG_RANGE) {
                return 1.2;
            }
        } else if (rackSize == 30) {
            if (range == AlphaStrikeElement.SHORT_RANGE) {
                return 1.992;
            } else if (range <= AlphaStrikeElement.LONG_RANGE) {
                return 1.8;
            }
        } else {
            if (range == AlphaStrikeElement.SHORT_RANGE) {
                return 2.656;
            } else if (range <= AlphaStrikeElement.LONG_RANGE) {
                return 2.4;
            }
        }
        return 0;
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_FLAK;
    }
}
