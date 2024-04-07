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

import megamek.common.ComputeECM;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.gameManager.*;

/**
 * @author Jason Tighe
 */
public class LRMFollowTheLeaderHandler extends LRMHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 1740643533757582922L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public LRMFollowTheLeaderHandler(ToHitData t, WeaponAttackAction w,
            Game g, GameManager m) {
        super(t, w, g, m);
        sSalvoType = " FTL missile(s) ";
        nSalvoBonus = 1;
    }

    @Override
    public int getSalvoBonus() {
        if (ComputeECM.isAffectedByECM(ae, ae.getPosition(), target.getPosition())) {
            return 0;
        } else {
            return nSalvoBonus;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calcnCluster() {
        if (ComputeECM.isAffectedByECM(ae, ae.getPosition(), target.getPosition())) {
            return super.calcnCluster();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return 1;
    }
}
