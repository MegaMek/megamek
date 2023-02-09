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
package megamek.client.ui.dialogs;

import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.MMToggleButton;
import megamek.client.ui.swing.SBFStatsTablePanel;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Game;
import megamek.common.force.Force;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationConverter;
import megamek.common.strategicBattleSystems.SBFUnit;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This non-modal dialog shows stats of one or more SBF Formations in the form of a table.
 * It also allows export of the table data (optimized for import into Excel).
 */
public class SBFStatsDialog extends AbstractDialog {

    private static final String COLUMN_SEPARATOR = "\t";

    private final Collection<Force> forceList;
    private final Game game;
    private Collection<SBFFormation> formations;
    private final MMToggleButton elementsToggle = new MMToggleButton("Show Elements");
    private final JButton clipBoardButton = new JButton("Copy to Clipboard");
    private final JScrollPane scrollPane = new JScrollPane();
    private final JPanel centerPanel = new JPanel();
    private SBFStatsTablePanel statsPanel;

    /**
     * Creates a non-modal dialog that shows SBF Formation stats for the given forces. In
     * the collection of Forces, each entry should be an company-sized force to convert to
     * Strategic Battle Force. In other words, for a single formation, the force Collection
     * should only contain a single force. That force might, for example, have three sub-forces
     * with 4 units each.
     *
     * @param frame The parent frame for this dialog (required as parent to conversion dialogs)
     * @param forceList The force or forces to be converted
     * @param gm The game object (necessary to retrieve the entities from the forces)
     */
    public SBFStatsDialog(JFrame frame, Collection<Force> forceList, Game gm) {
        super(frame, false, "SBFStatsDialog", "SBFStatsDialog.title");
        this.forceList = forceList;
        game = gm;
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));

        var optionsPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(Box.createHorizontalStrut(25));
        optionsPanel.add(elementsToggle);
        optionsPanel.add(clipBoardButton);
        elementsToggle.addActionListener(e -> setupTable());
        elementsToggle.setFont(UIUtil.getScaledFont());
        clipBoardButton.addActionListener(e -> copyToClipboard());
        clipBoardButton.setFont(UIUtil.getScaledFont());

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(optionsPanel);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(scrollPane);

        setupTable();
        return centerPanel;
    }

    private void setupTable() {
        formations = forceList.stream()
                .map(f -> new SBFFormationConverter(f, game).convert())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        statsPanel = new SBFStatsTablePanel(getFrame(), formations, elementsToggle.isSelected());
        scrollPane.setViewportView(statsPanel.getPanel());
    }

    private void copyToClipboard() {
        StringSelection stringSelection = new StringSelection(clipboardString(formations));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private String clipboardString(Collection<SBFFormation> formations) {
        StringBuilder result = new StringBuilder();
        result.append("SBF Formation").append(COLUMN_SEPARATOR);
        result.append("Type").append(COLUMN_SEPARATOR);
        result.append("Size").append(COLUMN_SEPARATOR);
        result.append("Move").append(COLUMN_SEPARATOR);
        result.append("JUMP").append(COLUMN_SEPARATOR);
        result.append("T. Move").append(COLUMN_SEPARATOR);
        result.append("TMM").append(COLUMN_SEPARATOR);
        result.append("Tactics").append(COLUMN_SEPARATOR);
        result.append("Morale").append(COLUMN_SEPARATOR);
        result.append("Skill").append(COLUMN_SEPARATOR);
        result.append("PV").append(COLUMN_SEPARATOR);
        result.append("Specials").append(COLUMN_SEPARATOR);
        result.append("\n");

        for (SBFFormation formation : formations) {
            result.append(formation.getName()).append(COLUMN_SEPARATOR);
            result.append(formation.getType()).append(COLUMN_SEPARATOR);
            result.append(formation.getSize()).append(COLUMN_SEPARATOR);
            result.append(formation.getMovement()).append(formation.getMovementCode()).append(COLUMN_SEPARATOR);
            result.append(formation.getJumpMove()).append(COLUMN_SEPARATOR);
            result.append(formation.getTrspMovement()).append(formation.getTrspMovementCode()).append(COLUMN_SEPARATOR);
            result.append(formation.getTmm()).append(COLUMN_SEPARATOR);
            result.append(formation.getTactics()).append(COLUMN_SEPARATOR);
            result.append(formation.getMorale()).append(COLUMN_SEPARATOR);
            result.append(formation.getSkill()).append(COLUMN_SEPARATOR);
            result.append(formation.getPointValue()).append(COLUMN_SEPARATOR);
            result.append(formation.getSpecialsDisplayString(", ", formation)).append(COLUMN_SEPARATOR);
            result.append("\n");

            result.append("Unit").append(COLUMN_SEPARATOR);
            result.append(COLUMN_SEPARATOR);
            result.append(COLUMN_SEPARATOR);
            result.append(COLUMN_SEPARATOR);
            result.append(COLUMN_SEPARATOR);
            result.append(COLUMN_SEPARATOR);
            result.append(COLUMN_SEPARATOR);
            result.append("Arm").append(COLUMN_SEPARATOR);
            result.append("Dmg").append(COLUMN_SEPARATOR);
            result.append(COLUMN_SEPARATOR);
            result.append(COLUMN_SEPARATOR);
            result.append(COLUMN_SEPARATOR);
            result.append(COLUMN_SEPARATOR);
            result.append("\n");

            for (SBFUnit unit : formation.getUnits()) {
                result.append(unit.getName()).append(COLUMN_SEPARATOR);
                result.append(unit.getType().toString()).append(COLUMN_SEPARATOR);
                result.append(unit.getSize()).append(COLUMN_SEPARATOR);
                result.append(unit.getMovement()).append(unit.getMovementCode()).append(COLUMN_SEPARATOR);
                result.append(unit.getJumpMove()).append(COLUMN_SEPARATOR);
                result.append(unit.getTrspMovement()).append(unit.getTrspMovementCode()).append(COLUMN_SEPARATOR);
                result.append(unit.getTmm()).append(COLUMN_SEPARATOR);
                result.append(unit.getArmor()).append(COLUMN_SEPARATOR);
                result.append("=\"").append(unit.getDamage()).append("\"").append(COLUMN_SEPARATOR);
                result.append(unit.getSkill()).append(COLUMN_SEPARATOR);
                result.append(unit.getPointValue()).append(COLUMN_SEPARATOR);
                result.append(unit.getSpecialsDisplayString(", ", unit)).append(COLUMN_SEPARATOR);
                result.append(COLUMN_SEPARATOR);
                result.append("\n");
            }
        }
        return result.toString();
    }
}