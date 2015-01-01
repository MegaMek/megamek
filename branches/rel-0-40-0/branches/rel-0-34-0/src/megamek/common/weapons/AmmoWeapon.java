/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */
/*
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public abstract class AmmoWeapon extends Weapon {
    /**
     * 
     */
    private static final long serialVersionUID = -1657672242932169730L;

    public AmmoWeapon() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#fire(megamek.common.actions.WeaponAttackAction,
     *      megamek.common.IGame)
     */
    public AttackHandler fire(WeaponAttackAction waa, IGame game, Server server) {
        // Just in case. Often necessary when/if multiple ammo weapons are
        // fired; if this line not present
        // then when one ammo slots run dry the rest silently don't fire.
        checkAmmo(waa, game);
        return super.fire(waa, game, server);
    }

    /**
     * 
     */
    protected void checkAmmo(WeaponAttackAction waa, IGame g) {
        Entity ae = waa.getEntity(g);
        Mounted weapon = ae.getEquipment(waa.getWeaponId());
        Mounted ammo = weapon.getLinked();
        if (ammo == null || ammo.getShotsLeft() < 1) {
            ae.loadWeaponWithSameAmmo(weapon);
            ammo = weapon.getLinked();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new AmmoWeaponHandler(toHit, waa, game, server);
    }
}
