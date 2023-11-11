/*
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
import megamek.common.enums.GamePhase;
import megamek.server.GameManager;
import org.apache.logging.log4j.LogManager;

import java.util.Vector;

/**
 * @author arlith
 */
public class MekMortarSmokeHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = -2073773899108954657L;

    public MekMortarSmokeHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    @Override
    protected int calcDamagePerHit() {
        return 0;
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        Coords targetPos = target.getPosition();

        Mounted ammoUsed = ae.getEquipment(waa.getAmmoId());
        final AmmoType ammoType = (ammoUsed == null) ? null : (AmmoType) ammoUsed.getType();

        if ((ammoType == null) || (!ammoType.getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD))) {
            LogManager.getLogger().error("Not using smoke ammo!");
            return true;
        }

        // Report weapon attack and its to-hit value.
        Report r = new Report(3120);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        if (wtype != null) {
            r.add(wtype.getName() + ' ' + ammoType.getSubMunitionName());
        } else {
            r.add("Error: From Nowhere");
        }

        r.add(target.getDisplayName(), true);
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
            r.add(toHit);
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.addDataWithTooltip(roll, rollReport);
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll < toHit.getValue();
        // Set Margin of Success/Failure.
        toHit.setMoS(roll - Math.max(2, toHit.getValue()));
        int duration = wtype.getRackSize() * 2;

        if (!bMissed) {
            r = new Report(3190);
            r.subject = subjectId;
            r.add(targetPos.getBoardNum());
            vPhaseReport.addElement(r);
        } else {
            // only scatters by one d6
            targetPos = Compute.scatter(targetPos, 1);
            if (game.getBoard().contains(targetPos)) {
                // misses and scatters to another hex
                r = new Report(3195);
                r.subject = subjectId;
                r.add(targetPos.getBoardNum());
                vPhaseReport.addElement(r);
            } else {
                // misses and scatters off-board
                r = new Report(3200);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                return !bMissed;
            }
        }

        gameManager.deliverSmokeMortar(targetPos, vPhaseReport, duration);
        return false;
    }

}
