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
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class MissileBayWeaponHandler extends AmmoBayWeaponHandler {

    private static final long serialVersionUID = -1618484541772117621L;
    boolean advancedPD = false;
    
    protected MissileBayWeaponHandler() {
        // deserialization only
    }

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public MissileBayWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
    }

    /**
     * Calculate the attack value based on range This needs to do additional
     * work for Weapon Bays with ammo. I need to use the ammo within this
     * function because I may run out of ammo while going through the loop Sine
     * this function is called in the WeaponHandler constructor it should be ok
     * to use the ammo here
     * 
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {

        double av = 0;
        double counterAV = 0;
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

                if (range == WeaponType.RANGE_SHORT) {
                    current_av = bayWType.getShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    current_av = bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    current_av = bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    current_av = bayWType.getExtAV();
                }
                current_av = updateAVforAmmo(current_av, atype, bayWType,
                        range, wId);
                av = av + current_av;
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
            }
        }
        //Bracketing fire reduces the number of missiles that hit
        av = (int) Math.floor(getBracketingMultiplier() * av);
        
        //Point Defenses engage the missiles still aimed at us
        counterAV = calcCounterAV();
        av = av - counterAV;
        
        //Apply direct/glancing blow modifiers to the survivors
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        if (bGlancing) {
            av = (int) Math.floor(av / 2.0);

        }        

        return (int) Math.ceil(av);
       
    }

    /*
     * check for special munitions and their effect on av 
     * 
     */
    protected double updateAVforAmmo(double current_av, AmmoType atype,
            WeaponType bayWType, int range, int wId) {
        Mounted bayW = ae.getEquipment(wId);
        Mounted mLinker = bayW.getLinkedBy();
        int bonus = 0;
        if ((mLinker != null && mLinker.getType() instanceof MiscType
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                        MiscType.F_ARTEMIS))
                && atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE) {
            bonus = (int) Math.ceil(atype.getRackSize() / 5.0);
            if ((atype.getAmmoType() == AmmoType.T_SRM) || (atype.getAmmoType() == AmmoType.T_SRM_IMP))  {
                bonus = 2;
            }
            current_av = current_av + bonus;
        }
        // check for Artemis V
        if (((mLinker != null) && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                        MiscType.F_ARTEMIS_V))
                && (atype.getMunitionType() == AmmoType.M_ARTEMIS_V_CAPABLE)) {
            // MML3 WOULD get a bonus from Artemis V, if you were crazy enough
            // to cross-tech it
            bonus = (int) Math.ceil(atype.getRackSize() / 5.0);
            if ((atype.getAmmoType() == AmmoType.T_SRM) || (atype.getAmmoType() == AmmoType.T_SRM_IMP))  {
                bonus = 2;
            }
        }

        if (atype.getMunitionType() == AmmoType.M_CLUSTER) {
            current_av = Math.floor(0.6 * current_av);
        } else if (AmmoType.T_ATM == atype.getAmmoType()) {
            if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                current_av = bayWType.getShortAV() / 2;
            } else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                current_av = 1.5 * current_av;
                if (range > WeaponType.RANGE_SHORT) {
                    current_av = 0.0;
                }
            }
        } else if (atype.getAmmoType() == AmmoType.T_MML
                && !atype.hasFlag(AmmoType.F_MML_LRM)) {
            current_av = 2 * current_av;
            if (range > WeaponType.RANGE_SHORT) {
                current_av = 0;
            }
        } 
        return current_av;
        
    }     
   
    // check for AMS and Point Defense Bay fire
    @Override
    protected int calcCounterAV () {
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
	                            // Point defenses only fire vs attacks in arc covered by ams
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
                            if (bayWAmmo.getBaseShotsLeft() == 0) {
                                continue;
                            }
                            // decrement the ammo
                        	bayWAmmo.setShotsLeft(Math.max(0,
                        		bayWAmmo.getBaseShotsLeft() - 1));
                        }
                        
                        // get the attack value
                        amsAV += bayWType.getShortAV();                                      
            		}
                    
                    // set the ams as having fired, if it did
                    if (amsAV > 0) {
                        amsBayEngaged = true;
                    }
                                        
                } else if (isPDBay) {
                    pdAV = 0;
                    // Point defenses can't fire if they're not ready for any reason
		            if (!(counter.getType() instanceof WeaponType)
	                         || !counter.isReady() || counter.isMissing()
	                            // shutdown means no Point defenses
	                            || pdEnt.isShutDown()
	                            // Point defenses only fire vs attacks in arc covered by ams
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
                            if (bayWAmmo.getBaseShotsLeft() == 0) {
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
                        pdBayEngaged = true;
                    }
                                 
                } //end PDBay fire 
                
                // non-AMS only add half their damage, rounded up
                counterAV += (int) Math.ceil(pdAV / 2.0); 
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
    
    @Override
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;

        if ((((null == entityTarget) || entityTarget.isAirborne()) 
                && (target.getTargetType() != Targetable.TYPE_HEX_CLEAR 
                &&  target.getTargetType() != Targetable.TYPE_HEX_IGNITE
                &&  target.getTargetType() != Targetable.TYPE_BUILDING)) 
        		|| game.getBoard().inSpace()) {
            return super.handle(phase, vPhaseReport);
        } 

        // then we have a ground target, so we need to handle it in a special
        // way
        insertAttacks(phase, vPhaseReport);

        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);
        final boolean bldgDamagedOnMiss = targetInBuilding
                && !(target instanceof Infantry)
                && ae.getPosition().distance(target.getPosition()) <= 1;

        if (entityTarget != null) {
            ae.setLastTarget(entityTarget.getId());
            ae.setLastTargetDisplayName(entityTarget.getDisplayName());
        }

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());
        String number = nweapons > 1 ? " (" + nweapons + ")" : "";

        // Report weapon attack and its to-hit value.
        Report r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName() + number);
        if (entityTarget != null) {
            r.addDesc(entityTarget);
        } else {
            r.messageId = 3120;
            r.add(target.getDisplayName(), true);
        }

        vPhaseReport.addElement(r);
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else {
            // roll to hit
            r = new Report(3150);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getValue());
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.add(roll);
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll < toHit.getValue();
                
        // are we a glancing hit?
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)) {
            if (roll == toHit.getValue()) {
                bGlancing = true;
                r = new Report(3186);
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                bGlancing = false;
            }
        } else {
            bGlancing = false;
        }

        // Set Margin of Success/Failure.
        toHit.setMoS(roll - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
        if (bDirect) {
            r = new Report(3189);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        nDamPerHit = calcAttackValue();
        addHeat();   
        
        // Report any AMS bay action against standard missiles.
        // This only gets used in atmosphere/ground battles
        // Non AMS point defenses only work in space
        CounterAV = getCounterAV();
        //use this if counterfire destroys all the missiles
        if (amsBayEngaged && (attackValue <= 0)) {
        	r = new Report(3356);
        	r.indent();
        	r.subject = subjectId;
        	vPhaseReport.addElement(r);
        } else if (amsBayEngaged) {
        	r = new Report(3354);
        	r.indent();
        	r.add(CounterAV);
        	r.subject = subjectId;
        	vPhaseReport.addElement(r);
        }

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);
        if (missReported) {
            bMissed = true;
        }

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }

        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
                    vPhaseReport)) {
                return false;
            }
        }

        if (bMissed) {
            return false;

        } // End missed-target

        if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
            handleIgnitionDamage(vPhaseReport, bldg, 1);
            return false;
        }
        if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
            handleClearDamage(vPhaseReport, bldg, nDamPerHit);
            return false;
        }
        // Targeting a building.
        if (target.getTargetType() == Targetable.TYPE_BUILDING) {
            // The building takes the full brunt of the attack
            handleBuildingDamage(vPhaseReport, bldg, nDamPerHit,
                    target.getPosition());
            return false;
        }

        Report.addNewline(vPhaseReport);
        // loop through weapons in bay and do damage
        int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true, false);
        int hits = 1;
        int nCluster = 1;
        for (int wId : weapon.getBayWeapons()) {
            double av = 0;
            Mounted m = ae.getEquipment(wId);
            if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                WeaponType bayWType = ((WeaponType) m.getType());
                // need to cycle through weapons and add av
                if (range == WeaponType.RANGE_SHORT) {
                    av = bayWType.getShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    av = bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    av = bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    av = bayWType.getExtAV();
                }
            }
            
            nDamPerHit = (int) (Math.ceil(av) - CounterAV);
            if (nDamPerHit <= 0) {
                continue;
            }
            bSalvo = true;

            // Buildings shield all units from a certain amount of damage.
            // Amount is based upon the building's CF at the phase's start.
            int bldgAbsorbs = 0;
            if (targetInBuilding && (bldg != null)
                    && (toHit.getThruBldg() == null)) {
                bldgAbsorbs = bldg.getAbsorbtion(target.getPosition());
            }
            
            // Attacking infantry in buildings from same building
            if (targetInBuilding && (bldg != null)
                    && (toHit.getThruBldg() != null)
                    && (entityTarget instanceof Infantry)) {
                // If elevation is the same, building doesn't absorb
                if (ae.getElevation() != entityTarget.getElevation()) {
                    int dmgClass = wtype.getInfantryDamageClass();
                    int nDamage;
                    if (dmgClass < WeaponType.WEAPON_BURST_1D6) {
                        nDamage = nDamPerHit * Math.min(nCluster, hits);
                    } else {
                        // Need to indicate to handleEntityDamage that the
                        // absorbed damage shouldn't reduce incoming damage,
                        // since the incoming damage was reduced in
                        // Compute.directBlowInfantryDamage
                        nDamage = -wtype.getDamage(nRange)
                                * Math.min(nCluster, hits);
                    }
                    bldgAbsorbs = (int) Math.round(nDamage
                            * bldg.getInfDmgFromInside());
                } else {
                    // Used later to indicate a special report
                    bldgAbsorbs = Integer.MIN_VALUE;
                }
            }

            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                    nCluster, bldgAbsorbs);
            server.creditKill(entityTarget, ae);
        } // Handle the next weapon in the bay
        Report.addNewline(vPhaseReport); 
        return false;
    }
}
