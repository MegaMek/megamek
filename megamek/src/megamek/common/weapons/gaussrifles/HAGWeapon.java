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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons.gaussrifles;

import megamek.common.AmmoType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.HAGWeaponHandler;
import megamek.server.gameManager.*;

/**
 * @author Sebastian Brocks
 */
public abstract class HAGWeapon extends GaussWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -2890339452762009216L;

    public HAGWeapon() {
        super();
        damage = DAMAGE_BY_CLUSTERTABLE;
        ammoType = AmmoType.T_HAG;
        flags = flags.or(F_NO_AIM);
        atClass = CLASS_AC;
        infDamageClass = WEAPON_CLUSTER_BALLISTIC;
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
            WeaponAttackAction waa, Game game, GameManager manager) {
        return new HAGWeaponHandler(toHit, waa, game, manager);
    }

    @Override
    public double getBattleForceDamage(int range) {
        if (rackSize == 20) {
            if (range == AlphaStrikeElement.SHORT_RANGE) {
                return 1.328;
            } else if (range <= AlphaStrikeElement.LONG_RANGE) {
                return 1.2;
            }
        } else if (rackSize == 30) {
            if (range == AlphaStrikeElement.SHORT_RANGE) {
                return 1.992;
            } else if (range <= AlphaStrikeElement.LONG_RANGE) {
                return 1.8;
            }
        } else {
            if (range == AlphaStrikeElement.SHORT_RANGE) {
                return 2.656;
            } else if (range <= AlphaStrikeElement.LONG_RANGE) {
                return 2.4;
            }
        }
        return 0;
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_FLAK;
    }
}
