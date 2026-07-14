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

package megamek.common;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.RecordingCompositeTechLevel.ComponentTechRecord;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Faction;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;

/**
 * Writes a breakdown of how a unit's composite tech level is put together into a {@link CalculationReport}: the tech
 * level of every component both with and without the Variable Tech Level rule, and the running composite the components
 * produce as they are folded in.
 * <p>
 * This answers questions of the form "why is my unit Experimental?", which the final tech level alone cannot.
 *
 * @since 0.51.01
 */
public final class CompositeTechLevelReport {

    private static final String NO_DATE = "--";
    private static final String CHANGES_THE_UNIT = "<-- changes the unit";

    private CompositeTechLevelReport() {}

    /**
     * Writes the composite tech level breakdown for the given unit into the given report.
     *
     * @param report               The report to write to
     * @param entity               The unit to break down
     * @param techFaction          The faction to evaluate faction-specific dates for
     * @param evaluationYear       The year to evaluate the variable tech level in
     * @param useVariableTechLevel {@code true} when the Variable Tech Level rule is in use, so that the variable level
     *                             is reported as the unit's effective tech level; {@code false} to report the static
     *                             level as the effective one
     *
     * @return The report that was written to
     */
    public static CalculationReport fillReport(CalculationReport report, Entity entity, Faction techFaction,
          int evaluationYear, boolean useVariableTechLevel) {
        RecordingCompositeTechLevel techLevel = entity.recordedTechLevel(techFaction, evaluationYear);

        addSettingsSection(report, entity, techFaction, evaluationYear, useVariableTechLevel);
        addComponentSection(report, techLevel, evaluationYear);
        addBuildUpSection(report, techLevel);
        addResultSection(report, entity, techLevel, evaluationYear, useVariableTechLevel);

        return report;
    }

    private static void addSettingsSection(CalculationReport report, Entity entity, Faction techFaction,
          int evaluationYear, boolean useVariableTechLevel) {
        report.addHeader("Composite Tech Level");
        report.addSubHeader(entity.getShortName());
        report.addLine("Tech base:", "", techBaseName(entity));
        report.addLine("Introduction year:", "", String.valueOf(entity.getYear()));
        report.addLine("Evaluation year:", "", String.valueOf(evaluationYear));
        report.addLine("Tech level rule:", "", useVariableTechLevel ? "Variable Tech Level" : "Basic (static)");
        if (techFaction != Faction.NONE) {
            report.addLine("Faction:", "", techFaction.getCodeMM());
        }
        report.addEmptyLine();
    }

    /**
     * Lists every component with its own advancement dates and the tech level it has under each of the two rules, so
     * that the two can be compared component by component.
     */
    private static void addComponentSection(CalculationReport report, RecordingCompositeTechLevel techLevel,
          int evaluationYear) {
        report.addSubHeader("Components");
        report.addLine("", "Prototype / Production / Common", "Basic -> Variable in " + evaluationYear);

        for (ComponentTechRecord component : techLevel.getComponentRecords()) {
            report.addLine(component.componentName(),
                  formatDates(component.prototypeDate(), component.productionDate(), component.commonDate()),
                  formatLevel(component.staticTechLevel()) + " -> " + formatLevel(component.variableTechLevel()));
        }
        report.addEmptyLine();
    }

    /**
     * Shows the running composite after each component is folded in, marking the components that actually move it. This
     * is what identifies the component responsible for the unit's tech level.
     */
    private static void addBuildUpSection(CalculationReport report, RecordingCompositeTechLevel techLevel) {
        report.addSubHeader("How the unit's dates are built up");
        report.addLine("", "Unit prototype / production / common", "");

        int previousPrototype = ITechnology.DATE_NONE;
        int previousProduction = ITechnology.DATE_NONE;
        int previousCommon = ITechnology.DATE_NONE;
        boolean isFirstComponent = true;

        for (ComponentTechRecord component : techLevel.getComponentRecords()) {
            boolean movedTheUnit = !isFirstComponent
                  && ((component.compositePrototypeDate() != previousPrototype)
                  || (component.compositeProductionDate() != previousProduction)
                  || (component.compositeCommonDate() != previousCommon));

            report.addLine(component.componentName(),
                  formatDates(component.compositePrototypeDate(),
                        component.compositeProductionDate(),
                        component.compositeCommonDate()),
                  movedTheUnit ? CHANGES_THE_UNIT : "");

            previousPrototype = component.compositePrototypeDate();
            previousProduction = component.compositeProductionDate();
            previousCommon = component.compositeCommonDate();
            isFirstComponent = false;
        }
        report.addEmptyLine();
    }

    private static void addResultSection(CalculationReport report, Entity entity,
          RecordingCompositeTechLevel techLevel, int evaluationYear, boolean useVariableTechLevel) {
        SimpleTechLevel staticLevel = entity.getStaticTechLevel();
        SimpleTechLevel variableLevel = entity.getSimpleLevel(evaluationYear);

        report.addSubHeader("Unit result");
        report.addLine("Basic (static) tech level:", "", formatLevel(staticLevel));
        report.addLine("Variable tech level in " + evaluationYear + ":", "", formatLevel(variableLevel));
        report.addLine("Prototype:", "", techLevel.getPrototypeDateRange());
        report.addLine("Production:", "", techLevel.getProductionDateRange());
        report.addLine("Common:", "", techLevel.getCommonDateRange());
        report.addResultLine("Effective tech level:", "",
              formatLevel(useVariableTechLevel ? variableLevel : staticLevel));
    }

    private static String techBaseName(Entity entity) {
        if (entity.isMixedTech()) {
            return "Mixed (" + (entity.isClan() ? "Clan" : "Inner Sphere") + " base)";
        }
        return entity.isClan() ? "Clan" : "Inner Sphere";
    }

    private static String formatDates(int prototypeDate, int productionDate, int commonDate) {
        return formatDate(prototypeDate) + " / " + formatDate(productionDate) + " / " + formatDate(commonDate);
    }

    private static String formatDate(int date) {
        return switch (date) {
            case ITechnology.DATE_NONE -> NO_DATE;
            case ITechnology.DATE_PS -> "PS";
            case ITechnology.DATE_ES -> "ES";
            default -> String.valueOf(date);
        };
    }

    private static String formatLevel(@Nullable SimpleTechLevel techLevel) {
        return (techLevel == null) ? NO_DATE : techLevel.toString();
    }
}
