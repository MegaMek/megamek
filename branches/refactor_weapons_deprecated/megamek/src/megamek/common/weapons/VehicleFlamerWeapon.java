/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * 
 */
public abstract class VehicleFlamerWeapon extends AmmoWeapon {
    /**
     * 
     */
    public VehicleFlamerWeapon() {
        super();
        this.flags |= F_FLAMER;
        this.ammoType = AmmoType.T_VEHICLE_FLAMER;
        String modes[] = { "Damage", "Heat", "Cool" };
        this.setModes(modes);
    }

    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        if ((game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId())
                .curMode().equals("Heat"))) {
            return new VehicleFlamerHeatHandler(toHit, waa, game, server);
        } else if ((game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId())
                .curMode().equals("Cool"))) {
            return new VehicleFlamerCoolHandler(toHit, waa, game, server);
        } else {
            return new VehicleFlamerHandler(toHit, waa, game, server);
        }
    }
}
