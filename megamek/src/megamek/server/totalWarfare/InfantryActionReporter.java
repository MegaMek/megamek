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
import java.util.ArrayList;
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
    /** The attackers lose some of their Marine Points. */
    static final int ATTACKERS_LOSE = 5642;
    /** The defenders lose some of their Marine Points. */
    static final int DEFENDERS_LOSE = 5643;
    /** The attackers lose every Marine Point. */
    static final int ATTACKERS_LOSE_ALL = 5647;
    /** The defenders lose every Marine Point. */
    static final int DEFENDERS_LOSE_ALL = 5648;
    /** "and N casualties:" */
    static final int CASUALTIES = 5633;
    /** "and one casualty:" */
    static final int ONE_CASUALTY = 5634;
    /** "and nobody:" */
    static final int NO_CASUALTIES = 5649;
    /** A unit's share of its side's loss, in troopers. */
    static final int TROOPER_LOSS = 5655;
    /** A building's share of its side's loss, in crew. */
    static final int CREW_LOSS = 5656;
    /** Every unit's share was under one, so nobody was lost. */
    static final int TOO_SMALL_FOR_ANYONE = 5661;
    /** A comma between unit fragments. */
    static final int SEPARATOR = 5709;

    /** Side headers and the roll sit one level under the action's header, like a weapon under its unit. */
    static final int SIDE_LINE_INDENT = 1;
    /** Unit lines sit under their side, like damage under a weapon. */
    private static final int UNIT_LINE_INDENT = 2;

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
        reportSides(attackerIds, defenderIds, building, sideTotal(attackerIds, null), sideTotal(defenderIds, building));
    }

    /**
     * Reports both sides' unit scores under headers that carry the side totals the action uses.
     *
     * @param attackerIds   the attacking units
     * @param defenderIds   the defending units
     * @param building      the contested building, or {@code null} for an action with no building modifier
     * @param attackerTotal the attackers' Marine Points, rounded up
     * @param defenderTotal the defenders' Marine Points, rounded up
     */
    void reportSides(List<Integer> attackerIds, List<Integer> defenderIds,
          @Nullable AbstractBuildingEntity building, int attackerTotal, int defenderTotal) {
        reportSide(ATTACKERS_HEADER, attackerIds, null, attackerTotal);
        reportSide(DEFENDERS_HEADER, defenderIds, building, defenderTotal);
    }

    private int sideTotal(List<Integer> entityIds, @Nullable AbstractBuildingEntity building) {
        double total = 0;
        for (int entityId : entityIds) {
            Entity entity = getGame().getEntity(entityId);
            if ((entity != null) && !isOutOfTheFight(entity)) {
                total += MarinePointsScoreCalculator.calculateScore(entity, building);
            }
        }
        return (int) Math.ceil(total);
    }

    private void reportSide(int headerId, List<Integer> entityIds, @Nullable AbstractBuildingEntity building,
          int total) {
        Report header = new Report(headerId);
        header.indent(SIDE_LINE_INDENT);
        header.add(total);
        addReport(header);
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
            modifierReport.addDesc(entity);
            modifierReport.add(number(breakdown.buildingModifier()));
            modifierReport.add(number(breakdown.modifiedScore()));
            addReport(modifierReport);
        }
    }

    private static Report conventionalInfantryLine(Entity entity, MarinePointsBreakdown breakdown) {
        Report report = new Report(CONVENTIONAL_INFANTRY_SCORE);
        report.addDesc(entity);
        report.add(breakdown.headCount());
        report.add(number(breakdown.perTrooper()));
        report.add(number(breakdown.score()));
        return report;
    }

    private static Report battleArmorLine(Entity entity, MarinePointsBreakdown breakdown) {
        Report report = new Report(BATTLE_ARMOR_SCORE);
        report.addDesc(entity);
        report.add(breakdown.headCount());
        report.add(number(breakdown.perTrooper()));
        report.add(number(breakdown.baseValue()));
        report.add(number(breakdown.weightClassModifier()));
        report.add(number(breakdown.equipmentModifier()));
        report.add(number(breakdown.headCount() * breakdown.perTrooper()));
        report.add(breakdown.intactArmor());
        report.add(number(breakdown.armorPoints()));
        report.add(number(breakdown.score()));
        return report;
    }


    private static Report crewLine(Entity entity, MarinePointsBreakdown breakdown) {
        Report report = new Report(CREW_SCORE);
        report.addDesc(entity);
        report.add(breakdown.marines());
        report.add(breakdown.crew());
        report.add(breakdown.bayPersonnel());
        report.add(breakdown.civilians());
        report.add(number(breakdown.score()));
        return report;
    }

    /**
     * Reports a side's loss on one line: the Marine Points lost of its strength, the casualties, then each unit's
     * share of the loss as head-count times points lost over the side's own strength (TO:AR p. 174), rounded down
     * when it became people.
     *
     * @param attackers {@code true} for the attackers' line, {@code false} for the defenders'
     * @param losses    the side's planned losses
     */
    void reportSideLosses(boolean attackers, InfantryActionSideLosses losses) {
        List<Report> line = new ArrayList<>();
        boolean lostEverything = (losses.ownStrength() > 0) && (losses.marinePointsLost() >= losses.ownStrength());
        Report head;
        if (lostEverything) {
            head = new Report(attackers ? ATTACKERS_LOSE_ALL : DEFENDERS_LOSE_ALL);
        } else {
            head = new Report(attackers ? ATTACKERS_LOSE : DEFENDERS_LOSE);
            head.add(losses.marinePointsLost());
        }
        head.add(losses.ownStrength());
        head.indent(SIDE_LINE_INDENT);
        line.add(head);

        int casualties = losses.personnelLost();
        if (casualties == 0) {
            line.add(new Report(NO_CASUALTIES));
        } else if (casualties == 1) {
            line.add(new Report(ONE_CASUALTY));
        } else {
            line.add(new Report(CASUALTIES).add(casualties));
        }

        boolean first = true;
        for (InfantryActionSideLosses.UnitLoss loss : losses.units()) {
            if (!first) {
                line.add(new Report(SEPARATOR));
            }
            first = false;
            line.add(unitLossFragment(loss, losses));
        }
        if ((casualties == 0) && !losses.units().isEmpty()) {
            line.add(new Report(TOO_SMALL_FOR_ANYONE));
        }
        addLine(line);
    }

    private static Report unitLossFragment(InfantryActionSideLosses.UnitLoss loss, InfantryActionSideLosses losses) {
        double share = (losses.ownStrength() <= 0) ? 0
              : ((double) loss.headCount() * losses.marinePointsLost()) / losses.ownStrength();
        Report fragment = new Report(loss.isCrew() ? CREW_LOSS : TROOPER_LOSS);
        fragment.subject = loss.entity().getId();
        fragment.addEntityName(loss.entity());
        fragment.add(number(share));
        fragment.add(loss.headCount());
        return fragment;
    }

    /** Runs the reports on as one line: no line break until the last of them. */
    private void addLine(List<Report> reports) {
        for (Report report : reports) {
            report.newlines = 0;
        }
        reports.getLast().newlines = 1;
        for (Report report : reports) {
            addReport(report);
        }
    }

    /** A Marine Points figure for the report: whole numbers plain, fractions to two places, no trailing zeros. */
    static String number(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
