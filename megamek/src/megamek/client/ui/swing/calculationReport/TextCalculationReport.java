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
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a Calculation Report that builds its output as a monospace-formatted pure text
 * String. The report can be obtained from toString() as a String or from toJComponent()
 * inside a JEditorPane.
 */
public class TextCalculationReport implements CalculationReport {

    private final static int COL_SPACER = 3;

    private enum LineType {
        LINE, HEADER, SUBHEADER, RESULT_LINE
    }

    private static class ReportLine {

        ReportLine(String c1, String c2, String c3, LineType lt) {
            lineType = lt;
            content1 = c1;
            content2 = c2;
            content3 = c3;
        }

        final LineType lineType;
        final String content1;
        final String content2;
        final String content3;

        int getWidth() {
            if (lineType == LineType.LINE) {
                return content1.length() + content2.length() + content3.length() + 3 * COL_SPACER;
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
     * In this report type the lines must be buffered as the text column count can only be found
     * when all lines are in.
     */
    private final List<ReportLine> reportLines = new ArrayList<>();

    @Override
    public CalculationReport addLine(String type, String calculation, String result) {
        reportLines.add(new ReportLine(type, calculation, result, LineType.LINE));
        return this;
    }

    @Override
    public CalculationReport addSubHeader(String text) {
        reportLines.add(new ReportLine(text, "", "", LineType.SUBHEADER));
        return this;
    }

    @Override
    public CalculationReport addHeader(String text) {
        reportLines.add(new ReportLine(text, "", "", LineType.HEADER));
        return this;
    }

    @Override
    public CalculationReport addResultLine(String type, String calculation, String result) {
        addLine(type, calculation, result);
        reportLines.add(new ReportLine("", "", "", LineType.RESULT_LINE));
        return this;
    }

    @Override
    public JComponent toJComponent() {
        final JEditorPane editorPane = new JEditorPane("text/plain", this.toString());
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);
        editorPane.setFont(new Font("Monospaced", Font.PLAIN, editorPane.getFont().getSize()));
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
                result.append(" ".repeat(COL_SPACER)).append(line.content1);
                result.append(" ".repeat(calcBegin - line.content1.length() - COL_SPACER)).append(line.content2);
                result.append(" ".repeat(width - line.content3.length() - calcBegin - line.content2.length())).append(line.content3);
            } else if (line.lineType == LineType.SUBHEADER) {
                result.append(System.lineSeparator());
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
        int lineMaxWidth = maxWidth(1) + maxWidth(2) + maxWidth(3) + 3 * COL_SPACER;
        int headerMaxWidth = reportLines.stream()
                .filter(l -> (l.lineType == LineType.HEADER) || (l.lineType == LineType.SUBHEADER))
                .mapToInt(ReportLine::getWidth)
                .max().orElse(1);
        return Math.max(lineMaxWidth, headerMaxWidth);
    }

    private int calculationBeginColumn() {
        return 2 * COL_SPACER + reportLines.stream()
                .filter(l -> l.lineType == LineType.LINE)
                .map(l -> l.content1)
                .mapToInt(String::length)
                .max().orElse(1);
    }
}
