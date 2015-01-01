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
 * Created on Oct 15, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jason Tighe
 */
public class BPodHandler extends AmmoWeaponHandler {


    /**
     *
     */
    private static final long serialVersionUID = -6710600713016145831L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public BPodHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /**
     * Calculate the clustering of the hits
     *
     * @return a <code>int</code> value saying how much hits are in each
     *         cluster of damage.
     */
    @Override
    protected int calcnCluster() {
        return 5;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        if (target instanceof BattleArmor) {
            return 10;
        } else {
            return 1;
        }
    }


    /**
     * Calculate the damage per hit.
     *
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = 0;
        // we default to direct fire weapons for anti-infantry damage
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            toReturn = Compute.d6();
            if ( ((Infantry)target).isMechanized() ){
                toReturn /=3;
            } else{
                toReturn /=2;
            }

            toReturn = Math.max(1, toReturn);
        }else if ( target instanceof BattleArmor ){
            toReturn = 1;
        }
        return (int) toReturn;
    }
}
