/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.util.UIUtil.uiGray;

import java.awt.Color;
import java.awt.Font;
import java.io.Serial;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.interfaces.ReportEntry;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Report encapsulates a single game event report in MegaMek.
 * <p>
 * Each Report contains message text, formatting information, visibility settings, and the data needed to display the
 * report to players. Reports are typically created by the server, then transmitted to clients for display.
 * <p>
 * The actual text of reports comes from the {@code report-messages.properties} resource file. Each report is identified
 * by a numeric ID that corresponds to a message template in this file. The template can contain tags that will be
 * replaced with data provided by the Report object:
 * <ul>
 *   <li>{@code <data>} - Replaced with values added via {@link #add(String)} or similar methods</li>
 *   <li>{@code <msg:id1,id2>} - Conditionally shows one of two messages based on a boolean value</li>
 *   <li>{@code <list>} - Lists all remaining data values, comma-separated</li>
 *   <li>{@code <newline>} - Inserts a line break</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * // Create a report with ID 3455
 * Report r = Report.subjectReport(3455, entity.getId());
 * r.indent()
 *  .addDesc(entity)
 *  .add(6)
 *  .choose(true);
 * vPhaseReport.addElement(r);
 * </pre>
 * <p>
 * The corresponding entry in report-messages.properties might be:
 * <pre>
 * 3455::&lt;data&gt; (&lt;data&gt;) does &lt;data&gt; damage to the &lt;msg:3456,3457&gt;.
 * 3456::tank
 * 3457::building
 * </pre>
 * <p>
 * This would produce output like: "Crusader (Bob) does 6 damage to the tank."
 * <p>
 * Reports can be public (visible to all) or hidden based on subject entity or player.
 * They support HTML formatting, entity links, tooltips, and other rich text features.
 *
 * @author Ryan McConnell (oscarmm)
 */
public class Report implements ReportEntry {
    private static final MMLogger logger = MMLogger.create(Report.class);

    /*
     * Note: some fields are marked transient because they are only used by the
     * server (or only the client). This shaves a few bytes off the packet size,
     * helping the dial-up people :)
     */

    @Serial
    private static final long serialVersionUID = -5586008091586682078L;

    private static final int MESSAGE_NONE = -1;

    /**
     * Report Type: visible to all players.
     */
    public static final int PUBLIC = 0;

    /**
     * Report Type: visible to all players, but all data marked for obscuration remains hidden. Note: Not used at this
     * time, since all reports are considered <code>obscured</code> unless explicitly marked
     * <code>public</code>.
     */
    public static final int OBSCURED = 1;

    /**
     * Report is only visible to those players who can see the subject. Note: Not used at this time, since all reports
     * are considered
     * <code>obscured</code> unless explicitly marked <code>public</code>.
     */
    public static final int HIDDEN = 2;

    /**
     * Testing only - remove me later.
     */
    public static final int TESTING = 3;

    /**
     * Messages which should be sent only to the player indicated by "player"
     */
    public static final int PLAYER = 4;

    /**
     * The string that appears in the report to obscure certain information.
     */
    public static final String OBSCURED_STRING = "????";

    /**
     * Number of spaces to use per indentation level.
     */
    public static final int DEFAULT_INDENTATION = 8; // was 4 previously

    /**
     * Number of indentation levels allowed. Currently, the same as the DEFAULT_INDENTATION value, to limit indentations
     * to one level for a cleaner look of the report.
     */
    public static final int MAX_INDENTATION = 8;

    /**
     * Prefix for entity hyperlinks
     */
    public static final String ENTITY_LINK = "#entity:";
    /**
     * Prefix for tooltip text
     */
    public static final String TOOLTIP_LINK = "#tooltip:";

    /**
     * Required - associates this object with its text.
     */
    public int messageId = Report.MESSAGE_NONE;

    /**
     * The number of spaces this report should be indented.
     */
    private int indentation = 0;

    /**
     * The number of newlines to add at the end of this report. Defaults to one.
     */
    public int newlines = 1;

    /**
     * The data values to fill in the report with.
     */
    private Vector<String> tagData = new Vector<>();

    /**
     * How to translate the tagData or not at all.
     */
    private String tagTranslate = null;

    /**
     * How this report is handled when double-blind play is in effect. See constants below for more details.
     */
    public transient int type = Report.HIDDEN;

    /**
     * The entity this report concerns, if applicable. If this is left blank, then the report will be considered
     * <code>public</code>.
     */
    public transient int subject = Entity.NONE;

    /**
     * The player this report concerns, if applicable. This should be filled in if this report is not public and still
     * does not belong to a specific visible entity
     */
    public transient int player = Player.PLAYER_NONE;

    public static int HIDDEN_ENTITY_NUM = -1;

    /**
     * This hash table will store the tagData Vector indexes that are supposed to be obscured before sending to clients.
     * This only applies when the report type is "obscured".
     */
    private Hashtable<Integer, Boolean> obscuredIndexes = new Hashtable<>();

    /**
     * Vector to store the player names of those who received an obscured version of this report. Used to reconstruct
     * individual client's reports from the master copy stored by the server.
     */
    private Vector<String> obscuredRecipients = new Vector<>();

    /**
     * Keep track of what data we have already substituted for tags.
     */
    private transient int tagCounter = 0;

    /**
     * bool for determining when code should be used to show image.
     */
    private transient boolean showImage = false;

    /**
     * string to add to reports to show sprites
     **/
    private String imageCode = "";

    /**
     * Default constructor, note that using this means the
     * <code>messageId</code> field must be explicitly set.
     */
    public Report() {
    }

    /**
     * Create a new report associated with the given report text.
     *
     * @param id the int value of the report from <i>report-messages.properties
     *           </i>
     */
    public Report(int id) {
        messageId = id;
    }

    /**
     * Create a new report associated with the given report text and having the given type.
     *
     * @param id   the int value of the report from report-messages.properties
     * @param type the constant specifying the visibility of the report (PUBLIC, OBSCURED, or HIDDEN)
     */
    public Report(int id, int type) {
        messageId = id;
        this.type = type;
    }

    /**
     * Create a new report which is an exact copy of the given report.
     *
     * @param r the report to be copied
     */
    @SuppressWarnings("unchecked")
    public Report(Report r) {
        messageId = r.messageId;
        indentation = r.indentation;
        newlines = r.newlines;
        tagData = (Vector<String>) r.tagData.clone();
        tagTranslate = r.tagTranslate;
        type = r.type;
        subject = r.subject;
        obscuredIndexes = (Hashtable<Integer, Boolean>) r.obscuredIndexes.clone();
        obscuredRecipients = (Vector<String>) r.obscuredRecipients.clone();
        tagCounter = r.tagCounter;
        imageCode = r.imageCode;
    }

    /**
     * Returns a new report associated with the given report text (ID) and having the type Report.PUBLIC.
     *
     * @param id the int value of the report from report-messages.properties
     *
     * @return A new Report
     */
    public static Report publicReport(int id) {
        return new Report(id, PUBLIC);
    }

    /**
     * Returns a new report associated with the given report text (ID) and having the given subject (Entity ID). The
     * Report will be the default type Report.HIDDEN.
     *
     * @param id        the int value of the report from report-messages.properties
     * @param subjectId The Entity ID of the subject entity
     *
     * @return A new Report
     */
    public static Report subjectReport(int id, int subjectId) {
        return new Report(id).subject(subjectId);
    }

    /**
     * Set the report to be public (Report.PUBLIC).
     *
     * @return This Report to allow chaining
     */
    public Report makePublic() {
        type = Report.PUBLIC;
        return this;
    }

    /**
     * Set the report to not add the given number of newlines at the end.
     *
     * @return This Report to allow chaining
     */
    public Report newLines(int newlines) {
        this.newlines = newlines;
        return this;
    }

    /**
     * Set the report to not add a newline at the end, so that the current line of text can be continued with another
     * report.
     *
     * @return This Report to allow chaining
     */
    public Report noNL() {
        return newLines(0);
    }

    /**
     * Set the report's subject (Entity ID).
     *
     * @return This Report to allow chaining
     */
    public Report subject(int subjectId) {
        subject = subjectId;
        return this;
    }

    /**
     * Set the report's subject and add its description. This is equivalent to calling
     * <pre>{@code
     * report.subject(entity.getId());
     * report.addDesc(entity);
     * }</pre>
     * Typically used in reports that start with {@literal "<data> (<data>) ..."}
     *
     * @return This Report to allow chaining
     */
    public Report with(Entity entity) {
        subject(entity.getId());
        addDesc(entity);
        return this;
    }

    /**
     * Add the given int to the list of data that will be substituted for the &lt;data&gt; tags in the report. The order
     * in which items are added must match the order of the tags in the report text.
     *
     * @param data the int to be substituted
     *
     * @return This Report to allow chaining
     */
    public Report add(int data) {
        return add(data, true);
    }

    /**
     * Add the given int to the list of data that will be substituted for the &lt;data&gt; tags in the report, and mark
     * it as double-blind sensitive information if <code>obscure</code> is true. The order in which items are added must
     * match the order of the tags in the report text.
     *
     * @param data    the int to be substituted
     * @param obscure boolean indicating whether the data is double-blind sensitive
     *
     * @return This Report to allow chaining
     */
    public Report add(int data, boolean obscure) {
        if (obscure) {
            obscuredIndexes.put(tagData.size(), Boolean.TRUE);
        }
        tagData.addElement(String.valueOf(data));
        return this;
    }

    /**
     * Add the given String to the list of data that will be substituted for the &lt;data&gt; tags in the report. The
     * order in which items are added must match the order of the tags in the report text.
     *
     * @param data the String to be substituted
     *
     * @return This Report to allow chaining
     */
    public Report add(String data) {
        add(data, true);
        tagTranslate = null;
        return this;
    }

    /**
     * Add the given string to the list of data that will be substituted for the &lt;data&gt; tags in the report. The
     * order in which items are added must match the order of the tags in the report text. The second string argument
     * sets the translation flag to the string value.
     *
     * @param data      the String to be substituted
     * @param translate the common Resource Bundle to be used for translation
     *
     * @return This Report to allow chaining
     */
    public Report add(String data, String translate) {
        add(data, true);
        tagTranslate = translate;
        return this;
    }

    /**
     * Add the given String to the list of data that will be substituted for the &lt;data&gt; tags in the report, and
     * mark it as double-blind sensitive information if <code>obscure</code> is true. The order in which items are added
     * must match the order of the tags in the report text.
     *
     * @param data    the String to be substituted
     * @param obscure boolean indicating whether the data is double-blind sensitive
     *
     * @return This Report to allow chaining
     */
    public Report add(String data, boolean obscure) {
        if (obscure) {
            obscuredIndexes.put(tagData.size(), Boolean.TRUE);
        }
        tagData.addElement(data);
        return this;
    }

    /**
     * Adds target roll to report with details available as a tooltip
     *
     * @param targetRoll the target roll
     *
     * @return This Report to allow chaining
     */
    public Report add(TargetRoll targetRoll) {
        addDataWithTooltip(targetRoll.getValueAsString(), targetRoll.getDesc());
        return this;
    }

    public Report add(Roll diceRoll) {
        return addDataWithTooltip(String.valueOf(diceRoll.getIntValue()), diceRoll.getReport());
    }

    public Report addDataWithTooltip(Integer data, String tooltip) {
        return addDataWithTooltip(String.valueOf(data), tooltip);
    }

    /**
     * Adds a field to the report with additional data available as a tooltip
     *
     * @param data    the data for the report field
     * @param tooltip the tooltip text
     *
     * @return This Report to allow chaining
     */
    public Report addDataWithTooltip(String data, String tooltip) {
        String tipFormat = "<a href='%s%s'>%s</a>";
        tagData.addElement(String.format(tipFormat, TOOLTIP_LINK, tooltip, data));
        return this;
    }

    /**
     * Indicate which of two possible messages should be substituted for the
     * <code>&lt;msg:<i>n</i>,<i>m</i>&gt;</code> tag. An argument of
     * <code>true</code> would select message <i>n</i> while an
     * argument of <code>false</code> would select <i>m</i>. In the future, this capability may be expanded to support
     * more than two choices.
     *
     * @param choice boolean indicating which message to substitute
     *
     * @return This Report to allow chaining
     */
    public Report choose(boolean choice) {
        tagData.addElement(String.valueOf(choice));
        return this;
    }

    public String span(String name, String text, String attributes) {
        return "<span class='" + name + "' " + attributes + ">" + text + "</span>";
    }

    /**
     * Shortcut method for adding entity name and owner data at the same time. Assumes that the entity name should be
     * obscured, but the owner should not.
     *
     * @param entity the entity you wish to add
     *
     * @return This Report to allow chaining
     */
    public Report addDesc(Entity entity) {
        if (entity != null) {
            if ((indentation <= Report.DEFAULT_INDENTATION) || showImage) {
                imageCode = "<span id='" + entity.getId() + "'></span>";
            }

            Player owner = entity.getOwner();
            Color ownerColor = (owner != null) ? owner.getColour().getColour() : uiGray();
            String ownerName = (owner != null) ? owner.getName() : ReportMessages.getString("report.unknownOwner");

            String unitName = href(ENTITY_LINK + entity.getId(), entity.getShortName());
            // Wrap unit name in span with class and data attribute
            unitName = span("entity-name", unitName, "data-entity-id='" + entity.getId() + "'");

            if ((entity.getCrew().getSize() >= 1) && !entity.getCrew().getNickname().isBlank()) {
                unitName += fgColor(ownerColor, ' ' + entity.getCrew().getNickname().toUpperCase());
            }

            add(unitName, true);
            add(bold(fgColor(ownerColor, ownerName)));
        }
        return this;
    }

    /**
     * Manually Toggle if the report should show an image of the entity
     */
    public void setShowImage(boolean showImage) {
        this.showImage = showImage;
    }

    public void obscureImg() {
        imageCode = "<span id='" + HIDDEN_ENTITY_NUM + "'></span>";
    }

    /**
     * Internal method. Not for typical use.
     * <p>
     * Tests whether the data value at the given index has been marked as obscured.
     *
     * @param index position of data value (indexes are chronological and start at zero)
     *
     * @return true if the data value was marked obscured
     */
    public boolean isValueObscured(int index) {
        return obscuredIndexes.get(index) != null;
    }

    /**
     * Internal method. Not for typical use.
     * <p>
     * Remove the data value from the report. This operation is irreversible.
     *
     * @param index position of data value (indexes are chronological and start at zero
     */
    public void hideData(int index) {
        tagData.setElementAt(null, index);
    }

    /**
     * Indent the report. Equivalent to calling {@link #indent(int)} with a parameter of 1.
     *
     * @return This Report to allow chaining
     */
    public Report indent() {
        return indent(1);
    }

    /**
     * Indent the report n times.
     *
     * @param n the number of times to indent the report
     *
     * @return This Report to allow chaining
     */
    public Report indent(int n) {
        indentation += (n * Report.DEFAULT_INDENTATION);
        return this;
    }

    /**
     * Internal method. Not for typical use.
     * <p>
     * Get the total number of data values associated with this report. Note that this includes the
     * <code>true/false</code> values added for &lt;msg&gt; tags as well.
     *
     * @return the number of data values
     */
    public int dataCount() {
        return tagData.size();
    }

    private String getTag() {
        return getTag(tagCounter);
    }

    private String getTag(int index) {
        try {
            String value = tagData.elementAt(index);
            if (value == null) {
                return Report.OBSCURED_STRING;
            } else if (tagTranslate != null) {
                // Each common Resource Bundle is found below
                if (tagTranslate.equals("Messages")) {
                    return Messages.getString(value);
                    // Others ifs will be here.
                }
            }
            return value;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error(
                  "Error: Report#getText --> Array Index out of Bounds Exception (index: {}) for a report with ID {}. Maybe Report#add wasn't called enough times for the amount of tags in the message?",
                  index,
                  messageId);
            return "[Reporting Error: see megamek.log for details]";
        }
    }

    /**
     * Get the report in its final form, with all the necessary substitutions made.
     *
     * @return a String with the final report
     */
    @Override
    public String text() {
        // The raw text of the message, with tags.
        String raw = ReportMessages.getString(String.valueOf(messageId));

        // This will be the finished product, with data substituted for tags.
        StringBuffer text = new StringBuffer();

        if (raw == null) {
            // Should we handle this better? Check alternate language files?
            logger.error("No message found for ID {}", messageId);
            text.append("[Reporting Error for message ID ").append(messageId).append("]");
        } else {
            int i = 0;
            int mark = 0;
            while (i < raw.length()) {
                if (raw.charAt(i) == '<') {
                    // find end of tag
                    int endTagIdx = raw.indexOf('>', i);
                    if ((raw.indexOf('<', i + 1) != -1) && (raw.indexOf('<', i + 1) < endTagIdx)) {
                        // hmm...this must be a literal '<' character
                        i++;
                        continue;
                    }
                    // copy the preceding characters into the buffer
                    text.append(raw, mark, i);
                    if (raw.substring(i + 1, endTagIdx).equals("data")) {
                        text.append(getTag());
                        tagCounter++;
                    } else if (raw.substring(i + 1, endTagIdx).equals("list")) {
                        for (int j = tagCounter; j < tagData.size(); j++) {
                            text.append(getTag(j)).append(", ");
                        }
                        text.setLength(text.length() - 2); // trim last comma
                    } else if (raw.substring(i + 1, endTagIdx).startsWith("msg:")) {
                        boolean selector = Boolean.parseBoolean(getTag());
                        if (selector) {
                            text.append(ReportMessages.getString(raw.substring(i + 5, raw.indexOf(',', i))));
                        } else {
                            text.append(ReportMessages.getString(raw.substring(raw.indexOf(',', i) + 1, endTagIdx)));
                        }
                        tagCounter++;
                    } else if (raw.substring(i + 1, endTagIdx).equals("newline")) {
                        text.append("<br>");
                    } else {
                        // not a special tag, so treat as literal text
                        text.append(raw, i, endTagIdx + 1);
                    }
                    mark = endTagIdx + 1;
                    i = endTagIdx;
                }
                i++;
            }

            if (indentation > MAX_INDENTATION) { // limit indentation for a cleaner look of the report
                indentation = MAX_INDENTATION;
            }

            // add the sprite code at the beginning of the line
            if (imageCode != null && !imageCode.isEmpty()) {
                if (text.toString().startsWith("<br>")) {
                    text.insert(4, imageCode);
                } else {
                    text.insert(0, imageCode);
                }
            }
            text.append(raw.substring(mark));
            handleIndentation(text);
            text.append(getNewlines());
        }
        tagCounter = 0;
        // debugReport
        if (type == Report.TESTING) {
            Report.mark(text);
        }

        String finalReport;
        if (messageId == 3100 || messageId == 3101 || messageId == 3102 || messageId == 4005) { // if new attack
            Color clr = new Color(0, 0, 0);

            // get attacker color
            Pattern clrRegex = Pattern.compile("#([A-Fa-f0-9]{6})");
            Matcher matcher = clrRegex.matcher(getTag(1));
            if (matcher.find()) {
                clr = Color.decode(matcher.group());
            }

            finalReport = "<div style='padding: 2px; background-color: rgba("
                  + clr.getRed()
                  + ","
                  + clr.getGreen()
                  + ","
                  + clr.getBlue()
                  + ","
                  + "0.15)'>"
                  + text
                  + "</div>";
            //shade lines of each attacker in its player color
        } else {
            finalReport = text.toString();
        }

        // Use span to keep reports inline - <br> tags handle line breaks
        return "<span class='report-entry'>" + finalReport + "</span>";
    }

    @Override
    public ReportEntry addRoll(Roll roll) {
        add(roll.getReport());
        return this;
    }

    private void handleIndentation(StringBuffer sb) {
        if ((indentation == 0) || (sb.isEmpty())) {
            return;
        }
        int i = 0;
        while (sb.substring(i, i + 4).equals("<br>")) {
            i += 4;
        }
        sb.insert(i, getSpaces());
    }

    private String getSpaces() {
        return "&nbsp;".repeat(Math.max(0, indentation));
    }

    private String getNewlines() {
        return "<br>".repeat(Math.max(0, newlines));
    }

    /**
     * Adds a newline to the last report in the given Vector.
     *
     * @param v a Vector of Report objects
     */
    public static void addNewline(Vector<Report> v) {
        if (v.isEmpty()) {
            // We can't add a new line to an empty report vector
            return;
        }

        try {
            v.elementAt(v.size() - 1).newlines++;
        } catch (Exception ex) {
            logger.error("Cannot add a new line", ex);
        }
    }

    public static void setupStylesheet(JTextPane pane) {
        pane.setContentType("text/html");
        StyleSheet styleSheet = ((HTMLEditorKit) pane.getEditorKit()).getStyleSheet();
        Report.setupStylesheet(styleSheet);
    }

    public static void setupStylesheet(StyleSheet styleSheet) {
        GUIPreferences GUIP = GUIPreferences.getInstance();
        Font font = new Font(GUIP.getReportFontType(), Font.PLAIN, UIUtil.FONT_SCALE1);
        int size = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);

        // styleSheet.addRule("html { text-align: left; }");
        styleSheet.addRule("div.report { font-family: "
              + font.getFamily()
              + "; font-size: "
              + size
              + "pt; font-style:normal;}");
        styleSheet.addRule("a { color: " + hexColor(GUIP.getReportLinkColor()) + " }");
        styleSheet.addRule("span.warning { color: " + hexColor(GUIP.getWarningColor()) + " }");
        styleSheet.addRule("span.success { color: " + hexColor(GUIP.getReportSuccessColor()) + " }");
        styleSheet.addRule("span.miss { color: " + hexColor(GUIP.getReportMissColor()) + " }");
        styleSheet.addRule("span.info { color: " + hexColor(GUIP.getReportInfoColor()) + " }");
        styleSheet.addRule("span.large { font-size: large; }");
        styleSheet.addRule("span.medium { font-size: medium; }");
        styleSheet.addRule("span.small { font-size: small; }");
        styleSheet.addRule("span.x-small { font-size: x-small; }");
        styleSheet.addRule("span.xx-small { font-size: xx-small; }");
    }

    /**
     * Wraps text in a span with the given class name.
     *
     * @param name The class name.
     * @param text The text to wrap.
     * @return The HTML string.
     */
    public String span(String name, String text) {
        return "<span class='" + name + "'>" + text + "</span>";
    }

    /**
     * Wraps text in a warning span.
     *
     * @param text The text to wrap.
     * @return The HTML string.
     */
    public String warning(String text) {
        return span("warning", text);
    }

    /**
     * Converts a Color object to a hex string.
     *
     * @param color The Color object.
     * @return The hex string (e.g., "#RRGGBB").
     */
    private static String hexColor(Color color) {
        return String.format("#%06x", color.getRGB() & 0x00FFFFFF);
    }

    /**
     * Wraps text in a span with the given foreground color.
     *
     * @param color The color to use.
     * @param str   The text to wrap.
     * @return The HTML string.
     */
    public String fgColor(Color color, String str) {
        return fgColor(hexColor(color), str);
    }

    /**
     * Wraps text in a span with the given hex foreground color.
     *
     * @param hexColor The hex color string (e.g., "#RRGGBB").
     * @param str      The text to wrap.
     * @return The HTML string.
     */
    public String fgColor(String hexColor, String str) {
        return "<span style='color:" + hexColor + "'>" + str + "</span>";
    }

    /**
     * Wraps text in a span with the given background color.
     *
     * @param color The color to use.
     * @param str   The text to wrap.
     * @return The HTML string.
     */
    public String bgColor(Color color, String str) {
        return bgColor(hexColor(color), str);
    }

    /**
     * Wraps text in a span with the given hex background color.
     *
     * @param hexColor The hex color string (e.g., "#RRGGBB").
     * @param str      The text to wrap.
     * @return The HTML string.
     */
    public String bgColor(String hexColor, String str) {
        return "<span style='background-color:" + hexColor + "'>" + str + "</span>";
    }

    /**
     * Wraps text in a bold tag.
     *
     * @param str The text to wrap.
     * @return The HTML string.
     */
    public static String bold(String str) {
        return "<B>" + str + "</B>";
    }

    /**
     * Creates an HTML anchor tag.
     *
     * @param href The URL.
     * @param str  The link text.
     * @return The HTML string.
     */
    public String href(String href, String str) {
        return "<a href='" + href + "'>" + str + "</a>";
    }

    /**
     * Internal method. Not for typical use.
     * <p>
     * Adds the given player name to the report's list of players who received an obscured version of this report from
     * the server at some time in the past.
     *
     * @param playerName the String containing the player's name
     */
    public void addObscuredRecipient(String playerName) {
        obscuredRecipients.addElement(playerName);
    }

    /**
     * Internal method. Not for typical use.
     * <p>
     * Tests whether the given player name is on the report's list of players who received an obscured version of this
     * report from the server at some time in the past.
     *
     * @param playerName the String containing the player's name
     *
     * @return true if the player was sent an obscured version of this report
     */
    public boolean isObscuredRecipient(String playerName) {
        for (int i = 0; i < obscuredRecipients.size(); i++) {
            String s = obscuredRecipients.elementAt(i);
            if (s.equals(playerName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Useful for debugging purposes.
     *
     * @return a String of the form "Report(messageId=n)"
     */
    @Override
    public String toString() {
        return "Report(messageId=" + messageId + ")";
    }

    // debugReport method
    private static StringBuffer mark(StringBuffer sb) {
        sb.insert(0, "<hidden>");
        sb.insert(sb.indexOf("<br>") + 4, "</hidden>");
        return sb;
    }

    /**
     * Sets the indentation for all reports of the given reports list to the given amount by calling
     * {@link #indent(int)}
     *
     * @param reports A list of reports to be affected
     * @param amount  The amount of indentation to give each report in the list
     */
    public static void indentAll(@Nullable Vector<Report> reports, int amount) {
        if (reports != null) {
            reports.forEach(report -> report.indent(amount));
        }
    }
}
