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
import megamek.client.ui.swing.AlphaStrikeStatsTablePanel;
import megamek.client.ui.swing.MMToggleButton;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.AlphaStrikeHelper;
import megamek.common.alphaStrike.conversion.ASConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This non-modal dialog shows stats of one or more AlphaStrike elements in the form of a table.
 * It shows a toggle to include pilot stats when converting the units from TW to
 * AS. It also allows export of the table data (optimized for import into Excel).
 */
public class AlphaStrikeStatsDialog extends AbstractDialog {
    
    private final Collection<Entity> entities;
    private final MMToggleButton pilotToggle = new MMToggleButton("Include Pilot");
    private final JButton clipBoardButton = new JButton("Copy to Clipboard");
    private JScrollPane scrollPane = new JScrollPane();
    private final JPanel centerPanel = new JPanel();
    private static final String COLUMN_SEPARATOR = "\t";
    private static final String INTERNAL_DELIMITER = ",";

    /**
     * Creates a non-modal dialog that shows AlphaStrike stats for the given entities. The
     * collection may include entities that cannot be converted to AlphaStrike; those will
     * be filtered out.
     *
     * @param frame The parent frame for this dialog
     * @param entities The entities to convert and show.
     */
    public AlphaStrikeStatsDialog(JFrame frame, Collection<Entity> entities) {
        super(frame, "AlphaStrikeStatsDialog", "AlphaStrikeStatsDialog.title");
        this.entities = new ArrayList<>(entities);
        initialize();
        adaptToGUIScale();
    }

    @Override
    protected Container createCenterPane() {
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
        
        var optionsPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(Box.createVerticalStrut(25));
        optionsPanel.add(pilotToggle);
        optionsPanel.add(clipBoardButton);
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
//        var asPanel = new AlphaStrikeStatsTablePanel(getFrame(), entities, pilotToggle.isSelected());
        JPanel asPanel = new AlphaStrikeStatsTablePanel(getFrame(), pilotToggle.isSelected()).add(entities, "Selected Units").getPanel();
        scrollPane.setViewportView(asPanel);
        centerPanel.add(scrollPane);
        adaptToGUIScale();
    }
    
    private void copyToClipboard() {
        StringSelection stringSelection = new StringSelection(clipboardString(entities));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
    
    /** Returns a String representing the entities to export to the clipboard. */
    private String clipboardString(Collection<Entity> entities) {
        StringBuilder result = new StringBuilder();
        result.append("Chassis").append(COLUMN_SEPARATOR);
        result.append("Model").append(COLUMN_SEPARATOR);
        result.append("Type").append(COLUMN_SEPARATOR);
        result.append("SZ").append(COLUMN_SEPARATOR);
        result.append("TMM").append(COLUMN_SEPARATOR);
        result.append("MV (THR)").append(COLUMN_SEPARATOR);
        result.append("Role").append(COLUMN_SEPARATOR);
        result.append("Dmg S/M/L").append(COLUMN_SEPARATOR);
        result.append("OV").append(COLUMN_SEPARATOR);
        result.append("Arm").append(COLUMN_SEPARATOR);
        result.append("Str").append(COLUMN_SEPARATOR);
        result.append("Th").append(COLUMN_SEPARATOR);
        result.append("Skill").append(COLUMN_SEPARATOR);
        result.append("PV").append(COLUMN_SEPARATOR);
        result.append("Specials").append(COLUMN_SEPARATOR);
        result.append("\n");
        entities.stream().filter(ASConverter::canConvert)
                .map(e -> ASConverter.convert(e, pilotToggle.isSelected()))
                .forEach(e -> result.append(dataLine(e)));
        return result.toString();
    }

    /** Returns a StringBuilder containing the data for one AS element. */
    private StringBuilder dataLine(AlphaStrikeElement element) {
        StringBuilder dataLine = new StringBuilder();
        dataLine.append("=\"").append(element.getChassis()).append("\"").append(COLUMN_SEPARATOR);
        dataLine.append("=\"").append(element.getModel()).append("\"").append(COLUMN_SEPARATOR);
        dataLine.append(element.getASUnitType()).append(COLUMN_SEPARATOR);
        dataLine.append(element.getSize()).append(COLUMN_SEPARATOR);
        dataLine.append(element.isAerospace() ? "" : element.getTMM()).append(COLUMN_SEPARATOR);
        dataLine.append(element.getMovementAsString()).append(COLUMN_SEPARATOR);
        dataLine.append(element.getRole()).append(COLUMN_SEPARATOR);
        dataLine.append("=\"").append(element.usesArcs() ? "" : " " + element.getStandardDamage()).append("\"").append(COLUMN_SEPARATOR);
        dataLine.append(element.usesOV() ? element.getOV() : "").append(COLUMN_SEPARATOR);
        dataLine.append(element.getFullArmor()).append(COLUMN_SEPARATOR);
        dataLine.append(element.getFullStructure()).append(COLUMN_SEPARATOR);
        dataLine.append(element.usesThreshold() ? element.getThreshold() : "").append(COLUMN_SEPARATOR);
        dataLine.append(element.getSkill()).append(COLUMN_SEPARATOR);
        dataLine.append(element.getPointValue()).append(COLUMN_SEPARATOR);
        dataLine.append(AlphaStrikeHelper.getSpecialsExportString(INTERNAL_DELIMITER, element));
        dataLine.append("\n");
        return dataLine;
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}