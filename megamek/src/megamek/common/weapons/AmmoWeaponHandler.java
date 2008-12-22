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

import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class AmmoWeaponHandler extends WeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = -4934490646657484486L;
    Mounted ammo;

    /**
     * Used for deserialization. DO NOT USE OTHERWISE.
     */
    protected AmmoWeaponHandler() {
    }

    /**
     * @param t
     * @param w
     * @param g
     */
    public AmmoWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        generalDamageType = HitData.DAMAGE_BALLISTIC;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#UseAmmo()
     */
    @Override
    protected void useAmmo() {
        checkAmmo();
        if (ammo == null) {// Can't happen. w/o legal ammo, the weapon
            // *shouldn't* fire.
            System.out.println("Handler can't find any ammo!  Oh no!");
        }

        if (ammo.getShotsLeft() <= 0) {
            ae.loadWeaponWithSameAmmo(weapon);
            ammo = weapon.getLinked();
        }
        ammo.setShotsLeft(ammo.getShotsLeft() - 1);
        super.useAmmo();
    }

    protected void checkAmmo() {
        ammo = weapon.getLinked();
        if (ammo == null) {
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
        }
    }

    /**
     * For ammo weapons, this number can be less than the full number if the amount of ammo is not high enough
     * @return the number of weapons of this type firing (for squadron weapon groups)
     */
    @Override
    protected int getNumberWeapons() {
        if(ammo == null) {
            //shouldn't happen
            return weapon.getNWeapons();
        }
        int totalShots = ae.getTotalAmmoOfType(ammo.getType());
        return Math.min(weapon.getNWeapons(), (int)Math.floor((double)totalShots / (double)weapon.getCurrentShots()));
    }
}
