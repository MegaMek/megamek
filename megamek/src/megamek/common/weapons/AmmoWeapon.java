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
package megamek.common.weapons;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public abstract class AmmoWeapon extends Weapon {
    private static final long serialVersionUID = -1657672242932169730L;

    public AmmoWeapon() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#fire(megamek.common.actions.WeaponAttackAction
     * , megamek.common.Game)
     */
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
        WeaponMounted weapon = (WeaponMounted) ae.getEquipment(waa.getWeaponId());
        AmmoMounted ammo = weapon.getLinkedAmmo();
        if (ammo == null || ammo.getUsableShotsLeft() < 1) {
            ae.loadWeaponWithSameAmmo(weapon);
            ammo = weapon.getLinkedAmmo();
            // We need to make the WAA ammoId match the new ammo bin
            waa.setAmmoId(ae.getEquipmentNum(ammo));
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
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, TWGameManager manager) {
        return new AmmoWeaponHandler(toHit, waa, game, manager);
    }
}
