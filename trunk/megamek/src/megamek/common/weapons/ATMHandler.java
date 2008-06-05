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

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ATMHandler extends MissileWeaponHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -2536312899803153911L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public ATMHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        int toReturn;
        AmmoType atype = (AmmoType) ammo.getType();
        if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
            sSalvoType = " high-explosive missile(s) ";
            toReturn = 3;
        } else if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
            sSalvoType = " extended-range missile(s) ";
            toReturn = 1;
        } else {
            toReturn = 2;
        }
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            toReturn = (int) Math.ceil(toReturn * wtype.getRackSize() / 5);
            if (bGlancing)
                toReturn = (int) Math.floor(toReturn / 2.0);;
            return toReturn;
        }
            
        return toReturn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    protected int calcnCluster() {
        return 5;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // don't need to check for BAs, because BA can't mount ATMs
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            return 1;
        }
        int hits = super.calcHits(vPhaseReport);
        // change to 5 damage clusters here, after AMS has been done
        hits = nDamPerHit * hits;
        nDamPerHit = 1;
        return hits;
    }
    
    /**
     * Calculate the attack value based on range
     * 
     * @return an <code>int</code> representing the attack value at that range.
     */
    protected int calcAttackValue() {
        int distance = ae.getPosition().distance(target.getPosition());
        int av = 0;
        int range = RangeType.rangeBracket(distance, wtype.getATRanges(), true);       
        AmmoType atype = (AmmoType) ammo.getType();
        if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
            if(range == WeaponType.RANGE_SHORT) {
                av = wtype.getRoundShortAV();
                av = av + av/2;
            }
        } else if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
            if(range == WeaponType.RANGE_SHORT) {
                av = wtype.getRoundShortAV();
            } else if(range == WeaponType.RANGE_MED) {
                av = wtype.getRoundMedAV();
            } else if (range == WeaponType.RANGE_LONG) {
                av = wtype.getRoundLongAV();
            } else if (range == WeaponType.RANGE_EXT) {
                av = wtype.getRoundLongAV();
            }
            av = av/2;
        } else {
            if(range == WeaponType.RANGE_SHORT) {
                av = wtype.getRoundShortAV();
            } else if(range == WeaponType.RANGE_MED) {
                av = wtype.getRoundMedAV();
            } else if (range == WeaponType.RANGE_LONG) {
                av = wtype.getRoundLongAV();
            } else if (range == WeaponType.RANGE_EXT) {
                av = wtype.getRoundExtAV();
            }
        }
        return av;
    }
}
