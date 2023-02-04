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

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class SBFStatsDialog extends AbstractDialog {

    private final Collection<Force> forceList;
    private final Game game;
    private Collection<SBFFormation> formations;
    private final MMToggleButton elementsToggle = new MMToggleButton("Show Elements");
    private final JButton clipBoardButton = new JButton("Copy to Clipboard");
    private final JScrollPane scrollPane = new JScrollPane();
    private final JPanel centerPanel = new JPanel();
    private SBFStatsTablePanel statsPanel;

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
//        StringSelection stringSelection = new StringSelection(clipboardString(entities));
//        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//        clipboard.setContents(stringSelection, null);
    }

    /** Returns a String representing the entities to export to the clipboard. */
//    private String clipboardString(Collection<Entity> entities) {
//        StringBuilder result = new StringBuilder();
//        result.append("Unit");
//        result.append("Type");
//        result.append("SZ");
//        result.append("TMM");
//        result.append("MV");
//        result.append("Role");
//        result.append("Dmg S/M/L");
//        result.append("OV");
//        result.append("Arm/Str");
//        result.append("PV");
//        result.append("Specials");
//
//        for (Entity entity: entities) {
//            var element = new AlphaStrikeElement(entity);
//            result.append(entity.getShortName());
//            result.append(element.getUnitType().toString());
//            result.append(element.getSize() + "");
//            result.append(element.getTargetMoveModifier()+"");
//            if (moveToggle.isSelected()) {
//                result.append(""+element.getPrimaryMovementValue()/2);
//            } else {
//                result.append(element.getMovementAsString());
//            }
//            result.append(UnitRoleHandler.getRoleFor(entity).toString());
////                addGridElement(element.getDamage(0)+"/"+element.getDamage(1)+"/"+element.getDamage(2));
//
//            result.append(element.calcHeatCapacity(entity)+"");
//            result.append(element.getFinalArmor() + "/" + element.getStructure());
//            result.append(element.getFinalPoints()+"");
//            result.append("?");
//            Locale cl = Locale.getDefault();
//            NumberFormat numberFormatter = NumberFormat.getNumberInstance(cl);
////            result.append(numberFormatter.format(entity.getWeight())).append("\t");
////            // Pilot name
////            result.append(entity.getCrew().getName()).append("\t");
////            // Crew Skill with text
////            result.append(CrewSkillSummaryUtil.getSkillNames(entity)).append(": ")
////                    .append(entity.getCrew().getSkillsAsString(false)).append("\t");
////            // BV without C3 but with pilot (as that gets exported too)
////            result.append(entity.calculateBattleValue(true, false)).append("\t");
//            result.append("\n");
//        }
//        return result.toString();
//    }

}

