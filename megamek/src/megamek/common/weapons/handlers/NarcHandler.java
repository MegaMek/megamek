/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeSideTable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.INarcPod;
import megamek.common.equipment.NarcPod;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class NarcHandler extends MissileWeaponHandler {
    @Serial
    private static final long serialVersionUID = 3195613885543781820L;

    public NarcHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        bSalvo = true;
        getAMSHitsMod(vPhaseReport);
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            // Or bay AMS if Aero Sanity is on
            Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                  : null;
            if (entityTarget != null && entityTarget.isLargeCraft()) {
                if (getParentBayHandler() != null) {
                    WeaponHandler bayHandler = getParentBayHandler();
                    amsBayEngagedMissile = bayHandler.amsBayEngagedMissile;
                    pdBayEngagedMissile = bayHandler.pdBayEngagedMissile;
                }
            }
        } else {
            calcCounterAV();
        }
        // Report AMS/Point defense failure due to Overheating.
        if (pdOverheated
              && (!(amsBayEngaged
              || amsBayEngagedCap
              || amsBayEngagedMissile
              || pdBayEngaged
              || pdBayEngagedCap
              || pdBayEngagedMissile))) {
            Report r = new Report(3359);
            r.subject = subjectId;
            r.indent();
            vPhaseReport.addElement(r);
        }
        if (amsEngaged || apdsEngaged || amsBayEngagedMissile || pdBayEngagedMissile) {
            Report report = new Report(3235);
            report.subject = subjectId;
            vPhaseReport.add(report);
            report = new Report(3230);
            report.indent(1);
            report.subject = subjectId;
            vPhaseReport.add(report);
            Roll diceRoll = Compute.rollD6(1);

            if (diceRoll.getIntValue() <= 3) {
                report = new Report(3240);
                report.subject = subjectId;
                report.add("pod");
                report.add(diceRoll);
                vPhaseReport.add(report);
                return 0;
            }
            report = new Report(3241);
            report.add("pod");
            report.add(diceRoll);
            report.subject = subjectId;
            vPhaseReport.add(report);
        }
        return 1;
    }

    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setAMSBayReportingFlag() {
        amsBayEngagedMissile = true;
    }

    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setPDBayReportingFlag() {
        pdBayEngagedMissile = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calculateNumCluster() {
        return 1;
    }

    /**
     * Narcs apply "damage" all in one block for AMS purposes This was referenced incorrectly for Aero damage.
     */
    @Override
    protected boolean usesClusterTable() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.handlers.WeaponHandler#handleEntityDamage(megamek.common
     * .Entity, java.util.Vector, megamek.common.units.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
          Vector<Report> vPhaseReport, IBuilding bldg, int hits, int nCluster,
          int bldgAbsorbs) {
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(),
              toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
              weaponAttackAction.getAimingMode(), toHit.getCover());

        // If our narc pod "hits" an already-missing head, reroll until we hit
        // somewhere else as per the errata for torso-mounted cockpits.
        if (entityTarget instanceof Mek
              && !narcCanAttachTo(entityTarget, Mek.LOC_HEAD)) {
            while (hit.getLocation() == Mek.LOC_HEAD) {
                hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                      toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
                      weaponAttackAction.getAimingMode(), toHit.getCover());
            }
        }
        hit.setAttackerId(getAttackerId());

        // Catch protomek near-misses here.
        // So what do we do for a near miss on a glider? Assume attach to wings.
        if (entityTarget instanceof ProtoMek
              && hit.getLocation() == ProtoMek.LOC_NEAR_MISS
              && !((ProtoMek) entityTarget).isGlider()) {
            Report r = new Report(6035);
            r.subject = entityTarget.getId();
            vPhaseReport.add(r);
            return;
        }

        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
              .getCover(), ComputeSideTable.sideTable(weaponEntity, entityTarget, weapon
              .getCalledShot().getCall()))) {
            // Weapon strikes Partial Cover.
            handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                  nCluster, bldgAbsorbs);
            return;
        }

        // If the pod attached to a destroyed location, have it transfer
        // inwards.
        if (entityTarget instanceof Mek) {
            while (!narcCanAttachTo(entityTarget, hit.getLocation())
                  && (hit.getLocation() != Mek.LOC_CENTER_TORSO)) {
                hit = entityTarget.getTransferLocation(hit);
            }
        }

        // Now the same check for ProtoMeks. We've already covered near-misses
        // above, so here we only have to worry about the actual hits left over.
        if (entityTarget instanceof ProtoMek) {
            while (!narcCanAttachTo(entityTarget, hit.getLocation())
                  && (hit.getLocation() != ProtoMek.LOC_TORSO)) {
                hit = entityTarget.getTransferLocation(hit);
            }
        }

        AmmoType ammoType = ammo.getType();
        if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.NARC) {
            // narced
            NarcPod pod = new NarcPod(weaponEntity.getOwner().getTeam(),
                  hit.getLocation());
            Report r = new Report(3250);
            r.subject = subjectId;
            r.add(entityTarget.getDisplayName());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
            entityTarget.attachNarcPod(pod);
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.INARC) {
            // iNarced
            INarcPod pod;
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ECM)) {
                pod = new INarcPod(attackingEntity.getOwner().getTeam(), INarcPod.ECM,
                      hit.getLocation());
                Report r = new Report(3251);
                r.subject = subjectId;
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.addElement(r);
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_HAYWIRE)) {
                pod = new INarcPod(attackingEntity.getOwner().getTeam(), INarcPod.HAYWIRE,
                      hit.getLocation());
                Report r = new Report(3252);
                r.subject = subjectId;
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.addElement(r);
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_NEMESIS)) {
                pod = new INarcPod(attackingEntity.getOwner().getTeam(), INarcPod.NEMESIS,
                      hit.getLocation());
                Report r = new Report(3253);
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            } else {
                pod = new INarcPod(attackingEntity.getOwner().getTeam(), INarcPod.HOMING,
                      hit.getLocation());
                Report r = new Report(3254);
                r.subject = subjectId;
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.addElement(r);
            }
            entityTarget.attachINarcPod(pod);
        }
    }

    private boolean narcCanAttachTo(Entity entity, int location) {
        return (entity.getInternal(location) > 0)
              && !entity.isLocationBlownOff(location)
              && !entity.isLocationBlownOffThisPhase(location);
    }
}
