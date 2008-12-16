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
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class SpaceBombAttackHandler extends WeaponHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -2439937071168853215L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public SpaceBombAttackHandler(ToHitData toHit, WeaponAttackAction waa, IGame g,
            Server s) {
        super(toHit, waa, g, s);
        generalDamageType = HitData.DAMAGE_NONE;
    }

    /**
     * Calculate the attack value based on range
     * 
     * @return an <code>int</code> representing the attack value at that range.
     */
    protected int calcAttackValue() {
        return waa.getBombPayload().size();
    }
    
    /**
     * Does this attack use the cluster hit table?
     * necessary to determine how Aero damage should be applied
     */
    protected boolean usesClusterTable() {
        return true;
    }
    
    @Override
    protected void useAmmo() {
        for(Mounted bomb : waa.getBombPayload()) {
            bomb.setShotsLeft(0);
        }
        super.useAmmo();
    }
}
