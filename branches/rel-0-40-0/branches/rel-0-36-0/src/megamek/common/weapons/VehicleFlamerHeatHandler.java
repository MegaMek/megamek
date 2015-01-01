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
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class VehicleFlamerHeatHandler extends AmmoWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = -4478740737686392126L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public VehicleFlamerHeatHandler(ToHitData toHit, WeaponAttackAction waa,
            IGame g, Server s) {
        super(toHit, waa, g, s);
        generalDamageType = HitData.DAMAGE_ENERGY;
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
        if ((entityTarget instanceof Mech)
                && game.getOptions().booleanOption("flamer_heat")) {

            HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                    toHit.getSideTable(), waa.getAimedLocation(),
                    waa.getAimingMode(), toHit.getCover());

            if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                    .getCover(), Compute.targetSideTable(ae, entityTarget,
                    weapon.getCalledShot().getCall()))) {
                // Weapon strikes Partial Cover.
                handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg,
                        hits, nCluster, bldgAbsorbs);
                return;
            }

            // heatDamage
            int heatDamage = wtype.getDamage();
            Report r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            r.add(heatDamage);
            r.newlines = 0;
            r.choose(true);
            vPhaseReport.addElement(r);
            entityTarget.heatFromExternal += heatDamage;
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
        double toReturn;
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            if (ae instanceof BattleArmor) {
                toReturn = Compute.d6(3);
            } else if ((wtype instanceof ISHeavyFlamer)
                    || (wtype instanceof CLHeavyFlamer)) {
                toReturn = Compute.d6(6);
            } else {
                toReturn = Compute.d6(4);
            }
            if (bDirect) {
                toReturn += toHit.getMoS() / 3;
            }
            // pain shunted infantry get half damage
            if (((Entity) target).getCrew().getOptions()
                    .booleanOption("pain_shunt")) {
                toReturn /= 2;
            }
            if (bGlancing) {
                toReturn = Math.ceil(toReturn / 2.0);
            }
        } else {
            toReturn = super.calcDamagePerHit();
        }
        return (int) toReturn;
    }

    /**
     * @return a <code>boolean</code> value indicating wether or not this attack
     *         needs further calculating, like a missed shot hitting a building,
     *         or an AMS only shooting down some missiles.
     */
    @Override
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean targetInBuilding, Building bldg, Vector<Report> vPhaseReport) {
        // Shots that miss an entity can set fires.
        // Buildings can't be accidentally ignited,
        // and some weapons can't ignite fires.
        if ((entityTarget != null)
                && ((bldg == null) && (wtype.getFireTN() != TargetRoll.IMPOSSIBLE))) {
            server.tryIgniteHex(target.getPosition(), subjectId, true, false,
                    new TargetRoll(wtype.getFireTN(), wtype.getName()), 3,
                    vPhaseReport);
        }

        // shots that miss an entity can also potential cause explosions in a
        // heavy industrial hex
        server.checkExplodeIndustrialZone(target.getPosition(), vPhaseReport);

        // BMRr, pg. 51: "All shots that were aimed at a target inside
        // a building and miss do full damage to the building instead."
        if (!targetInBuilding
                || (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
            return false;
        }
        return true;
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
