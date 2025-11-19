/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.weapons.lrms.LRMWeapon;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author arlith
 */
public class MissileMineClearanceHandler extends AmmoWeaponHandler {
    private static final MMLogger logger = MMLogger.create(MissileMineClearanceHandler.class);

    @Serial
    private static final long serialVersionUID = 2753652169368638804L;

    public MissileMineClearanceHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        final Coords targetPos = target.getPosition();

        Mounted<?> ammoUsed = attackingEntity.getEquipment(weaponAttackAction.getAmmoId());
        final AmmoType ammoType = (ammoUsed == null) ? null : (AmmoType) ammoUsed.getType();
        if ((ammoType == null) || (!ammoType.getMunitionType().contains(AmmoType.Munitions.M_MINE_CLEARANCE))) {
            logger.error("Not using mine clearance ammo!");
            return true;
        }

        // Report weapon attack and its to-hit value.
        Report r = new Report(3120);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        if (weaponType != null) {
            r.add(weaponType.getName() + ' ' + ammoType.getSubMunitionName());
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
        bMissed = roll.getIntValue() < toHit.getValue();
        // Set Margin of Success/Failure.
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));

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
        int missileDamage = (weaponType instanceof LRMWeapon) ? 1 : 2;
        int mineDamage = weaponType.getRackSize() * missileDamage;
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
        int damage = (weaponType.getRackSize() * missileDamage) / 10;
        r = new Report(9940);
        r.subject = subjectId;
        r.indent(2);
        r.newlines++;
        r.add(weaponType.getName() + " " + ammoType.getSubMunitionName());
        r.add(damage);
        vPhaseReport.addElement(r);

        Vector<Report> newReports;

        // Damage building directly
        IBuilding bldg = game.getBuildingAt(targetPos, target.getBoardId()).orElse(null);
        if (bldg != null) {
            newReports = gameManager.damageBuilding(bldg, damage, " receives ", targetPos);
            adjustReports(newReports);
            vPhaseReport.addAll(newReports);
        }

        // Damage Terrain if applicable
        Hex h = game.getHexOf(target);
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
        newReports.addAll(gameManager.tryClearHex(targetPos, target.getBoardId(), damage, subjectId));
        adjustReports(newReports);
        vPhaseReport.addAll(newReports);

        for (Entity target : game.getEntitiesVector(targetPos, target.getBoardId())) {
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
                String colorCode = tOwner.getColour().getHexString(0x00F0F0F0);
                newReports = gameManager.damageBuilding(bldg, damage, " shields "
                            + target.getShortName() + " (<B><font color='"
                            + colorCode + "'>" + tOwner.getName() + "</font></B>)"
                            + " from the mine clearance munitions, receiving ",
                      targetPos);
                adjustReports(newReports);
                vPhaseReport.addAll(newReports);
            } else {
                hit = target.rollHitLocation(toHit.getHitTable(),
                      toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
                      weaponAttackAction.getAimingMode(), toHit.getCover());
                if (target.getBARRating(hit.getLocation()) <= 6) {
                    hit.setGeneralDamageType(generalDamageType);
                    hit.setCapital(weaponType.isCapital());
                    hit.setBoxCars(roll.getIntValue() == 12);
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
     * Indents all reports in the collection, and adds a new line to the last one. This is used to make nested reports
     * line-up and look nicer.
     */
    private void adjustReports(Vector<Report> reports) {
        for (Report report : reports) {
            report.indent();
        }

        if (!reports.isEmpty()) {
            reports.get(reports.size() - 1).newlines++;
        }
    }
}
