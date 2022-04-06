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

/**
 * This is a Calculation Report that builds its output as an assembly of JLabels in a JPanel.
 * This is best suited to being displayed in a dialog as it knows how wide and high it needs to be
 * (better than an HTML report does). Note that its toString() method does not yield a readable report..
 */
public class SwingCalculationReport implements CalculationReport {

    private final JPanel report = new JPanel(new GridBagLayout());
    private final GridBagConstraints gbc = new GridBagConstraints();
    private final String COL_SPACER = "   ";
    private final static int PADDING = 15;

    @Override
    public JComponent toJComponent() {
        return report;
    }

    @Override
    public CalculationReport addResultLine(String type, String calculation, String result) {
        newLine();
        gbc.gridx = 3;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel resultLabel = new JLabel();
        Color color = UIManager.getColor("Label.foreground");
        resultLabel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 0, PADDING),
                new MatteBorder(1, 0, 0, 0, color)));
        report.add(resultLabel, gbc);
        return addLine(type, calculation, result);
    }

    @Override
    public CalculationReport addLine(String type, String calculation, String result) {
        newLine();
        gbc.ipadx = 0;
        report.add(new JLabel(COL_SPACER), gbc);
        gbc.gridx++;
        gbc.ipadx = PADDING;
        report.add(new JLabel(type), gbc);
        gbc.gridx++;
        report.add(new JLabel(calculation), gbc);
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.LINE_END;
        report.add(new JLabel(result), gbc);
        return this;
    }

    @Override
    public CalculationReport addSubHeader(String text) {
        newLine();
        gbc.gridwidth = 4;
        gbc.ipadx = 0;
        report.add(new JLabel(text), gbc);
        return this;
    }

    @Override
    public CalculationReport addHeader(String text) {
        newLine();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = 4;
        JLabel header = new JLabel(text);
        Font font = header.getFont();
        int size = font.getSize() + 4;
        header.setFont(new Font(font.getName(), Font.PLAIN, size));
        report.add(header, gbc);
        addEmptyLine();
        return this;
    }

    @Override
    public CalculationReport addEmptyLine() {
        newLine();
        gbc.gridwidth = 4;
        report.add(Box.createVerticalStrut(8), gbc);
        return this;
    }

    private void newLine() {
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.ipadx = PADDING;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
    }

}
