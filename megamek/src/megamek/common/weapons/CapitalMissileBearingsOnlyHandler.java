/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.TeleOperatedMissileBayWeapon;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * @author MKerensky
 * @since Sep 24, 2004
 */
public class CapitalMissileBearingsOnlyHandler extends AmmoBayWeaponHandler {
    private static final long serialVersionUID = -1277549123532227298L;
    boolean handledAmmoAndReport = false;
    boolean detRangeShort = (weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_SHORT)
            || weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_SHORT));
    boolean detRangeMedium = (weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_MED)
            || weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_MED));
    boolean detRangeLong = (weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_LONG)
            || weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_LONG));
    boolean detRangeExtreme = (weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_EXT)
            || weapon.curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_EXT));

    // Defined here so we can use it in multiple methods
    Mounted bayWAmmo;
    int range;
    Coords targetCoords;

    /**
     * This constructor can only be used for deserialization.
     */
    protected CapitalMissileBearingsOnlyHandler() {
        super(); 
    }

    public CapitalMissileBearingsOnlyHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
        super(t, w, g, s);
    }

    @Override
    public boolean cares(GamePhase phase) {
        return phase.isFiring() || phase.isTargeting();
    }
    
    protected void getMountedAmmo() {
        for (int wId : weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            bayWAmmo = bayW.getLinked();

            if (bayWAmmo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                LogManager.getLogger().debug("Handler can't find any ammo! Oh no!");
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

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) waa;
        if (phase == GamePhase.TARGETING) {
            if (!handledAmmoAndReport) {
                addHeat();
                // Report the firing itself
                Report r = new Report(3122);
                r.indent();
                r.newlines = 0;
                r.subject = subjectId;
                r.add(wtype.getName());
                r.add(aaa.getTurnsTilHit());
                vPhaseReport.addElement(r);
                Report.addNewline(vPhaseReport);
                handledAmmoAndReport = true;
            }
            // if this is the last targeting phase before we hit,
            // make it so the firing entity is announced in the
            // off-board attack phase that follows.
            if (aaa.getTurnsTilHit() == 0) {
                setAnnouncedEntityFiring(false);
            }
            return true;
        }
        if (aaa.getTurnsTilHit() > 0) {
            aaa.decrementTurnsTilHit();
            return true;
        }
        Entity entityTarget = (aaa.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) aaa
                .getTarget(game) : null;
        if (game.getPhase() == GamePhase.FIRING && entityTarget == null) {
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
        
        // are we a glancing hit?  Check for this here, report it later
        setGlancingBlowFlags(entityTarget);
        
        // Point Defense fire vs Capital Missiles
        
        // Set Margin of Success/Failure and check for Direct Blows
        toHit.setMoS(roll - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

        // This has to be up here so that we don't screw up glancing/direct blow reports
        attackValue = calcAttackValue();
        
        // CalcAttackValue triggers counterfire, so now we can safely get this
        CapMissileAMSMod = getCapMissileAMSMod();
        
        // Only do this if the missile wasn't destroyed
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
            // This is reported elsewhere
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
            r.add(toHit);
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

        // Report Glancing/Direct Blow here because of Capital Missile weirdness
        // TODO : Can't figure out a good way to make Capital Missile bays report direct/glancing
        // TODO : blows when Advanced Point Defense is on, but they work correctly.
        if (!(amsBayEngagedCap || pdBayEngagedCap)) {
            addGlancingBlowReports(vPhaseReport);
    
            if (bDirect) {
                r = new Report(3189);
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }

        // we may still have to use ammo, if direct fire
        if (!handledAmmoAndReport) {
            addHeat();
        }
        
        CounterAV = getCounterAV();
        // use this if AMS counterfire destroys all the Capital missiles
        if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3356);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
        }
        // use this if PD counterfire destroys all the Capital missiles
        if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3355);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
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
        // Aero Sanity Handling
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY) && !bMissed) {
            // New toHit data to hold our bay auto hit. We want to be able to get glacing/direct blow
            // data from the 'real' toHit data of this bay handler
            ToHitData autoHit = new ToHitData();
            autoHit.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "if the bay hits, all bay weapons hit");
            int replaceReport;
            for (int wId : weapon.getBayWeapons()) {
                Mounted m = ae.getEquipment(wId);
                if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                    WeaponType bayWType = ((WeaponType) m.getType());
                    if (bayWType instanceof Weapon) {
                        replaceReport = vPhaseReport.size();
                        WeaponAttackAction bayWaa = new WeaponAttackAction(waa.getEntityId(), waa.getTargetType(), waa.getTargetId(), wId);
                        AttackHandler bayWHandler = ((Weapon) bayWType).getCorrectHandler(autoHit, bayWaa, game, server);
                        bayWHandler.setAnnouncedEntityFiring(false);
                        // This should always be true. Maybe there's a better way to write this?
                        if (bayWHandler instanceof WeaponHandler) {
                            WeaponHandler wHandler = (WeaponHandler) bayWHandler;
                            wHandler.setParentBayHandler(this);
                        }
                        bayWHandler.handle(phase, vPhaseReport);
                        if (vPhaseReport.size() > replaceReport) {
                            // fix the reporting - is there a better way to do this
                            Report currentReport = vPhaseReport.get(replaceReport);
                            while (null != currentReport) {
                                vPhaseReport.remove(replaceReport);
                                if ((currentReport.newlines > 0) || (vPhaseReport.size() <= replaceReport)) {
                                    currentReport = null;
                                } else {
                                    currentReport = vPhaseReport.get(replaceReport);
                                }
                            }
                            r = new Report(3115);
                            r.indent(2);
                            r.newlines = 1;
                            r.subject = subjectId;
                            r.add(bayWType.getName());
                            if (entityTarget != null) {
                                r.addDesc(entityTarget);
                            } else {
                                r.messageId = 3120;
                                r.add(target.getDisplayName(), true);
                            }
                            vPhaseReport.add(replaceReport, r);
                        }
                    }
                }
            } // Handle the next weapon in the bay
            Report.addNewline(vPhaseReport);
            return false;
        }
        
        // Handle damage.
        int nCluster = calcnCluster();
        int id = vPhaseReport.size();
        int hits = calcHits(vPhaseReport);
        // Set the hit location table based on where the missile goes active
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
        
        // Bearings-only missiles shouldn't be able to target buildings, being space-only weapons
        // but if these two things aren't defined, handleEntityDamage() doesn't work.
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
            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            server.creditKill(entityTarget, ae);
        } else if (!bMissed) { // Hex is targeted, need to report a hit
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
        // Set the original missile target data. AMS and to-hit table calculations need this.
        aaa.setOldTargetCoords(tc);
        aaa.setOriginalTargetId(target.getTargetId());
        aaa.setOriginalTargetType(target.getTargetType());
        int missileFacing = ae.getPosition().direction(tc);
        Targetable newTarget = null;
        Vector<Aero> targets = new Vector<>();
        
        // get all entities on the opposing side
        for (Iterator<Entity> enemies = game.getAllEnemyEntities(ae); enemies.hasNext();) {
            Entity e = enemies.next();
            // Narrow the list to small craft and larger
            if (((e.getEntityType() & (Entity.ETYPE_SMALL_CRAFT)) != 0)) {
                Aero a = (Aero) e;
                targets.add(a);
            } else if (((e.getEntityType() & (Entity.ETYPE_JUMPSHIP)) != 0)) {
                Aero a = (Aero) e;
                targets.add(a);
            }            
        }
        if (targets.isEmpty()) {
            // We're not dealing with targets in arc or in range yet.
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "no valid targets in play");
            return;
        }

        assert (newTarget != null);
        
        // Add only targets in arc
        Vector<Aero> inArc = new Vector<>();
        for (Aero a : targets) {
            boolean isInArc = Compute.isInArc(tc, missileFacing, a, Compute.ARC_NOSE);
            if (isInArc) {
                inArc.add(a);
            }
        }
        if (inArc.isEmpty()) {
            newTarget = aaa.getTarget(game);
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                    "no targets detected within the missile's nose arc");
            return;
        }
        // Empty out the targets vector and only put valid targets in arc back in
        targets.removeAllElements();
        targets.addAll(inArc);
        
        // Detection range for targets is based on the range set at firing
        Vector<Aero> detected = new Vector<>();
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
        if (detected.isEmpty()) {
            newTarget = aaa.getTarget(game);
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                    "no targets detected within the missile's detection range");
            return;
        }
        // Empty out the targets vector and only put valid targets in range back in
        targets.removeAllElements();
        targets.addAll(detected);

        // If we're using tele-operated missiles, the player gets to select the target
        if (weapon.getType() instanceof TeleOperatedMissileBayWeapon) {
            List<Integer> targetIds = new ArrayList<>();
            List<Integer> toHitValues = new ArrayList<>();
            for (Aero target : targets) {
                setToHit(target);
                targetIds.add(target.getId());
                toHitValues.add(toHit.getValue());
            }
            int choice = server.processTeleguidedMissileCFR(ae.getOwnerId(), targetIds, toHitValues);
            newTarget = targets.get(choice);
            target = newTarget;
            aaa.setTargetId(target.getTargetId());
            aaa.setTargetType(target.getTargetType());
            // Run this again, otherwise toHit is left set to the value for the last target in the list...
            setToHit(target);
            server.assignAMS();
        } else {
            // Otherwise, find the largest and closest target of those available
            int bestDistance = Integer.MAX_VALUE;
            int bestTonnage = 0;
            Aero currTarget = targets.firstElement();
            newTarget = null;
            // Target the closest large craft
            for (Aero a : targets) {
                // Ignore small craft for now
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
        
    /**
     * Checks to see if this point defense/AMS bay can engage a capital missile
     * This should return true. Only when handling capital missile attacks can this be false.
     */
    @Override
    protected boolean canEngageCapitalMissile(Mounted counter) {
        if (counter.getBayWeapons().size() < 2) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setAMSBayReportingFlag() {
        amsBayEngagedCap = true;
    }
    
    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setPDBayReportingFlag() {
        pdBayEngagedCap = true;
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
            av = applyGlancingBlowModifier(av, false);
            av = (int) Math.floor(getBracketingMultiplier() * av);
            return (int) Math.ceil(av);
    }
    
    /**
     * Calculate the damage per hit.
     *
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    @Override
    protected int calcDamagePerHit() {
        AmmoType atype = (AmmoType) ammo.getType();
        double toReturn = wtype.getDamage(nRange);
        
        //AR10 munitions
        if (atype != null) {
            if (atype.getAmmoType() == AmmoType.T_AR10) {
                if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                    toReturn = 4;
                } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                    toReturn = 3;
                } else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
                    toReturn = 1000;
                } else if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                    toReturn = 100;
                } else {
                    toReturn = 2;
                }
            }
            //Nuclear Warheads for non-AR10 missiles
            if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                toReturn = 100;
            } else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
                toReturn = 1000;
            } 
            nukeS2S = atype.hasFlag(AmmoType.F_NUCLEAR);
        }
        
        // we default to direct fire weapons for anti-infantry damage
        if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }

        toReturn = applyGlancingBlowModifier(toReturn, false);

        return (int) toReturn;
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
    
    /**
     * Calculate the starting armor value of a flight of Capital Missiles
     * Used for Aero Sanity. This is done in calcAttackValue() otherwise
     *
     */
    @Override
    protected int initializeCapMissileArmor() {
        int armor = 0;
        for (int wId : weapon.getBayWeapons()) {
            int curr_armor = 0;
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();
            AmmoType atype = (AmmoType) bayWAmmo.getType();
            WeaponType bayWType = ((WeaponType) bayW.getType());
            if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                    && (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                            || atype.hasFlag(AmmoType.F_PEACEMAKER))) {
                curr_armor = 40;
            } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                    && (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
                            || atype.hasFlag(AmmoType.F_SANTA_ANNA))) {
                curr_armor = 30;
            } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                    && atype.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                curr_armor = 20;
            } else {
                curr_armor = bayWType.getMissileArmor();
            }
            armor = armor + curr_armor;
        }
        return armor;
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
