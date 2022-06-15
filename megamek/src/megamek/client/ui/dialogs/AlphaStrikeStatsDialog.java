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
import megamek.client.ui.swing.AlphaStrikeViewPanel;
import megamek.client.ui.swing.MMToggleButton;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.alphaStrike.ASConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

public class AlphaStrikeStatsDialog extends AbstractDialog {
    
    private final Collection<Entity> entities;
    private final MMToggleButton moveToggle = new MMToggleButton("Movement in Hexes");
    private final MMToggleButton pilotToggle = new MMToggleButton("Include Pilot");
    private final JButton clipBoardButton = new JButton("Copy to Clipboard");
    private JScrollPane scrollPane = new JScrollPane();
    private final JPanel centerPanel = new JPanel();

    public AlphaStrikeStatsDialog(JFrame frame, Collection<Entity> en) {
        super(frame, "AlphaStrikeStatsDialog", "AlphaStrikeStatsDialog.title");
        entities = en;
        initialize();
        UIUtil.adjustDialog(this);
    }

    @Override
    protected Container createCenterPane() {
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
        
        var optionsPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(Box.createVerticalStrut(25));
        optionsPanel.add(moveToggle);
        optionsPanel.add(pilotToggle);
        optionsPanel.add(clipBoardButton);
        moveToggle.addActionListener(e -> setupTable());
        pilotToggle.addActionListener(e -> setupTable());
        clipBoardButton.addActionListener(e -> copyToClipboard());
        
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(optionsPanel);
        centerPanel.add(Box.createVerticalStrut(15));
        setupTable();
        return centerPanel;
    }
    
    private void setupTable() {
        centerPanel.remove(scrollPane);
        var asPanel = new AlphaStrikeViewPanel(entities, moveToggle.isSelected(), pilotToggle.isSelected(), getFrame());
        scrollPane = new JScrollPane(asPanel);
        centerPanel.add(scrollPane);
        UIUtil.adjustDialog(this);
    }
    
    private void copyToClipboard() {
        StringSelection stringSelection = new StringSelection(clipboardString(entities));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
    
    /** Returns a String representing the entities to export to the clipboard. */
    private String clipboardString(Collection<Entity> entities) {
        StringBuilder result = new StringBuilder();
        result.append("Unit");
        result.append("Type");
        result.append("SZ");
        result.append("TMM");
        result.append("MV");
        result.append("Role");
        result.append("Dmg S/M/L");
        result.append("OV");
        result.append("Arm/Str");
        result.append("PV");
        result.append("Specials");
        
        for (Entity entity: entities) {
            var element = ASConverter.convert(entity);
//                result.append(entity.getShortName());
//                result.append(element.getUnitType().toString());
//                result.append(element.getSize() + "");
//                result.append(element.getTargetMoveModifier()+"");
//                if (moveToggle.isSelected()) {
//                    result.append("" + element.getPrimaryMovementValue()/2);
//                } else {
//                    result.append(element.getMovementAsString());
//                }
//                result.append(UnitRoleHandler.getRoleFor(entity).toString());
//                result.append(element.calcHeatCapacity(entity)+"");
//                result.append(element.getFinalArmor() + "/" + element.getStructure());
//                result.append(element.getFinalPoints()+"");
//                result.append("?");
            Locale cl = Locale.getDefault();
            NumberFormat numberFormatter = NumberFormat.getNumberInstance(cl);
//            result.append(numberFormatter.format(entity.getWeight())).append("\t");
//            // Pilot name
//            result.append(entity.getCrew().getName()).append("\t");
//            // Crew Skill with text
//            result.append(CrewSkillSummaryUtil.getSkillNames(entity)).append(": ")
//                    .append(entity.getCrew().getSkillsAsString(false)).append("\t");
//            // BV without C3 but with pilot (as that gets exported too)
//            result.append(entity.calculateBattleValue(true, false)).append("\t");
            result.append("\n");
        }
        return result.toString();
    }

}
