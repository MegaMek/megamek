/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.artillery;

import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.ArtilleryWeaponDirectFireHandler;
import megamek.common.weapons.ArtilleryWeaponDirectHomingHandler;
import megamek.common.weapons.ArtilleryWeaponIndirectFireHandler;
import megamek.common.weapons.ArtilleryWeaponIndirectHomingHandler;
import megamek.common.weapons.AttackHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 25, 2004
 */
public abstract class ArtilleryWeapon extends AmmoWeapon {
    private static final long serialVersionUID = -732023379991213890L;

    public ArtilleryWeapon() {
        super();
        flags = flags.or(F_ARTILLERY).or(F_MEK_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON);
        damage = DAMAGE_ARTILLERY;
        atClass = CLASS_ARTILLERY;
    }

    @Override
    public boolean isAlphaStrikeIndirectFire() {
        return false;
    }

    @Override
    public boolean hasIndirectFire() {
        return true;
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
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        Mounted<?> ammo = game.getEntity(waa.getEntityId())
              .getEquipment(waa.getWeaponId()).getLinked();

        if (ammo.isHomingAmmoInHomingMode()) {
            if (game.getPhase().isFiring()) {
                return new ArtilleryWeaponDirectHomingHandler(toHit, waa, game, manager);
            }
            return new ArtilleryWeaponIndirectHomingHandler(toHit, waa, game, manager);
        } else if (game.getPhase().isFiring()) {
            return new ArtilleryWeaponDirectFireHandler(toHit, waa, game, manager);
        } else {
            return new ArtilleryWeaponIndirectFireHandler(toHit, waa, game, manager);
        }
    }
}
