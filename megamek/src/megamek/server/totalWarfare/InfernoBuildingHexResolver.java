/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.server.totalWarfare;

import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

/**
 * Resolves inferno missiles fired at a building hex against the units in that hex (TW p. 141 and its errata). Every
 * unit inside the building on the level that was struck rolls 1D6 per missile and is struck on a 5 or 6; units on
 * other levels, and units standing on the roof, are not inside on that level and are left alone. Struck missiles hit
 * every unit type at full effect, except conventional infantry inside the building: the Infantry Damage in Buildings
 * Table (TW p. 172) gives the share of the struck missiles that affect the platoon, rounded normally, so a hardened
 * building shields it completely.
 */
class InfernoBuildingHexResolver extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(InfernoBuildingHexResolver.class);

    private static final int REPORT_MISSILE_ROLL = 3570;
    private static final int REPORT_INFANTRY_BURN_THROUGH = 3571;
    private static final int REPORT_INFANTRY_SHIELDED = 3572;
    private static final int STRUCK_ON = 5;
    /** A building hex target carries no level choice, so the missiles strike the ground level of the building. */
    private static final int LEVEL_STRUCK = 0;

    InfernoBuildingHexResolver(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Rolls the missiles against each unit in the building hex and delivers the ones that strike.
     *
     * @param attacker the unit that fired the missiles, or {@code null} when unknown
     * @param target   the building hex that was hit
     * @param hex      the hex of the target
     * @param missiles the number of missiles that hit the hex
     * @param called   the called-shot mode used for the attack
     *
     * @return the reports of the rolls and the resulting damage
     */
    Vector<Report> strikeUnitsInHex(@Nullable Entity attacker, Targetable target, Hex hex, int missiles, int called) {
        Vector<Report> reports = new Vector<>();
        IBuilding building = getGame().getBoard(target.getBoardId()).getBuildingAt(target.getPosition());
        for (Entity unit : getGame().getEntitiesVector(target.getPosition())) {
            if (unit.getElevation() != LEVEL_STRUCK) {
                LOGGER.debug("[Inferno] {} is on level {} of the building, not the struck level {}; not affected",
                      unit.getShortName(), unit.getElevation(), LEVEL_STRUCK);
                continue;
            }
            int struck = rollMissilesAgainst(unit, missiles, reports);
            boolean isConventionalInfantryInside = unit.isConventionalInfantry()
                  && (building != null)
                  && Compute.isInBuilding(getGame(), unit, target.getPosition());
            if (isConventionalInfantryInside) {
                deliverThroughBuilding(attacker, unit, building, struck, called, reports);
            } else {
                deliverEachMissile(attacker, unit, struck, called, reports);
            }
        }
        return reports;
    }

    private int rollMissilesAgainst(Entity unit, int missiles, Vector<Report> reports) {
        int struck = 0;
        for (int missile = 0; missile < missiles; missile++) {
            Roll diceRoll = Compute.rollD6(1);
            Report report = new Report(REPORT_MISSILE_ROLL);
            report.subject = unit.getId();
            report.indent(3);
            report.addDesc(unit);
            report.add(diceRoll);
            reports.add(report);
            if (diceRoll.getIntValue() >= STRUCK_ON) {
                struck++;
            }
        }
        return struck;
    }

    private void deliverEachMissile(@Nullable Entity attacker, Entity unit, int struck, int called,
          Vector<Report> reports) {
        for (int missile = 0; missile < struck; missile++) {
            deliver(attacker, unit, 1, called, reports);
        }
    }

    private void deliverThroughBuilding(@Nullable Entity attacker, Entity infantry, IBuilding building, int struck,
          int called, Vector<Report> reports) {
        if (struck == 0) {
            return;
        }
        int affecting = Math.round(struck * building.getDamageReductionFromOutside());
        LOGGER.info("[Inferno] {} is inside a {} building: {} of {} struck missiles burn through",
              infantry.getShortName(), building.getBuildingType(), affecting, struck);
        Report report = new Report(affecting > 0 ? REPORT_INFANTRY_BURN_THROUGH : REPORT_INFANTRY_SHIELDED);
        report.subject = infantry.getId();
        report.indent(3);
        report.addDesc(infantry);
        report.add(building.getBuildingType().toString());
        if (affecting > 0) {
            report.add(affecting);
        }
        report.add(struck);
        reports.add(report);
        if (affecting > 0) {
            deliver(attacker, infantry, affecting, called, reports);
        }
    }

    private void deliver(@Nullable Entity attacker, Entity unit, int missiles, int called, Vector<Report> reports) {
        Vector<Report> damageReports = gameManager.deliverInfernoMissiles(attacker, unit, missiles, called);
        for (Report damageReport : damageReports) {
            damageReport.indent(4);
        }
        reports.addAll(damageReports);
    }
}
