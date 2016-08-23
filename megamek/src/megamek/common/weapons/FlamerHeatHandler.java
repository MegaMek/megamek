/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class FlamerHeatHandler extends WeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = -7785915075211288648L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public FlamerHeatHandler(ToHitData toHit, WeaponAttackAction waa, IGame g,
            Server s) {
        super(toHit, waa, g, s);
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        if ((entityTarget instanceof Mech)
                && game.getOptions().booleanOption(OptionsConstants.BASE_FLAMER_HEAT)) {
            // heat
            hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                    toHit.getSideTable(), waa.getAimedLocation(),
                    waa.getAimingMode(), toHit.getCover());
            hit.setAttackerId(getAttackerId());

            if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                    .getCover(), Compute.targetSideTable(ae, entityTarget,
                    weapon.getCalledShot().getCall()))) {
                // Weapon strikes Partial Cover.
                handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg,
                        hits, nCluster, bldgAbsorbs);
                return;
            }
            Report r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
            
            r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            if (entityTarget.getArmor(hit) > 0 && 
                    ((entityTarget.getArmorType(hit.getLocation()) == 
                        EquipmentType.T_ARMOR_HEAT_DISSIPATING) ||
                     (entityTarget.getArmorType(hit.getLocation()) == 
                        EquipmentType.T_ARMOR_REFLECTIVE))){
                entityTarget.heatFromExternal += 1;
                r.add(1);
                r.choose(true);
                r.messageId=3406;
                r.add(2);
                r.add(EquipmentType.armorNames
                        [entityTarget.getArmorType(hit.getLocation())]);
            } else {
                entityTarget.heatFromExternal += 2;
                r.add(2);
                r.choose(true);
            }                        
            vPhaseReport.addElement(r);
        } else {
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                    nCluster, bldgAbsorbs);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        int toReturn = super.calcDamagePerHit();
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            // pain shunted infantry get half damage
            if (((Entity) target).getCrew().getOptions()
                    .booleanOption(OptionsConstants.MD_PAIN_SHUNT)) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
        } else if ((target instanceof BattleArmor)
                && ((BattleArmor) target).isFireResistant()) {
            toReturn = 0;
        }
        return toReturn;
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport,
            Building bldg, int hits) {
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
            server.tryIgniteHex(target.getPosition(), subjectId, true, false,
                    tn, true, -1, vPhaseReport);
        }
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage) {
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
                && server.tryIgniteHex(target.getPosition(), subjectId, true,
                        false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5,
                        vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = server.tryClearHex(target.getPosition(),
                nDamage, subjectId);
        if (clearReports.size() > 0) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
        return;
    }
}