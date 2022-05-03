/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.Server.DamageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class ArtilleryWeaponIndirectHomingHandler extends ArtilleryWeaponIndirectFireHandler {
    private static final long serialVersionUID = -7243477723032010917L;
    boolean amsEngaged = false;
    boolean apdsEngaged = false;
    boolean advancedAMS = false;
    boolean advancedPD = false;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ArtilleryWeaponIndirectHomingHandler(ToHitData t,
            WeaponAttackAction w, Game g, Server s) {
        super(t, w, g, s);
        advancedAMS = g.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_AMS);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
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

        // are we a glancing hit?
        setGlancingBlowFlags(entityTarget);
        addGlancingBlowReports(vPhaseReport);

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
        
        //Any AMS/Point Defense fire against homing rounds?
        int hits = handleAMS(vPhaseReport);

        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
                    vPhaseReport)) {
                return false;
            }
        }
        int nCluster = 1;       
        if ((entityTarget != null) && (entityTarget.getTaggedBy() != -1)) {
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
        if (nDamPerHit <= 0) {
            r = new Report(3365);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
        }
        
        boolean targetingHex = false;
        
        if (!bMissed && (entityTarget != null)) {
            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            server.creditKill(entityTarget, ae);
        } else if (!bMissed && // The attack is targeting a specific building
                (target.getTargetType() == Targetable.TYPE_BLDG_TAG)) {
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            vPhaseReport.addAll(server.damageBuilding(bldg,
                    nDamPerHit, target.getPosition()));
        } else if (!bMissed) { // Hex is targeted, need to report a hit
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            targetingHex = true;
        }

        Coords coords = target.getPosition();
        int ratedDamage = 5; // splash damage is 5 from all launchers
        
        //If AMS shoots down a missile, it shouldn't deal any splash damage
        if (hits == 0) {
            ratedDamage = 0;
        }
        
        // homing artillery splash damage is area effect.
        // do damage to woods, 2 * normal damage (TW page 112)
        // on the other hand, if the hex *is* the target, do full damage
        int hexDamage = targetingHex ? wtype.getRackSize() : ratedDamage * 2;
        
        bldg = null;
        bldg = game.getBoard().getBuildingAt(coords);
        bldgAbsorbs = (bldg != null) ? bldg.getAbsorbtion(coords) : 0;
        bldgAbsorbs = Math.min(bldgAbsorbs, ratedDamage);
        handleClearDamage(vPhaseReport, bldg, hexDamage, false);
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
                vPhaseReport.addAll(server.damageEntity(entity, hit,
                            ratedDamage, false, DamageType.NONE, false, true,
                            throughFront, underWater));
                server.creditKill(entity, ae);
            }
        }
        Report.addNewline(vPhaseReport);
        return false;
    }

    /**
     * Find the tagged entity for this attack 
     * Uses a CFR to let the player choose from eligible TAG
     */
    public void convertHomingShotToEntityTarget() {
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) waa;

        final Coords tc = target.getPosition();
        Targetable newTarget = null;

        Vector<TagInfo> v = game.getTagInfo();
        Vector<TagInfo> allowed = new Vector<>();
        Entity attacker = game.getEntityFromAllSources(getAttackerId());
        
        // get only TagInfo on the same side
        for (TagInfo ti : v) { 
            Entity tagger = game.getEntityFromAllSources(ti.attackerId);
            if (attacker.getOwner().isEnemyOf(tagger.getOwner())) {
                continue;
            }
            
            switch (ti.targetType) {
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

        if (allowed.isEmpty()) {
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "no targets tagged this turn");
            return;
        }

        // get TAGs that hit
        v = new Vector<>();
        for (TagInfo ti : allowed) {
            newTarget = ti.target;
            if (!ti.missed && (newTarget != null)) {
                v.add(ti);
            }
        }

        Objects.requireNonNull(newTarget);
        if (v.isEmpty()) {
            aaa.setTargetId(newTarget.getTargetId());
            aaa.setTargetType(newTarget.getTargetType());
            target = newTarget;
            toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "tag missed the target");
            return;
        }

        // get TAGs that are on the same map
        allowed = new Vector<>();
        for (TagInfo ti : v) {
            newTarget = ti.target;
            // homing target area is 8 hexes
            if (tc.distance(newTarget.getPosition()) <= 8) {
                allowed.add(ti);
            }

        }
        if (allowed.isEmpty()) {
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
            List<Integer> targetIds = new ArrayList<>();
            List<Integer> targetTypes = new ArrayList<>();
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
    
    /**
     * Checks to see if the basic conditions needed for point defenses to work are in place
     * Artillery weapons need to change this slightly
     */
    @Override
    protected boolean checkPDConditions() {
        advancedPD = game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
        if ((target == null) || !advancedPD || (target.getTargetType() != Targetable.TYPE_ENTITY)) {
            return false;
        }
        return true;
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
    protected int calcCapMissileAMSMod() {
        CapMissileAMSMod = (int) Math.ceil(CounterAV / 10.0);
        return CapMissileAMSMod;
    }
    
    @Override
    protected int getCapMissileAMSMod() {
        return CapMissileAMSMod;
    }
    
    protected int handleAMS(Vector<Report> vPhaseReport) {
        
        int hits = 1;
        if (((AmmoType) ammo.getType()).getAmmoType() == AmmoType.T_ARROW_IV
                || ((AmmoType) ammo.getType()).getAmmoType() == BombType.B_HOMING) {

            //this has to be called here or it fires before the TAG shot and we have no target
            server.assignAMS();
            calcCounterAV();
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
            }
        }
        return hits;
    }
}
