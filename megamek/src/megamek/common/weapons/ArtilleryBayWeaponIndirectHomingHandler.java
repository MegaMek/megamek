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
import java.util.List;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BombType;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.TagInfo;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.Server.DamageType;

public class ArtilleryBayWeaponIndirectHomingHandler extends
        ArtilleryBayWeaponIndirectFireHandler {

    /**
     *
     */
    private static final long serialVersionUID = -7243477723032010917L;
    boolean advancedPD = false;
    boolean advancedAMS = false;
    boolean multiAMS = false;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ArtilleryBayWeaponIndirectHomingHandler(ToHitData t,
            WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
        advancedAMS = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_AMS);
        multiAMS = g.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_MULTI_USE_AMS);
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
                Report r = new Report(3121);
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
        
        convertHomingShotToEntityTarget();
        Entity entityTarget = (aaa.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) aaa
                .getTarget(game) : null;
                
        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);
        final boolean bldgDamagedOnMiss = targetInBuilding
                && !(target instanceof Infantry)
                && ae.getPosition().distance(target.getPosition()) <= 1;

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());
        
        //Determine what ammo we're firing for reporting and (later) damage
        Mounted ammoUsed = ae.getEquipment(aaa.getAmmoId());
        final AmmoType atype = (AmmoType) ammoUsed.getType();
        // Report weapon attack and its to-hit value.
        Report r = new Report(3124);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName());
        r.add(nweaponsHit);
        r.add(atype.getShortName());
        if (entityTarget != null) {
            r.addDesc(entityTarget);
        } else {
            r.messageId = 3126;
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

        //Set up the damage
        nDamPerHit = atype.getRackSize();
        
        // copperhead gets 10 damage less than standard
        if (atype != null && atype.getAmmoType() != AmmoType.T_ARROW_IV) {
            nDamPerHit -= 10;
        }
        
        if (bGlancing) {
            nDamPerHit /= 2;
        }

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }
        
        //this has to be called here or it triggers before the TAG shot and we have no entityTarget
        //mounting AMS
        if (atype != null 
                && atype.getAmmoType() == AmmoType.T_ARROW_IV) {
            server.assignAMS();
        }
        while (nweaponsHit > 0) {
            int hits = 1;
            int nCluster = 1;        
            if ((entityTarget != null) && (entityTarget.getTaggedBy() != -1)) {
                //Do point defenses shoot down this homing missile? (Copperheads don't count)
                hits = handleAMS(vPhaseReport);
                
                if (bMissed && !missReported) {
                    reportMiss(vPhaseReport);

                    // Works out fire setting and whether continuation is
                    // necessary.
                    if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
                            vPhaseReport)) {
                        return false;
                    }
                }
                
                if (aaa.getCoords() != null && hits > 0) {
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
            nweaponsHit--;
        }
        return false;
    }

    /**
     * Find the tagged entity for this attack 
     * Uses a CFR to let the player choose from eligible TAGs
     */
    public void convertHomingShotToEntityTarget() {
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) waa;

        final Coords tc = target.getPosition();
        Targetable newTarget = null;

        Vector<TagInfo> v = game.getTagInfo();
        Vector<TagInfo> allowed = new Vector<TagInfo>();
        // get only TagInfo on the same side
        for (TagInfo ti : v) {
            switch (ti.targetType){
            case Targetable.TYPE_BLDG_TAG:
            case Targetable.TYPE_HEX_TAG:
                allowed.add(ti);
                break;
            case Targetable.TYPE_ENTITY:
                if (ae.isEnemyOf((Entity) ti.target)
                        || game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
                    allowed.add(ti);
                }
                break;
            }
        }
        if (allowed.size() == 0) {
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                    "no targets tagged this turn");
            return;
        }

        // get TAGs that hit
        v = new Vector<TagInfo>();
        for (TagInfo ti : allowed) {
            newTarget = ti.target;
            if (!ti.missed && (newTarget != null)) {
                v.add(ti);
            }
        }
        assert (newTarget != null);
        if (v.size() == 0) {
            aaa.setTargetId(newTarget.getTargetId());
            aaa.setTargetType(newTarget.getTargetType());
            target = newTarget;
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                    "tag missed the target");
            return;
        }
        // get TAGs that are on the same map
        allowed = new Vector<TagInfo>();
        for (TagInfo ti : v) {
            newTarget = ti.target;
            // homing target area is 8 hexes
            if (tc.distance(newTarget.getPosition()) <= 8) {
                allowed.add(ti);
            }
        }
        
        if (allowed.size() == 0) {
            aaa.setTargetId(newTarget.getTargetId());
            aaa.setTargetType(newTarget.getTargetType());
            target = newTarget;
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE,
                    "no tag in 8 hex radius of target hex");
        } else if (allowed.size() == 1) {
            //Just use target 0...
            newTarget = allowed.get(0).target;
            target = newTarget;
            aaa.setTargetId(target.getTargetId());
            aaa.setTargetType(target.getTargetType());
        } else {
            //The player gets to select the target
            List<Integer> targetIds = new ArrayList<Integer>();
            List<Integer> targetTypes = new ArrayList<Integer>();
            for (TagInfo target : allowed) {
                targetIds.add(target.target.getTargetId());
                targetTypes.add(target.target.getTargetType());
            }
            int choice = server.processTAGTargetCFR(ae.getOwnerId(), targetIds, targetTypes);
            newTarget = allowed.get(choice).target;
            target = newTarget;
            aaa.setTargetId(target.getTargetId());
            aaa.setTargetType(target.getTargetType());
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
    
    protected int getAMSHitsMod(Vector<Report> vPhaseReport) {
        if ((target == null)
                || target.getTargetType() != Targetable.TYPE_ENTITY
                || CounterAV > 0) {
            return 0;
        }
        int apdsMod = 0;
        int amsMod = 0;
        Entity entityTarget = (Entity) target;
        // any AMS attacks by the target?
        ArrayList<Mounted> lCounters = waa.getCounterEquipment();
        if (null != lCounters) {
            // resolve AMS counter-fire
            for (Mounted counter : lCounters) {
                boolean isAMS = counter.getType().hasFlag(WeaponType.F_AMS);
                if (isAMS && counter.isAPDS() && !apdsEngaged) {
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
                        apdsEnt.heatBuildup += Compute.d6(counter
                                .getCurrentHeat());
                    } else {
                        apdsEnt.heatBuildup += counter.getCurrentHeat();
                    }

                    // decrement the ammo
                    if (mAmmo != null) {
                        mAmmo.setShotsLeft(Math.max(0,
                                mAmmo.getBaseShotsLeft() - 1));
                    }

                    // Determine Modifier
                    int dist = target.getPosition().distance(
                            apdsEnt.getPosition());
                    int minApdsMod = -4;
                    if (apdsEnt instanceof BattleArmor) {
                        int numTroopers = ((BattleArmor) apdsEnt)
                                .getNumberActiverTroopers();
                        switch (numTroopers) {
                            case 1:
                                minApdsMod = -2;
                                break;
                            case 2:
                            case 3:
                                minApdsMod = -3;
                                break;
                            default: // 4+
                                minApdsMod = -4;
                        }
                    }
                    apdsMod = Math.min(minApdsMod + dist, 0);

                    // set the ams as having fired
                    counter.setUsedThisRound(true);
                    apdsEngaged = true;

                } else if (isAMS && !amsEngaged) {
                    Mounted mAmmo = counter.getLinked();
                    if (!(counter.getType() instanceof WeaponType)
                            || !counter.isReady() || counter.isMissing()
                            // no AMS when a shield in the AMS location
                            || (entityTarget.hasShield() && entityTarget
                                    .hasActiveShield(counter.getLocation(),
                                            false))
                            // shutdown means no AMS
                            || entityTarget.isShutDown()
                            // AMS only fires vs attacks in arc covered by ams
                            || !Compute.isInArc(game, entityTarget.getId(),
                                    entityTarget.getEquipmentNum(counter), ae)) {
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
                    amsMod = -4;
                }
            }
        }
        return apdsMod + amsMod;
    }
    
    /**
     * This is a unified method that handles single AMS and AMS Bay counterfire against Arrow IV homing missiles
     * Artillery bays resolve each weapon individually and don't use Aero AV, so we can safely do this
     * @param vPhaseReport The report for this game phase, be it offboard (Indirect) or firing (Direct)
     * @return 1 hit if this missile survives any AMS fire, 0 if it is destroyed
     */
    protected int handleAMS(Vector<Report> vPhaseReport) {
        
        int hits = 1;
        if (((AmmoType) ammo.getType()).getAmmoType() == AmmoType.T_ARROW_IV
                || ((AmmoType) ammo.getType()).getAmmoType() == BombType.B_HOMING) {

            //this has to be called here or it fires before the TAG shot and we have no target
            server.assignAMS();
            calcCounterAV();
            getAMSHitsMod(vPhaseReport);
            // Report AMS/Pointdefense failure due to Overheating.
            if (pdOverheated 
                    && (!(amsBayEngaged
                            || amsBayEngagedCap
                            || amsBayEngagedMissile
                            || pdBayEngaged
                            || pdBayEngagedCap
                            || pdBayEngagedMissile))) {
                Report r = new Report (3359);
                r.subject = subjectId;
                r.indent();
                vPhaseReport.addElement(r);
            }
            //PD/AMS bays should engage using AV and missile armor per SO Errata
            if (amsBayEngagedCap || pdBayEngagedCap) {
                CapMissileArmor = wtype.getMissileArmor() - CounterAV;
                CapMissileAMSMod = calcCapMissileAMSMod();
                Report r = new Report(3235);
                r.subject = subjectId;
                r.indent(1);
                vPhaseReport.add(r);
                if (CapMissileArmor <= 0) {
                    r = new Report(3356);
                    r.subject = subjectId;
                    vPhaseReport.add(r);
                    nDamPerHit = 0;
                    hits = 0;
                } else {
                    r = new Report(3358);
                    r.subject = subjectId;
                    r.add(CapMissileAMSMod);
                    vPhaseReport.add(r);
                    toHit.addModifier(CapMissileAMSMod, "damage from AMS");
                    // If the damage was enough to make us miss, record it for reporting and set 0 hits
                    if (roll < toHit.getValue()) {
                        bMissed = true;
                        nDamPerHit = 0;
                        hits = 0;
                    }
                }
            } else if (amsEngaged || apdsEngaged) {
                //Single AMS/APDS should continue to engage per TW rules, which have not changed
                bSalvo = true;
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
                    nDamPerHit = 0;
                    hits = 0;
                                           
                } else {
                    r = new Report(3241);
                    r.add("missile");
                    r.add(destroyRoll);
                    r.subject = subjectId;
                    vPhaseReport.add(r);
                    hits = 1;
                }
            }
        }
        return hits;
    }
    
    /**
     * Checks to see if the basic conditions needed for point defenses to work are in place
     * Artillery weapons need to change this slightly compared to other types of missiles
     */
    @Override
    protected boolean checkPDConditions() {
        if ((target == null) 
                || target.getTargetType() != Targetable.TYPE_ENTITY 
                || !advancedPD
                || !advancedAMS
                || waa.getCounterEquipment() == null) {
            return false;
        }
        return true;
    }
}
