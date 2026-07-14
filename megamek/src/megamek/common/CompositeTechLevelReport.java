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

import java.util.ArrayList;
import java.util.List;

import megamek.common.RecordingCompositeTechLevel.ComponentTechRecord;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Faction;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;

/**
 * Builds a breakdown of how a unit's composite tech level is put together: the tech level of every component both with
 * and without the Variable Tech Level rule, and the running composite the components produce as they are folded in.
 * <p>
 * This answers questions of the form "why is my unit Experimental?", which the final tech level alone cannot.
 * <p>
 * The breakdown is available as HTML for display and as plain text for copying. Both are rendered from the same
 * collected data, so they always say the same thing.
 *
 * @since 0.51.01
 */
public final class CompositeTechLevelReport {

    private static final String NO_DATE = "--";
    private static final String MOVES_THE_UNIT = "moves the unit";

    private CompositeTechLevelReport() {}

    /**
     * One component of the unit, as it is presented in the report.
     *
     * @param componentName    The component's name
     * @param prototypeDate    The component's own prototype date
     * @param productionDate   The component's own production date
     * @param commonDate       The component's own common date
     * @param basicTechLevel   The component's tech level with the Variable Tech Level rule off
     * @param variableTechLevel The component's tech level in the evaluated year
     * @param unitPrototypeDate  The unit's prototype date once this component has been folded in
     * @param unitProductionDate The unit's production date once this component has been folded in
     * @param unitCommonDate     The unit's common date once this component has been folded in
     * @param movesTheUnit     Whether this component changed any of the unit's dates
     */
    private record ReportRow(String componentName, String prototypeDate, String productionDate, String commonDate,
                             String basicTechLevel, String variableTechLevel, String unitPrototypeDate,
                             String unitProductionDate, String unitCommonDate, boolean movesTheUnit) {}

    /**
     * Everything the report displays, collected once so that the HTML and plain text renderings cannot disagree.
     */
    private record ReportData(String unitName, String techBase, int introductionYear, int evaluationYear,
                              String techLevelRule, @Nullable String factionName, List<ReportRow> rows,
                              String basicTechLevel, String variableTechLevel, String prototypeRange,
                              String productionRange, String commonRange, String effectiveTechLevel) {}

    /**
     * Returns the breakdown as an HTML document for display in a themed HTML pane.
     *
     * @param entity               The unit to break down
     * @param techFaction          The faction to evaluate faction-specific dates for
     * @param evaluationYear       The year to evaluate the variable tech level in
     * @param useVariableTechLevel {@code true} when the Variable Tech Level rule is in use, so that the variable level
     *                             is reported as the unit's effective tech level
     *
     * @return The breakdown as HTML
     */
    public static String toHtml(Entity entity, Faction techFaction, int evaluationYear,
          boolean useVariableTechLevel) {
        return renderHtml(collect(entity, techFaction, evaluationYear, useVariableTechLevel));
    }

    /**
     * Returns the breakdown as plain text, laid out in aligned columns for copying into a bug report or a forum post.
     *
     * @param entity               The unit to break down
     * @param techFaction          The faction to evaluate faction-specific dates for
     * @param evaluationYear       The year to evaluate the variable tech level in
     * @param useVariableTechLevel {@code true} when the Variable Tech Level rule is in use, so that the variable level
     *                             is reported as the unit's effective tech level
     *
     * @return The breakdown as plain text
     */
    public static String toPlainText(Entity entity, Faction techFaction, int evaluationYear,
          boolean useVariableTechLevel) {
        return renderPlainText(collect(entity, techFaction, evaluationYear, useVariableTechLevel));
    }

