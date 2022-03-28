/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.calculationReport;

import javax.swing.*;

/**
 * This is a Calculation Report that builds its output as an HTML text which can be obtained
 * from toString() as a String or from toJComponent() inside a JEditorPane.
 */
public class HTMLCalculationReport implements CalculationReport {

    private final static String HTML_START = "<HTML><BODY>";
    private final static String HTML_END = "</BODY></HTML>";
    private final static String TABLE_START = "<TABLE CELLPADDING=0 CELLSPACING=0 BORDER=0>";
    private final static String TABLE_END = "</TABLE>";

    private final static String ROW_START = "<TR>";
    private final static String ROW_END = "</TR>";
    private final static String COL_START = "<TD>";
    private final static String COL_RESULT_START = "<TD ALIGN=RIGHT>";
    private final static String COL_HEADER_START = "<TD COLSPAN=6 ALIGN=CENTER>";
    private final static String COL_END = "</TD>";
    private final static String COLSPAN_START = "<TD COLSPAN=6>";
    private final static String COL_SPACER = "<TD>&nbsp;&nbsp;&nbsp;&nbsp;</TD>";

    private final static String HEADER_START = "<H2>";
    private final static String HEADER_END = "</H2>";
    private final static String SUBHEADER_START = "<U>";
    private final static String SUBHEADER_END = "</U>";

    private final StringBuilder report = new StringBuilder();

    public HTMLCalculationReport() {
        report.append(HTML_START).append(TABLE_START);
    }

    @Override
    public JComponent toJComponent() {
        final JEditorPane editorPane = new JEditorPane("text/html", this.toString());
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);
        return editorPane;
    }

    @Override
    public CalculationReport addResultLine(String type, String calculation, String result) {
        report.append(ROW_START);
        report.append(COL_START).append(COL_END);
        report.append(COL_START).append(COL_END);
        report.append(COL_START).append(COL_END);
        report.append(COL_START).append(COL_END);
        report.append(COL_START).append(COL_END);
        report.append(COL_RESULT_START).append("<HR>").append(COL_END);
        report.append(ROW_END);
        return addLine(type, calculation, result);
    }

    @Override
    public CalculationReport addLine(String type, String calculation, String result) {
        report.append(ROW_START);
        report.append(COL_START).append("&nbsp;&nbsp;").append(COL_END);
        report.append(COL_START).append(type).append(COL_END);
        report.append(COL_SPACER);
        report.append(COL_START).append(calculation).append(COL_END);
        report.append(COL_SPACER);
        report.append(COL_RESULT_START).append(result).append(COL_END);
        report.append(ROW_END);
        return this;
    }

    @Override
    public CalculationReport addSubHeader(String text) {
        report.append(ROW_START);
        report.append(COLSPAN_START).append(SUBHEADER_START).append(text).append(SUBHEADER_END).append(COL_END);
        report.append(ROW_END);
        return this;
    }

    @Override
    public CalculationReport addHeader(String text) {
        report.append(ROW_START);
        report.append(COL_HEADER_START).append(HEADER_START).append(text).append(HEADER_END).append(COL_END);
        report.append(ROW_END);
        addEmptyLine();
        return this;
    }

    @Override
    public String toString() {
        return report + TABLE_END + HTML_END;
    }
}
