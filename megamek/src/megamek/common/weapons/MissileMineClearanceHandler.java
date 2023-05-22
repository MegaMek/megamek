/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
import megamek.common.weapons.lrms.LRMWeapon;
import megamek.server.GameManager;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author arlith
 */
public class MissileMineClearanceHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = 2753652169368638804L;

    public MissileMineClearanceHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        Coords targetPos = target.getPosition();

        Mounted ammoUsed = ae.getEquipment(waa.getAmmoId());
        final AmmoType ammoType = (ammoUsed == null) ? null : (AmmoType) ammoUsed.getType();
        if ((ammoType == null) || (ammoType.getMunitionType() != AmmoType.M_MINE_CLEARANCE)) {
            LogManager.getLogger().error("Not using mine clearance ammo!");
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

        // Handle mine clearance
        List<Minefield> mfRemoved = new ArrayList<>();
        int missileDamage = (wtype instanceof LRMWeapon) ? 1 : 2;
        int mineDamage = wtype.getRackSize() * missileDamage;
        boolean updateMinefields = false;
        for (Minefield mf : game.getMinefields(targetPos)) {
            int density = mf.getDensity() - mineDamage;
            if (density > 0) {
                mf.setDensity(density);
                updateMinefields = true;
                r = new Report(2251);
                r.indent(2);
                r.subject = subjectId;
                r.add(targetPos.toString());
                r.add(mineDamage);
                vPhaseReport.addElement(r);
            } else {
                r = new Report(2252);
                r.indent(2);
                r.subject = subjectId;
                r.add(targetPos.toString());
                vPhaseReport.addElement(r);
                mfRemoved.add(mf);
            }
        }
        for (Minefield mf : mfRemoved) {
            gameManager.removeMinefield(mf);
        }
        if (updateMinefields) {
            gameManager.sendChangedMines(targetPos);
        }

        // Report amount of damage
        int damage = (wtype.getRackSize() * missileDamage) / 10;
        r = new Report(9940);
        r.subject = subjectId;
        r.indent(2);
        r.newlines++;
        r.add(wtype.getName() + " " + ammoType.getSubMunitionName());
        r.add(damage);
        vPhaseReport.addElement(r);

        Vector<Report> newReports;
        
        // Damage building directly
        Building bldg = game.getBoard().getBuildingAt(targetPos);
        if (bldg != null) {
            newReports = gameManager.damageBuilding(bldg, damage, " receives ", targetPos);
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
            r.add(damage);
            newReports.add(r);
        }

        // Update hex and report any changes
        newReports.addAll(gameManager.tryClearHex(targetPos, damage, subjectId));
        adjustReports(newReports);
        vPhaseReport.addAll(newReports);

        for (Entity target : game.getEntitiesVector(targetPos)) {
            // Ignore airborne units
            if (target.isAirborne() || target.isAirborneVTOLorWIGE()) {
                continue;
            }
            
            // Units in a building apply damage to building
            // The rules don't state this, but I'm going to treat mine clearance
            // munitions like airburst mortars for purposes of units in
            // buildings
            if (Compute.isInBuilding(game, target, targetPos)) {
                Player tOwner = target.getOwner();
                String colorcode = tOwner.getColour().getHexString(0x00F0F0F0);
                newReports = gameManager.damageBuilding(bldg, damage, " shields "
                        + target.getShortName() + " (<B><font color='"
                        + colorcode + "'>" + tOwner.getName() + "</font></B>)"
                        + " from the mine clearance munitions, receiving ",
                        targetPos);
                adjustReports(newReports);
                vPhaseReport.addAll(newReports);
            } else {
                hit = target.rollHitLocation(toHit.getHitTable(),
                        toHit.getSideTable(), waa.getAimedLocation(),
                        waa.getAimingMode(), toHit.getCover());
                if (target.getBARRating(hit.getLocation()) <= 6) {
                    hit.setGeneralDamageType(generalDamageType);
                    hit.setCapital(wtype.isCapital());
                    hit.setBoxCars(roll == 12);
                    hit.setCapMisCritMod(getCapMisMod());
                    hit.setFirstHit(firstHit);
                    hit.setAttackerId(getAttackerId());
                    // Technically some unit types would have special handling
                    // for AE damage, like BA, but those units shouldn't have
                    // BAR low enough for this to trigger
                    newReports = gameManager.damageEntity(target, hit, damage);
                    adjustReports(newReports);
                    vPhaseReport.addAll(newReports);
                } else {
                    r = new Report(2253);
                    r.subject = target.getId();
                    r.addDesc(target);
                    r.indent(3);
                    vPhaseReport.add(r);
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