    private static ReportData collect(Entity entity, Faction techFaction, int evaluationYear,
          boolean useVariableTechLevel) {
        RecordingCompositeTechLevel techLevel = entity.recordedTechLevel(techFaction, evaluationYear);

        List<ReportRow> rows = new ArrayList<>();
        int previousPrototype = ITechnology.DATE_NONE;
        int previousProduction = ITechnology.DATE_NONE;
        int previousCommon = ITechnology.DATE_NONE;
        boolean isFirstComponent = true;

        for (ComponentTechRecord component : techLevel.getComponentRecords()) {
            boolean movesTheUnit = !isFirstComponent
                  && ((component.compositePrototypeDate() != previousPrototype)
                  || (component.compositeProductionDate() != previousProduction)
                  || (component.compositeCommonDate() != previousCommon));

            rows.add(new ReportRow(component.componentName(),
                  formatDate(component.prototypeDate()),
                  formatDate(component.productionDate()),
                  formatDate(component.commonDate()),
                  formatLevel(component.staticTechLevel()),
                  formatLevel(component.variableTechLevel()),
                  formatDate(component.compositePrototypeDate()),
                  formatDate(component.compositeProductionDate()),
                  formatDate(component.compositeCommonDate()),
                  movesTheUnit));

            previousPrototype = component.compositePrototypeDate();
            previousProduction = component.compositeProductionDate();
            previousCommon = component.compositeCommonDate();
            isFirstComponent = false;
        }

        SimpleTechLevel basicTechLevel = entity.getStaticTechLevel();
        SimpleTechLevel variableTechLevel = entity.getSimpleLevel(evaluationYear);

        return new ReportData(entity.getShortName(),
              techBaseName(entity),
              entity.getYear(),
              evaluationYear,
              useVariableTechLevel ? "Variable Tech Level" : "Basic (static)",
              (techFaction == Faction.NONE) ? null : techFaction.getCodeMM(),
              rows,
              formatLevel(basicTechLevel),
              formatLevel(variableTechLevel),
              techLevel.getPrototypeDateRange(),
              techLevel.getProductionDateRange(),
              techLevel.getCommonDateRange(),
              formatLevel(useVariableTechLevel ? variableTechLevel : basicTechLevel));
    }

    private static String renderHtml(ReportData data) {
        StringBuilder html = new StringBuilder("<div class='report'>");

        html.append("<h2>Composite Tech Level</h2>");
        html.append("<b>").append(escape(data.unitName())).append("</b>");

        html.append("<table cellpadding='2' cellspacing='0'>");
        appendHtmlSettingRow(html, "Tech base", data.techBase());
        appendHtmlSettingRow(html, "Introduction year", String.valueOf(data.introductionYear()));
        appendHtmlSettingRow(html, "Evaluation year", String.valueOf(data.evaluationYear()));
        appendHtmlSettingRow(html, "Tech level rule", data.techLevelRule());
        if (data.factionName() != null) {
            appendHtmlSettingRow(html, "Faction", data.factionName());
        }
        html.append("</table>");

        html.append("<h3>Components</h3>");
        html.append("<table cellpadding='6' cellspacing='0'>");
        html.append("<tr>");
        appendHtmlHeaderCell(html, "Component");
        appendHtmlHeaderCell(html, "Prototype");
        appendHtmlHeaderCell(html, "Production");
        appendHtmlHeaderCell(html, "Common");
        appendHtmlHeaderCell(html, "Basic");
        appendHtmlHeaderCell(html, "Variable in " + data.evaluationYear());
        html.append("</tr>");
        for (ReportRow row : data.rows()) {
            html.append("<tr>");
            appendHtmlCell(html, row.componentName());
            appendHtmlCell(html, row.prototypeDate());
            appendHtmlCell(html, row.productionDate());
            appendHtmlCell(html, row.commonDate());
            appendHtmlCell(html, row.basicTechLevel());
            appendHtmlCell(html, row.variableTechLevel());
            html.append("</tr>");
        }
        html.append("</table>");

        html.append("<h3>How the unit's dates are built up</h3>");
        html.append("<table cellpadding='6' cellspacing='0'>");
        html.append("<tr>");
        appendHtmlHeaderCell(html, "Component");
        appendHtmlHeaderCell(html, "Unit prototype");
        appendHtmlHeaderCell(html, "Unit production");
        appendHtmlHeaderCell(html, "Unit common");
        appendHtmlHeaderCell(html, "");
        html.append("</tr>");
        for (ReportRow row : data.rows()) {
            html.append("<tr>");
            appendHtmlCell(html, row.componentName());
            appendHtmlCell(html, row.unitPrototypeDate());
            appendHtmlCell(html, row.unitProductionDate());
            appendHtmlCell(html, row.unitCommonDate());
            if (row.movesTheUnit()) {
                html.append("<td><span class='warning'>&lt;-- ").append(MOVES_THE_UNIT).append("</span></td>");
            } else {
                html.append("<td></td>");
            }
            html.append("</tr>");
        }
        html.append("</table>");

        html.append("<h3>Unit result</h3>");
        html.append("<table cellpadding='2' cellspacing='0'>");
        appendHtmlSettingRow(html, "Basic (static) tech level", data.basicTechLevel());
        appendHtmlSettingRow(html, "Variable tech level in " + data.evaluationYear(), data.variableTechLevel());
        appendHtmlSettingRow(html, "Prototype", data.prototypeRange());
        appendHtmlSettingRow(html, "Production", data.productionRange());
        appendHtmlSettingRow(html, "Common", data.commonRange());
        html.append("<tr><td><b>Effective tech level</b></td><td>&nbsp;&nbsp;</td><td><b>")
              .append(escape(data.effectiveTechLevel()))
              .append("</b></td></tr>");
        html.append("</table>");

        return html.append("</div>").toString();
    }

