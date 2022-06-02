/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.autocannons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.RACHandler;
import megamek.common.weapons.UltraWeaponHandler;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public abstract class RACWeapon extends UACWeapon {
    private static final long serialVersionUID = 659000035767322660L;

    public RACWeapon() {
        super();
        ammoType = AmmoType.T_AC_ROTARY;
        String[] modeStrings = {MODE_AC_SINGLE, MODE_RAC_TWO_SHOT, MODE_RAC_THREE_SHOT,
                MODE_RAC_FOUR_SHOT, MODE_RAC_FIVE_SHOT, MODE_RAC_SIX_SHOT};
        setModes(modeStrings);
        // explosive when jammed
        explosive = true;
        explosionDamage = damage;
        atClass = CLASS_AC;
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
        Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(
                waa.getWeaponId());
        if (weapon.curMode().equals(MODE_RAC_SIX_SHOT)
                || weapon.curMode().equals(MODE_RAC_FIVE_SHOT)
                || weapon.curMode().equals(MODE_RAC_FOUR_SHOT)
                || weapon.curMode().equals(MODE_RAC_THREE_SHOT)) {
            return new RACHandler(toHit, waa, game, manager);
        } else {
            return new UltraWeaponHandler(toHit, waa, game, manager);
        }
    }

    @Override
    public double getBattleForceDamage(int range) {
        double damage = 0;
        if (range <= getLongRange()) {
            damage = getRackSize() * 6;
            if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
        }
        return damage / 10.0;
    }
}
