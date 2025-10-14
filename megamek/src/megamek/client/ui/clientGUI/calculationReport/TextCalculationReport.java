/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JEditorPane;

import megamek.MMConstants;

/**
 * This is a Calculation Report that builds its output as a monospace-formatted pure text String. The report can be
 * obtained from toString() as a String or from toJComponent() inside a JEditorPane.
 */
public class TextCalculationReport implements CalculationReport {

    private final static int LINE_START_SPACER = 3;

    private record ReportLine(String content1, String content2, String content3, LineType lineType) {

        int getWidth() {
            if (lineType == LineType.LINE) {
                return content1.length() + content2.length() + content3.length() + 3 * LINE_START_SPACER;
            } else if ((lineType == LineType.HEADER) || (lineType == LineType.SUBHEADER)) {
                return content1.length();
            } else {
                return 0;
            }
        }

        String getContent(int column) {
            if (column == 1) {
                return content1;
            } else if (column == 2) {
                return content2;
            } else {
                return content3;
            }
        }
    }

    /**
     * In this report type the lines must be buffered as the text column count can only be found when all lines are in.
     */
    private final List<ReportLine> reportLines = new ArrayList<>();

    /** Tentative Section lines are kept in their own list. */
    private final List<ReportLine> tentativeLines = new ArrayList<>();

    private boolean tentativeSectionActive = false;


    @Override
    public void startTentativeSection() {
        tentativeSectionActive = true;
    }

    @Override
    public void endTentativeSection() {
        reportLines.addAll(tentativeLines);
        tentativeLines.clear();
        tentativeSectionActive = false;
    }

    @Override
    public void discardTentativeSection() {
        tentativeLines.clear();
        tentativeSectionActive = false;
    }

    private List<ReportLine> listToWrite() {
        return tentativeSectionActive ? tentativeLines : reportLines;
    }

    @Override
    public CalculationReport addLine(String type, String calculation, String result) {
        listToWrite().add(new ReportLine(type, calculation, result, LineType.LINE));
        return this;
    }

    @Override
    public CalculationReport addSubHeader(String text) {
        listToWrite().add(new ReportLine(text, "", "", LineType.SUBHEADER));
        return this;
    }

    @Override
    public CalculationReport addHeader(String text) {
        listToWrite().add(new ReportLine(text, "", "", LineType.HEADER));
        return this;
    }

    @Override
    public CalculationReport addResultLine(String type, String calculation, String result) {
        listToWrite().add(new ReportLine("", "", "", LineType.RESULT_LINE));
        addLine(type, calculation, result);
        return this;
    }

    @Override
    public JComponent toJComponent() {
        final JEditorPane editorPane = new JEditorPane("text/plain", this.toString());
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);
        editorPane.setFont(new Font(MMConstants.FONT_MONOSPACED, Font.PLAIN, editorPane.getFont().getSize()));
        return editorPane;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        int width = requiredColumns();
        int resultWidth = maxWidth(3);
        int calcBegin = calculationBeginColumn();
        for (ReportLine line : reportLines) {
            if (line.lineType == LineType.LINE) {
                result.append(" ".repeat(LINE_START_SPACER)).append(line.content1);
                result.append(" ".repeat(calcBegin - line.content1.length() - LINE_START_SPACER)).append(line.content2);
                result.append(" ".repeat(width - line.content3.length() - calcBegin - line.content2.length()))
                      .append(line.content3);
            } else if (line.lineType == LineType.SUBHEADER) {
                result.append(line.content1);
            } else if (line.lineType == LineType.HEADER) {
                result.append(line.content1).append(System.lineSeparator());
                result.append("-".repeat(line.content1.length()));
            } else {
                result.append(" ".repeat(width - resultWidth)).append("-".repeat(resultWidth));
            }
            result.append(System.lineSeparator());
        }
        return result.toString();
    }

    private int maxWidth(int column) {
        return reportLines.stream()
              .filter(l -> l.lineType == LineType.LINE)
              .map(l -> l.getContent(column))
              .mapToInt(String::length)
              .max().orElse(1);
    }

    private int requiredColumns() {
        int lineMaxWidth = maxWidth(1) + maxWidth(2) + maxWidth(3) + 3 * LINE_START_SPACER;
        int headerMaxWidth = reportLines.stream()
              .filter(l -> (l.lineType == LineType.HEADER) || (l.lineType == LineType.SUBHEADER))
              .mapToInt(ReportLine::getWidth)
              .max().orElse(1);
        return Math.max(lineMaxWidth, headerMaxWidth);
    }

    private int calculationBeginColumn() {
        return 2 * LINE_START_SPACER + reportLines.stream()
              .filter(l -> l.lineType == LineType.LINE)
              .map(l -> l.content1)
              .mapToInt(String::length)
              .max().orElse(1);
    }
}
