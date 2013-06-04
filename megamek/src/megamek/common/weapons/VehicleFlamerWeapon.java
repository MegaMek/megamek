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
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public abstract class VehicleFlamerWeapon extends AmmoWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -8729838198434670197L;

    /**
     *
     */
    public VehicleFlamerWeapon() {
        super();
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
                .or(F_BA_WEAPON).or(F_FLAMER).or(F_ENERGY).or(F_BURST_FIRE);
        ammoType = AmmoType.T_VEHICLE_FLAMER;
        String[] modeStrings = { "Damage", "Heat" };
        setModes(modeStrings);
        availRating = new int[] { EquipmentType.RATING_A,
                EquipmentType.RATING_A, EquipmentType.RATING_B };
        atClass = CLASS_POINT_DEFENSE;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                .getEquipment(waa.getWeaponId()).getLinked().getType();
        if ((game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId())
                .curMode().equals("Heat"))) {
            return new VehicleFlamerHeatHandler(toHit, waa, game, server);
        } else if (atype.getMunitionType() == AmmoType.M_COOLANT) {
            return new VehicleFlamerCoolHandler(toHit, waa, game, server);
        } else {
            return new VehicleFlamerHandler(toHit, waa, game, server);
        }
    }
}
