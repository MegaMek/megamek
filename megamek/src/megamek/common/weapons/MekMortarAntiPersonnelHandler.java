/**
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author arlith
 */
public class MekMortarAntiPersonnelHandler extends AmmoWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = -2073773899108954657L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public MekMortarAntiPersonnelHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        Coords targetPos = target.getPosition();

        Mounted ammoUsed = ae.getEquipment(waa.getAmmoId());
        final AmmoType atype = ammoUsed == null ? null : (AmmoType) ammoUsed
                .getType();
        
        if ((atype == null) 
                || (atype.getMunitionType() != AmmoType.M_ANTI_PERSONNEL)) {
            System.err.println("MekMortarFlareHandler: "
                    + "not using anti-personnel ammo!");
            return true;
        }


        // Report weapon attack and its to-hit value.
        Report r = new Report(3120);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        if (wtype != null) {
            r.add(wtype.getName() + " " + atype.getSubMunitionName());
        } else {
            r.add("Error: From Nowhwere");
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
        // Set Margin of Success/Failure.
        toHit.setMoS(roll - Math.max(2, toHit.getValue()));
        
        // Report hit
        r = new Report(3190);
        r.subject = subjectId;
        r.add(targetPos.getBoardNum());
        vPhaseReport.addElement(r);
        
        // Report amount of damage
        r = new Report(9940);
        r.subject = subjectId;
        r.indent(2);
        r.newlines++;
        r.add(wtype.getName() + " " + atype.getSubMunitionName());
        r.add(wtype.getRackSize());
        vPhaseReport.addElement(r);
        
        Vector<Report> newReports;
        int numRounds = wtype.getRackSize();
        for (Entity target : game.getEntitiesVector(targetPos)) {
             for (int i = 0; i < numRounds; i++) {
                hit = target.rollHitLocation(toHit.getHitTable(),
                        toHit.getSideTable(), waa.getAimedLocation(),
                        waa.getAimingMode(), toHit.getCover());
                hit.setGeneralDamageType(generalDamageType);
                hit.setCapital(wtype.isCapital());
                hit.setBoxCars(roll == 12);
                hit.setCapMisCritMod(getCapMisMod());
                hit.setFirstHit(firstHit);
                hit.setAttackerId(getAttackerId());
                if (target instanceof Infantry) {
                    int damage = (int) Math.ceil(Compute.d6());
                    newReports = server.damageEntity(target, hit, damage);
                } else {
                    newReports = server.damageEntity(target, hit, 1);
                }
                for (Report nr : newReports) {
                    nr.indent();
                }
                if (newReports.size() > 0) {
                    newReports.get(newReports.size() - 1).newlines++;
                }
                vPhaseReport.addAll(newReports);
            }
        }
        
        return false;
    }

}
