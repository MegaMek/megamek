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

import java.util.Vector;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.gameManager.*;

/**
 * @author Sebastian Brocks
 */
public class NarcHandler extends MissileWeaponHandler {
    private static final long serialVersionUID = 3195613885543781820L;

    public NarcHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        bSalvo = true;
        getAMSHitsMod(vPhaseReport);
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
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
        if (amsEngaged || apdsEngaged || amsBayEngagedMissile || pdBayEngagedMissile) {
            Report r = new Report(3235);
            r.subject = subjectId;
            vPhaseReport.add(r);
            r = new Report(3230);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.add(r);
            Roll diceRoll = Compute.rollD6(1);

            if (diceRoll.getIntValue() <= 3) {
                r = new Report(3240);
                r.subject = subjectId;
                r.add("pod");
                r.add(diceRoll);
                vPhaseReport.add(r);
                return 0;
            }
            r = new Report(3241);
            r.add("pod");
            r.add(diceRoll);
            r.subject = subjectId;
            vPhaseReport.add(r);
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
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calcnCluster() {
        return 1;
    }

    @Override
    /**
     * Narcs apply "damage" all in one block for AMS purposes
     * This was referenced incorrectly for Aero damage.
     */
    protected boolean usesClusterTable() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common
     * .Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                toHit.getSideTable(), waa.getAimedLocation(),
                waa.getAimingMode(), toHit.getCover());

        // If our narc pod "hits" an already-missing head, reroll until we hit
        // somewhere else as per the errata for torso-mounted cockpits.
        if (entityTarget instanceof Mech
            && !narcCanAttachTo(entityTarget, Mech.LOC_HEAD)) {
            while (hit.getLocation() == Mech.LOC_HEAD) {
                hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                toHit.getSideTable(), waa.getAimedLocation(),
                waa.getAimingMode(), toHit.getCover());
            }
        }
        hit.setAttackerId(getAttackerId());

        // Catch Protomech near-misses here.
        // So what do we do for a near miss on a glider? Assume attach to wings.
        if (entityTarget instanceof Protomech
                && hit.getLocation() == Protomech.LOC_NMISS
                && !((Protomech) entityTarget).isGlider()) {
            Report r = new Report(6035);
            r.subject = entityTarget.getId();
            vPhaseReport.add(r);
            return;
        }

        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                .getCover(), Compute.targetSideTable(ae, entityTarget, weapon
                .getCalledShot().getCall()))) {
            // Weapon strikes Partial Cover.
            handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                    nCluster, bldgAbsorbs);
            return;
        }

        // If the pod would attach to a destroyed location, have it transfer
        // inwards.
        if (entityTarget instanceof Mech) {
            while (!narcCanAttachTo(entityTarget, hit.getLocation())
                && (hit.getLocation() != Mech.LOC_CT)) {
                hit = entityTarget.getTransferLocation(hit);
            }
        }

        // Now the same check for ProtoMechs. We've already covered near-misses
        // above, so here we only have to worry about the actual hits left over.
        if (entityTarget instanceof Protomech) {
            while (!narcCanAttachTo(entityTarget, hit.getLocation())
                && (hit.getLocation() != Protomech.LOC_TORSO)) {
                hit = entityTarget.getTransferLocation(hit);
            }
        }

        AmmoType atype = (AmmoType) ammo.getType();
        if (atype.getAmmoType() == AmmoType.T_NARC) {
            // narced
            NarcPod pod = new NarcPod(ae.getOwner().getTeam(),
                    hit.getLocation());
            Report r = new Report(3250);
            r.subject = subjectId;
            r.add(entityTarget.getDisplayName());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
            entityTarget.attachNarcPod(pod);
        } else if (atype.getAmmoType() == AmmoType.T_INARC) {
            // iNarced
            INarcPod pod = null;
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_ECM)) {
                pod = new INarcPod(ae.getOwner().getTeam(), INarcPod.ECM,
                        hit.getLocation());
                Report r = new Report(3251);
                r.subject = subjectId;
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.addElement(r);
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_HAYWIRE)) {
                pod = new INarcPod(ae.getOwner().getTeam(), INarcPod.HAYWIRE,
                        hit.getLocation());
                Report r = new Report(3252);
                r.subject = subjectId;
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.addElement(r);
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_NEMESIS)) {
                pod = new INarcPod(ae.getOwner().getTeam(), INarcPod.NEMESIS,
                        hit.getLocation());
                Report r = new Report(3253);
                r.add(entityTarget.getDisplayName());
                r.add(entityTarget.getLocationAbbr(hit));
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            } else {
                pod = new INarcPod(ae.getOwner().getTeam(), INarcPod.HOMING,
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
