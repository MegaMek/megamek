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
import megamek.server.Server;

import java.util.Vector;

/**
 * @author arlith
 */
public class MekMortarAirburstHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = -2073773899108954657L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public MekMortarAirburstHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        Coords targetPos = target.getPosition();

        Mounted ammoUsed = ae.getEquipment(waa.getAmmoId());
        final AmmoType atype = ammoUsed == null ? null : (AmmoType) ammoUsed.getType();
        
        if ((atype == null) 
                || (atype.getMunitionType() != AmmoType.M_AIRBURST)) {
            System.err
                    .println("MekMortarFlareHandler: not using airburst ammo!");
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
            r.add(toHit);
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
        
        if (bMissed) {
            // misses
            r = new Report(3196);                    
            r.subject = subjectId;
            r.add(targetPos.getBoardNum());
            vPhaseReport.addElement(r);     
            return false;
        }
        
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
        // Damage building directly
        Building bldg = game.getBoard().getBuildingAt(targetPos);
        if (bldg != null) {
            newReports = server.damageBuilding(bldg, numRounds, " receives ", targetPos);
            adjustReports(newReports);
            vPhaseReport.addAll(newReports);
        }
        
        // Damage Terrain if applicable
        Hex h = game.getBoard().getHex(targetPos);
        newReports = new Vector<>();
        if ((h != null) && h.hasTerrainFactor()) {
            r = new Report(3384);
            r.indent(2);
            r.subject = subjectId;
            r.add(targetPos.getBoardNum());
            r.add(numRounds);
            newReports.add(r);
        }
        
        // Update hex and report any changes
        newReports.addAll(server.tryClearHex(targetPos, numRounds, subjectId));
        adjustReports(newReports);
        vPhaseReport.addAll(newReports);
        
        for (Entity target : game.getEntitiesVector(targetPos)) {
            // Ignore airborne units
            if (target.isAirborne() || target.isAirborneVTOLorWIGE()) {
                continue;
            }
            
            // Units in a building apply damage to building
            if (Compute.isInBuilding(game, target, targetPos)) {
                Player tOwner = target.getOwner();
                String colorcode = tOwner.getColour().getHexString(0x00F0F0F0);
                newReports = server.damageBuilding(bldg, numRounds, " shields "
                        + target.getShortName() + " (<B><font color='"
                        + colorcode + "'>" + tOwner.getName()
                        + "</font></B>)"
                        + " from the airburst mortar, receiving ", targetPos);
                adjustReports(newReports);
                vPhaseReport.addAll(newReports);
            } else {
                // Conventional Inf take burst-fire damage
                if (target.isConventionalInfantry()) {
                    // Infantry take a bit more damage
                    int damage = 0;
                    // Roll 1d6 for each shell
                    for (int i = 0; i < numRounds; i++) {
                        damage += (int) Math.ceil(Compute.d6() / 2.0);
                    }
                    hit = target.rollHitLocation(toHit.getHitTable(),
                            toHit.getSideTable(), waa.getAimedLocation(),
                            waa.getAimingMode(), toHit.getCover());
                    hit.setGeneralDamageType(generalDamageType);
                    hit.setCapital(wtype.isCapital());
                    hit.setBoxCars(roll == 12);
                    hit.setCapMisCritMod(getCapMisMod());
                    hit.setFirstHit(firstHit);
                    hit.setAttackerId(getAttackerId());
                    hit.setBurstFire(true);
                    newReports = server.damageEntity(target, hit, damage);
                    adjustReports(newReports);
                    vPhaseReport.addAll(newReports);
                    continue;
                // Battlarmor take damage to each trooper
                } else if (target instanceof BattleArmor) {
                    newReports = new Vector<>();
                    for (int loc = 0; loc < target.locations(); loc++) {
                        if (target.getInternal(loc) > 0) {
                            HitData hit = new HitData(loc);
                            newReports.addAll(server.damageEntity(target, hit, numRounds));
                        }
                    }
                    adjustReports(newReports);
                    vPhaseReport.addAll(newReports);
                    continue;
                }
                // Each round deals 1 damage
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
                    newReports = server.damageEntity(target, hit, 1);
                    adjustReports(newReports);
                    vPhaseReport.addAll(newReports);
                }
            }
        }
        
        return false;
    }
    
    /**
     * Indents all reports in the collection, and adds a new line to the last
     * one.  This is used to make nested reports line-up and look nicer.
     * 
     * @param reports
     */
    private void adjustReports(Vector<Report> reports) {
        for (Report nr : reports) {
            nr.indent();
        }

        if (!reports.isEmpty()) {
            reports.get(reports.size() - 1).newlines++;
        }
    }

}
