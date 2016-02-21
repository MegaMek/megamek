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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.util.PlayerColors;
import megamek.common.AmmoType;
import megamek.common.Building;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.IPlayer;
import megamek.common.Minefield;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.server.Compute;
import megamek.common.server.Report;
import megamek.server.Server;

/**
 * @author arlith
 */
public class MissileMineClearanceHandler extends AmmoWeaponHandler {


    /**
     * 
     */
    private static final long serialVersionUID = 2753652169368638804L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public MissileMineClearanceHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
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
                || (atype.getMunitionType() != AmmoType.M_MINE_CLEARANCE)) {
            System.err.println("MissileMineClearance: "
                    + "not using mine clearance ammo!");
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
        List<Minefield> mfRemoved = new ArrayList<Minefield>();
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
            server.removeMinefield(mf);
        }
        if (updateMinefields) {
            server.sendChangedMines(targetPos);
        }

        // Report amount of damage
        int damage = (wtype.getRackSize() * missileDamage) / 10;
        r = new Report(9940);
        r.subject = subjectId;
        r.indent(2);
        r.newlines++;
        r.add(wtype.getName() + " " + atype.getSubMunitionName());
        r.add(damage);
        vPhaseReport.addElement(r);

        Vector<Report> newReports;
        
        // Damage building directly
        Building bldg = game.getBoard().getBuildingAt(targetPos);
        if (bldg != null) {
            newReports = server.damageBuilding(bldg, damage, " receives ",
                    targetPos);
            adjustReports(newReports);
            vPhaseReport.addAll(newReports);
        }

        // Damage Terrain if applicable
        IHex h = game.getBoard().getHex(targetPos);
        newReports = new Vector<Report>();
        if ((h != null) && h.hasTerrainfactor()) {
            r = new Report(3384);
            r.indent(2);
            r.subject = subjectId;
            r.add(targetPos.getBoardNum());
            r.add(damage);
            newReports.add(r);
        }

        // Update hex and report any changes
        newReports.addAll(server.tryClearHex(targetPos, damage, subjectId));
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
                IPlayer tOwner = target.getOwner();
                String colorcode = Integer.toHexString(PlayerColors.getColor(
                        tOwner.getColorIndex()).getRGB() & 0x00f0f0f0);
                newReports = server.damageBuilding(bldg, damage, " shields "
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
                    newReports = server.damageEntity(target, hit, damage);
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
        if (reports.size() > 0) {
            reports.get(reports.size() - 1).newlines++;
        }
    }
}
