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

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.HitData;
import megamek.common.Game;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * @author Jay Lawson
 * @since Feb 21, 2013
 */
public class SwarmWeaponAttackHandler extends WeaponHandler {
    private static final long serialVersionUID = -2439937071168853215L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public SwarmWeaponAttackHandler(ToHitData toHit, WeaponAttackAction waa,
            Game g, GameManager m) {
        super(toHit, waa, g, m);
        generalDamageType = HitData.DAMAGE_NONE;
    }

    @Override
    protected int calcDamagePerHit() {
        int damage = 0;
        if (ae instanceof BattleArmor) {
            BattleArmor ba = (BattleArmor) ae;
            damage = ba.calculateSwarmDamage();
        }
        // should this be affected by direct blows?
        // assume so for now
        if (bDirect) {
            damage = Math.min(damage + (toHit.getMoS() / 3), damage * 2);
        }
        return damage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        return 1;
    }
}
