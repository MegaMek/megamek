/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.client.ui.dialogs.ASConversionInfoDialog;
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.swing.util.SpringUtilities;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class SBFStatsTablePanel {

    public static final int COLUMNS = 13;
    
    private final JFrame parent;
    private final boolean showElements;
    private final Collection<SBFFormation> formations;
    private final Box contentPane = Box.createVerticalBox();

    public SBFStatsTablePanel(JFrame parent, Collection<SBFFormation> formations, boolean showElements) {
        this.parent = parent;
        this.formations = formations;
        this.showElements = showElements;
        updatePanel();
    }

    public JComponent getPanel() {
        return contentPane;
    }

    private void updatePanel() {
        contentPane.removeAll();
        for (SBFFormation formation : formations) {
            contentPane.add(formationPanel(formation));
        }
        contentPane.revalidate();
        contentPane.repaint();
    }

    private Component formationPanel(SBFFormation formation) {
        Box formationPanel = Box.createVerticalBox();
        JPanel summaryPanel = new JPanel(new SpringLayout());

        addFormationHeaders(summaryPanel);
        addGroupName(summaryPanel, formation.getName());
        addGridElement(summaryPanel, formation.getType().toString(), UIUtil.uiDarkBlue());
        addGridElement(summaryPanel, formation.getSize() + "", UIUtil.uiDarkBlue());
        addGridElement(summaryPanel, formation.getMovement() + formation.getMovementCode() + "", UIUtil.uiDarkBlue());
        addGridElement(summaryPanel, formation.getJumpMove() + "", UIUtil.uiDarkBlue());
        addGridElement(summaryPanel, formation.getTrspMovement() + formation.getTrspMovementCode() + "", UIUtil.uiDarkBlue());
        addGridElement(summaryPanel, formation.getTmm() + "", UIUtil.uiDarkBlue());
        addGridElement(summaryPanel, formation.getTactics() + "", UIUtil.uiDarkBlue());
        addGridElement(summaryPanel, formation.getMorale() + "", UIUtil.uiDarkBlue());
        addGridElement(summaryPanel, formation.getSkill() + "", UIUtil.uiDarkBlue());
        addGridElement(summaryPanel, formation.getPointValue() + "", UIUtil.uiDarkBlue());
        addGridElement(summaryPanel, formation.getSpecialsDisplayString(", ", formation) + "", UIUtil.uiDarkBlue(), FlowLayout.LEFT);
        addConversionInfo(summaryPanel, (FlexibleCalculationReport) formation.getConversionReport(), parent);

        addUnitHeaders(summaryPanel);
        int row = 1;
        for (SBFUnit unit : formation.getUnits()) {
            boolean oddRow = (row++ % 2) == 1;
            Color bgColor = oddRow ? UIUtil.alternateTableBGColor() : null;
            addGridElement(summaryPanel, "  " + unit.getName(), bgColor, FlowLayout.LEFT);
            addGridElement(summaryPanel, unit.getType().toString(), bgColor);
            addGridElement(summaryPanel, unit.getSize() + "", bgColor);
            addGridElement(summaryPanel, unit.getMovement() + unit.getMovementCode(), bgColor);
            addGridElement(summaryPanel, unit.getJumpMove() + "", bgColor);
            addGridElement(summaryPanel, unit.getTrspMovement() + unit.getTrspMovementCode(), bgColor);
            addGridElement(summaryPanel, unit.getTmm() + "", bgColor);
            addGridElement(summaryPanel, unit.getArmor() + "", bgColor);
            addGridElement(summaryPanel, unit.getDamage() + "", bgColor);
            addGridElement(summaryPanel, unit.getSkill() + "", bgColor);
            addGridElement(summaryPanel, unit.getPointValue() + "", bgColor);
            addGridElement(summaryPanel, unit.getSpecialsDisplayString(", ", unit), bgColor, FlowLayout.LEFT);
            addGridElement(summaryPanel, "", bgColor);
        }
        SpringUtilities.makeCompactGrid(summaryPanel, summaryPanel.getComponentCount() / COLUMNS, COLUMNS, 5, 5, 1, 5);
        formationPanel.add(summaryPanel);

        if (showElements) {
            var p = new ASStatsTablePanel(null);
            formation.getUnits().forEach(u -> p.add(u.getElements(), u.getName()));
            formationPanel.add(p.getPanel());
        }
        formationPanel.add(Box.createVerticalStrut(25));
        return formationPanel;
    }


    private void addConversionInfo(JComponent targetPanel, FlexibleCalculationReport conversionReport,
                                   JFrame frame) {
        var panel = new UIUtil.FixedYPanel();
        JButton button = new JButton("?");
        button.setEnabled(conversionReport != null);
        button.addActionListener(e -> new ASConversionInfoDialog(frame, conversionReport).setVisible(true));
        panel.add(button);
        targetPanel.add(panel);
    }

    private void addGridElement(JComponent targetPanel, String text, Color bgColor) {
        addGridElement(targetPanel, text, bgColor, FlowLayout.CENTER);
    }

    private void addGridElement(JComponent targetPanel, String text, Color bgColor, int alignment) {
        writeGridElement(targetPanel, text, bgColor, alignment, null);
    }

    private void writeGridElement(JComponent targetPanel, String text, Color bgColor,
                                  int alignment, @Nullable Color textColor) {
        var panel = new UIUtil.FixedYPanel(new FlowLayout(alignment));
        panel.setBackground(bgColor);
        panel.setForeground(textColor);
        var textLabel = new JLabel(text);
        textLabel.setForeground(textColor);
        textLabel.setFont(UIUtil.getScaledFont());
        panel.add(textLabel);
        targetPanel.add(panel);
    }

    private void addHeader(JComponent targetPanel, String text, float alignment) {
        writeHeader(targetPanel, text, alignment, UIUtil.uiLightBlue());
    }

    private void addHeader(JComponent targetPanel, String text) {
        addHeader(targetPanel, text, JComponent.CENTER_ALIGNMENT);
    }

    private void addGroupName(JComponent targetPanel, String text) {
        writeGridElement(targetPanel, text, UIUtil.uiDarkBlue(), FlowLayout.LEFT, UIUtil.uiLightGreen());
    }

    private void writeHeader(JComponent targetPanel, String text, float alignment, Color color) {
        var namePanel = new UIUtil.FixedYPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.PAGE_AXIS));
        var textLabel = new JLabel(text);
        textLabel.setAlignmentX(alignment);
        textLabel.setForeground(color);
        textLabel.setFont(UIUtil.getScaledFont());
        namePanel.add(textLabel);
        namePanel.add(Box.createVerticalStrut(5));
        namePanel.add(new JSeparator());
        targetPanel.add(namePanel);
    }

    private void addFormationHeaders(JComponent targetPanel) {
        addHeader(targetPanel, "SBF Formation", JComponent.LEFT_ALIGNMENT);
        addHeader(targetPanel, "Type");
        addHeader(targetPanel, "Size");
        addHeader(targetPanel, "Move");
        addHeader(targetPanel, "JUMP");
        addHeader(targetPanel, "T. Move");
        addHeader(targetPanel, "TMM");
        addHeader(targetPanel, "Tactics");
        addHeader(targetPanel, "Morale");
        addHeader(targetPanel, "Skill");
        addHeader(targetPanel, "PV");
        addHeader(targetPanel, "Specials");
        addHeader(targetPanel, "Conversion");
    }

    private void addUnitHeaders(JComponent targetPanel) {
        addHeader(targetPanel, "   Unit", JComponent.LEFT_ALIGNMENT);
        addHeader(targetPanel, " ");
        addHeader(targetPanel, " ");
        addHeader(targetPanel, " ");
        addHeader(targetPanel, " ");
        addHeader(targetPanel, " ");
        addHeader(targetPanel, " ");
        addHeader(targetPanel, "Arm");
        addHeader(targetPanel, "Dmg");
        addHeader(targetPanel, " ");
        addHeader(targetPanel, " ");
        addHeader(targetPanel, " ");
        addHeader(targetPanel, " ");
    }
}