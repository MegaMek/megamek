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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    /** Swing's HTML renderer draws gridlines from the table's border attribute, not from CSS. */
    private static final String TABLE_START = "<table border='1' cellpadding='6' cellspacing='0'>";
    private static final String INDENT = "  ";
    private static final String COLUMN_GAP = "  ";

    private CompositeTechLevelReport() {}

    /**
     * One component of the unit, as it is presented in the report. Components that are identical in every respect, such
     * as the ten heat sinks of a Mek, are collected into a single row with a count rather than repeated.
     *
     * @param componentName          The component's name
     * @param count                  How many identical copies of this component the unit carries
     * @param prototypeDate          The component's own prototype date
     * @param productionDate         The component's own production date
     * @param commonDate             The component's own common date
     * @param basicTechLevel         The component's tech level with the Variable Tech Level rule off
     * @param variableTechLevel      The component's tech level in the evaluated year
     * @param unitPrototypeDate      The unit's prototype date once this component has been folded in
     * @param unitProductionDate     The unit's production date once this component has been folded in
     * @param unitCommonDate         The unit's common date once this component has been folded in
     * @param movesUnitPrototypeDate Whether this component set or moved the unit's prototype date
     * @param movesUnitProductionDate Whether this component set or moved the unit's production date
     * @param movesUnitCommonDate    Whether this component set or moved the unit's common date
     */
    private record ReportRow(String componentName, int count, String prototypeDate, String productionDate,
                             String commonDate, String basicTechLevel, String variableTechLevel,
                             String unitPrototypeDate, String unitProductionDate, String unitCommonDate,
                             boolean movesUnitPrototypeDate, boolean movesUnitProductionDate,
                             boolean movesUnitCommonDate) {

        /** @return The component's name, with a count appended when the unit carries more than one */
        String displayName() {
            return (count > 1) ? componentName + " x" + count : componentName;
        }

        ReportRow withOneMore() {
            return new ReportRow(componentName, count + 1, prototypeDate, productionDate, commonDate, basicTechLevel,
                  variableTechLevel, unitPrototypeDate, unitProductionDate, unitCommonDate, movesUnitPrototypeDate,
                  movesUnitProductionDate, movesUnitCommonDate);
        }

        /** @return A key identifying components that are identical and can therefore share a row */
        String mergeKey() {
            return String.join("|", componentName, prototypeDate, productionDate, commonDate, basicTechLevel,
                  variableTechLevel);
        }

        /**
         * @return A note naming the tech progression points this component drives -- when it sets when the unit
         *       becomes a prototype, enters production or becomes common -- or an empty string when it changes none.
         */
        String progressionNote() {
            List<String> becomes = new ArrayList<>();
            if (movesUnitPrototypeDate) {
                becomes.add("Becomes Prototype");
            }
            if (movesUnitProductionDate) {
                becomes.add("Becomes Production");
            }
            if (movesUnitCommonDate) {
                becomes.add("Becomes Common");
            }
            return String.join(", ", becomes);
        }
    }

    /**
     * A group of adjacent columns that share a heading spanning all of them.
     *
     * @param label       The heading to draw above the group
     * @param firstColumn The index of the first column in the group
     * @param lastColumn  The index of the last column in the group
     */
    private record ColumnGroup(String label, int firstColumn, int lastColumn) {}

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
        Map<String, Integer> rowIndexByComponent = new HashMap<>();
        int previousPrototype = ITechnology.DATE_NONE;
        int previousProduction = ITechnology.DATE_NONE;
        int previousCommon = ITechnology.DATE_NONE;
        boolean isFirstComponent = true;

        for (ComponentTechRecord component : techLevel.getComponentRecords()) {
            // A date "moves" when this component changes it to a real date. Changing it only to "no date"
            // means the component removed a phase rather than driving one, which "Becomes X" would misdescribe.
            boolean movesPrototype = !isFirstComponent
                  && (component.compositePrototypeDate() != previousPrototype)
                  && (component.compositePrototypeDate() != ITechnology.DATE_NONE);
            boolean movesProduction = !isFirstComponent
                  && (component.compositeProductionDate() != previousProduction)
                  && (component.compositeProductionDate() != ITechnology.DATE_NONE);
            boolean movesCommon = !isFirstComponent
                  && (component.compositeCommonDate() != previousCommon)
                  && (component.compositeCommonDate() != ITechnology.DATE_NONE);

            ReportRow row = new ReportRow(component.componentName(),
                  1,
                  formatDate(component.prototypeDate()),
                  formatDate(component.productionDate()),
                  formatDate(component.commonDate()),
                  formatLevel(component.staticTechLevel()),
                  formatLevel(component.variableTechLevel()),
                  formatDate(component.compositePrototypeDate()),
                  formatDate(component.compositeProductionDate()),
                  formatDate(component.compositeCommonDate()),
                  movesPrototype,
                  movesProduction,
                  movesCommon);

            // Folding the same component in twice cannot change the unit's dates a second time, so every copy
            // after the first is a repeat of a row the report has already shown. Count them on the first row
            // instead of listing ten identical heat sinks.
            Integer existingRowIndex = rowIndexByComponent.get(row.mergeKey());
            if (existingRowIndex == null) {
                rowIndexByComponent.put(row.mergeKey(), rows.size());
                rows.add(row);
            } else {
                rows.set(existingRowIndex, rows.get(existingRowIndex).withOneMore());
            }

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
        html.append(TABLE_START);
        // The component's own dates and the level it has in the evaluated year all belong to the Variable Tech
        // Level rule; the Basic level is the static one and does not depend on the year, so it sits outside.
        html.append("<tr><th></th><th colspan='4' align='center'>Variable Tech Level</th><th></th></tr>");
        html.append("<tr>");
        appendHtmlHeaderCell(html, "Component");
        appendHtmlHeaderCell(html, "Prototype");
        appendHtmlHeaderCell(html, "Production");
        appendHtmlHeaderCell(html, "Common");
        appendHtmlHeaderCell(html, "Variable in " + data.evaluationYear());
        appendHtmlHeaderCell(html, "Basic");
        html.append("</tr>");
        for (ReportRow row : data.rows()) {
            html.append("<tr>");
            appendHtmlCell(html, row.displayName());
            appendHtmlCell(html, row.prototypeDate());
            appendHtmlCell(html, row.productionDate());
            appendHtmlCell(html, row.commonDate());
            appendHtmlCell(html, row.variableTechLevel());
            appendHtmlCell(html, row.basicTechLevel());
            html.append("</tr>");
        }
        html.append("</table>");

        html.append("<h3>How the unit's dates are built up</h3>");
        html.append(TABLE_START);
        html.append("<tr>");
        appendHtmlHeaderCell(html, "Component");
        appendHtmlHeaderCell(html, "Unit prototype");
        appendHtmlHeaderCell(html, "Unit production");
        appendHtmlHeaderCell(html, "Unit common");
        appendHtmlHeaderCell(html, "");
        html.append("</tr>");
        for (ReportRow row : data.rows()) {
            html.append("<tr>");
            appendHtmlCell(html, row.displayName());
            appendHtmlDateCell(html, row.unitPrototypeDate(), row.movesUnitPrototypeDate());
            appendHtmlDateCell(html, row.unitProductionDate(), row.movesUnitProductionDate());
            appendHtmlDateCell(html, row.unitCommonDate(), row.movesUnitCommonDate());
            String progressionNote = row.progressionNote();
            if (progressionNote.isEmpty()) {
                html.append("<td></td>");
            } else {
                html.append("<td><span class='warning'>").append(escape(progressionNote)).append("</span></td>");
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

    /**
     * Appends a date cell to the build-up table, drawing it in the themed warning colour when this is the component
     * that set that date, so the eye is drawn to the exact value that changed.
     *
     * @param html      The document being built
     * @param date      The unit date to show
     * @param highlight Whether this component set this date
     */
    private static void appendHtmlDateCell(StringBuilder html, String date, boolean highlight) {
        if (highlight) {
            html.append("<td><span class='warning'>").append(escape(date)).append("</span></td>");
        } else {
            html.append("<td>").append(escape(date)).append("</td>");
        }
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
        componentTable.add(new String[] { "Component", "Prototype", "Production", "Common",
                                          "Variable in " + data.evaluationYear(), "Basic" });
        for (ReportRow row : data.rows()) {
            componentTable.add(new String[] { row.displayName(), row.prototypeDate(), row.productionDate(),
                                              row.commonDate(), row.variableTechLevel(), row.basicTechLevel() });
        }
        text.append("\nComponents\n")
              .append(formatTextTable(componentTable, new ColumnGroup("Variable Tech Level", 1, 4)));

        List<String[]> buildUpTable = new ArrayList<>();
        buildUpTable.add(new String[] { "Component", "Unit prototype", "Unit production", "Unit common", "" });
        for (ReportRow row : data.rows()) {
            buildUpTable.add(new String[] { row.displayName(), row.unitPrototypeDate(), row.unitProductionDate(),
                                            row.unitCommonDate(), row.progressionNote() });
        }
        text.append("\nHow the unit's dates are built up\n").append(formatTextTable(buildUpTable, null));

        List<String[]> resultTable = new ArrayList<>();
        resultTable.add(new String[] { "Basic (static) tech level:", data.basicTechLevel() });
        resultTable.add(new String[] { "Variable tech level in " + data.evaluationYear() + ":",
                                       data.variableTechLevel() });
        resultTable.add(new String[] { "Prototype:", data.prototypeRange() });
        resultTable.add(new String[] { "Production:", data.productionRange() });
        resultTable.add(new String[] { "Common:", data.commonRange() });
        resultTable.add(new String[] { "Effective tech level:", data.effectiveTechLevel() });
        text.append("\nUnit result\n").append(formatTextTable(resultTable, null));

        return text.toString();
    }

    /**
     * Lays a table out with every column padded to the width of its widest entry, so that the columns line up when the
     * report is pasted somewhere that uses a fixed-width font.
     *
     * @param table       The rows of the table, each holding one entry per column
     * @param columnGroup A heading to draw above a span of columns, or {@code null} for none
     *
     * @return The table as aligned plain text
     */
    private static String formatTextTable(List<String[]> table, @Nullable ColumnGroup columnGroup) {
        int columnCount = table.getFirst().length;
        int[] columnWidths = new int[columnCount];
        for (String[] row : table) {
            for (int column = 0; column < columnCount; column++) {
                columnWidths[column] = Math.max(columnWidths[column], row[column].length());
            }
        }

        StringBuilder text = new StringBuilder();
        if (columnGroup != null) {
            text.append(formatColumnGroupLine(columnGroup, columnWidths)).append('\n');
        }
        for (String[] row : table) {
            StringBuilder line = new StringBuilder(INDENT);
            for (int column = 0; column < columnCount; column++) {
                line.append(padRight(row[column], columnWidths[column]));
                if (column < columnCount - 1) {
                    line.append(COLUMN_GAP);
                }
            }
            text.append(line.toString().stripTrailing()).append('\n');
        }
        return text.toString();
    }

    /**
     * Draws the heading of a column group centred in dashes above the columns it spans, so that the plain text shows
     * the same grouping the HTML table shows with a spanning header cell.
     *
     * @param columnGroup  The group to draw
     * @param columnWidths The width of every column in the table
     *
     * @return The line to place above the table's header row
     */
    private static String formatColumnGroupLine(ColumnGroup columnGroup, int[] columnWidths) {
        int leadingWidth = 0;
        for (int column = 0; column < columnGroup.firstColumn(); column++) {
            leadingWidth += columnWidths[column] + COLUMN_GAP.length();
        }

        int groupWidth = 0;
        for (int column = columnGroup.firstColumn(); column <= columnGroup.lastColumn(); column++) {
            groupWidth += columnWidths[column];
        }
        groupWidth += COLUMN_GAP.length() * (columnGroup.lastColumn() - columnGroup.firstColumn());

        String label = " " + columnGroup.label() + " ";
        int dashCount = Math.max(0, groupWidth - label.length());
        int leadingDashes = dashCount / 2;
        String groupLabel = "-".repeat(leadingDashes) + label + "-".repeat(dashCount - leadingDashes);

        return INDENT + " ".repeat(leadingWidth) + groupLabel;
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
