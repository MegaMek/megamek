/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;

import java.util.Vector;

/**
 * @author Sebastian Brocks
 * Created on Sep 23, 2004
 */
public class FlamerHandler extends WeaponHandler {
    private static final long serialVersionUID = -7348456582587703751L;

    public FlamerHandler(ToHitData toHit, WeaponAttackAction waa, Game g, GameManager m) {
        super(toHit, waa, g, m);
        generalDamageType = HitData.DAMAGE_ENERGY;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
                                      Building bldg, int hits, int nCluster, int bldgAbsorbs) {
        boolean bmmFlamerDamage = game.getOptions().booleanOption(OptionsConstants.BASE_FLAMER_HEAT);
        EquipmentMode currentWeaponMode = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId()).curMode();
        
        boolean flamerDoesHeatOnlyDamage = currentWeaponMode != null && currentWeaponMode.equals(Weapon.MODE_FLAMER_HEAT);
        boolean flamerDoesOnlyDamage = currentWeaponMode != null && currentWeaponMode.equals(Weapon.MODE_FLAMER_DAMAGE);
        
        if (bmmFlamerDamage || flamerDoesOnlyDamage || (flamerDoesHeatOnlyDamage && !entityTarget.tracksHeat())) {
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            
            if (bmmFlamerDamage && entityTarget.tracksHeat()) {
                FlamerHandlerHelper.doHeatDamage(entityTarget, vPhaseReport, wtype, subjectId, hit);
            }
        } else if (flamerDoesHeatOnlyDamage) {
            hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                    toHit.getSideTable(), waa.getAimedLocation(),
                    waa.getAimingMode(), toHit.getCover());
            hit.setAttackerId(getAttackerId());

            if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit.getCover(),
                    Compute.targetSideTable(ae, entityTarget, weapon.getCalledShot().getCall()))) {
                // Weapon strikes Partial Cover.
                handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits, nCluster, bldgAbsorbs);
                return;
            }
            Report r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
            
            FlamerHandlerHelper.doHeatDamage(entityTarget, vPhaseReport, wtype, subjectId, hit);
        }
    }

    @Override
    protected int calcDamagePerHit() {
        int toReturn = super.calcDamagePerHit();
        if (target.isConventionalInfantry()) {
            // pain shunted infantry get half damage
            if (((Entity) target).hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
        } else if ((target instanceof BattleArmor) && ((BattleArmor) target).isFireResistant()) {
            toReturn = 0;
        }
        return toReturn;
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport, Building bldg, int hits) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        TargetRoll tn = new TargetRoll(wtype.getFireTN(), wtype.getName());
        if (tn.getValue() != TargetRoll.IMPOSSIBLE) {
            Report.addNewline(vPhaseReport);
            gameManager.tryIgniteHex(target.getPosition(), subjectId, true, false,
                    tn, true, -1, vPhaseReport);
        }
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport, Building bldg, int nDamage) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        // report that damage was "applied" to terrain
        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        // TODO: change this for TacOps - now you roll another 2d6 first and on
        // a 5 or less
        // you do a normal ignition as though for intentional fires
        if ((bldg != null)
                && gameManager.tryIgniteHex(target.getPosition(), subjectId, true,
                        false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5,
                        vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(), nDamage, subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }
}