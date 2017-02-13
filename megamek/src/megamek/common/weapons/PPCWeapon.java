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
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.BattleForceElement;
import megamek.common.IGame;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public abstract class PPCWeapon extends EnergyWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -8128018700095507410L;

    public PPCWeapon() {
        super();
        flags = flags.or(F_DIRECT_FIRE).or(F_PPC);
        atClass = CLASS_PPC;
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
        return new PPCHandler(toHit, waa, game, server);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted capacitor) {
        double damage = 0;
        if (range <= getLongRange()) {
            //Variable damage weapons that cannot reach into the BF long range band use LR damage for the MR band
            if (getDamage() == DAMAGE_VARIABLE
                    && range == BattleForceElement.MEDIUM_RANGE
                    && getLongRange() < BattleForceElement.LONG_RANGE) {
                damage = getDamage(BattleForceElement.LONG_RANGE);
            } else {
                damage = getDamage(range);
            }
            if (capacitor != null && capacitor.getType() instanceof MiscType
                    && ((MiscType)capacitor.getType()).hasFlag(MiscType.F_PPC_CAPACITOR)) {
                damage = (damage + 5) / 2;
            }
            if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
            if (getToHitModifier() != 0) {
                damage -= damage * getToHitModifier() * 0.05; 
            }
        }
        return damage / 10.0;        
    }
}
