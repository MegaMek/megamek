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
/*
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.ComputeECM;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IAero;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.logging.LogLevel;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.MMLogger;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.TeleOperatedMissileBayWeapon;
import megamek.server.Server;

/**
 * @author MKerensky
 */
public class CapitalMissileBearingsOnlyHandler extends AmmoBayWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = -1277549123532227298L;
    boolean handledAmmoAndReport = false;
    private MMLogger logger = null;
    boolean advancedPD = game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
    boolean detRangeShort = weapon.curMode().equals("Bearings-Only Short Detection Range");
    boolean detRangeMedium = weapon.curMode().equals("Bearings-Only Medium Detection Range");
    boolean detRangeLong = weapon.curMode().equals("Bearings-Only Long Detection Range");
    boolean detRangeExtreme = weapon.curMode().equals("Bearings-Only Extreme Detection Range");

    /**
     * This constructor may only be used for deserialization.
     */
    protected CapitalMissileBearingsOnlyHandler() {
        super(); 
    }
    
    /**
     * Write debug information to the logs.
     *
     * @param methodName Name of the method logging is coming from
     * @param message Message to log
     */
    @SuppressWarnings("unused")
    private void logDebug(String methodName, String message) {
        getLogger().log(getClass(), methodName, LogLevel.DEBUG, message);
    }
    
    private MMLogger getLogger() {
        if (null == logger) {
            logger = DefaultMmLogger.getInstance();
        }

        return logger;
    }

    /**
     * @param t
     * @param w
     * @param g
     */
    public CapitalMissileBearingsOnlyHandler(ToHitData t,
            WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }
    
    
    //Defined here so we can use it in multiple methods
    Mounted bayWAmmo;
    int range;
    Coords targetCoords;

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.AttackHandler#cares(int)
     */
    @Override
    public boolean cares(IGame.Phase phase) {
        if ((phase == IGame.Phase.PHASE_FIRING)
                || (phase == IGame.Phase.PHASE_TARGETING)) {
            return true;
        }
        return false;
    }
    
    protected void getMountedAmmo() {
        final String METHOD_NAME = "getMountedAmmo()";
        for (int wId : weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            bayWAmmo = bayW.getLinked();

            if (bayWAmmo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                logDebug(METHOD_NAME, "Handler can't find any ammo! Oh no!");
            }
        }    
    }
        
    @Override
    protected void useAmmo() {
        getMountedAmmo();
        Mounted bayW = bayWAmmo.getLinkedBy();        
        int shots = (bayW.getCurrentShots() * weapon.getBayWeapons().size());
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

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) waa;
        if (phase == IGame.Phase.PHASE_TARGETING) {
            if (!handledAmmoAndReport) {
                addHeat();
                // Report the firing itself
                Report r = new Report(3122);
                r.indent();
                r.newlines = 0;
                r.subject = subjectId;
                r.add(wtype.getName());
                r.add(aaa.turnsTilHit);
                vPhaseReport.addElement(r);
                Report.addNewline(vPhaseReport);
                handledAmmoAndReport = true;
            }
            // if this is the last targeting phase before we hit,
            // make it so the firing entity is announced in the
            // off-board attack phase that follows.
            if (aaa.turnsTilHit == 0) {
                setAnnouncedEntityFiring(false);
            }
            return true;
        }
        if (aaa.turnsTilHit > 0) {
            aaa.turnsTilHit--;
            return true;
        }
        Entity entityTarget = (aaa.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) aaa
                .getTarget(game) : null;
        if (game.getPhase() == IGame.Phase.PHASE_FIRING && entityTarget == null) {
            convertHexTargetToEntityTarget(vPhaseReport);
            entityTarget = (aaa.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) aaa
                    .getTarget(game) : null;
        }

        // Report weapon attack and its to-hit value.
        Report r = new Report(3118);
        r.newlines = 0;
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report (3123);
            r.subject = subjectId;
            r.add(" " + target.getPosition(), true);
            vPhaseReport.addElement(r);
        } else {        
            r = new Report(3119);
            r.indent();
            r.newlines = 1;
            r.subject = subjectId;        
            r.addDesc(entityTarget);        
            vPhaseReport.addElement(r);
        }
        
        //Point Defense fire vs Capital Missiles
        
        // are we a glancing hit?  Check for this here, report it later
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)) {
            if (roll == toHit.getValue()) {
                bGlancing = true;
            } else {
                bGlancing = false;
            }
        }
        
        // Set Margin of Success/Failure and check for Direct Blows
        toHit.setMoS(roll - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

        //This has to be up here so that we don't screw up glancing/direct blow reports
        attackValue = calcAttackValue();
        
        //CalcAttackValue triggers counterfire, so now we can safely get this
        CapMissileAMSMod = getCapMissileAMSMod();
        
        //Only do this if the missile wasn't destroyed
        if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
            toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
            if (roll < toHit.getValue()) {
                CapMissileMissed = true;
            }
        }
        
        // Report any AMS bay action against Capital missiles that doesn't destroy them all.
        if (amsBayEngagedCap && CapMissileArmor > 0) {
            r = new Report(3358);
            r.add(CapMissileAMSMod);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
                    
        // Report any PD bay action against Capital missiles that doesn't destroy them all.
        } else if (pdBayEngagedCap && CapMissileArmor > 0) {
            r = new Report(3357);
            r.add(CapMissileAMSMod);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            //This is reported elsewhere
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

        //Report Glancing/Direct Blow here because of Capital Missile weirdness
        //TODO: Can't figure out a good way to make Capital Missile bays report direct/glancing blows
        //when Advanced Point Defense is on, but they work correctly.
        if ((bGlancing) && !(amsBayEngagedCap || pdBayEngagedCap)) {
            r = new Report(3186);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        } 

        if ((bDirect) && !(amsBayEngagedCap || pdBayEngagedCap)) {
            r = new Report(3189);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        // we may still have to use ammo, if direct fire
        if (!handledAmmoAndReport) {
            addHeat();
        }
        
        CounterAV = getCounterAV();
        //use this if AMS counterfire destroys all the Capital missiles
        if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3356);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        //use this if PD counterfire destroys all the Capital missiles
        if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3355);
            r.indent();
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
        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);
        }
        // Handle damage.
        int nCluster = calcnCluster();
        int id = vPhaseReport.size();
        int hits = calcHits(vPhaseReport);
        //Set the hit location table based on where the missile goes active
        if (entityTarget != null) {
            if (aaa.getOldTargetCoords() != null) {
                toHit.setSideTable(entityTarget.sideTable(aaa.getOldTargetCoords()));
            }    
        }
        if (target.isAirborne() || game.getBoard().inSpace() || ae.usesWeaponBays()) {
            // if we added a line to the phase report for calc hits, remove
            // it now
            while (vPhaseReport.size() > id) {
                vPhaseReport.removeElementAt(vPhaseReport.size() - 1);
            }
            int[] aeroResults = calcAeroDamage(entityTarget, vPhaseReport);
            hits = aeroResults[0];
            // If our capital missile was destroyed, it shouldn't hit
            if ((amsBayEngagedCap || pdBayEngagedCap) && (CapMissileArmor <= 0)) {
                hits = 0;
            }
            nCluster = aeroResults[1];
        }
        
        //Bearings-only missiles shouldn't be able to target buildings, being space-only weapons
        //but if these two things aren't defined, handleEntityDamage() doesn't work.
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());
        int bldgAbsorbs = 0;

        // We have to adjust the reports on a miss, so they line up
        if (bMissed && id != vPhaseReport.size()) {
            vPhaseReport.get(id - 1).newlines--;
            vPhaseReport.get(id).indent(2);
            vPhaseReport.get(vPhaseReport.size() - 1).newlines++;
        }

        // Make sure the player knows when his attack causes no damage.
        if (nDamPerHit == 0) {
            r = new Report(3365);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
        }
        if (!bMissed && (entityTarget != null)) {
            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                    nCluster, bldgAbsorbs);
            server.creditKill(entityTarget, ae);
        } else if (!bMissed){ // Hex is targeted, need to report a hit
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        Report.addNewline(vPhaseReport);
        return false;
    }
    
    /**
     * Find the available targets within sensor range. Bearings-only
     * missiles scan within the nose arc and target the closest large craft
     * within the preset range band. If none are found, it targets the closest
     * small craft. 
     */
    public void convertHexTargetToEntityTarget(Vector<Report> vPhaseReport) {
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) waa;
        
        final Coords tc = target.getPosition();
        targetCoords = tc;
        //Set the original missile target data. AMS and to-hit table calculations need this.
        aaa.setOldTargetCoords(tc);
        aaa.setOriginalTargetId(target.getTargetId());
        aaa.setOriginalTargetType(target.getTargetType());
        int missileFacing = ae.getPosition().direction(tc);
        Targetable newTarget = null;
        Vector<Aero> targets = new Vector<Aero>();
        
        // get all entities on the opposing side
        for(Iterator<Entity> enemies = game.getAllEnemyEntities(ae); enemies.hasNext();) {
            Entity e = enemies.next();
            //Narrow the list to small craft and larger
            if (((e.getEntityType() & (Entity.ETYPE_SMALL_CRAFT)) != 0)) {
                Aero a = (Aero) e;
                targets.add(a);
            } else if (((e.getEntityType() & (Entity.ETYPE_JUMPSHIP)) != 0)) {
                Aero a = (Aero) e;
                targets.add(a);
            }            
        }
        if (targets.size() == 0) {
            //We're not dealing with targets in arc or in range yet.
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                    "no valid targets in play");
            return;
        }

        assert (newTarget != null);
        
        // Add only targets in arc
        Vector<Aero> inArc = new Vector<Aero>();
        for (Aero a : targets) {
            Boolean isInArc = Compute.isInArc(tc, missileFacing, a, Compute.ARC_NOSE);
            if (isInArc) {
                inArc.add(a);
            }
        }
        if (inArc.size() == 0) {
            newTarget = aaa.getTarget(game);
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                    "no targets detected within the missile's nose arc");
            return;
        }
        //Empty out the targets vector and only put valid targets in arc back in
        targets.removeAllElements();
        for (Aero a : inArc) {
            targets.add(a);
        }
        
        // Detection range for targets is based on the range set at firing
        Vector<Aero> detected = new Vector<Aero>();
        if (detRangeExtreme) {
            for (Aero a : targets) {
                if (tc.distance(a.getPosition()) <= 25) {
                    detected.add(a);
                }
            }
        } else if (detRangeLong) {
            for (Aero a : targets) {
                if (tc.distance(a.getPosition()) <= 20) {
                    detected.add(a);
                }
            }
        } else if (detRangeMedium) {
            for (Aero a : targets) {
                if (tc.distance(a.getPosition()) <= 12) {
                    detected.add(a);
                }
            }
        } else if (detRangeShort) {
            for (Aero a : targets) {
                if (tc.distance(a.getPosition()) <= 6) {
                    detected.add(a);
                }
            }
        }
        if (detected.size() == 0) {
            newTarget = aaa.getTarget(game);
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                    "no targets detected within the missile's detection range");
            return;
        }
        //Empty out the targets vector and only put valid targets in range back in
        targets.removeAllElements();
        for (Aero a : detected) {
            targets.add(a);
        }

        //If we're using tele-operated missiles, the player gets to select the target
        if (weapon.getType() instanceof TeleOperatedMissileBayWeapon) {
            List<String> targetDescriptions = new ArrayList<String>();
            for (Aero target : targets) {
                setToHit(target);
                targetDescriptions.add(target.getDisplayName() + ": Needs " + toHit.getValue() + " to hit.");
            }
            int choice = server.processTeleguidedMissileCFR(ae.getOwnerId(), targetDescriptions);
            newTarget = targets.get(choice);
            target = newTarget;
            aaa.setTargetId(target.getTargetId());
            aaa.setTargetType(target.getTargetType());
            //Run this again, otherwise toHit is left set to the value for the last target in the list...
            setToHit(target);
            server.assignAMS();

         } else {
            // Otherwise, find the largest and closest target of those available
            int bestDistance = Integer.MAX_VALUE;
            int bestTonnage = 0;
            Aero currTarget = targets.firstElement();
            newTarget = null;
            //Target the closest large craft
            for (Aero a : targets) {
                //Ignore small craft for now
                if (((a.getEntityType() & Entity.ETYPE_SMALL_CRAFT) > 0)
                        && ((a.getEntityType() & Entity.ETYPE_DROPSHIP) == 0)) {
                    continue;
                }
                int distance = tc.distance(a.getPosition());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    currTarget = a;
                    newTarget = currTarget;
                    continue;
                }
                // same distance
                int tonnage = (int) a.getWeight();
                if (distance == bestDistance) {
                    //Find the largest target                    
                    if (tonnage > bestTonnage) {
                        bestTonnage = tonnage;
                        currTarget = a;
                        newTarget = currTarget;
                        continue;
                    }
                }
                // same distance and tonnage? Roll randomly
                if (distance == bestDistance && tonnage == bestTonnage) {
                    int tiebreaker = Compute.d6();
                    if (tiebreaker < 4) {
                        newTarget = a;
                    } else {
                        newTarget = currTarget;
                    }
                }    
            }
            //Repeat the process for small craft if no large craft are found
            if (newTarget == null) {
                for (Aero a : targets) {
                    int distance = tc.distance(a.getPosition());
                    if (distance > bestDistance) {
                        bestDistance = distance;
                        currTarget = a;
                        newTarget = currTarget;
                        continue;
                    }
                    // same distance
                    int tonnage = (int) a.getWeight();
                    if (distance == bestDistance) {
                        //Find the largest target                    
                        if (tonnage > bestTonnage) {
                            bestTonnage = tonnage;
                            currTarget = a;
                            newTarget = currTarget;
                            continue;
                        }
                    }
                    // same distance and tonnage? Roll randomly
                    if (distance == bestDistance && tonnage == bestTonnage) {
                        int tiebreaker = Compute.d6();
                        if (tiebreaker < 4) {
                            newTarget = a;
                        } else {
                            newTarget = currTarget;
                        }
                    } 
                }
            }
            // Now, assign our chosen target to the missile
            target = newTarget;
            aaa.setTargetId(target.getTargetId());
            aaa.setTargetType(target.getTargetType());
            setToHit(target);
            server.assignAMS();
        }

    }

    private void setToHit(Targetable target) {    
        //Once we have a ship target, set up the to-hit modifiers
        Aero targetship = (Aero) target;
        toHit = new ToHitData(4, "Base");
        if (range > 20 && range <= 25) {
            toHit.addModifier(6, "extreme range");
        } else if (range > 12 && range <= 20) {
            toHit.addModifier(4, "long range");
        } else if (range > 6 && range <= 12) {
            toHit.addModifier(2, "medium range");
        } else if (range <= 6) {
            toHit.addModifier(0, "short range");
        } 
        //If the target is closer than the set range band, add a +1 modifier
        if ((detRangeExtreme && range <= 20)
                || (detRangeLong && range <= 12) 
                || (detRangeMedium && range <= 6)) {
            toHit.addModifier(1, "target closer than range setting");
        }
   
        // evading bonuses
        if ((target != null) && targetship.isEvading()) {
            toHit.addModifier(2, "target is evading");
        }
    
        // is the target at zero velocity
        if ((targetship.getCurrentVelocity() == 0) && !(targetship.isSpheroid() && !game.getBoard().inSpace())) {
            toHit.addModifier(-2, "target is not moving");
        }
    
        //Barracuda Missile Modifier
        getMountedAmmo();
        AmmoType bayAType = (AmmoType) bayWAmmo.getType();
        if ((bayWAmmo.getType().hasFlag(AmmoType.F_AR10_BARRACUDA))
                || (bayAType.getAmmoType() == AmmoType.T_BARRACUDA)) {
            toHit.addModifier(-2, "Barracuda Missile");
        }
   
        if (target.isAirborne() && target.isAero()) {
            if (!(((IAero) target).isSpheroid() && !game.getBoard().inSpace())) {
                // get mods for direction of attack
                int side = toHit.getSideTable();
                // if this is an aero attack using advanced movement rules then
                // determine side differently
                if (game.useVectorMove()) {
                    boolean usePrior = false;
                    side = ((Entity) target).chooseSide(targetCoords, usePrior);
                }
                if (side == ToHitData.SIDE_FRONT) {
                    toHit.addModifier(+1, "attack against nose");
                }
                if ((side == ToHitData.SIDE_LEFT) || (side == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(+2, "attack against side");
                }
            }
        }
            
        // Space ECM
        if (game.getBoard().inSpace() && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
            int ecm = ComputeECM.getLargeCraftECM(ae, targetCoords, target.getPosition());
            ecm = Math.min(4, ecm);
            if (ecm > 0) {
                toHit.addModifier(ecm, "ECM");
            }
        }
    }
            
            
            

            
       

    
    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.WeaponHandler#handleSpecialMiss(megamek.common
     * .Entity, boolean, megamek.common.Building, java.util.Vector)
     */
    @Override
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean bldgDamagedOnMiss, Building bldg,
            Vector<Report> vPhaseReport) {
        return true;
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
    
    @Override
    protected int calcAttackValue() {

        double av = 0;
        double counterAV = calcCounterAV();
        int armor = 0;
        int weaponarmor = 0;
        //A bearings-only shot is, by definition, always going to be at extreme range...
        int range = RangeType.RANGE_EXTREME;

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
                || atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                || atype.getAmmoType() == AmmoType.T_MANTA_RAY
                || atype.getAmmoType() == AmmoType.T_ALAMO) {
            return 10;
        }  else if (atype.getAmmoType() == AmmoType.T_STINGRAY) {
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

}
