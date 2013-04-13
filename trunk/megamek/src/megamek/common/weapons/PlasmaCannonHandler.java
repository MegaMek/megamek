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

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
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

public class PlasmaCannonHandler extends AmmoWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = 2304364403526293671L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public PlasmaCannonHandler(ToHitData toHit, WeaponAttackAction waa, IGame g, Server s) {
        super(toHit, waa, g, s);
        generalDamageType = HitData.DAMAGE_ENERGY;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common.Entity,
     *      java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster, int bldgAbsorbs) {

        if ((entityTarget instanceof Mech) || (entityTarget instanceof Aero)) {
            HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit.getSideTable(), waa.getAimedLocation(), waa.getAimingMode(), toHit.getCover());
            hit.setGeneralDamageType(generalDamageType);
            if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit.getCover(), Compute.targetSideTable(ae, entityTarget,
                    weapon.getCalledShot().getCall()))) {
                // Weapon strikes Partial Cover.
                Report r = new Report(3460);
                r.subject = subjectId;
                r.add(entityTarget.getShortName());
                r.add(entityTarget.getLocationAbbr(hit));
                r.indent(2);
                vPhaseReport.addElement(r);
                missed = true;
                return;
            }
            if (!bSalvo) {
                // Each hit in the salvo get's its own hit location.
                Report r = new Report(3405);
                r.subject = subjectId;
                r.add(toHit.getTableDesc());
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.addElement(r);
            }
            Report r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            int extraHeat = Compute.d6(2);
            r.add(extraHeat);
            r.choose(true);
            vPhaseReport.addElement(r);
            entityTarget.heatFromExternal += extraHeat;
        } else {
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if ((target instanceof Mech) || (target instanceof Aero)) {
            return 0;
        }
        int toReturn = 1;
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            toReturn = Compute.d6(3);
            // pain shunted infantry get half damage
            if (bDirect) {
                toReturn += toHit.getMoS()/3;
            }
            if (((Entity) target).getCrew().getOptions().booleanOption("pain_shunt")) {
                toReturn = Math.max(toReturn / 2, 1);
            }
        } else if (bDirect){
            toReturn = Math.min(toReturn+(toHit.getMoS()/3), toReturn*2);
        }
        if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }
        return toReturn;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calcnCluster() {
        if ((target instanceof Mech) || (target instanceof Aero)) {
            bSalvo = false;
            return 1;
        }
        bSalvo = true;
        return 5;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs can't mount Plasma Cannons
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            return 1;
        }
        if ((target instanceof Mech) || (target instanceof Aero)) {
            return 1;
        }
        if ((target instanceof BattleArmor) && ((BattleArmor) target).isFireResistant()) {
            return 0;
        }
        return Compute.d6(3);
    }


    /**
     * @return a <code>boolean</code> value indicating wether or not this
     *         attack needs further calculating, like a missed shot hitting a
     *         building, or an AMS only shooting down some missiles.
     */
    @Override
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean targetInBuilding, Building bldg, Vector<Report> vPhaseReport) {
        // Shots that miss an entity can set fires.
        // Buildings can't be accidentally ignited,
        // and some weapons can't ignite fires.
        if ((entityTarget != null)
                && ((bldg == null) && (wtype.getFireTN() != TargetRoll.IMPOSSIBLE))) {
            server.tryIgniteHex(target.getPosition(), subjectId, true, false, new TargetRoll(wtype.getFireTN(), wtype.getName()),
                    3, vPhaseReport);
        }

        //shots that miss an entity can also potential cause explosions in a heavy industrial hex
        server.checkExplodeIndustrialZone(target.getPosition(), vPhaseReport);

        // BMRr, pg. 51: "All shots that were aimed at a target inside
        // a building and miss do full damage to the building instead."
        if (!targetInBuilding || (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
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
            server.tryIgniteHex(target.getPosition(), subjectId, true, false, tn,
                    true, -1, vPhaseReport);
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

        nDamage *= 2; // Plasma weapons deal double damage to woods.

        // report that damage was "applied" to terrain
        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        //TODO: change this for TacOps - now you roll another 2d6 first and on a 5 or less
        //you do a normal ignition as though for intentional fires
        if ((bldg != null)
                && server.tryIgniteHex(target.getPosition(), subjectId, true,false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5, vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = server.tryClearHex(target.getPosition(), nDamage, subjectId);
        if (clearReports.size() > 0) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
        return;
    }

    @Override
    protected void handleBuildingDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage, Coords coords) {
        // Plasma weapons deal double damage to buildings.
        super.handleBuildingDamage(vPhaseReport, bldg, nDamage * 2, coords);
    }

}
