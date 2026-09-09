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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.compute.MarinePointsBreakdown;
import megamek.common.compute.MarinePointsScoreCalculator;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;

/**
 * Writes the working of an infantry vs. infantry action into the round report: what each unit is worth in Marine
 * Points and how a side's loss in points became people (TO:AR pp. 170 to 174). Used when an action starts, when a
 * unit joins one, before every roll and after every roll, so a player can check the ratio and the casualties from
 * the lines above them.
 */
class InfantryActionReporter extends AbstractTWRuleHandler {

    /** The sub-header before the attackers' unit lines. */
    static final int ATTACKERS_HEADER = 5657;
    /** The sub-header before the defenders' unit lines. */
    static final int DEFENDERS_HEADER = 5658;
    /** A conventional infantry unit's score: troopers at a value each. */
    static final int CONVENTIONAL_INFANTRY_SCORE = 5651;
    /** A battle armor squad's score: troopers at a base plus modifiers each, plus armor. */
    static final int BATTLE_ARMOR_SCORE = 5652;
    /** A crewed unit's or building's score: marines, crew, bay personnel and civilians. */
    static final int CREW_SCORE = 5653;
    /** The building modifier applied to a defender's score. */
    static final int BUILDING_MODIFIER = 5654;
    /** A unit's share of its side's loss, converted to troopers. */
    static final int TROOPER_LOSS = 5655;
    /** A crewed unit's share of its side's loss, converted to crew. */
    static final int CREW_LOSS = 5656;

    private static final int UNIT_LINE_INDENT = 1;

    InfantryActionReporter(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Reports both sides' unit scores under their headers: attackers first, without a building modifier, then the
     * defenders with the modifier of the building they hold.
     *
     * @param attackerIds the attacking units
     * @param defenderIds the defending units
     * @param building    the contested building, or {@code null} for an action with no building modifier
     */
    void reportSides(List<Integer> attackerIds, List<Integer> defenderIds,
          @Nullable AbstractBuildingEntity building) {
        reportSide(ATTACKERS_HEADER, attackerIds, null);
        reportSide(DEFENDERS_HEADER, defenderIds, building);
    }

    private void reportSide(int headerId, List<Integer> entityIds, @Nullable AbstractBuildingEntity building) {
        addReport(new Report(headerId));
        for (int entityId : entityIds) {
            Entity entity = getGame().getEntity(entityId);
            boolean counts = (entity != null) && !isOutOfTheFight(entity);
            if (counts) {
                reportUnitScore(entity, building);
            }
        }
    }

    /**
     * Whether a unit no longer counts for its side: destroyed, doomed or a carcass. The side total and the report
     * use this one test, so the working shown always adds up to the total used.
     *
     * @param entity the unit
     *
     * @return {@code true} when the unit contributes nothing and is not listed
     */
    static boolean isOutOfTheFight(Entity entity) {
        return entity.isDestroyed() || entity.isDoomed() || entity.isCarcass();
    }

    /**
     * Reports what one unit is worth in Marine Points, with the working, and the building modifier when one applies.
     *
     * @param entity   the unit
     * @param building the building whose modifier applies to this unit, or {@code null} for none
     */
    void reportUnitScore(Entity entity, @Nullable AbstractBuildingEntity building) {
        MarinePointsBreakdown breakdown = MarinePointsScoreCalculator.breakdown(entity, building);
        Report report = switch (breakdown.kind()) {
            case CONVENTIONAL_INFANTRY -> conventionalInfantryLine(entity, breakdown);
            case BATTLE_ARMOR -> battleArmorLine(entity, breakdown);
            case CREW -> crewLine(entity, breakdown);
        };
        report.subject = entity.getId();
        report.indent(UNIT_LINE_INDENT);
        addReport(report);
        if (breakdown.hasBuildingModifier()) {
            Report modifierReport = new Report(BUILDING_MODIFIER);
            modifierReport.subject = entity.getId();
            modifierReport.indent(UNIT_LINE_INDENT);
            modifierReport.add(entity.getDisplayName());
            modifierReport.add(number(breakdown.buildingModifier()));
            modifierReport.add(number(breakdown.modifiedScore()));
            addReport(modifierReport);
        }
    }

    private static Report conventionalInfantryLine(Entity entity, MarinePointsBreakdown breakdown) {
        Report report = new Report(CONVENTIONAL_INFANTRY_SCORE);
        report.add(entity.getDisplayName());
        report.add(breakdown.headCount());
        report.add(number(breakdown.perTrooper()));
        report.add(number(breakdown.score()));
        return report;
    }

    private static Report battleArmorLine(Entity entity, MarinePointsBreakdown breakdown) {
        Report report = new Report(BATTLE_ARMOR_SCORE);
        report.add(entity.getDisplayName());
        report.add(breakdown.headCount());
        report.add(number(breakdown.perTrooper()));
        report.add(number(breakdown.baseValue()));
        report.add(number(breakdown.weightClassModifier()));
        report.add(number(breakdown.equipmentModifier()));
        report.add(breakdown.intactArmor());
        report.add(number(breakdown.armorPoints()));
        report.add(number(breakdown.score()));
        return report;
    }

    private static Report crewLine(Entity entity, MarinePointsBreakdown breakdown) {
        Report report = new Report(CREW_SCORE);
        report.add(entity.getDisplayName());
        report.add(breakdown.marines());
        report.add(breakdown.crew());
        report.add(breakdown.bayPersonnel());
        report.add(breakdown.civilians());
        report.add(number(breakdown.score()));
        return report;
    }

    /**
     * Reports how a unit's share of its side's Marine Points loss became people (TO:AR p. 174): head-count times
     * points lost over the side's own strength, rounded down.
     *
     * @param entity           the unit
     * @param headCount        the troopers or crew it had before the loss
     * @param marinePointsLost the side's loss in points
     * @param ownStrength      the side's own strength the loss is measured against
     * @param personnelLost    the people this unit loses
     * @param isCrew           {@code true} for a crewed unit or building, {@code false} for troopers
     */
    void reportUnitLoss(Entity entity, int headCount, int marinePointsLost, int ownStrength, int personnelLost,
          boolean isCrew) {
        double share = (ownStrength <= 0) ? 0 : ((double) headCount * marinePointsLost) / ownStrength;
        Report report = new Report(isCrew ? CREW_LOSS : TROOPER_LOSS);
        report.subject = entity.getId();
        report.indent(UNIT_LINE_INDENT);
        report.add(entity.getDisplayName());
        report.add(headCount);
        report.add(marinePointsLost);
        report.add(ownStrength);
        report.add(number(share));
        report.add(personnelLost);
        addReport(report);
    }

    /** A Marine Points figure for the report: whole numbers plain, fractions to two places, no trailing zeros. */
    static String number(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
