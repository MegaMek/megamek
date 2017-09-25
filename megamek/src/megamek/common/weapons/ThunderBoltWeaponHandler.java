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
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ThunderBoltWeaponHandler extends MissileWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = 6329291710822071023L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public ThunderBoltWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        AmmoType atype = (AmmoType) ammo.getType();
        double toReturn = atype.getDamagePerShot();
        int minRange;
        if (ae.isAirborne()) {
            minRange = wtype.getATRanges()[RangeType.RANGE_MINIMUM];
        } else {
            minRange = wtype.getMinimumRange();
        }
        if ((nRange <= minRange) && !weapon.isHotLoaded()) {
            toReturn /= 2;
            toReturn = Math.floor(toReturn);
        }
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                    bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }
        return (int) Math.ceil(toReturn);
    }
    
    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        calcCounterAV();
        int av = 0;
        int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true, false);
        if (range == WeaponType.RANGE_SHORT) {
            av = wtype.getRoundShortAV();
        } else if (range == WeaponType.RANGE_MED) {
            av = wtype.getRoundMedAV();
        } else if (range == WeaponType.RANGE_LONG) {
            av = wtype.getRoundLongAV();
        } else if (range == WeaponType.RANGE_EXT) {
            av = wtype.getRoundExtAV();
        }
                        
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        if (bGlancing) {
            av = (int) Math.floor(av / 2.0);

        }
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return (av);
    }
    
    @Override
    //Thunderbolts apply damage all in one block.
    //This was referenced incorrectly for Aero damage.
    protected boolean usesClusterTable() {
        return false;
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
                        
                        // build up some heat (assume target is ams owner)		            
                        if (counter.getType().hasFlag(WeaponType.F_HEATASDICE)) {
		            		entityTarget.heatBuildup += Compute.d6(bayW
		            				.getCurrentHeat());	                    
		            	} else {
	                        entityTarget.heatBuildup += bayW.getCurrentHeat();
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
                        
                        // build up some heat (assume target is ams owner)		            
                        if (counter.getType().hasFlag(WeaponType.F_HEATASDICE)) {
		            		entityTarget.heatBuildup += Compute.d6(bayW
		            				.getCurrentHeat());	                    
		            	} else {
	                        entityTarget.heatBuildup += bayW.getCurrentHeat();
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
                    pdBayEngagedMissile = true;
                }
                counterAV += (int) Math.ceil(pdAV / 2.0);
                
                // set the ams as having fired, if it did
                if (amsAV > 0) {
                    amsBayEngagedMissile = true;
                }
                // AMS add their full damage
                counterAV += amsAV;
            } //end "for Mounted counter"
        } // end check for counterfire
        CounterAV = (int) counterAV;
        return counterAV;
    } // end getAMSAV

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.MissileWeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        getAMSHitsMod(vPhaseReport);
        bSalvo = true;
        if (amsEngaged) {
            Report r = new Report(3235);
            r.subject = subjectId;
            vPhaseReport.add(r);
            r = new Report(3230);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.add(r);
            int destroyRoll = Compute.d6();
            if (destroyRoll <= 3) {
                r = new Report(3240);
                r.subject = subjectId;
                r.add("missile");
                r.add(destroyRoll);
                vPhaseReport.add(r);
                return 0;
            }
            r = new Report(3241);
            r.add("missile");
            r.add(destroyRoll);
            r.subject = subjectId;
            vPhaseReport.add(r);
        }
        return 1;
    }

}
