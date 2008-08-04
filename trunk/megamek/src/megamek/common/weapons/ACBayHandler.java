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
public class ACBayHandler extends AmmoBayWeaponHandler {
    
    private static final long serialVersionUID = -1618484541772117621L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public ACBayHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
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
            int ammoUsed = bayW.getCurrentShots();
            Mounted bayWAmmo = bayW.getLinked();
            if(null == bayWAmmo || bayWAmmo.getShotsLeft() < 1) {
                //try loading something else
                ae.loadWeaponWithSameAmmo(bayW);
                bayWAmmo = bayW.getLinked();
            }
           
            for(int i = 0; i < ammoUsed; i++) {
                //check the currently loaded ammo
                if(null == bayWAmmo || bayWAmmo.getShotsLeft() < 1) {
                    //try loading something else
                    ae.loadWeaponWithSameAmmo(bayW);
                    bayWAmmo = bayW.getLinked();
                }

                //this isn't exactly by the book, but if we run out of ammo
                //let's just multiple the av by the fraction of the shots
                //that we have out of the total.  Makes sense. Same thing goes for RAC
                //and UAC not in weapon bays
                
                if(!bayW.isBreached() && !bayW.isDestroyed() && !bayW.isJammed()
                        && bayWAmmo != null && bayWAmmo.getShotsLeft() > 0) {
                    //need to cycle through weapons and add av
                    WeaponType bayWType = ((WeaponType)bayW.getType());
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
                    //need to divide current_av by the number of shots in order for it to come out
                    //right
                    current_av = current_av / ammoUsed;
                    av = av + current_av;
                    //now use the ammo that we had loaded
                    if(current_av > 0) {
                        bayWAmmo.setShotsLeft(bayWAmmo.getShotsLeft() - 1);
                    }
                }
            }
        }       
        return (int)Math.ceil(av);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.UltraWeaponHandler#doChecks(java.util.Vector)
     */
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        for(int wId: weapon.getBayWeapons()) {    
            Mounted bayW = ae.getEquipment(wId);
            WeaponType bayWType = ((WeaponType)bayW.getType());
            int ammoUsed = bayW.getCurrentShots();
            if(bayWType.getAmmoType() == AmmoType.T_AC_ROTARY) {
                boolean jams = false;
                switch (ammoUsed) {
                    case 6:
                        if (roll <= 4) {
                            jams = true;
                        }
                        break;
                    case 5:
                    case 4:
                        if (roll <= 3) {
                            jams = true;
                        }
                        break;
                    case 3:
                    case 2:
                        if (roll <= 2) {
                            jams = true;
                        }
                        break;
                    default:
                        break;
                }
                if (jams) {
                    r = new Report(3170);
                    r.subject = subjectId;
                    r.add(" shot(s)");
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                    bayW.setJammed(true);
                }
            }
            else if (bayWType.getAmmoType() == AmmoType.T_AC_ULTRA) {
                if (roll == 2 && ammoUsed == 2) {
                    r = new Report();
                    r.subject = subjectId;
                    r.messageId = 3160;
                    r.newlines = 0;
                    bayW.setJammed(true);
                    bayW.setHit(true);
                    vPhaseReport.addElement(r);
                }
            }
        }
        
            return false;
    }
}
