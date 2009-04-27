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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.TagInfo;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

public class ArtilleryWeaponIndirectHomingHandler extends
        ArtilleryWeaponIndirectFireHandler implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -7243477723032010917L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ArtilleryWeaponIndirectHomingHandler(ToHitData t,
            WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!this.cares(phase)) {
            return true;
        }
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) waa;
        if (phase == IGame.Phase.PHASE_TARGETING) {
            if (!handledAmmoAndReport) {
                addHeat();
                // Report the firing itself
                r = new Report(3121);
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
                announcedEntityFiring = false;
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
                    .getTarget(game)
                    : null;
        } else {
            entityTarget = (Entity) target;
        }
        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());

        // Report weapon attack and its to-hit value.
        r = new Report(3115);
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
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
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
        if (game.getOptions().booleanOption("tacops_glancing_blows")) {
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

        //Set Margin of Success/Failure.
        toHit.setMoS(roll-Math.max(2,toHit.getValue()));
        bDirect = game.getOptions().booleanOption("tacops_direct_blow") && ((toHit.getMoS()/3) >= 1) && entityTarget != null;
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
        if (((AmmoType)ammo.getType()).getAmmoType() != AmmoType.T_ARROW_IV) {
            nDamPerHit -= 10;
        }

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget, bMissed)) {
            return false;
        }

        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, targetInBuilding, bldg,
                    vPhaseReport)) {
                return false;
            }
        }
        int hits = 1;
        int nCluster = 1;
        if (entityTarget != null && entityTarget.getTaggedBy() != -1) {
            if (aaa.getCoords() != null) {
                toHit.setSideTable(entityTarget.sideTable(aaa.getCoords()));
            }
        }

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if (targetInBuilding && bldg != null) {
            bldgAbsorbs = (int) Math.ceil(bldg.getPhaseCF(target.getPosition()) / 10.0);
        }
        if ((bldg != null) && (bldgAbsorbs > 0)) {
            // building absorbs some damage
            r = new Report(6010);
            if (entityTarget != null)
                r.subject = entityTarget.getId();
            r.add(bldgAbsorbs);
            vPhaseReport.addElement(r);
            Vector<Report> buildingReport = server.damageBuilding(bldg,
                    nDamPerHit, entityTarget.getPosition());
            for (Report report : buildingReport) {
                report.subject = entityTarget.getId();
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
        if (!bMissed && entityTarget != null) {
            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                    nCluster, nDamPerHit, bldgAbsorbs);
            server.creditKill(entityTarget, ae);
        }
        Coords coords = target.getPosition();
        int ratedDamage = 5; // splash damage is 5 from all launchers
        bldg = null;
        bldg = game.getBoard().getBuildingAt(coords);
        bldgAbsorbs = (bldg != null) ? bldg.getPhaseCF(coords) / 10 : 0;
        bldgAbsorbs = Math.min(bldgAbsorbs, ratedDamage);
        // assumption: homing artillery splash damage is area effect.
        // do damage to woods, 2 * normal damage (TW page 112)
        handleClearDamage(vPhaseReport, bldg, ratedDamage * 2, bSalvo);
        ratedDamage -= bldgAbsorbs;
        if (ratedDamage > 0) {
            for (Enumeration<Entity> impactHexHits = game.getEntities(coords); impactHexHits
                    .hasMoreElements();) {
                Entity entity = impactHexHits.nextElement();
                if (!bMissed) {
                    if (entity == entityTarget)
                        continue; // don't splash the target unless missile
                    // missed
                }
                toHit.setSideTable(entity.sideTable(aaa.getCoords()));
                HitData hit = entity.rollHitLocation(toHit.getHitTable(), toHit
                        .getSideTable(), waa.getAimedLocation(), waa
                        .getAimingMode());
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
        Entity entityTarget = null;

        TagInfo info = null;
        Entity tagger = null;

        for (int pass = 0; pass < 2; pass++) {
            int bestDistance = Integer.MAX_VALUE;
            int bestIndex = -1;
            Vector<TagInfo> v = game.getTagInfo();
            for (int i = 0; i < v.size(); i++) {
                info = v.elementAt(i);
                tagger = game.getEntity(info.attackerId);
                if (info.shots < info.priority && !ae.isEnemyOf(tagger)) {
                    entityTarget = game.getEntity(info.targetId);
                    if (entityTarget != null && entityTarget.isOnSameSheet(tc)) {
                        if (tc.distance(entityTarget.getPosition()) < bestDistance) {
                            bestIndex = i;
                            bestDistance = tc.distance(entityTarget
                                    .getPosition());
                            if (!game.getOptions().booleanOption(
                                    "a4homing_target_area")) {
                                break; // first will do if mapsheets can't
                                // overlap
                            }
                        }
                    }
                }
            }
            if (bestIndex != -1) {
                info = v.elementAt(bestIndex);
                entityTarget = game.getEntity(info.targetId);
                tagger = game.getEntity(info.attackerId);
                info.shots++;
                game.updateTagInfo(info, bestIndex);
                break; // got a target, stop searching
            }
            entityTarget = null;
            // nothing found on 1st pass, so clear shots fired to 0
            game.clearTagInfoShots(ae, tc);
        }

        if (entityTarget == null || info == null) {
            toHit = new ToHitData(ToHitData.IMPOSSIBLE,
                    "no targets tagged on map sheet");
        } else if (info.missed) {
            aaa.setTargetId(entityTarget.getId());
            aaa.setTargetType(Targetable.TYPE_ENTITY);
            target = entityTarget;
            toHit = new ToHitData(ToHitData.IMPOSSIBLE, "tag missed the target");
        } else {
            // update for hit table resolution
            target = entityTarget;
            aaa.setCoords(tagger.getPosition());
            aaa.setTargetId(entityTarget.getId());
            aaa.setTargetType(Targetable.TYPE_ENTITY);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#handleSpecialMiss(megamek.common.Entity,
     *      boolean, megamek.common.Building, java.util.Vector)
     */
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean targetInBuilding, Building bldg, Vector<Report> vPhaseReport) {
        return true;
    }
}
