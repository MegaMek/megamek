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
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

public class BATaserHandler extends AmmoWeaponHandler {

    private static final long serialVersionUID = 1308895663099714573L;

    protected BATaserHandler() {
        //deserialization only
    }

    /**
     * @param t
     * @param w
     * @param g
     */
    public BATaserHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        generalDamageType = HitData.DAMAGE_ENERGY;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#specialResolution(java.util.Vector, megamek.common.Entity, boolean)
     */
    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget) {
        boolean done = false;
        if (bMissed) {
            return done;
        }
        Report r = new Report(3700);
        int taserRoll = Compute.d6(2);
        r.add(taserRoll);
        r.newlines = 0;
        vPhaseReport.add(r);
        if (entityTarget instanceof BattleArmor) {
            if (taserRoll >= 9) {
                r = new Report(3706);
                r.addDesc(entityTarget);
                // shut down for rest of scenario, so we actually kill it
                // TODO: fix for salvage purposes
                HitData targetTrooper = entityTarget.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                r.add(entityTarget.getLocationAbbr(targetTrooper));
                vPhaseReport.add(r);
                vPhaseReport.addAll(server.criticalEntity(ae, targetTrooper.getLocation(), 0, false, false));
                done = true;
            }
        } else if (entityTarget instanceof Mech) {
            if (((Mech)entityTarget).isIndustrial()) {
                if (taserRoll >= 11) {
                    entityTarget.baTaserShutdown(3);
                } else {
                    // suffer +1 to piloting and gunnery for 3 rounds
                    entityTarget.setTaserInterference(1, 3);
                }
            } else {
                if (taserRoll >= 12) {
                    r = new Report(3705);
                    r.addDesc(entityTarget);
                    r.add(3);
                    vPhaseReport.add(r);
                    entityTarget.baTaserShutdown(3);
                } else {
                    r = new Report(3710);
                    r.addDesc(entityTarget);
                    r.add(1);
                    r.add(3);
                    vPhaseReport.add(r);
                    entityTarget.setTaserInterference(1, 3);
                }
            }
        } else if ((entityTarget instanceof Protomech)
                || (entityTarget instanceof Tank)
                || (entityTarget instanceof Aero)) {
            if (taserRoll >= 11) {
                r = new Report(3705);
                r.addDesc(entityTarget);
                r.add(3);
                vPhaseReport.add(r);
                entityTarget.baTaserShutdown(3);
            } else {
                r = new Report(3710);
                r.addDesc(entityTarget);
                r.add(1);
                r.add(3);
                vPhaseReport.add(r);
                entityTarget.setTaserInterference(1, 3);
            }
        }
        taserRoll = Compute.d6(2);
        r = new Report(3715);
        r.addDesc(ae);
        r.add(taserRoll);
        r.newlines = 0;
        r.indent(2);
        vPhaseReport.add(r);
        if (taserRoll >= 7) {
            r = new Report(3720);
            vPhaseReport.add(r);
            // +1 to-hit for 3 turns
            ae.setTaserFeedback(3);
        } else {
            r = new Report(3725);
            vPhaseReport.add(r);
            // kill the firing trooper
            // TODO: should just be shut down for remainder of scenario
            vPhaseReport.addAll(server.criticalEntity(ae, weapon.getLocation(), 0, false, false));
        }
        return done;
    }
}
