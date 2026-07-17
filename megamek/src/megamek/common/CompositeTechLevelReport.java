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
    private static final String MESSAGE_PREFIX = "CompositeTechLevelReport.";
    /** Swing's HTML renderer draws gridlines from the table's border attribute, not from CSS. */
    private static final String TABLE_START = "<table border='1' cellpadding='6' cellspacing='0'>";
    private static final String INDENT = "  ";
    private static final String COLUMN_GAP = "  ";

    private CompositeTechLevelReport() {}

    /**
     * @param key A message key, without the shared {@value #MESSAGE_PREFIX} prefix
     *
     * @return The localized text for the key
     */
    private static String message(String key) {
        return Messages.getString(MESSAGE_PREFIX + key);
    }

    /**
     * @param key       A message key, without the shared {@value #MESSAGE_PREFIX} prefix
     * @param arguments The arguments to substitute into the message
     *
     * @return The localized, formatted text for the key
     */
    private static String message(String key, Object... arguments) {
        return Messages.getString(MESSAGE_PREFIX + key, arguments);
    }

    /**
     * @param key A message key, without the shared {@value #MESSAGE_PREFIX} prefix
     *
     * @return The localized text for the key, followed by a colon, for use as a row label
     */
    private static String withColon(String key) {
        return message(key) + ":";
    }

    /**
     * One component of the unit, as it is presented in the report. Components that are identical in every respect, such
     * as the ten heat sinks of a Mek, are collected into a single row with a count rather than repeated.
     *
     * @param componentName          The component's name
     * @param count                  How many identical copies of this component the unit carries
     * @param prototypeDate          The component's own prototype date
     * @param productionDate         The component's own production date
     * @param commonDate             The component's own common date
     * @param extinctionDate         The component's own extinction date, or "--" when it never goes extinct
     * @param reintroductionDate     The component's own reintroduction date, or "--" when it never returns
     * @param staticTechLevel        The component's tech level with the Variable Tech Level rule off
     * @param variableTechLevel      The component's tech level in the evaluated year
     * @param equipmentPrototypeDate           The combined equipment's prototype date once this component has been
     *                                         folded in
     * @param equipmentProductionDate          The combined equipment's production date once this component has been
     *                                         folded in
     * @param equipmentCommonDate              The combined equipment's common date once this component has been
     *                                         folded in
     * @param equipmentExtinctionDate          The combined equipment's extinction date once this component has been
     *                                         folded in
     * @param equipmentReintroductionDate      The combined equipment's reintroduction date once this component has
     *                                         been folded in
     * @param movesEquipmentPrototypeDate      Whether this component set the equipment prototype date
     * @param movesEquipmentProductionDate     Whether this component set the equipment production date
     * @param movesEquipmentCommonDate         Whether this component set the equipment common date
     * @param movesEquipmentExtinctionDate     Whether this component set the equipment extinction date
     * @param movesEquipmentReintroductionDate Whether this component set the equipment reintroduction date
     */
    private record ReportRow(String componentName, int count, String prototypeDate, String productionDate,
                             String commonDate, String extinctionDate, String reintroductionDate,
                             String staticTechLevel, String variableTechLevel, String equipmentPrototypeDate,
                             String equipmentProductionDate, String equipmentCommonDate, String equipmentExtinctionDate,
                             String equipmentReintroductionDate, boolean movesEquipmentPrototypeDate,
                             boolean movesEquipmentProductionDate, boolean movesEquipmentCommonDate,
                             boolean movesEquipmentExtinctionDate, boolean movesEquipmentReintroductionDate) {

        /** @return The component's name, with a count appended when the unit carries more than one */
        String displayName() {
            return (count > 1) ? message("componentCount", componentName, String.valueOf(count)) : componentName;
        }

        ReportRow withOneMore() {
            return new ReportRow(componentName, count + 1, prototypeDate, productionDate, commonDate, extinctionDate,
                  reintroductionDate, staticTechLevel, variableTechLevel, equipmentPrototypeDate, equipmentProductionDate,
                  equipmentCommonDate, equipmentExtinctionDate, equipmentReintroductionDate, movesEquipmentPrototypeDate,
                  movesEquipmentProductionDate, movesEquipmentCommonDate, movesEquipmentExtinctionDate,
                  movesEquipmentReintroductionDate);
        }

        /** @return A key identifying components that are identical and can therefore share a row */
        String mergeKey() {
            return String.join("|", componentName, prototypeDate, productionDate, commonDate, extinctionDate,
                  reintroductionDate, staticTechLevel, variableTechLevel);
        }

        /**
         * @return A note naming the progression points this component drives, each with the year it sets -- for
         *       example {@code "Becomes Common (3045)"} -- or an empty string when it drives none. Extinction is
         *       shown in its own columns rather than here.
         */
        String progressionNote() {
            List<String> becomes = new ArrayList<>();
            if (movesEquipmentPrototypeDate) {
                becomes.add(message("noteBecomesPrototype", equipmentPrototypeDate));
            }
            if (movesEquipmentProductionDate) {
                becomes.add(message("noteBecomesProduction", equipmentProductionDate));
            }
            if (movesEquipmentCommonDate) {
                becomes.add(message("noteBecomesCommon", equipmentCommonDate));
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
                              String staticTechLevel, String variableTechLevel, String prototypeRange,
                              String productionRange, String commonRange, String extinctionRange,
                              String effectiveTechLevel) {}

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
        int previousExtinction = ITechnology.DATE_NONE;
        int previousReintroduction = ITechnology.DATE_NONE;
        boolean isFirstComponent = true;

        for (ComponentTechRecord component : techLevel.getComponentRecords()) {
            int equipmentPrototype = component.compositePrototypeDate();
            int equipmentProduction = component.compositeProductionDate();
            int equipmentCommon = component.compositeCommonDate();
            int equipmentExtinction = component.compositeExtinctionDate();
            int equipmentReintroduction = component.compositeReintroductionDate();

            // A date "moves" when this component changes it to a real date. When a later phase's date is pushed
            // out, the algorithm backfills the earlier phase with that phase's old date -- for example, pushing
            // the common date later leaves the old common date sitting in the production column. That backfill
            // makes the unit available no earlier than before, so it is not the component "becoming" that phase;
            // suppressing it keeps a single common-date push from also reading as "Becomes Production".
            boolean movesPrototype = !isFirstComponent
                  && (equipmentPrototype != previousPrototype)
                  && (equipmentPrototype != ITechnology.DATE_NONE)
                  && (equipmentPrototype != previousProduction);
            boolean movesProduction = !isFirstComponent
                  && (equipmentProduction != previousProduction)
                  && (equipmentProduction != ITechnology.DATE_NONE)
                  && (equipmentProduction != previousCommon);
            boolean movesCommon = !isFirstComponent
                  && (equipmentCommon != previousCommon)
                  && (equipmentCommon != ITechnology.DATE_NONE);
            boolean movesExtinction = !isFirstComponent
                  && (equipmentExtinction != previousExtinction)
                  && (equipmentExtinction != ITechnology.DATE_NONE);
            boolean movesReintroduction = !isFirstComponent
                  && (equipmentReintroduction != previousReintroduction)
                  && (equipmentReintroduction != ITechnology.DATE_NONE);

            ReportRow row = new ReportRow(component.componentName(),
                  1,
                  formatDate(component.prototypeDate()),
                  formatDate(component.productionDate()),
                  formatDate(component.commonDate()),
                  formatDate(component.extinctionDate()),
                  formatDate(component.reintroductionDate()),
                  formatLevel(component.staticTechLevel()),
                  formatLevel(component.variableTechLevel()),
                  formatDate(equipmentPrototype),
                  formatDate(equipmentProduction),
                  formatDate(equipmentCommon),
                  formatDate(equipmentExtinction),
                  formatDate(equipmentReintroduction),
                  movesPrototype,
                  movesProduction,
                  movesCommon,
                  movesExtinction,
                  movesReintroduction);

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

            previousPrototype = equipmentPrototype;
            previousProduction = equipmentProduction;
            previousCommon = equipmentCommon;
            previousExtinction = equipmentExtinction;
            previousReintroduction = equipmentReintroduction;
            isFirstComponent = false;
        }

        SimpleTechLevel staticTechLevel = entity.getStaticTechLevel();
        String variableTechLevelText = variableTechLevelText(entity, techLevel, evaluationYear);

        return new ReportData(entity.getShortName(),
              techBaseName(entity),
              entity.getYear(),
              evaluationYear,
              useVariableTechLevel ? message("groupVariable") : message("groupStatic"),
              (techFaction == Faction.NONE) ? null : techFaction.getCodeMM(),
              rows,
              formatLevel(staticTechLevel),
              variableTechLevelText,
              normalizeRange(techLevel.getPrototypeDateRange()),
              normalizeRange(techLevel.getProductionDateRange()),
              normalizeRange(techLevel.getCommonDateRange()),
              normalizeRange(techLevel.getExtinctionRange()),
              useVariableTechLevel ? variableTechLevelText : formatLevel(staticTechLevel));
    }

    /**
     * Returns the unit's variable tech level in the evaluated year, as the report presents it. A unit that cannot be
     * built in that year -- because it is not yet available, or because a component has gone extinct -- does not get a
     * bare level, which would read as "you can build this": it gets the level it will hold when it first becomes
     * available, and the year that happens, such as {@code "Becomes Experimental (3058)"}.
     *
     * @param entity         The unit being reported on
     * @param techLevel      The unit's composite tech level
     * @param evaluationYear The year the variable tech level is evaluated in
     *
     * @return The variable tech level line for the report
     */
    private static String variableTechLevelText(Entity entity, RecordingCompositeTechLevel techLevel,
          int evaluationYear) {
        int firstAvailableYear = techLevel.firstAvailableYearFrom(evaluationYear);
        if (firstAvailableYear == evaluationYear) {
            return formatLevel(entity.getSimpleLevel(evaluationYear));
        }
        if (firstAvailableYear == ITechnology.DATE_NONE) {
            return message("resultExtinctNoReturn");
        }
        SimpleTechLevel levelWhenAvailable = entity.getSimpleLevel(firstAvailableYear);
        return message("resultBecomesAvailable", formatLevel(levelWhenAvailable), String.valueOf(firstAvailableYear));
    }

    private static String renderHtml(ReportData data) {
        String year = String.valueOf(data.evaluationYear());
        StringBuilder html = new StringBuilder("<div class='report'>");

        appendTitle(html, message("title"));
        html.append("<b>").append(escape(data.unitName())).append("</b>");

        html.append("<table cellpadding='2' cellspacing='0'>");
        appendHtmlSettingRow(html, message("labelTechBase"), data.techBase());
        appendHtmlSettingRow(html, message("labelIntroductionYear"), String.valueOf(data.introductionYear()));
        appendHtmlSettingRow(html, message("labelEvaluationYear"), year);
        appendHtmlSettingRow(html, message("labelTechLevelRule"), data.techLevelRule());
        if (data.factionName() != null) {
            appendHtmlSettingRow(html, message("labelFaction"), data.factionName());
        }
        html.append("</table>");

        appendSectionHeader(html, message("sectionComponentProgression"));
        html.append(TABLE_START);
        // The component's own dates and the level it has in the evaluated year all belong to the Variable Tech
        // Level rule and are grouped under it. The Static level does not depend on the year, so it gets its own
        // heading over its single column, merged down over both header rows to sit alongside the group.
        html.append("<tr>");
        html.append("<th rowspan='2' align='left' valign='bottom'><u>").append(escape(message("columnComponent")))
              .append("</u></th>");
        html.append("<th colspan='6' align='center'>").append(escape(message("groupVariable"))).append("</th>");
        html.append("<th rowspan='2' align='left' valign='bottom'><u>").append(escape(message("groupStatic")))
              .append("</u></th>");
        html.append("</tr>");
        html.append("<tr>");
        appendHtmlHeaderCell(html, message("columnPrototype"));
        appendHtmlHeaderCell(html, message("columnProduction"));
        appendHtmlHeaderCell(html, message("columnCommon"));
        appendHtmlHeaderCell(html, message("columnExtinct"));
        appendHtmlHeaderCell(html, message("columnReturns"));
        appendHtmlHeaderCell(html, message("columnVariableInYear", year));
        html.append("</tr>");
        for (ReportRow row : data.rows()) {
            html.append("<tr>");
            appendHtmlCell(html, row.displayName());
            appendHtmlCell(html, row.prototypeDate());
            appendHtmlCell(html, row.productionDate());
            appendHtmlCell(html, row.commonDate());
            appendHtmlCell(html, row.extinctionDate());
            appendHtmlCell(html, row.reintroductionDate());
            appendHtmlCell(html, row.variableTechLevel());
            appendHtmlCell(html, row.staticTechLevel());
            html.append("</tr>");
        }
        html.append("</table>");

        appendSectionHeader(html, message("sectionUnitProgression"));
        html.append(TABLE_START);
        html.append("<tr>");
        appendHtmlHeaderCell(html, message("columnComponent"));
        appendHtmlHeaderCell(html, message("columnEquipmentPrototype"));
        appendHtmlHeaderCell(html, message("columnEquipmentProduction"));
        appendHtmlHeaderCell(html, message("columnEquipmentCommon"));
        appendHtmlHeaderCell(html, message("columnEquipmentExtinct"));
        appendHtmlHeaderCell(html, message("columnEquipmentReturns"));
        appendHtmlHeaderCell(html, "");
        html.append("</tr>");
        for (ReportRow row : data.rows()) {
            html.append("<tr>");
            appendHtmlCell(html, row.displayName());
            appendHtmlDateCell(html, row.equipmentPrototypeDate(), row.movesEquipmentPrototypeDate());
            appendHtmlDateCell(html, row.equipmentProductionDate(), row.movesEquipmentProductionDate());
            appendHtmlDateCell(html, row.equipmentCommonDate(), row.movesEquipmentCommonDate());
            appendHtmlDateCell(html, row.equipmentExtinctionDate(), row.movesEquipmentExtinctionDate());
            appendHtmlDateCell(html, row.equipmentReintroductionDate(), row.movesEquipmentReintroductionDate());
            String progressionNote = row.progressionNote();
            if (progressionNote.isEmpty()) {
                html.append("<td></td>");
            } else {
                html.append("<td><b><span class='warning'>").append(escape(progressionNote))
                      .append("</span></b></td>");
            }
            html.append("</tr>");
        }
        html.append("</table>");

        appendSectionHeader(html, message("sectionUnitResult"));
        html.append("<table cellpadding='2' cellspacing='0'>");
        appendHtmlSettingRow(html, message("resultStaticTechLevel"), data.staticTechLevel());
        appendHtmlSettingRow(html, message("resultVariableTechLevelInYear", year),
              data.variableTechLevel());
        appendHtmlSettingRow(html, message("columnPrototype"), data.prototypeRange());
        appendHtmlSettingRow(html, message("columnProduction"), data.productionRange());
        appendHtmlSettingRow(html, message("columnCommon"), data.commonRange());
        appendHtmlSettingRow(html, message("columnExtinct"), data.extinctionRange());
        html.append("<tr><td><b>").append(escape(message("resultEffectiveTechLevel")))
              .append("</b></td><td>&nbsp;&nbsp;</td><td><b>")
              .append(escape(data.effectiveTechLevel()))
              .append("</b></td></tr>");
        html.append("</table>");

        return html.append("</div>").toString();
    }

    /**
     * Appends the report's main title. Swing gives {@code <h2>} a fixed size that ends up smaller than the report's
     * body font once it is scaled for the GUI, so the size is set relative to the body font instead.
     *
     * @param html The document being built
     * @param text The title text
     */
    private static void appendTitle(StringBuilder html, String text) {
        html.append("<p><font size='+3'><b>").append(escape(text)).append("</b></font></p>");
    }

    /**
     * Appends a section heading, sized relative to the body font for the same reason as {@link #appendTitle}.
     *
     * @param html The document being built
     * @param text The heading text
     */
    private static void appendSectionHeader(StringBuilder html, String text) {
        html.append("<p><font size='+2'><b>").append(escape(text)).append("</b></font></p>");
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
            html.append("<td><b><span class='warning'>").append(escape(date)).append("</span></b></td>");
        } else {
            html.append("<td>").append(escape(date)).append("</td>");
        }
    }

    private static String renderPlainText(ReportData data) {
        String year = String.valueOf(data.evaluationYear());
        StringBuilder text = new StringBuilder();
        text.append(message("title")).append(" - ").append(data.unitName()).append('\n');

        List<String[]> settingsTable = new ArrayList<>();
        settingsTable.add(new String[] { withColon("labelTechBase"), data.techBase() });
        settingsTable.add(new String[] { withColon("labelIntroductionYear"),
                                         String.valueOf(data.introductionYear()) });
        settingsTable.add(new String[] { withColon("labelEvaluationYear"), year });
        settingsTable.add(new String[] { withColon("labelTechLevelRule"), data.techLevelRule() });
        if (data.factionName() != null) {
            settingsTable.add(new String[] { withColon("labelFaction"), data.factionName() });
        }
        text.append(formatTextTable(settingsTable, null));

        List<String[]> componentTable = new ArrayList<>();
        componentTable.add(new String[] { message("columnComponent"), message("columnPrototype"),
                                          message("columnProduction"), message("columnCommon"),
                                          message("columnExtinct"), message("columnReturns"),
                                          message("columnVariableInYear", year), message("groupStatic") });
        for (ReportRow row : data.rows()) {
            componentTable.add(new String[] { row.displayName(), row.prototypeDate(), row.productionDate(),
                                              row.commonDate(), row.extinctionDate(), row.reintroductionDate(),
                                              row.variableTechLevel(), row.staticTechLevel() });
        }
        text.append('\n').append(message("sectionComponentProgression")).append('\n')
              .append(formatTextTable(componentTable, new ColumnGroup(message("groupVariable"), 1, 6)));

        List<String[]> buildUpTable = new ArrayList<>();
        buildUpTable.add(new String[] { message("columnComponent"), message("columnEquipmentPrototype"),
                                        message("columnEquipmentProduction"), message("columnEquipmentCommon"),
                                        message("columnEquipmentExtinct"), message("columnEquipmentReturns"), "" });
        for (ReportRow row : data.rows()) {
            buildUpTable.add(new String[] { row.displayName(), row.equipmentPrototypeDate(), row.equipmentProductionDate(),
                                            row.equipmentCommonDate(), row.equipmentExtinctionDate(),
                                            row.equipmentReintroductionDate(), row.progressionNote() });
        }
        text.append('\n').append(message("sectionUnitProgression")).append('\n')
              .append(formatTextTable(buildUpTable, null));

        List<String[]> resultTable = new ArrayList<>();
        resultTable.add(new String[] { withColon("resultStaticTechLevel"), data.staticTechLevel() });
        resultTable.add(new String[] { message("resultVariableTechLevelInYear", year) + ":",
                                       data.variableTechLevel() });
        resultTable.add(new String[] { withColon("columnPrototype"), data.prototypeRange() });
        resultTable.add(new String[] { withColon("columnProduction"), data.productionRange() });
        resultTable.add(new String[] { withColon("columnCommon"), data.commonRange() });
        resultTable.add(new String[] { withColon("columnExtinct"), data.extinctionRange() });
        resultTable.add(new String[] { withColon("resultEffectiveTechLevel"), data.effectiveTechLevel() });
        text.append('\n').append(message("sectionUnitResult")).append('\n')
              .append(formatTextTable(resultTable, null));

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
        String baseName = entity.isClan() ? message("techBaseClan") : message("techBaseInnerSphere");
        return entity.isMixedTech() ? message("techBaseMixed", baseName) : baseName;
    }

    private static String formatDate(int date) {
        return switch (date) {
            case ITechnology.DATE_NONE -> NO_DATE;
            case ITechnology.DATE_PS -> "PS";
            case ITechnology.DATE_ES -> "ES";
            default -> String.valueOf(date);
        };
    }

    /**
     * The composite prints an empty range as a single dash; the report uses a double dash for every other empty cell,
     * so this brings the two into line.
     *
     * @param range A date range from the composite tech level
     *
     * @return The range, with an empty range shown as "--"
     */
    private static String normalizeRange(String range) {
        return "-".equals(range) ? NO_DATE : range;
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
