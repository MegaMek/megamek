/*
 * Copyright (c) 2021-2023 - The MegaMek Team. All Rights Reserved.
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
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.MMToggleButton;
import megamek.client.ui.swing.SBFStatsTablePanel;
import megamek.client.ui.swing.util.FontHandler;
import megamek.client.ui.swing.util.UIUtil;
import megamek.codeUtilities.StringUtility;
import megamek.common.Game;
import megamek.common.force.Force;
import megamek.common.jacksonadapters.MMUWriter;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationConverter;
import megamek.common.strategicBattleSystems.SBFRecordSheetBook;
import megamek.common.strategicBattleSystems.SBFUnit;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * This non-modal dialog shows stats of one or more SBF Formations in the form of a table.
 * It also allows export of the table data (optimized for import into Excel).
 */
public class SBFStatsDialog extends AbstractDialog {

    private static final String COLUMN_SEPARATOR = "\t";

    private final Collection<Force> forceList;
    private final Game game;
    private List<SBFFormation> formations;
    private final MMToggleButton elementsToggle = new MMToggleButton(Messages.getString("SBFStatsDialog.showElements"));
    private final JButton clipBoardButton = new JButton(Messages.getString("SBFStatsDialog.copy"));
    private final JButton saveButton = new JButton(Messages.getString("Save.text"));
    private final JButton printButton = new JButton(Messages.getString("SBFStatsDialog.print"));
    private final JLabel headerFontLabel = new JLabel(Messages.getString("SBFStatsDialog.headerFont"));
    private JComboBox<String> headerFontChooser;
    private final JLabel valueFontLabel = new JLabel(Messages.getString("SBFStatsDialog.valueFont"));
    private JComboBox<String> valueFontChooser;
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
        optionsPanel.add(saveButton);

        headerFontChooser = new JComboBox<>(new Vector<>(FontHandler.getAvailableNonSymbolFonts()));
        headerFontChooser.addItem("");
        valueFontChooser = new JComboBox<>(new Vector<>(FontHandler.getAvailableNonSymbolFonts()));
        valueFontChooser.addItem("");
        headerFontChooser.setSelectedItem(GUIPreferences.getInstance().getSbfSheetHeaderFont());
        valueFontChooser.setSelectedItem(GUIPreferences.getInstance().getSbfSheetValueFont());
        headerFontChooser.setFont(UIUtil.getScaledFont());
        valueFontChooser.setFont(UIUtil.getScaledFont());

        var printPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        printPanel.add(Box.createHorizontalStrut(25));
        printPanel.add(printButton);
        printPanel.add(Box.createHorizontalStrut(10));
        printPanel.add(headerFontLabel);
        printPanel.add(headerFontChooser);
        printPanel.add(Box.createHorizontalStrut(10));
        printPanel.add(valueFontLabel);
        printPanel.add(valueFontChooser);

        elementsToggle.addActionListener(e -> setupTable());
        elementsToggle.setFont(UIUtil.getScaledFont());
        clipBoardButton.addActionListener(e -> copyToClipboard());
        clipBoardButton.setFont(UIUtil.getScaledFont());
        saveButton.addActionListener(e -> save());
        saveButton.setFont(UIUtil.getScaledFont());
        printButton.addActionListener(e -> printRecordSheets());
        printButton.setFont(UIUtil.getScaledFont());
        headerFontLabel.setFont(UIUtil.getScaledFont());
        valueFontLabel.setFont(UIUtil.getScaledFont());

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(optionsPanel);
        centerPanel.add(printPanel);
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

    public void printRecordSheets() {
        PrinterJob job = PrinterJob.getPrinterJob();
        boolean doPrint = job.printDialog();
        if (doPrint) {
            try {
                var recordSheetBook = new SBFRecordSheetBook(formations, getSelectedTextFont(), getSelectedValueFont());
                job.setPrintable(recordSheetBook);
                job.print();
            } catch (PrinterException e) {
                e.printStackTrace();
            }
        }
    }

    private Font getSelectedTextFont() {
        String selectedFont = (String) headerFontChooser.getSelectedItem();
        GUIPreferences.getInstance().setSbfSheetHeaderFont(selectedFont);
        return decodeSelectedFont(selectedFont);
    }

    private Font getSelectedValueFont() {
        String selectedFont = (String) valueFontChooser.getSelectedItem();
        GUIPreferences.getInstance().setSbfSheetValueFont(selectedFont);
        return decodeSelectedFont(selectedFont);
    }

    private Font decodeSelectedFont(String fontName) {
        return StringUtility.isNullOrBlank(fontName) ? null : Font.decode(fontName);
    }

    private void copyToClipboard() {
        StringSelection stringSelection = new StringSelection(clipboardString(formations));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private void save() {
        if (formations.isEmpty()) {
            return;
        }
        var fileChooser = new JFileChooser(".");
        fileChooser.setDialogTitle(Messages.getString("Save.text"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("MUL files", "mmu"));
        fileChooser.setSelectedFile(new File(formations.get(0).generalName() + ".mmu"));
        int returnVal = fileChooser.showSaveDialog(getParent());
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fileChooser.getSelectedFile() == null)) {
            return;
        }

        File unitFile = fileChooser.getSelectedFile();

        try {
            new MMUWriter().writeMMUFile(unitFile, formations);
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(getParent(), "The MMU file could not be written. "
                    + e.getMessage());
        }
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