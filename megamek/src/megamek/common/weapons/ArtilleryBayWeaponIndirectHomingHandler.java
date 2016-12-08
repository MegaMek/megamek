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

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.TagInfo;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
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

    /**
     * @param t
     * @param w
     * @param g
     */
    public ArtilleryBayWeaponIndirectHomingHandler(ToHitData t,
            WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
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
            convertHomingShotToEntityTarget();
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
     * Find the tagged entity for this attack Each TAG will attract a number of
     * shots up to its priority number (mode setting) When all the TAGs are used
     * up, the shots fired are reset. So if you leave them all on 1-shot, then
     * homing attacks will be evenly split, however many shots you fire.
     * Priority setting is to allocate more homing attacks to a more important
     * target as decided by player. TAGs fired by the enemy aren't eligible, nor
     * are TAGs fired at a target on a different map sheet.
     */
    protected void convertHomingShotToEntityTarget() {
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
        } else {
            // find the TAG hit with the most shots left, and closest
            int bestDistance = Integer.MAX_VALUE;
            TagInfo targetTag = allowed.firstElement();
            for (TagInfo ti : allowed) {
                int distance = tc.distance(newTarget.getPosition());

                // higher # of shots left
                if (ti.shots > targetTag.shots) {
                    bestDistance = distance;
                    targetTag = ti;
                    continue;
                }
                // same # of shots left
                if (ti.shots == targetTag.shots) {
                    // higher priority
                    if (ti.priority > targetTag.priority) {
                        bestDistance = distance;
                        targetTag = ti;
                        continue;
                    }
                    // same priority and closer
                    if ((ti.priority == targetTag.priority)
                            && (bestDistance > distance)) {
                        bestDistance = distance;
                        targetTag = ti;
                    }
                }
            }

            // if the best TAG has no shots left
            if (targetTag.shots == 0) {
                game.clearTagInfoShots(ae, tc);
            }

            targetTag.shots--;
            target = targetTag.target;
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
}
