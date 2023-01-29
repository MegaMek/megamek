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

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.*;
import megamek.server.GameManager;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class ArtilleryBayWeapon extends AmmoBayWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public ArtilleryBayWeapon() {
        super();
        // tech levels are a little tricky
        this.flags = flags.or(F_ARTILLERY);
        this.name = "Artillery Bay";
        this.setInternalName(EquipmentTypeLookup.ARTILLERY_BAY);
        this.heat = 0;
        this.damage = DAMAGE_VARIABLE;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 20;
        this.extremeRange = 25;
        this.tonnage = 0.0;
        this.bv = 0;
        this.cost = 0;
        this.atClass = CLASS_ARTILLERY;
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
        Entity ae = game.getEntity(waa.getEntityId());
        boolean useHoming = false;
        for (int wId : ae.getEquipment(waa.getWeaponId()).getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();
            waa.setAmmoId(ae.getEquipmentNum(bayWAmmo));
            waa.setAmmoCarrier(ae.getId());
            if (bayWAmmo.isHomingAmmoInHomingMode()) {
                useHoming = true;
            }
            //We only need to get this information for the first weapon in the bay to return the right handler
            break;
        }
        if (useHoming) {
            if (game.getPhase().isFiring()) {
                return new ArtilleryBayWeaponDirectHomingHandler(toHit, waa, game, manager);
            }
            return new ArtilleryBayWeaponIndirectHomingHandler(toHit, waa, game, manager);
        } else if (game.getPhase().isFiring()) {
            return new ArtilleryBayWeaponDirectFireHandler(toHit, waa, game, manager);
        } else {
            return new ArtilleryBayWeaponIndirectFireHandler(toHit, waa, game, manager);
        }
    }
}
