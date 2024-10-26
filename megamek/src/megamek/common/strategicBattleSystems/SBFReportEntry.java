/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.strategicBattleSystems;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Report;
import megamek.common.ReportEntry;
import megamek.common.Roll;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static megamek.client.ui.swing.util.UIUtil.hexColor;

public class SBFReportEntry implements ReportEntry {

    record DataEntry(String data, boolean isObscured) implements Serializable {
    }

    protected static final String CSS_HEADER = "header";

    private final int messageId;
    private final List<DataEntry> data = new ArrayList<>();
    private boolean endLine = true;
    private boolean endSpace = false;
    private int indentation = 0;

    public SBFReportEntry(int messageId) {
        this.messageId = messageId;
    }

    public static void setupStylesheet(JTextPane pane) {
        pane.setContentType("text/html");
        StyleSheet styleSheet = ((HTMLEditorKit) pane.getEditorKit()).getStyleSheet();
        setupStylesheet(styleSheet);
    }

    public static void setupStylesheet(StyleSheet styleSheet) {
        GUIPreferences GUIP = GUIPreferences.getInstance();
        int size = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);

        styleSheet.addRule("pre { font-family:Noto Sans; font-size:" + size + "pt; }");
        styleSheet.addRule("a { color: " + hexColor(GUIP.getReportLinkColor()) + " }");
        styleSheet.addRule("span.warning { color: " + hexColor(GUIP.getWarningColor()) + " }");
        styleSheet.addRule("span.success { color: " + hexColor(GUIP.getReportSuccessColor()) + " }");
        styleSheet.addRule("span.miss { color: " + hexColor(GUIP.getReportMissColor()) + " }");
        styleSheet.addRule(".roll { font-weight:bold; }");
        styleSheet.addRule(".dice { font-family:Noto Sans Symbols 2; }");
        styleSheet.addRule(".header { font-weight:bold; font-size: "
                + (int) (1.2 * size) + "pt; padding:10 0; margin:0; text-decoration: underline; }");
        styleSheet.addRule("span.info { color: " + hexColor(GUIP.getReportInfoColor()) + " }");
    }

    // public static String styles() {
    // float base = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);
    // int labelSize = (int) (0.8 * base);
    // int valueSize = (int) (1.1 * base);
    // int nameSize = (int) (1.3 * base);
    //
    // return ".value { font-family:Exo; font-size:20; }" +
    // ".label { font-family:Noto Sans; font-size:" + labelSize + "; color:gray; }"
    // +
    // ".idnum { font-family:Exo; font-size:" + labelSize + "; color:gray;
    // text-align:right; }" +
    // ".valuecell { padding-right:10; font-family:Exo; font-size:" + valueSize + ";
    // text-align: center; }" +
    // ".armornodmg { font-family:Exo; font-size:" + valueSize + "; text-align:
    // center; }" +
    // ".valuedmg { font-family:Exo; font-size:" + valueSize + "; text-align:
    // center; color: #FAA; }" +
    // ".valuedeemph { font-family:Exo; font-size:" + labelSize + "; color:gray; }"
    // +
    // ".pvcell { font-family:Exo; font-size:" + nameSize + "; text-align: right; }"
    // +
    // ".speccell { font-family:Exo; font-size:" + labelSize + "; }" +
    // ".fullwidth { width:100%; }" +
    // ".formation { font-family:Noto Sans; font-size:" + nameSize + "; }" +
    // "th, td { padding:0 2; }";
    // }

    /**
     * Add the given int to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report. The order in which items are added must
     * match the order of the tags in the report text.
     *
     * @param data the int to be substituted
     * @return This Report to allow chaining
     */
    public SBFReportEntry add(int data) {
        return add(String.valueOf(data), true);
    }

    /**
     * Add the given int to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report, and mark it as double-blind sensitive
     * information if <code>obscure</code> is true. The order in which items
     * are added must match the order of the tags in the report text.
     *
     * @param data    the int to be substituted
     * @param obscure boolean indicating whether the data is double-blind
     *                sensitive
     * @return This Report to allow chaining
     */
    public SBFReportEntry add(int data, boolean obscure) {
        return add(String.valueOf(data), obscure);
    }

    /**
     * Add the given String to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report. The order in which items are added must
     * match the order of the tags in the report text.
     *
     * @param data the String to be substituted
     * @return This Report to allow chaining
     */
    public SBFReportEntry add(String data) {
        return add(data, true);
    }

    /**
     * Add the given String to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report, and mark it as double-blind sensitive
     * information if <code>obscure</code> is true. The order in which items
     * are added must match the order of the tags in the report text.
     *
     * @param data    the String to be substituted
     * @param obscure boolean indicating whether the data is double-blind
     *                sensitive
     * @return This Report to allow chaining
     */
    public SBFReportEntry add(String data, boolean obscure) {
        this.data.add(new DataEntry(data, obscure));
        return this;
    }

    @Override
    public final String text() {
        return " ".repeat(indentation) + reportText() + lineEnd();
    }

    @Override
    public ReportEntry addRoll(Roll roll) {
        return this;
    }

    /**
     * Indent the report. Equivalent to calling {@link #indent(int)} with a
     * parameter of 1.
     * 
     * @return This Report to allow chaining
     */
    public SBFReportEntry indent() {
        return indent(1);
    }

    /**
     * Indent the report n times.
     *
     * @param n the number of times to indent the report
     * @return This Report to allow chaining
     */
    public SBFReportEntry indent(int n) {
        indentation += (n * Report.DEFAULT_INDENTATION);
        return this;
    }

    public SBFReportEntry subject(int id) {
        return this;
    }

    public SBFReportEntry newLines(int count) {
        return this;
    }

    public SBFReportEntry noNL() {
        endLine = false;
        return this;
    }

    public SBFReportEntry endSpace() {
        endSpace = true;
        return this;
    }

    public SBFReportEntry addNL() {
        endLine = true;
        return this;
    }

    private String lineEnd() {
        return (endSpace ? " " : "") + (endLine ? "<BR>" : "");
    }

    protected String reportText() {
        return SBFReportMessages.getString(String.valueOf(messageId), data.stream().map(d -> (Object) d.data).toList());
    }
}
