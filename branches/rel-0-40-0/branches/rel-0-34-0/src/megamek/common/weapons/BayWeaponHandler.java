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

import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class BayWeaponHandler extends WeaponHandler {

    /**
     * 
     */
    
    private static final long serialVersionUID = -1618484541772117621L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public BayWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }
 
    /**
     * Calculate the attack value based on range
     * 
     * @return an <code>int</code> representing the attack value at that range.
     */
    protected int calcAttackValue() {
        int distance = ae.getPosition().distance(target.getPosition());
        double av = 0;
        int range = RangeType.rangeBracket(distance, wtype.getATRanges(), true);
        
        for(int wId: weapon.getBayWeapons()) {
            Mounted m = ae.getEquipment(wId);
            if(!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                WeaponType bayWType = ((WeaponType)m.getType());
                //need to cycle through weapons and add av
                if(range == WeaponType.RANGE_SHORT) {
                    av = av + bayWType.getShortAV();
                } else if(range == WeaponType.RANGE_MED) {
                    av = av + bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    av = av + bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    av = av + bayWType.getExtAV();
                }
            }
        }
        return (int)Math.ceil(av);
    }
    
    protected void addHeat() {
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            if(game.getOptions().booleanOption("heat_by_bay")) {
                for(int wId:weapon.getBayWeapons()) {
                    Mounted m = ae.getEquipment(wId);
                    ae.heatBuildup += m.getCurrentHeat();
                }
            } else {           
                int loc = weapon.getLocation();
                boolean rearMount = weapon.isRearMounted();
                if(!ae.hasArcFired(loc, rearMount)) {
                    ae.heatBuildup += ae.getHeatInArc(loc, rearMount);
                    ae.setArcFired(loc, rearMount);
                }
            }
        }
    }
    
}
