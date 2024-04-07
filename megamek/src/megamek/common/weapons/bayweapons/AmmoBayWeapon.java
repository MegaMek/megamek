/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.bayweapons;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AmmoBayWeaponHandler;
import megamek.common.weapons.AttackHandler;
import megamek.server.gameManager.*;

/**
 * This is my attempt to get weapon bays treated as normal weapons rather than the current hack in
 * place
 * @author Jay Lawson
 * @since Sep 24, 2004
 */
public abstract class AmmoBayWeapon extends BayWeapon {
    private static final long serialVersionUID = 4718603486868464292L;

    public AmmoBayWeapon() {
        super();
    }

    @Override
    public AttackHandler fire(WeaponAttackAction waa, Game game, GameManager manager) {
        // Just in case. Often necessary when/if multiple ammo weapons are
        // fired; if this line not present
        // then when one ammo slots run dry the rest silently don't fire.
        checkAmmo(waa, game);
        return super.fire(waa, game, manager);
    }

    protected void checkAmmo(WeaponAttackAction waa, Game g) {
        Entity ae = waa.getEntity(g);
        Mounted m = ae.getEquipment(waa.getWeaponId());
        for (int wId : m.getBayWeapons()) {
            Mounted weapon = ae.getEquipment(wId);
            Mounted ammo = weapon.getLinked();
            if (ammo == null || ammo.getUsableShotsLeft() < 1) {
                ae.loadWeaponWithSameAmmo(weapon);
                ammo = weapon.getLinked();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new AmmoBayWeaponHandler(toHit, waa, game, manager);
    }
}
