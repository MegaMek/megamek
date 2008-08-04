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
import megamek.common.IGame;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class AmmoBayWeaponHandler extends BayWeaponHandler {
    
    private static final long serialVersionUID = -1618484541772117621L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public AmmoBayWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }
 
    /**
     * Calculate the attack value based on range
     * This needs to do additional work for Weapon Bays with ammo.
     * I need to use the ammo within this function because I may run out of ammo
     * while going through the loop
     * Sine this function is called in the WeaponHandler constructor it should
     * be ok to use the ammo here
     * @return an <code>int</code> representing the attack value at that range.
     */
    protected int calcAttackValue() {
        int distance = ae.getPosition().distance(target.getPosition());
        double av = 0;
        int range = RangeType.rangeBracket(distance, wtype.getATRanges(), true);

        for(int wId: weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            //check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();
            if(null == bayWAmmo || bayWAmmo.getShotsLeft() < 1) {
                //try loading something else
                ae.loadWeaponWithSameAmmo(bayW);
                bayWAmmo = bayW.getLinked();
            }
            
            if(!bayW.isBreached() && !bayW.isDestroyed() && !bayW.isJammed()
                    && bayWAmmo != null && bayWAmmo.getShotsLeft() > 0) {
                WeaponType bayWType = ((WeaponType)bayW.getType());
                //need to cycle through weapons and add av
                double current_av = 0;
                if(range == WeaponType.RANGE_SHORT) {
                    current_av =  bayWType.getShortAV();
                } else if(range == WeaponType.RANGE_MED) {
                    current_av = bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    current_av = bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    current_av = bayWType.getExtAV();
                }
                current_av = updateAVforAmmo(current_av, (AmmoType)bayWAmmo.getType(), 
                                             bayWType, range);               
                av = av + current_av;
                //now use the ammo that we had loaded
                if(current_av > 0) {
                    bayWAmmo.setShotsLeft(bayWAmmo.getShotsLeft() - 1);
                }
            }
        }       
        return (int)Math.ceil(av);
    }
    
    /*
     * check for special munitions and their effect on av
     * TODO: it might be better to have unique weapon handlers 
     * for these by bay, but I am lazy
     */   
    protected double updateAVforAmmo(double current_av, AmmoType atype, WeaponType wtype, int range) {     
        
        //check for artemisIV
        Mounted mLinker = weapon.getLinkedBy();
        int bonus = 0;
        if ((mLinker != null && mLinker.getType() instanceof MiscType
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                MiscType.F_ARTEMIS))
                && atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE) {
            bonus = (int)Math.ceil(atype.getRackSize() / 5.0);
            if(atype.getAmmoType() == AmmoType.T_SRM) {
                bonus = 2;
            }
            current_av = current_av + bonus;
        }
        
        if(atype.getMunitionType() == AmmoType.M_CLUSTER) {
            current_av = Math.floor(0.6*current_av);
        }
        else if (AmmoType.T_ATM == atype.getAmmoType()) {
            if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                current_av = wtype.getExtAV()/2;                       
            } else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                current_av = 1.5 * current_av;
                if(range > WeaponType.RANGE_SHORT) {
                    current_av = 0.0;
                }
            } 
        }
        else if (atype.getAmmoType() == AmmoType.T_MML && !atype.hasFlag(AmmoType.F_MML_LRM)) {
          current_av = 2 * current_av;
          if(range > WeaponType.RANGE_SHORT) {
              current_av = 0;
          }
        } 
        else if (atype.getAmmoType() == AmmoType.T_AR10) {
            if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                current_av = 4;
            } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                current_av = 3;
            } else {
                current_av = 2;
            }
        }
        return current_av;
    }   
}
