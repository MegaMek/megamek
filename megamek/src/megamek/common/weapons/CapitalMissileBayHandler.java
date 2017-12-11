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

import java.util.ArrayList;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class CapitalMissileBayHandler extends AmmoBayWeaponHandler {

    /**
     * 
     */

    private static final long serialVersionUID = -1618484541772117621L;
    boolean advancedPD = false;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public CapitalMissileBayHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
    }
    @Override
    protected int calcAttackValue() {

        double av = 0;
        double counterAV = calcCounterAV();
        int armor = 0;
        int weaponarmor = 0;
        int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true, false);

        for (int wId : weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();
            if (null == bayWAmmo || bayWAmmo.getUsableShotsLeft() < 1) {
                // try loading something else
                ae.loadWeaponWithSameAmmo(bayW);
                bayWAmmo = bayW.getLinked();
            }
            if (!bayW.isBreached()
                    && !bayW.isDestroyed()
                    && !bayW.isJammed()
                    && bayWAmmo != null
                    && ae.getTotalAmmoOfType(bayWAmmo.getType()) >= bayW
                            .getCurrentShots()) {
                WeaponType bayWType = ((WeaponType) bayW.getType());
                // need to cycle through weapons and add av
                double current_av = 0;

                AmmoType atype = (AmmoType) bayWAmmo.getType();
                if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                		&& (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                		        || atype.hasFlag(AmmoType.F_PEACEMAKER))) {
                	weaponarmor = 40;
                } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                		&& (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
                                || atype.hasFlag(AmmoType.F_SANTA_ANNA))) {
                	weaponarmor = 30;
                } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                		&& atype.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                	weaponarmor = 20;
                } else {
                weaponarmor = bayWType.getMissileArmor();
                }
                if (range == WeaponType.RANGE_SHORT) {
                    current_av = bayWType.getShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    current_av = bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    current_av = bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    current_av = bayWType.getExtAV();
                }
                
                if (atype.hasFlag(AmmoType.F_NUCLEAR)) {
                    nukeS2S = true;
                }
                
                current_av = updateAVforAmmo(current_av, atype, bayWType,
                        range, wId);
                av = av + current_av;
                armor = armor + weaponarmor;
                // now use the ammo that we had loaded
                if (current_av > 0) {
                    int shots = bayW.getCurrentShots();
                    for (int i = 0; i < shots; i++) {
                        if (null == bayWAmmo
                                || bayWAmmo.getUsableShotsLeft() < 1) {
                            // try loading something else
                            ae.loadWeaponWithSameAmmo(bayW);
                            bayWAmmo = bayW.getLinked();
                        }
                        if (null != bayWAmmo) {
                            bayWAmmo.setShotsLeft(bayWAmmo.getBaseShotsLeft() - 1);
                        }
                    }
                }
                
                // check for tele-missiles and if they are there then
                // I will need to
                // add them to an inserted attack list and reset the av
                //TODO: Telemissiles are broken with the PD changes. I'll fix this soon.
                /* if (atype.hasFlag(AmmoType.F_TELE_MISSILE)) {
                    insertedAttacks.addElement(wId);
                    av = av - current_av;
                } */
            }
        }
        
        CapMissileArmor = armor - (int) counterAV;
        CapMissileAMSMod = calcCapMissileAMSMod();
        
        
            if (bDirect) {
                av = Math.min(av + (toHit.getMoS() / 3), av * 2);
            }
            if (bGlancing) {
                av = (int) Math.floor(av / 2.0);
            }
            av = (int) Math.floor(getBracketingMultiplier() * av);
            return (int) Math.ceil(av);
        // }
    }
        
    @Override
    protected int calcCapMissileAMSMod() {
        CapMissileAMSMod = (int) Math.ceil(CounterAV / 10.0);
        return CapMissileAMSMod;
    }
    
    @Override
    protected int getCapMissileAMSMod() {
        return CapMissileAMSMod;
    }

    @Override
    protected int getCapMisMod() {
        int mod = 0;
        for (int wId : weapon.getBayWeapons()) {
            int curr_mod = 0;
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();
            AmmoType atype = (AmmoType) bayWAmmo.getType();
            curr_mod = getCritMod(atype);
            if (curr_mod > mod) {
                mod = curr_mod;
            }
        }
        return mod;
    }

    /*
     * get the cap mis mod given a single ammo type
     */
    protected int getCritMod(AmmoType atype) {
        if (atype == null || atype.getAmmoType() == AmmoType.T_PIRANHA) {
            return 0;
        }
        if (atype.getAmmoType() == AmmoType.T_WHITE_SHARK
                || atype.getAmmoType() == AmmoType.T_WHITE_SHARK_T
                || atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
                // Santa Anna, per IO rules
                || atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
            return 9;
        } else if (atype.getAmmoType() == AmmoType.T_KRAKEN_T
                || atype.getAmmoType() == AmmoType.T_KRAKENM
                // Peacemaker, per IO rules
                || atype.hasFlag(AmmoType.F_PEACEMAKER)) {
            return 8;
        } else if (atype.getAmmoType() == AmmoType.T_KILLER_WHALE
                || atype.getAmmoType() == AmmoType.T_KILLER_WHALE_T
                || atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                || atype.getAmmoType() == AmmoType.T_MANTA_RAY
                || atype.getAmmoType() == AmmoType.T_ALAMO) {
            return 10;
        } else if (atype.getAmmoType() == AmmoType.T_STINGRAY) {
            return 12;
        } else {
            return 11;
        }
    }
    
    @Override
    protected double updateAVforAmmo(double current_av, AmmoType atype,
            WeaponType bayWType, int range, int wId) {
        //AR10 munitions
    	if (atype.getAmmoType() == AmmoType.T_AR10) {
            if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                current_av = 4;
            } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                current_av = 3;
            } else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
                current_av = 1000;
            } else if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                current_av = 100;
            } else {
                current_av = 2;
            }
        }
    	//Nuclear Warheads for non-AR10 missiles
    	if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
    	    current_av = 100;
    	} else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
            current_av = 1000;
        }    	
        return current_av;
    } 
    /**
     * Insert any additional attacks that should occur before this attack
     */
    @Override
    protected void insertAttacks(IGame.Phase phase, Vector<Report> vPhaseReport) {
        // If there are no other missiles in the bay that aren't inserted
        // attacks, there will be a spurious "no damage" report
        if (attackValue < 1) {
            vPhaseReport.clear();
        }

        for (int wId : insertedAttacks) {
            Mounted bayW = ae.getEquipment(wId);
            WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),
                    waa.getTargetId(), wId);
            Weapon w = (Weapon) bayW.getType();
            // increase ammo by one, we'll use one that we shouldn't use
            // in the next line
            Vector<Report> newReports = new Vector<>();
            bayW.getLinked().setShotsLeft(
                    bayW.getLinked().getBaseShotsLeft() + 1);
            (w.fire(newWaa, game, server)).handle(phase, newReports);
            for (Report r : newReports) {
                r.indent();
            }
            vPhaseReport.addAll(newReports);
        }
    }
    
    // check for AMS and Point Defense Bay fire
    @Override
    protected int calcCounterAV() {
        if ((target == null)
                || (target.getTargetType() != Targetable.TYPE_ENTITY)
                || !advancedPD) {
            return 0;
        }
        int counterAV = 0;
        int amsAV = 0;
        double pdAV = 0;
        Entity entityTarget = (Entity) target;
        // any AMS bay attacks by the target?
        ArrayList<Mounted> lCounters = waa.getCounterEquipment();
        if (null != lCounters) {
            for (Mounted counter : lCounters) {               
                boolean isAMSBay = counter.getType().hasFlag(WeaponType.F_AMSBAY);
                boolean isPDBay = counter.getType().hasFlag(WeaponType.F_PDBAY);
                Entity pdEnt = counter.getEntity();
                boolean isInArc;
                // If the defending unit is the target, use attacker for arc
                if (entityTarget.equals(pdEnt)) {
                    isInArc = Compute.isInArc(game, pdEnt.getId(),
                            pdEnt.getEquipmentNum(counter),
                            ae);
                } else { // Otherwise, the attack must pass through an escort unit's hex
                	// TODO: We'll get here, eventually
                    isInArc = Compute.isInArc(game, pdEnt.getId(),
                            pdEnt.getEquipmentNum(counter),
                            entityTarget);
                }
                if (isAMSBay) {
                	amsAV = 0;
                    // Point defenses can't fire if they're not ready for any reason
		            if (!(counter.getType() instanceof WeaponType)
	                         || !counter.isReady() || counter.isMissing()
	                            // shutdown means no Point defenses
	                            || pdEnt.isShutDown()
	                            // Point defenses only fire vs attacks in arc
	                            || !isInArc
	                            // Point defense bays must have at least 2 weapons to affect capital missiles
	                            || (counter.getBayWeapons().size() < 2)) {
	                        continue;
	                }
		            // Now for heat, damage and ammo we need the individual weapons in the bay
                    for (int wId : counter.getBayWeapons()) {
                        Mounted bayW = pdEnt.getEquipment(wId);
                        Mounted bayWAmmo = bayW.getLinked();
                        WeaponType bayWType = ((WeaponType) bayW.getType());
                        
                        // build up some heat
                        //First Check to see if we have enough heat capacity to fire
                        if ((pdEnt.heatBuildup + bayW.getCurrentHeat()) > pdEnt.getHeatCapacity()) {
                            continue;
                        }
                        if (counter.getType().hasFlag(WeaponType.F_HEATASDICE)) {
		            		pdEnt.heatBuildup += Compute.d6(bayW
		            				.getCurrentHeat());	                    
		            	} else {
		            	    pdEnt.heatBuildup += bayW.getCurrentHeat();
	                    }
                        
                        //Bays use lots of ammo. Check to make sure we haven't run out
                        if (bayWAmmo != null) {
                            if (bayWAmmo.getBaseShotsLeft() < counter.getBayWeapons().size()) {
                                continue;
                            }
                            // decrement the ammo
                        	bayWAmmo.setShotsLeft(Math.max(0,
                        		bayWAmmo.getBaseShotsLeft() - 1));
                        }
                        
                        // get the attack value
                        amsAV += bayWType.getShortAV();                                      
            		}
                                                            
                } else if (isPDBay) {
                    pdAV = 0;
                    // Point defenses can't fire if they're not ready for any reason
		            if (!(counter.getType() instanceof WeaponType)
	                         || !counter.isReady() || counter.isMissing()
	                            // shutdown means no Point defenses
	                            || pdEnt.isShutDown()
	                            // Point defenses only fire vs attacks in arc
	                            || !isInArc
	                            // Point defense bays must have at least 2 weapons to affect capital missiles
	                            || (counter.getBayWeapons().size() < 2)
	                            // Point defense bays only fire once per round
	                            || counter.isUsedThisRound() == true) {
	                        continue;
	                }
		            // Now for heat, damage and ammo we need the individual weapons in the bay
                    for (int wId : counter.getBayWeapons()) {
                        Mounted bayW = pdEnt.getEquipment(wId);
                        Mounted bayWAmmo = bayW.getLinked();
                        WeaponType bayWType = ((WeaponType) bayW.getType());
                        
                        // build up some heat
                        //First Check to see if we have enough heat capacity to fire
                        if ((pdEnt.heatBuildup + bayW.getCurrentHeat()) > pdEnt.getHeatCapacity()) {
                            continue;
                        }
                        if (counter.getType().hasFlag(WeaponType.F_HEATASDICE)) {
                            pdEnt.heatBuildup += Compute.d6(bayW
                                    .getCurrentHeat());                     
                        } else {
                            pdEnt.heatBuildup += bayW.getCurrentHeat();
                        }
                        
                        //Bays use lots of ammo. Check to make sure we haven't run out
                        if (bayWAmmo != null) {
                            if (bayWAmmo.getBaseShotsLeft() < counter.getBayWeapons().size()) {
                                continue;
                            }
                            // decrement the ammo
                            bayWAmmo.setShotsLeft(Math.max(0,
                                bayWAmmo.getBaseShotsLeft() - 1));
                        }
                        
                        // get the attack value
                        pdAV += bayWType.getShortAV();                    
            		}
                    
                    // set the pdbay as having fired, if it was able to
                    if (pdAV > 0 ) {
                        counter.setUsedThisRound(true);                        
                    }
                                 
                } //end PDBay fire 
                
                // non-AMS only add half their damage, rounded up
                
                // set the pdbay as having fired, if it did
                if (pdAV > 0) {
                    pdBayEngagedCap = true;
                }
                counterAV += (int) Math.ceil(pdAV / 2.0);
                
                // set the ams as having fired, if it did
                if (amsAV > 0) {
                    amsBayEngagedCap = true;
                }
                // AMS add their full damage
                counterAV += amsAV;
            } //end "for Mounted counter"
        } // end check for counterfire
        CounterAV = (int) counterAV;
        return counterAV;
    } // end getAMSAV
    
    @Override
    protected int getCounterAV() {
    	return CounterAV;
    }
}
