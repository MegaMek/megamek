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

package megamek.common.weapons.bayWeapons;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.AmmoBayWeaponHandler;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * This is my attempt to get weapon bays treated as normal weapons rather than the current hack in place
 *
 * @author Jay Lawson
 * @since Sep 24, 2004
 */
public abstract class AmmoBayWeapon extends BayWeapon {
    private static final long serialVersionUID = 4718603486868464292L;

    public AmmoBayWeapon() {
        super();
    }

    @Override
    public AttackHandler fire(WeaponAttackAction waa, Game game, TWGameManager manager) {
        // Just in case. Often necessary when/if multiple ammo weapons are
        // fired; if this line not present
        // then when one ammo slots run dry the rest silently don't fire.
        checkAmmo(waa, game);
        return super.fire(waa, game, manager);
    }

    protected void checkAmmo(WeaponAttackAction waa, Game g) {
        Entity ae = waa.getEntity(g);
        WeaponMounted bay = ae.getWeapon(waa.getWeaponId());
        for (WeaponMounted weapon : bay.getBayWeapons()) {
            AmmoMounted ammo = weapon.getLinkedAmmo();
            if (ammo == null || ammo.getUsableShotsLeft() < 1) {
                ae.loadWeaponWithSameAmmo(weapon);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.game.Game)
     */
    @Override
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        return new AmmoBayWeaponHandler(toHit, waa, game, manager);
    }
}
