/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public abstract class MRMWeapon extends MissileWeapon {

    private static final long serialVersionUID = 274817921444431878L;

    /**
     *
     */
    public MRMWeapon() {
        super();
        ammoType = AmmoType.T_MRM;
        toHitModifier = 1;
        availRating = new int[] { EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E };
        atClass = CLASS_MRM;
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
            WeaponAttackAction waa, IGame game, Server server) {
        return new MRMHandler(toHit, waa, game, server);
    }
    
    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        double damage = 0;
        if (range > getLongRange()) {
            return damage;
        }
        if (fcs != null && fcs.getType() instanceof MiscType
                && ((MiscType)fcs.getType()).hasFlag(MiscType.F_APOLLO)) {
            damage = Compute.calculateClusterHitTableAmount(6, getRackSize());
        } else {
            damage = Compute.calculateClusterHitTableAmount(7, getRackSize());
            damage *= 0.95; // +1 to hit            
        }
        if (range == 0 && getMinimumRange() > 0) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }
}
