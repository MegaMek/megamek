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
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a Calculation Report that builds its output as an assembly of JLabels in a JPanel.
 * This is best suited to being displayed in a dialog as it knows how wide and high it needs to be
 * (better than an HTML report does). Its toString() method yields the HTML-coded report.
 */
public class SwingCalculationReport implements CalculationReport {

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
    }

    private final JPanel report = new JPanel(new GridBagLayout());
    private final GridBagConstraints gbc = new GridBagConstraints();
    private final String LINE_START_SPACER = "        ";
    private final static int COL_SPACER = 35;

    /** Tentative Section lines are kept in their own list. */
    private final List<ReportLine> tentativeLines = new ArrayList<>();

    private boolean tentativeSectionActive = false;

    @Override
    public JComponent toJComponent() {
        return report;
    }

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
                    addEmptyLine();
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
            newLine();
            gbc.gridx = 3;
            gbc.anchor = GridBagConstraints.LINE_END;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            JLabel resultLabel = new JLabel();
            Color color = UIManager.getColor("Label.foreground");
            resultLabel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 0, COL_SPACER),
                    new MatteBorder(1, 0, 0, 0, color)));
            gbc.weightx = 0;
            report.add(resultLabel, gbc);
            return addLine(type, calculation, result);
        }
    }

    @Override
    public CalculationReport addLine(String type, String calculation, String result) {
        if (tentativeSectionActive) {
            tentativeLines.add(new ReportLine(type, calculation, result, LineType.LINE));
        } else {
            newLine();
            gbc.ipadx = 0;
            report.add(new JLabel(LINE_START_SPACER), gbc);
            gbc.gridx++;
            gbc.ipadx = COL_SPACER;
            report.add(new JLabel(type), gbc);
            gbc.gridx++;
            report.add(new JLabel(calculation), gbc);
            gbc.gridx++;
            gbc.anchor = GridBagConstraints.LINE_END;
            gbc.weightx = 0;
            report.add(new JLabel(result), gbc);
        }
        return this;
    }

    @Override
    public CalculationReport addSubHeader(String text) {
        if (tentativeSectionActive) {
            tentativeLines.add(new ReportLine(text, "", "", LineType.SUBHEADER));
        } else {
            newLine();
            gbc.gridwidth = 4;
            gbc.ipadx = 0;
            report.add(new JLabel(text), gbc);
        }
        return this;
    }

    @Override
    public CalculationReport addHeader(String text) {
        if (tentativeSectionActive) {
            tentativeLines.add(new ReportLine(text, "", "", LineType.HEADER));
        } else {
            newLine();
            gbc.gridwidth = 4;
            JLabel header = new JLabel("<HTML><U><FONT SIZE=+2>" + text);
            report.add(header, gbc);
        }
        return this;
    }

    @Override
    public CalculationReport addEmptyLine() {
        if (tentativeSectionActive) {
            tentativeLines.add(new ReportLine("", "", "", LineType.EMPTY));
        } else {
            newLine();
            gbc.gridwidth = 4;
            report.add(Box.createVerticalStrut(8), gbc);
        }
        return this;
    }

    private void newLine() {
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.ipadx = COL_SPACER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
    }

}
