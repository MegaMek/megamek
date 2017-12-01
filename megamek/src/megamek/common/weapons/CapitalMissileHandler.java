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

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class CapitalMissileHandler extends AmmoWeaponHandler {

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
    public CapitalMissileHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
    }
    
    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        int av = 0;
        double counterAV = calcCounterAV();
        int armor = wtype.getMissileArmor();
        // if we have a ground firing unit, then AV should not be determined by
        // aero range brackets
        if (!ae.isAirborne() || game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UAC_TWOROLLS)) {
            if (usesClusterTable()) {
                // for cluster weapons just use the short range AV
                av = wtype.getRoundShortAV();
            } else {
                // otherwise just use the full weapon damage by range
                av = wtype.getDamage(nRange);
            }
        } else {
            // we have an airborne attacker, so we need to use aero range
            // brackets
            int range = RangeType.rangeBracket(nRange, wtype.getATRanges(),
                    true, false);
            if (range == WeaponType.RANGE_SHORT) {
                av = wtype.getRoundShortAV();
            } else if (range == WeaponType.RANGE_MED) {
                av = wtype.getRoundMedAV();
            } else if (range == WeaponType.RANGE_LONG) {
                av = wtype.getRoundLongAV();
            } else if (range == WeaponType.RANGE_EXT) {
                av = wtype.getRoundExtAV();
            }
        }
        
        if (ammo.getType().hasFlag(AmmoType.F_NUCLEAR)) {
            nukeS2S = true;
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
        
        return av;
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
	                            || !isInArc) {
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
    protected int getCapMisMod() {
        return getCritMod((AmmoType) ammo.getType());

    }


    /*
     * get the cap mis mod given a single ammo type
     */
    protected int getCritMod(AmmoType atype) {
        if (atype == null || atype.getAmmoType() == AmmoType.T_PIRANHA
                || atype.getAmmoType() == AmmoType.T_AAA_MISSILE
                || atype.getAmmoType() == AmmoType.T_ASEW_MISSILE
                || atype.getAmmoType() == AmmoType.T_LAA_MISSILE) {
            return 0;
        }
        if (atype.getAmmoType() == AmmoType.T_WHITE_SHARK
                || atype.getAmmoType() == AmmoType.T_WHITE_SHARK_T
                || atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
                // Santa Anna, per IO rules
                || atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
            return 9;
        } else if (atype.getAmmoType() == AmmoType.T_KILLER_WHALE
                || atype.getAmmoType() == AmmoType.T_KILLER_WHALE_T
                || atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                || atype.getAmmoType() == AmmoType.T_MANTA_RAY
                || atype.getAmmoType() == AmmoType.T_ALAMO) {
            return 10;
        } else if (atype.getAmmoType() == AmmoType.T_KRAKEN_T
                || atype.getAmmoType() == AmmoType.T_KRAKENM
                // Peacemaker, per IO rules
                || atype.hasFlag(AmmoType.F_PEACEMAKER)) {
            return 8;
        } else if (atype.getAmmoType() == AmmoType.T_STINGRAY) {
            return 12;
        } else {
            return 11;
        }
    }
    
    @Override
    protected int getCounterAV() {
        return CounterAV;
    }
}
