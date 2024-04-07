/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.artillery;

import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.*;
import megamek.server.gameManager.GameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 25, 2004
 */
public abstract class ArtilleryWeapon extends AmmoWeapon {
    private static final long serialVersionUID = -732023379991213890L;

    public ArtilleryWeapon() {
        super();
        flags = flags.or(F_ARTILLERY).or(F_MECH_WEAPON).or(F_TANK_WEAPON);
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
            WeaponAttackAction waa, Game game, GameManager manager) {
        Mounted ammo = game.getEntity(waa.getEntityId())
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