    private static void appendHtmlSettingRow(StringBuilder html, String label, String value) {
        html.append("<tr><td>").append(escape(label)).append(":</td><td>&nbsp;&nbsp;</td><td>")
              .append(escape(value))
              .append("</td></tr>");
    }

    private static void appendHtmlHeaderCell(StringBuilder html, String text) {
        html.append("<th align='left'><u>").append(escape(text)).append("</u></th>");
    }

    private static void appendHtmlCell(StringBuilder html, String text) {
        html.append("<td>").append(escape(text)).append("</td>");
    }

    private static String renderPlainText(ReportData data) {
        StringBuilder text = new StringBuilder();
        text.append("Composite Tech Level - ").append(data.unitName()).append('\n');
        text.append("Tech base:          ").append(data.techBase()).append('\n');
        text.append("Introduction year:  ").append(data.introductionYear()).append('\n');
        text.append("Evaluation year:    ").append(data.evaluationYear()).append('\n');
        text.append("Tech level rule:    ").append(data.techLevelRule()).append('\n');
        if (data.factionName() != null) {
            text.append("Faction:            ").append(data.factionName()).append('\n');
        }

        List<String[]> componentTable = new ArrayList<>();
        componentTable.add(new String[] { "Component", "Prototype", "Production", "Common", "Basic",
                                          "Variable in " + data.evaluationYear() });
        for (ReportRow row : data.rows()) {
            componentTable.add(new String[] { row.componentName(), row.prototypeDate(), row.productionDate(),
                                              row.commonDate(), row.basicTechLevel(), row.variableTechLevel() });
        }
        text.append("\nComponents\n").append(formatTextTable(componentTable));

        List<String[]> buildUpTable = new ArrayList<>();
        buildUpTable.add(new String[] { "Component", "Unit prototype", "Unit production", "Unit common", "" });
        for (ReportRow row : data.rows()) {
            buildUpTable.add(new String[] { row.componentName(), row.unitPrototypeDate(), row.unitProductionDate(),
                                            row.unitCommonDate(), row.movesTheUnit() ? "<-- " + MOVES_THE_UNIT : "" });
        }
        text.append("\nHow the unit's dates are built up\n").append(formatTextTable(buildUpTable));

        List<String[]> resultTable = new ArrayList<>();
        resultTable.add(new String[] { "Basic (static) tech level:", data.basicTechLevel() });
        resultTable.add(new String[] { "Variable tech level in " + data.evaluationYear() + ":",
                                       data.variableTechLevel() });
        resultTable.add(new String[] { "Prototype:", data.prototypeRange() });
        resultTable.add(new String[] { "Production:", data.productionRange() });
        resultTable.add(new String[] { "Common:", data.commonRange() });
        resultTable.add(new String[] { "Effective tech level:", data.effectiveTechLevel() });
        text.append("\nUnit result\n").append(formatTextTable(resultTable));

        return text.toString();
    }

    /**
     * Lays a table out with every column padded to the width of its widest entry, so that the columns line up when the
     * report is pasted somewhere that uses a fixed-width font.
     *
     * @param table The rows of the table, each holding one entry per column
     *
     * @return The table as aligned plain text
     */
    private static String formatTextTable(List<String[]> table) {
        int columnCount = table.getFirst().length;
        int[] columnWidths = new int[columnCount];
        for (String[] row : table) {
            for (int column = 0; column < columnCount; column++) {
                columnWidths[column] = Math.max(columnWidths[column], row[column].length());
            }
        }

        StringBuilder text = new StringBuilder();
        for (String[] row : table) {
            StringBuilder line = new StringBuilder("  ");
            for (int column = 0; column < columnCount; column++) {
                line.append(padRight(row[column], columnWidths[column]));
                if (column < columnCount - 1) {
                    line.append("  ");
                }
            }
            text.append(line.toString().stripTrailing()).append('\n');
        }
        return text.toString();
    }

    private static String padRight(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }

    private static String techBaseName(Entity entity) {
        if (entity.isMixedTech()) {
            return "Mixed (" + (entity.isClan() ? "Clan" : "Inner Sphere") + " base)";
        }
        return entity.isClan() ? "Clan" : "Inner Sphere";
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

    /**
     * Escapes the characters that would otherwise be read as markup. Component names come from unit files and can
     * contain characters such as {@code &} that must not reach the HTML pane raw.
     *
     * @param text The text to escape
     *
     * @return The text, safe to place in the HTML document
     */
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
