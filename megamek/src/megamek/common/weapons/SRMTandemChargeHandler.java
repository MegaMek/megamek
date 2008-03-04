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

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Jason Tighe
 */
public class SRMTandemChargeHandler extends SRMHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 6292692766500970690L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public SRMTandemChargeHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        sSalvoType = " tandem charge missile(s) ";
    }

    /**
     * Handle damage against an entity, called once per hit by default.
     * 
     * @param entityTarget
     * @param vPhaseReport
     * @param bldg
     * @param hits
     * @param nCluster
     * @param nDamPerHit
     * @param bldgAbsorbs
     */
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int nDamPerHit, int bldgAbsorbs) {
        int nDamage;
        missed = false;

        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit
                .getSideTable(), waa.getAimedLocation(), waa.getAimingMode());

        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                .getCover(), Compute.targetSideTable(ae, entityTarget))) {
            // Weapon strikes Partial Cover.
            r = new Report(3460);
            r.subject = subjectId;
            r.add(entityTarget.getShortName());
            r.add(entityTarget.getLocationAbbr(hit));
            r.newlines = 0;
            r.indent(2);
            vPhaseReport.addElement(r);
            nDamage = 0;
            missed = true;
            return;
        }

        if (!bSalvo) {
            // Each hit in the salvo get's its own hit location.
            r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);

        // A building may be damaged, even if the squad is not.
        if (bldgAbsorbs > 0) {
            int toBldg = Math.min(bldgAbsorbs, nDamage);
            nDamage -= toBldg;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = server
                    .damageBuilding(bldg, nDamage);
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }

        // A building may absorb the entire shot.
        if (nDamage == 0) {
            r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            r.newlines = 0;
            vPhaseReport.addElement(r);
            missed = true;
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }

            if (entityTarget.hasActiveShield(hit.getLocation(), hit.isRear())
                    || entityTarget.hasPassiveShield(hit.getLocation(), hit
                            .isRear())
                    || entityTarget.hasNoDefenseShield(hit.getLocation())) {
                vPhaseReport.addAll(server.damageEntity(entityTarget, hit,
                        nDamage, false, ae.getSwarmTargetId() == entityTarget
                                .getId() ? DamageType.IGNORE_PASSENGER
                                : DamageType.NONE, false, false, throughFront));
                if (hit.getLocation() == Mech.LOC_RARM
                        || hit.getLocation() == Mech.LOC_RLEG
                        || hit.getLocation() == Mech.LOC_RT) {
                    hit = new HitData(Mech.LOC_RARM);
                } else if (hit.getLocation() == Mech.LOC_LARM
                        || hit.getLocation() == Mech.LOC_LLEG
                        || hit.getLocation() == Mech.LOC_LT) {
                    hit = new HitData(Mech.LOC_LARM);
                } else if (entityTarget.hasActiveShield(Mech.LOC_LARM)
                        || entityTarget.hasPassiveShield(Mech.LOC_LARM)
                        || entityTarget.hasNoDefenseShield(Mech.LOC_LARM)) {
                    hit = new HitData(Mech.LOC_LARM);
                } else {
                    hit = new HitData(Mech.LOC_RARM);
                }
                hit.setEffect(HitData.EFFECT_NO_CRITICALS);
                vPhaseReport.addAll(server.damageEntity(entityTarget, hit,
                        nDamage, false, ae.getSwarmTargetId() == entityTarget
                                .getId() ? DamageType.IGNORE_PASSENGER
                                : damageType, false, false, throughFront));
            } else if (entityTarget.getArmor(hit.getLocation(), hit.isRear()) > 0) {
                vPhaseReport.addAll(server.damageEntity(entityTarget, hit,
                        nDamage, false, ae.getSwarmTargetId() == entityTarget
                                .getId() ? DamageType.IGNORE_PASSENGER
                                : damageType, false, false, throughFront));
                hit.setEffect(HitData.EFFECT_NO_CRITICALS);
                Report.addNewline(vPhaseReport);
                vPhaseReport.addAll(server.damageEntity(entityTarget, hit,
                        nDamage, false, ae.getSwarmTargetId() == entityTarget
                                .getId() ? DamageType.IGNORE_PASSENGER
                                : damageType, true, false, throughFront));
            } else {
                vPhaseReport.addAll(server.damageEntity(entityTarget, hit,
                        nDamage, false, ae.getSwarmTargetId() == entityTarget
                                .getId() ? DamageType.IGNORE_PASSENGER
                                : DamageType.NONE, true, false, throughFront));
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            int toReturn = (int) Math.ceil(((float) wtype.getRackSize()) / 5);
            if (bGlancing)
                toReturn /= 2;
            return toReturn;
        }
        return 1;
    }

}
