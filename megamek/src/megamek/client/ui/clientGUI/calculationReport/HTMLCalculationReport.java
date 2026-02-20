/*
 * Copyright (C) 2022-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.calculationReport;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JEditorPane;

/**
 * This is a Calculation Report that builds its output as an HTML text which can be obtained from toString() as a String
 * or from toJComponent() inside a JEditorPane.
 */
public class HTMLCalculationReport implements CalculationReport {

    private record ReportLine(String content1, String content2, String content3, LineType lineType) {

    }

    public final static String HTML_START = "<HTML><BODY>";
    private final static String HTML_END = "</BODY></HTML>";
    private final static String TABLE_START = "<TABLE CELLPADDING=0 CELLSPACING=0 BORDER=0>";
    private final static String TABLE_END = "</TABLE>";

    private final static String ROW_START = "<TR>";
    private final static String ROW_END = "</TR>";
    private final static String COL_START = "<TD>";
    private final static String COL_RESULT_START = "<TD ALIGN=RIGHT>";
    private final static String COL_HEADER_START = "<TD COLSPAN=6>";
    private final static String COL_END = "</TD>";
    private final static String COLSPAN_START = "<TD COLSPAN=6>";
    private final static String COL_SPACER = "<TD>&nbsp;&nbsp;&nbsp;&nbsp;</TD>";
    private final static String LINE_START_SPACER = "<TD>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</TD>";

    private final static String HEADER_START = "<FONT SIZE=+2><U>";
    private final static String HEADER_END = "</U></FONT>";
    private final static String SUBHEADER_START = "";
    private final static String SUBHEADER_END = "";

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

    /** Tentative Section lines are kept in their own list. */
    private final List<ReportLine> tentativeLines = new ArrayList<>();

    private boolean tentativeSectionActive = false;

    @Override
    public void startTentativeSection() {
        tentativeSectionActive = true;
    }

    @Override
    public void endTentativeSection() {
        tentativeSectionActive = false;
        for (ReportLine line : tentativeLines) {
            switch (line.lineType) {
                case LINE:
                    addLine(line.content1, line.content2, line.content3);
                    break;
                case HEADER:
                    addHeader(line.content1);
                    break;
                case SUBHEADER:
                    addSubHeader(line.content1);
                    break;
                case RESULT_LINE:
                    addResultLine(line.content1, line.content2, line.content3);
                    break;
                default:
            }
        }
        tentativeLines.clear();
    }

    @Override
    public void discardTentativeSection() {
        tentativeSectionActive = false;
        tentativeLines.clear();
    }

    @Override
    public CalculationReport addResultLine(String type, String calculation, String result) {
        if (tentativeSectionActive) {
            tentativeLines.add(new ReportLine(type, calculation, result, LineType.RESULT_LINE));
            return this;
        } else {
            report.append(ROW_START);
            report.append(COL_START).append(COL_END);
            report.append(COL_START).append(COL_END);
            report.append(COL_START).append(COL_END);
            report.append(COL_START).append(COL_END);
            report.append(COL_START).append(COL_END);
            report.append(COL_START).append(COL_END);
            report.append(COL_RESULT_START).append("<HR>").append(COL_END);
            report.append(ROW_END);
            return addLine(type, calculation, result);
        }
    }

    @Override
    public CalculationReport addLine(String type, String calculation, String result) {
        if (tentativeSectionActive) {
            tentativeLines.add(new ReportLine(type, calculation, result, LineType.LINE));
        } else {
            report.append(ROW_START);
            report.append(COL_START).append(LINE_START_SPACER).append(COL_END);
            report.append(COL_START).append(type).append(COL_END);
            report.append(COL_SPACER);
            report.append(COL_START).append(calculation).append(COL_END);
            report.append(COL_SPACER);
            report.append(COL_RESULT_START).append(result).append(COL_END);
            report.append(ROW_END);
        }
        return this;
    }

    @Override
    public CalculationReport addSubHeader(String text) {
        if (tentativeSectionActive) {
            tentativeLines.add(new ReportLine(text, "", "", LineType.SUBHEADER));
        } else {
            report.append(ROW_START);
            report.append(COLSPAN_START).append(SUBHEADER_START).append(text).append(SUBHEADER_END).append(COL_END);
            report.append(ROW_END);
        }
        return this;
    }

    @Override
    public CalculationReport addHeader(String text) {
        if (tentativeSectionActive) {
            tentativeLines.add(new ReportLine(text, "", "", LineType.HEADER));
        } else {
            report.append(ROW_START);
            report.append(COL_HEADER_START).append(HEADER_START).append(text).append(HEADER_END).append(COL_END);
            report.append(ROW_END);
        }
        return this;
    }

    @Override
    public String toString() {
        return report + TABLE_END + HTML_END;
    }
}
