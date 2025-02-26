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

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.ASStatsTablePanel;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.ASStatsExporter;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.AlphaStrikeHelper;
import megamek.common.alphaStrike.cardDrawer.ASCardPrinter;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.jacksonadapters.MMUWriter;

import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * This non-modal dialog shows stats of one or more AlphaStrike elements in the form of a table.
 * It also allows export of the table data (optimized for import into Excel).
 */
public class ASStatsDialog extends AbstractDialog {

    private final Collection<Entity> entities;
    private final JButton clipBoardButton = new JButton(Messages.getString("CASCardPanel.copyCard"));
    private final JButton copyStatsButton = new JButton(Messages.getString("CASCardPanel.copyStats"));
    private final JButton printButton = new JButton(Messages.getString("CASCardPanel.printCard"));
    private final JButton saveButton = new JButton(Messages.getString("Save.text"));
    private final JScrollPane scrollPane = new JScrollPane();
    private final JPanel centerPanel = new JPanel();
    private ASStatsTablePanel tablePanel;
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
    public ASStatsDialog(JFrame frame, Collection<Entity> entities) {
        super(frame, "AlphaStrikeStatsDialog", "AlphaStrikeStatsDialog.title");
        this.entities = new ArrayList<>(entities);
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));

        var optionsPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(Box.createVerticalStrut(25));
        optionsPanel.add(clipBoardButton);
        optionsPanel.add(copyStatsButton);
        optionsPanel.add(printButton);
        optionsPanel.add(saveButton);
        saveButton.addActionListener(e -> save());
        saveButton.setFont(UIUtil.getScaledFont());
        clipBoardButton.addActionListener(e -> copyToClipboard());
        copyStatsButton.addActionListener(e -> copyStats());
        printButton.addActionListener(ev -> printCards());

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(optionsPanel);
        centerPanel.add(Box.createVerticalStrut(15));
        setupTable();
        return centerPanel;
    }

    private void setupTable() {
        centerPanel.remove(scrollPane);
        tablePanel = new ASStatsTablePanel(getFrame()).add(entities, "Selected Units");
        scrollPane.setViewportView(tablePanel.getPanel());
        centerPanel.add(scrollPane);
    }

    private void copyToClipboard() {
        StringSelection stringSelection = new StringSelection(clipboardString(entities));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private void copyStats() {
        StringBuilder allStats = new StringBuilder();
        for (ASCardDisplayable element : tablePanel.getSortedElements()) {
            var statsExporter = new ASStatsExporter(element);
            allStats.append(statsExporter.getStats()).append("\n");
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(allStats.toString()), null);
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
                .map(e -> ASConverter.convert(e, true))
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

    private void save() {
        List<AlphaStrikeElement> elements = entities.stream().filter(ASConverter::canConvert)
                .map(e -> ASConverter.convert(e, true))
                .collect(Collectors.toList());
        if (elements.isEmpty()) {
            return;
        }
        var fileChooser = new JFileChooser(".");
        fileChooser.setDialogTitle(Messages.getString("Save.text"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("MUL files", "mmu"));
        fileChooser.setSelectedFile(new File(elements.get(0).generalName() + ".mmu"));
        int returnVal = fileChooser.showSaveDialog(getParent());
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fileChooser.getSelectedFile() == null)) {
            return;
        }

        File unitFile = fileChooser.getSelectedFile();

        try {
            new MMUWriter().writeMMUFileFullStats(unitFile, elements);
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(getParent(), "The MMU file could not be written. "
                    + e.getMessage());
        }
    }

    private void printCards() {
        new ASCardPrinter(tablePanel.getSortedElements(), getFrame()).printCards();
    }
}
