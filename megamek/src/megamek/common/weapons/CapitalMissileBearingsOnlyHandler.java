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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mounted;
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
import megamek.server.Server;
import megamek.server.Server.DamageType;

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
    boolean advancedPD = false;

    /**
     * This consructor may only be used for deserialization.
     */
    protected CapitalMissileBearingsOnlyHandler() {
        super();
        advancedPD = game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
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

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.AttackHandler#cares(int)
     */
    @Override
    public boolean cares(IGame.Phase phase) {
        if ((phase == IGame.Phase.PHASE_OFFBOARD)
                || (phase == IGame.Phase.PHASE_TARGETING)) {
            return true;
        }
        return false;
    }
        
    @Override
    protected void useAmmo() {
        final String METHOD_NAME = "useAmmo()";
        for (int wId : weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();

            if (bayWAmmo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                logDebug(METHOD_NAME, "Handler can't find any ammo! Oh no!");
            }

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
        Entity entityTarget;
        if (game.getPhase() == IGame.Phase.PHASE_OFFBOARD) {
            convertHexTargetToEntityTarget();
            entityTarget = (aaa.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) aaa
                    .getTarget(game) : null;
        } else {
            entityTarget = (Entity) target;
        }
        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);
        final boolean bldgDamagedOnMiss = targetInBuilding
                && !(target instanceof Infantry)
                && ae.getPosition().distance(target.getPosition()) <= 1;

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());

        // Report weapon attack and its to-hit value.
        Report r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName());
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
                r.subject = subjectId;
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
        bDirect = game.getOptions().booleanOption("tacops_direct_blow")
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
        if (bDirect) {
            r = new Report(3189);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        // we may still have to use ammo, if direct fire
        if (!handledAmmoAndReport) {
            addHeat();
        }

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);
        if (missReported) {
            bMissed = true;
        }
        nDamPerHit = wtype.getRackSize();

        // copperhead gets 10 damage less than standard
        if (((AmmoType) ammo.getType()).getAmmoType() != AmmoType.T_ARROW_IV) {
            nDamPerHit -= 10;
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
        int hits = 1;
        int nCluster = 1;        
        if ((entityTarget != null) && (entityTarget.getTaggedBy() != -1)) {
            if (aaa.getCoords() != null) {
                toHit.setSideTable(entityTarget.sideTable(aaa.getCoords()));
            }

        }
               

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if (targetInBuilding && (bldg != null)) {
            bldgAbsorbs = bldg.getAbsorbtion(target.getPosition());
        }
        if ((bldg != null) && (bldgAbsorbs > 0)) {
            // building absorbs some damage
            r = new Report(6010);
            if (entityTarget != null) {
                r.subject = entityTarget.getId();
            }
            r.add(bldgAbsorbs);
            vPhaseReport.addElement(r);
            Vector<Report> buildingReport = server.damageBuilding(bldg,
                    nDamPerHit, target.getPosition());
            if (entityTarget != null) {
                for (Report report : buildingReport) {
                    report.subject = entityTarget.getId();
                }
            }
            vPhaseReport.addAll(buildingReport);
        }
        nDamPerHit -= bldgAbsorbs;

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
        } else if (!bMissed && // The attack is targeting a specific building
                (target.getTargetType() == Targetable.TYPE_BLDG_TAG)){
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            vPhaseReport.addAll(server.damageBuilding(bldg,
                    nDamPerHit, target.getPosition()));
        } else if (!bMissed){ // Hex is targeted, need to report a hit
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        Coords coords = target.getPosition();
        int ratedDamage = 5; // splash damage is 5 from all launchers
        
        //If AMS shoots down a missile, it shouldn't deal any splash damage
        if (hits == 0) {
            ratedDamage = 0;
        }
        
        bldg = null;
        bldg = game.getBoard().getBuildingAt(coords);
        bldgAbsorbs = (bldg != null) ? bldg.getAbsorbtion(coords) : 0;
        bldgAbsorbs = Math.min(bldgAbsorbs, ratedDamage);
        // assumption: homing artillery splash damage is area effect.
        // do damage to woods, 2 * normal damage (TW page 112)
        handleClearDamage(vPhaseReport, bldg, ratedDamage * 2, false);
        ratedDamage -= bldgAbsorbs;
        if (ratedDamage > 0) {
            for (Entity entity : game.getEntitiesVector(coords)) {
                if (!bMissed) {
                    if (entity == entityTarget) {
                        continue; // don't splash the target unless missile
                        // missed
                    }
                }
                toHit.setSideTable(entity.sideTable(aaa.getCoords()));
                HitData hit = entity.rollHitLocation(toHit.getHitTable(),
                        toHit.getSideTable(), waa.getAimedLocation(),
                        waa.getAimingMode(), toHit.getCover());
                hit.setAttackerId(getAttackerId());
                // BA gets damage to all troopers
                if (entity instanceof BattleArmor) {
                    BattleArmor ba = (BattleArmor) entity;
                    for (int loc = 1; loc <= ba.getTroopers(); loc++) {
                        hit.setLocation(loc);
                        vPhaseReport.addAll(server.damageEntity(entity, hit,
                                ratedDamage, false, DamageType.NONE, false,
                                true, throughFront, underWater));
                    }
                } else {
                    vPhaseReport.addAll(server.damageEntity(entity, hit,
                            ratedDamage, false, DamageType.NONE, false, true,
                            throughFront, underWater));
                }
                server.creditKill(entity, ae);
            }
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
    public void convertHexTargetToEntityTarget() {
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) waa;

        final Coords tc = target.getPosition();
        int missileFacing = ae.getPosition().direction(tc);
        boolean detRangeShort = weapon.curMode().equals("Bearings-Only Short Detection Range");
        boolean detRangeMedium = weapon.curMode().equals("Bearings-Only Medium Detection Range");
        boolean detRangeLong = weapon.curMode().equals("Bearings-Only Long Detection Range");
        boolean detRangeExtreme = weapon.curMode().equals("Bearings-Only Extreme Detection Range");
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
            Boolean isInArc = Compute.isInArc(aaa.getCoords(), missileFacing, a, Compute.ARC_NOSE);
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
        
            // find the largest and closest target of those available
            int bestDistance = Integer.MAX_VALUE;
            int bestTonnage = 0;
            Aero currTarget = targets.firstElement();
            newTarget = null;
            //Target the closest large craft
            for (Aero a : targets) {
                //Ignore small craft for now
                if (((a.getEntityType() & (Entity.ETYPE_SMALL_CRAFT)) == Entity.ETYPE_SMALL_CRAFT)) {
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
            //Now, assign our chosen target to the missile
            target = newTarget;
            aaa.setTargetId(target.getTargetId());
            aaa.setTargetType(target.getTargetType());
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

}
