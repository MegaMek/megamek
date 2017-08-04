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
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class AmmoBayWeaponHandler extends BayWeaponHandler {

    private static final long serialVersionUID = -1618484541772117621L;

    protected AmmoBayWeaponHandler() {
        // deserialization only
    }

    public boolean amsEngaged = false;
    
    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public AmmoBayWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
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
        int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true, false);

        for (int wId : weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();
            if (null == bayWAmmo || bayWAmmo.getUsableShotsLeft() < 1) {
                // try loadinsg something else
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

                // check for nukes and tele-missiles and if they are there then
                // I will need to
                // add them to an inserted attack list and reset the av
                if (atype.hasFlag(AmmoType.F_NUCLEAR)
                        || atype.hasFlag(AmmoType.F_TELE_MISSILE)) {
                    insertedAttacks.addElement(wId);
                    av = av - current_av;
                }
            }
        }
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        if (bGlancing) {
            av = (int) Math.floor(av / 2.0);

        }
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return (int) Math.ceil(av);
    }

    /*
     * check for special munitions and their effect on av TODO: it might be
     * better to have unique weapon handlers for these by bay, but I am lazy
     */
    protected double updateAVforAmmo(double current_av, AmmoType atype,
            WeaponType bayWType, int range, int wId) {

        // check for artemisIV
        Mounted mLinker = weapon.getLinkedBy();
        int bonus = 0;
        if ((mLinker != null && mLinker.getType() instanceof MiscType
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                MiscType.F_ARTEMIS))
                && atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE) {
            bonus = (int) Math.ceil(atype.getRackSize() / 5.0);
            if (atype.getAmmoType() == AmmoType.T_SRM) {
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
            if (atype.getAmmoType() == AmmoType.T_SRM) {
                bonus = 2;
            }
        }
        // check for Artemis IV Proto Type
        if ((mLinker != null && mLinker.getType() instanceof MiscType
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                MiscType.F_ARTEMIS_PROTO))
                && atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE) {
            bonus = (int) Math.ceil(atype.getRackSize() / 5.0);
            if (atype.getAmmoType() == AmmoType.T_SRM) {
                bonus = 2;
            }
            current_av = current_av + bonus;
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
        } else if (atype.getAmmoType() == AmmoType.T_AR10) {
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
  // Start of AMS Counterfire
    
    protected int getAMSHitsMod(Vector<Report> vPhaseReport) {
        if ((target == null)
                || (target.getTargetType() != Targetable.TYPE_ENTITY)) {
            return 0;
        }
        int apdsMod = 0;
        int amsMod = 0;
        Entity entityTarget = (Entity) target;
 // any AMS attacks by the target?
    ArrayList<Mounted> lCounters = waa.getCounterEquipment();
    if (null != lCounters) {
        // resolve AMS counter-fire
        // for (int x = 0; x < lCounters.size(); x++) {
        for (Mounted counter : lCounters) {
            boolean isAMS = counter.getType().hasFlag(WeaponType.F_AMS);
           // boolean isPDBay = counter.getType().hasModes() && mounted.curMode().equals("Point Defense"));
            if (isAMS) /* && counter.ispdBay() && !pdbayEngaged) */ {
                Mounted mAmmo = counter.getLinked();
                Entity apdsEnt = counter.getEntity();
                boolean isInArc;
                // If the apdsUnit is the target, use attacker for arc
                if (entityTarget.equals(apdsEnt)) {
                    isInArc = Compute.isInArc(game, apdsEnt.getId(),
                            apdsEnt.getEquipmentNum(counter),
                            ae);
                } else { // Otherwise, the attack target must be in arc
                    isInArc = Compute.isInArc(game, apdsEnt.getId(),
                            apdsEnt.getEquipmentNum(counter),
                            entityTarget);
                }
                if (!(counter.getType() instanceof WeaponType)
                        || !counter.isReady() || counter.isMissing()
                        // no AMS when a shield in the AMS location
                        || (apdsEnt.hasShield() && apdsEnt.hasActiveShield(
                                counter.getLocation(), false))
                        // shutdown means no AMS
                        || apdsEnt.isShutDown()
                        // AMS only fires vs attacks in arc covered by ams
                        || !isInArc) {
                    continue;
                }

                // build up some heat (assume target is ams owner)
                if (counter.getType().hasFlag(WeaponType.F_HEATASDICE)) {
                    entityTarget.heatBuildup += Compute.d6(counter
                            .getCurrentHeat());
                } else {
                    entityTarget.heatBuildup += counter.getCurrentHeat();
                }

                // decrement the ammo
                if (mAmmo != null) {
                    mAmmo.setShotsLeft(Math.max(0,
                            mAmmo.getBaseShotsLeft() - 1));
                }
                // set the ams as having fired
                counter.setUsedThisRound(true);
                amsEngaged = true;
                Report r = new Report(3350);
                r.subject = entityTarget.getId();
                r.newlines = 0;
                vPhaseReport.add(r);
                amsMod = -4;
            }
        }
    }
    return apdsMod + amsMod;
    } 

         //Handle point defense damage to the missile bay attack
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
            int nCluster = calcnCluster();
            int id = vPhaseReport.size();
            int hits;
                if (target.isAirborne() || game.getBoard().inSpace()) {
                // Ensures AMS state is properly updated
                getAMSHitsMod(new Vector<Report>());
                int[] aeroResults = calcAeroDamage(entityTarget, vPhaseReport);
                hits = aeroResults[0];
                nCluster = aeroResults[1];
                // Need to report hit (normally reported in calcHits)
                if (!bMissed && amsEngaged && !ae.isCapitalFighter()) {
                int amsRoll = Compute.d6();
                Report r = new Report(3352);
                r.subject = subjectId;
                r.add(amsRoll);
                vPhaseReport.add(r);
                hits = Math.max(0, hits - amsRoll);
                } else if (!bMissed) {
                Report r = new Report(3390);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                }
            } else {
              hits = calcHits(vPhaseReport);
            }
            Report.addNewline(vPhaseReport);
            return false;
        }
    }