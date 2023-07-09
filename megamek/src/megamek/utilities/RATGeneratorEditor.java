/*
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.utilities;

import megamek.MMConstants;
import megamek.client.ratgenerator.*;
import megamek.client.ratgenerator.FactionRecord.TechCategory;
import megamek.client.ui.swing.util.UIUtil.FixedXPanel;
import megamek.client.ui.swing.util.UIUtil.FixedYPanel;
import megamek.common.Configuration;
import megamek.common.EntityMovementMode;
import megamek.common.UnitRoleHandler;
import megamek.common.UnitType;
import megamek.common.eras.Eras;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author neoancient
 */
public class RATGeneratorEditor extends JFrame {
    private static final String[] MOVEMENT_TYPE_NAMES = {
            "Leg", "Tracked", "Wheeled", "Hover", "WiGE", "VTOL",
            "Naval", "Underwater", "Jump", "Motorized", "Atmospheric",
            "Aerospace", "Space", "None"
    };

    private static final FactionRecord GENERAL_FACTION = new FactionRecord("General", "General");

    private static RATGenerator rg;
    
    private static Integer[] ERAS;

    private final JComboBox<String> cbUnitType = new JComboBox<>();
    private final JComboBox<String> cbMovementType = new JComboBox<>();

    private final JTabbedPane panMain = new JTabbedPane();

    private final JTextField txtSearch = new JTextField(20);
    private final JButton butMUL = new JButton("Open MUL");
    private int currentMulId = -1;
    private final JTable tblMasterUnitList = new JTable();
    private MasterUnitListTableModel masterUnitListModel;
    private TableRowSorter<MasterUnitListTableModel> masterUnitListSorter;

    private final JComboBox<FactionRecord> factionChooserForModel = new JComboBox<>();
    private final JTable tblUnitModelEditor = new JTable();
    private final UnitEditorTableModel unitModelEditorModel = new UnitEditorTableModel();

    private final JComboBox<FactionRecord> factionChooserForChassis = new JComboBox<>();
    private final JTable tblUnitChassisEditor = new JTable();
    private final UnitEditorTableModel unitChassisEditorModel = new UnitEditorTableModel();

    private final JTextField txtNewFaction = new JTextField(20);
    private final JCheckBox chkShowSubfactions = new JCheckBox();
    private final JCheckBox chkShowMinorFactions = new JCheckBox();

    private final JTable tblMasterFactionList = new JTable();
    private FactionListTableModel masterFactionListModel;
    private TableRowSorter<FactionListTableModel> masterFactionListSorter;

    private final List<String> currentChassisFactions = new ArrayList<>();

    private final JTable tblFactionEditor = new JTable();
    private FactionEditorTableModel factionEditorModel;

    private final JTextField txtSalvageFaction = new JTextField(20);

    private final JTable tblSalvageEditor = new JTable();
    private SalvageEditorTableModel salvageEditorModel;

    private File lastDir = Configuration.forceGeneratorDir();

    public RATGeneratorEditor() {
        rg = RATGenerator.getInstance();
        while (!rg.isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                // Do nothing
            }
        }
        rg.getEraSet().forEach(e -> rg.loadYear(e));
        ERAS = rg.getEraSet().toArray(new Integer[0]);
        rg.initRemainingUnits();

        initUI();
    }

    public RATGeneratorEditor(File dir) {
        this();
        if (dir.exists() && dir.isDirectory()) {
            lastDir = dir;
            rg.reloadFromDir(lastDir);
            ERAS = rg.getEraSet().toArray(new Integer[0]);
            rg.initRemainingUnits();
        }
    }

    private void initUI() {
        setTitle("Unit Selector Editor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(new Dimension(1200, 700));

        masterUnitListModel = new MasterUnitListTableModel(rg.getModelList());

        JPanel panButtons = new JPanel();
        JButton button = new JButton("Load");
        button.setToolTipText("Load data from alternate location");
        panButtons.add(button);
        button.addActionListener(ev -> loadAltDir());

        button = new JButton("Save");
        button.setToolTipText("Export data to a selected directory");
        panButtons.add(button);
        button.addActionListener(ev -> saveValues());

        button = new JButton("Export as Excel CSV");
        button.setToolTipText("Export all availability data as an excel-optimized csv file");
        panButtons.add(button);
        button.addActionListener(ev -> RATDataCSVExporter.exportToCSV(rg));

        panMain.addTab("Edit", createUnitTab());
        panMain.addTab("Edit Factions", createFactionTab());

        add(panMain, BorderLayout.CENTER);
        add(buildOptionPanel(), BorderLayout.PAGE_START);
        add(panButtons, BorderLayout.PAGE_END);
    }

    private void loadAltDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(lastDir);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select load directory");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            lastDir = chooser.getSelectedFile();
            rg.reloadFromDir(lastDir);
            ERAS = rg.getEraSet().toArray(new Integer[0]);
            rg.initRemainingUnits();
            tblMasterUnitList.clearSelection();
            tblMasterFactionList.clearSelection();
        }
    }

    private void saveValues() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(lastDir);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select save directory");
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            lastDir = chooser.getSelectedFile();
            rg.exportRATGen(lastDir);
        }
    }

    private void fillFactionChoosers() {
        if (rg.getFactionList() == null) {
            return;
        }
        fillFactionChooser(factionChooserForModel);
        fillFactionChooser(factionChooserForChassis);
    }

    private void fillFactionChooser(JComboBox<FactionRecord> combo) {
        combo.removeAllItems();
        combo.addItem(GENERAL_FACTION);
        rg.getFactionList().stream()
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .forEach(combo::addItem);
        combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            if (value == null) {
                return new JLabel();
            } else {
                return new JLabel(value.getName() + " (" + value.getKey() + ")");
            }
        });
    }

    private JComponent buildOptionPanel() {
        JPanel result = new JPanel();

        cbUnitType.addItem("All");
        for (int i = 0; i < UnitType.SIZE; i++) {
            cbUnitType.addItem(UnitType.getTypeName(i));
        }
        cbUnitType.addActionListener(ev -> filterMasterUnitList());

        cbMovementType.addItem("All");
        for (String movementTypeName : MOVEMENT_TYPE_NAMES) {
            cbMovementType.addItem(movementTypeName);
        }
        cbMovementType.addActionListener(evt -> filterMasterUnitList());

        result.add(new JLabel("Unit Type:"));
        result.add(cbUnitType);
        result.add(new JLabel("Movement Type:"));
        result.add(cbMovementType);

        return result;
    }

    private void setCurrentUnitFactions() {
        currentChassisFactions.clear();
        if (tblMasterUnitList.getSelectedRow() >= 0) {
            ModelRecord model = masterUnitListModel.getUnitRecord(tblMasterUnitList.convertRowIndexToModel(tblMasterUnitList.getSelectedRow()));
            for (int i = 0; i < rg.getEraSet().size(); i++) {
                Collection<AvailabilityRating> chassisRecs = rg.getChassisFactionRatings(ERAS[i], model.getChassisKey());
                if (chassisRecs != null) {
                    for (AvailabilityRating rec : chassisRecs) {
                        currentChassisFactions.add(rec.getFactionCode());
                    }
                }
            }
        }
    }

    private JComponent createUnitTab() {
        Box unitSelectorSide = Box.createVerticalBox();
        JPanel unitSearchPanel = new FixedYPanel();
        unitSelectorSide.add(unitSearchPanel);

        unitSearchPanel.add(new JLabel("Search:"));
        unitSearchPanel.add(txtSearch);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent evt) {
                filterMasterUnitList();
            }

            @Override
            public void insertUpdate(DocumentEvent evt) {
                filterMasterUnitList();
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                filterMasterUnitList();
            }
        });
        unitSearchPanel.add(Box.createHorizontalStrut(15));
        unitSearchPanel.add(butMUL);
        butMUL.addActionListener(e -> showMUL());

        tblMasterUnitList.setModel(masterUnitListModel);
        masterUnitListSorter = new TableRowSorter<>(masterUnitListModel);
        masterUnitListSorter.setComparator(MasterUnitListTableModel.COL_UNIT_TYPE, new UnitTypeComparator());
        masterUnitListSorter.setComparator(MasterUnitListTableModel.COL_WEIGHT, Comparator.comparingDouble(d -> (double) d));
        List<SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new SortKey(MasterUnitListTableModel.COL_UNIT_TYPE, SortOrder.ASCENDING));
        sortKeys.add(new SortKey(MasterUnitListTableModel.COL_CHASSIS, SortOrder.ASCENDING));
        sortKeys.add(new SortKey(MasterUnitListTableModel.COL_MODEL, SortOrder.ASCENDING));
        sortKeys.add(new SortKey(MasterUnitListTableModel.COL_YEAR, SortOrder.ASCENDING));
        masterUnitListSorter.setSortKeys(sortKeys);
        tblMasterUnitList.setRowSorter(masterUnitListSorter);
        tblMasterUnitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMasterUnitList.getSelectionModel().addListSelectionListener(evt -> {
            setCurrentUnitFactions();
            if (tblMasterUnitList.getSelectedRow() >= 0) {
                ModelRecord rec = masterUnitListModel.
                        getUnitRecord(tblMasterUnitList.convertRowIndexToModel(tblMasterUnitList.getSelectedRow()));
                currentMulId = rec.getMechSummary().getMulId();
                unitModelEditorModel.setData(rec, UnitEditorTableModel.MODE_MODEL);
                unitChassisEditorModel.setData(rec, UnitEditorTableModel.MODE_CHASSIS);
            } else {
                currentMulId = -1;
                unitModelEditorModel.clearData();
                unitChassisEditorModel.clearData();
            }
        });

        JScrollPane scroll = new JScrollPane(tblMasterUnitList);
        JPanel unitContainer = new FixedXPanel();
        unitContainer.setLayout(new BorderLayout());
        unitSelectorSide.add(scroll);
        unitContainer.add(unitSelectorSide, BorderLayout.CENTER);

        Box factionEditSide = Box.createVerticalBox();
        factionEditSide.add(createUnitModelEditor());
        factionEditSide.add(Box.createVerticalStrut(5));
        factionEditSide.add(createCopyBetweenButtonPanel());
        factionEditSide.add(Box.createVerticalStrut(5));
        factionEditSide.add(createUnitChassisEditor());
        return new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, unitContainer, factionEditSide);
    }

    private void showMUL() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
                    && currentMulId > 0) {
                Desktop.getDesktop().browse(new URL(MMConstants.MUL_URL_PREFIX + currentMulId).toURI());
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JComponent createCopyBetweenButtonPanel() {
        JPanel panel = new JPanel();
        JButton copyToModelButton = new JButton("\u25B2");
        copyToModelButton.addActionListener(ev -> {
            if (tblUnitChassisEditor.getSelectedRow() >= 0) {
                int modelRow = tblUnitChassisEditor.convertRowIndexToModel(tblUnitChassisEditor.getSelectedRow());
                copyBetween(unitChassisEditorModel, unitModelEditorModel, modelRow);
            }
        });
        JButton copyToChassisButton = new JButton("\u25BC");
        copyToChassisButton.addActionListener(ev -> {
            if (tblUnitModelEditor.getSelectedRow() >= 0) {
                int modelRow = tblUnitModelEditor.convertRowIndexToModel(tblUnitModelEditor.getSelectedRow());
                copyBetween(unitModelEditorModel, unitChassisEditorModel, modelRow);
            }
        });
        panel.add(copyToModelButton);
        panel.add(Box.createHorizontalStrut(100));
        panel.add(copyToChassisButton);
        return panel;
    }

    private void copyBetween(UnitEditorTableModel source, UnitEditorTableModel destination, int row) {
        destination.addEntry(source.getRow(row));
    }

    private JComponent createUnitModelEditor() {
        Box unitModelEditor = Box.createVerticalBox();
        JPanel topPanel = new FixedYPanel();
        unitModelEditor.add(topPanel);
        topPanel.add(new JLabel("--- Model ---       "));
        topPanel.add(factionChooserForModel);
        fillFactionChoosers();

        JButton button = new JButton("Add Row");
        topPanel.add(button);
        button.addActionListener(ev -> {
            if ((factionChooserForModel.getSelectedItem() != null) && !unitModelEditorModel.addEntry(factionChooserForModel.getSelectedItem().toString())) {
                JOptionPane.showMessageDialog(this,
                        "Unable to add model or chassis entry. Please select a unit model. " +
                                "If adding a model entry, make sure you already have a chassis entry defined.");
            }
        });

        button = new JButton("Delete Row");
        topPanel.add(button);
        button.addActionListener(ev -> {
            if (tblUnitModelEditor.getSelectedRow() >= 0) {
                unitModelEditorModel.removeEntry(tblUnitModelEditor.convertRowIndexToModel(tblUnitModelEditor.getSelectedRow()));
            }
        });

        button = new JButton("Copy Row");
        topPanel.add(button);
        button.addActionListener(ev -> {
            if ((tblUnitModelEditor.getSelectedRow() >= 0) && (factionChooserForModel.getSelectedItem() != null)) {
                unitModelEditorModel.copyRow(tblUnitModelEditor.convertRowIndexToModel(tblUnitModelEditor.getSelectedRow()),
                        factionChooserForModel.getSelectedItem().toString());
            }
        });

        tblUnitModelEditor.setModel(unitModelEditorModel);
        tblUnitModelEditor.createDefaultColumnsFromModel();
        tblUnitModelEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblUnitModelEditor.getTableHeader().setPreferredSize(
                new Dimension(tblMasterUnitList.getColumnModel().getTotalColumnWidth(), 32));
        var columnModel = tblUnitModelEditor.getColumnModel();
        for (int i = 1; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setPreferredWidth(50);
        }
        tblUnitModelEditor.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblUnitModelEditor.setDefaultRenderer(String.class, unitListRenderer);
        tblUnitModelEditor.getSelectionModel().addListSelectionListener(evt -> tblUnitChassisEditor.repaint());

        TableRowSorter<UnitEditorTableModel> unitEditorListSorter = new TableRowSorter<>(unitModelEditorModel);
        unitEditorListSorter.setComparator(0, Comparator.comparing(String::toString));
        List<SortKey> unitSortKeys = new ArrayList<>();
        unitSortKeys.add(new SortKey(0, SortOrder.ASCENDING));
        unitEditorListSorter.setSortKeys(unitSortKeys);
        tblUnitModelEditor.setRowSorter(unitEditorListSorter);
        JScrollPane scroll = new JScrollPane(tblUnitModelEditor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        unitModelEditor.add(scroll);
        return unitModelEditor;
    }

    private JComponent createUnitChassisEditor() {
        Box unitChassisEditor = Box.createVerticalBox();
        JPanel topPanel = new FixedYPanel();
        unitChassisEditor.add(topPanel);
        topPanel.add(new JLabel("--- Chassis ---       "));
        topPanel.add(factionChooserForChassis);
        fillFactionChoosers();

        JButton button = new JButton("Add Row");
        topPanel.add(button);
        button.addActionListener(ev -> {
            if ((factionChooserForChassis.getSelectedItem() != null) && !unitChassisEditorModel.addEntry(factionChooserForChassis.getSelectedItem().toString())) {
                JOptionPane.showMessageDialog(this,
                        "Unable to add model or chassis entry. Please select a unit model. " +
                                "If adding a model entry, make sure you already have a chassis entry defined.");
            }
            setCurrentUnitFactions();
            unitModelEditorModel.fireTableDataChanged();
        });

        button = new JButton("Delete Row");
        topPanel.add(button);
        button.addActionListener(ev -> {
            if (tblUnitChassisEditor.getSelectedRow() >= 0) {
                unitChassisEditorModel.removeEntry(tblUnitChassisEditor.convertRowIndexToModel(tblUnitChassisEditor.getSelectedRow()));
            }
            setCurrentUnitFactions();
            unitModelEditorModel.fireTableDataChanged();
        });

        button = new JButton("Copy Row");
        topPanel.add(button);
        button.addActionListener(ev -> {
            if ((tblUnitChassisEditor.getSelectedRow() >= 0) && (factionChooserForChassis.getSelectedItem() != null)) {
                unitChassisEditorModel.copyRow(tblUnitChassisEditor.convertRowIndexToModel(tblUnitChassisEditor.getSelectedRow()),
                        factionChooserForChassis.getSelectedItem().toString());
            }
            setCurrentUnitFactions();
            unitModelEditorModel.fireTableDataChanged();
        });

        tblUnitChassisEditor.setModel(unitChassisEditorModel);
        tblUnitChassisEditor.createDefaultColumnsFromModel();
        tblUnitChassisEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblUnitChassisEditor.getTableHeader().setPreferredSize(
                new Dimension(tblUnitChassisEditor.getColumnModel().getTotalColumnWidth(), 32));
        var columnModel = tblUnitChassisEditor.getColumnModel();
        for (int i = 1; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setPreferredWidth(50);
        }
        tblUnitChassisEditor.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblUnitChassisEditor.setDefaultRenderer(String.class, chassisListRenderer);
        tblUnitChassisEditor.getSelectionModel().addListSelectionListener(evt -> tblUnitModelEditor.repaint());

        TableRowSorter<UnitEditorTableModel> unitEditorListSorter = new TableRowSorter<>(unitChassisEditorModel);
        unitEditorListSorter.setComparator(0, Comparator.comparing(String::toString));
        List<SortKey> unitSortKeys = new ArrayList<>();
        unitSortKeys.add(new SortKey(0, SortOrder.ASCENDING));
        unitEditorListSorter.setSortKeys(unitSortKeys);
        tblUnitChassisEditor.setRowSorter(unitEditorListSorter);
        JScrollPane scroll = new JScrollPane(tblUnitChassisEditor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        unitChassisEditor.add(scroll);
        return unitChassisEditor;
    }

    private JPanel createFactionTab() {
        JButton newButton = new JButton("New");
        newButton.addActionListener(ev -> {
            if (!txtNewFaction.getText().isBlank()) {
                FactionRecord rec = new FactionRecord(txtNewFaction.getText());
                rg.addFaction(rec);
                masterFactionListModel.addRecord(rec);
            }
        });

        JButton delButton = new JButton("Delete");
        delButton.addActionListener(ev -> {
            if (tblMasterFactionList.getSelectedRow() >= 0) {
                FactionRecord rec = masterFactionListModel.getFactionRecord(
                        tblMasterFactionList.convertRowIndexToModel(
                                tblMasterFactionList.getSelectedRow()));
                masterFactionListModel.delRecord(rec);
                rg.removeFaction(rec);
            }
        });

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(ev -> {
            if (!txtNewFaction.getText().isBlank()
                    && (tblMasterFactionList.getSelectedRow() >= 0)) {
                FactionRecord from = masterFactionListModel.getFactionRecord(
                        tblMasterFactionList.convertRowIndexToModel(
                                tblMasterFactionList.getSelectedRow()));
                FactionRecord rec = new FactionRecord(txtNewFaction.getText());
                rec.setClan(from.isClan());
                rec.setRatings(String.join(",", from.getRatingLevels()));
                rg.addFaction(rec);
                masterFactionListModel.addRecord(rec);
            }
        });

        chkShowSubfactions.setText("Show Subfactions");
        chkShowSubfactions.setSelected(true);
        chkShowSubfactions.addActionListener(ev -> filterFactionList());

        chkShowMinorFactions.setText("Show Minor Factions");
        chkShowMinorFactions.setSelected(true);
        chkShowMinorFactions.addActionListener(ev -> filterFactionList());

        masterFactionListModel = new FactionListTableModel(rg.getFactionList());
        tblMasterFactionList.setModel(masterFactionListModel);
        masterFactionListSorter = new TableRowSorter<>(masterFactionListModel);
        List<SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new SortKey(FactionListTableModel.COL_CODE, SortOrder.ASCENDING));
        sortKeys.add(new SortKey(FactionListTableModel.COL_NAME, SortOrder.ASCENDING));
        sortKeys.add(new SortKey(FactionListTableModel.COL_CLAN, SortOrder.ASCENDING));
        masterFactionListSorter.setSortKeys(sortKeys);
        tblMasterFactionList.setRowSorter(masterFactionListSorter);
        tblMasterFactionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMasterFactionList.getSelectionModel().addListSelectionListener(evt -> {
            if (tblMasterFactionList.getSelectedRow() >= 0) {
                FactionRecord rec = masterFactionListModel.
                        getFactionRecord(tblMasterFactionList.convertRowIndexToModel(tblMasterFactionList.getSelectedRow()));
                factionEditorModel.setData(rec);
                salvageEditorModel.setData(rec);
            } else {
                factionEditorModel.clearData();
                salvageEditorModel.clearData();
            }
        });
        JScrollPane masterScroll = new JScrollPane(tblMasterFactionList);
        JPanel masterPanel = new FixedXPanel();
        masterPanel.add(masterScroll);

        factionEditorModel = new FactionEditorTableModel(null);
        tblFactionEditor.setModel(factionEditorModel);
        tblFactionEditor.createDefaultColumnsFromModel();
        tblFactionEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane editorScroll = new JScrollPane(tblFactionEditor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JButton addSalvageButton = new JButton("Add Row");
        addSalvageButton.addActionListener(ev -> salvageEditorModel.addEntry(txtSalvageFaction.getText()));

        JButton delSalvageButton = new JButton("Delete Row");
        delSalvageButton.addActionListener(ev -> {
                if (tblSalvageEditor.getSelectedRow() >= 0) {
                    salvageEditorModel.removeEntry(tblSalvageEditor.convertRowIndexToModel(tblSalvageEditor.getSelectedRow()));
                }
        });

        JButton copySalvageButton = new JButton("Copy Row");
        copySalvageButton.addActionListener(ev -> {
                if (tblSalvageEditor.getSelectedRow() >= 0) {
                    salvageEditorModel.copyRow(tblSalvageEditor.convertRowIndexToModel(tblSalvageEditor.getSelectedRow()),
                            txtSalvageFaction.getText());
                }
        });

        salvageEditorModel = new SalvageEditorTableModel();
        tblSalvageEditor.setModel(salvageEditorModel);
        tblSalvageEditor.createDefaultColumnsFromModel();
        tblSalvageEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane salvageScroll = new JScrollPane(tblSalvageEditor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(txtNewFaction);
        optionsPanel.add(newButton);
        optionsPanel.add(delButton);
        optionsPanel.add(copyButton);
        optionsPanel.add(chkShowSubfactions);
        optionsPanel.add(chkShowMinorFactions);

        JPanel salvageButtonPanel = new FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        salvageButtonPanel.add(txtSalvageFaction);
        salvageButtonPanel.add(addSalvageButton);
        salvageButtonPanel.add(delSalvageButton);
        salvageButtonPanel.add(copySalvageButton);

        Box rightSide = Box.createVerticalBox();
        rightSide.add(editorScroll);
        rightSide.add(Box.createVerticalStrut(8));
        rightSide.add(salvageButtonPanel);
        rightSide.add(salvageScroll);

        JPanel masterContainer = new FixedXPanel();
        masterContainer.setLayout(new BorderLayout());
        masterContainer.add(masterScroll, BorderLayout.CENTER);

        JSplitPane centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, masterContainer, rightSide);

        JPanel factionTab = new JPanel(new BorderLayout());
        factionTab.add(optionsPanel, BorderLayout.PAGE_START);
        factionTab.add(centerPanel, BorderLayout.CENTER);

        return factionTab;
    }

    private void filterFactionList() {
        RowFilter<FactionListTableModel, Integer> rf = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends FactionListTableModel, ? extends Integer> entry) {
                FactionListTableModel model = entry.getModel();
                FactionRecord rec = model.getFactionRecord(entry.getIdentifier());
                if (!chkShowSubfactions.isSelected() && rec.getKey().contains(".")) {
                    return false;
                }

                return chkShowMinorFactions.isSelected() || !rec.isMinor();
            }
        };
        masterFactionListSorter.setRowFilter(rf);
    }

    private void filterMasterUnitList() {
        RowFilter<MasterUnitListTableModel, Integer> rf = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends MasterUnitListTableModel, ? extends Integer> entry) {
                MasterUnitListTableModel model = entry.getModel();
                ModelRecord rec = model.getUnitRecord(entry.getIdentifier());
                if (cbUnitType.getSelectedIndex() > 0 && !UnitType.getTypeName(rec.getUnitType()).equals(cbUnitType.getSelectedItem())) {
                    return false;
                }

                if (cbMovementType.getSelectedIndex() > 0 && (rec.getMovementMode() != EntityMovementMode.parseFromString((String) cbMovementType.getSelectedItem()))) {
                    return false;
                }

                if (!txtSearch.getText().isBlank()) {
                    return rec.getKey().toLowerCase().contains(txtSearch.getText().toLowerCase());
                }
                return true;
            }
        };
        masterUnitListSorter.setRowFilter(rf);
    }

    private static class MasterUnitListTableModel extends DefaultTableModel {
        private static final long serialVersionUID = 2792332961159226169L;

        public static final int COL_CHASSIS = 0;
        public static final int COL_MODEL = 1;
        public static final int COL_UNIT_TYPE = 2;
        public static final int COL_WEIGHT = 3;
        public static final int COL_YEAR = 4;
        public static final int COL_EXTINCT_RANGE = 5;
        public static final int COL_CANON_ROLE = 6;
        public static final int COL_ROLE = 7;
        public static final int COL_DEPLOYED_WITH = 8;
        public static final int COL_EXCLUDE_FACTIONS = 9;
        public static final int NUM_COLS = 10;
        public static final String[] colNames = {
                "Chassis", "Model", "Unit Type", "Weight", "Year", "Extinct Years", "MUL Role", "Role", "Deployed With", "Exclude Factions"
        };

        private final ArrayList<ModelRecord> data;

        public MasterUnitListTableModel(Collection<ModelRecord> modelList) {
            data = new ArrayList<>();
            data.addAll(modelList);
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        @Override
        public int getColumnCount() {
            return NUM_COLS;
        }

        @Override
        public int getRowCount() {
            if (data == null) {
                return 0;
            }
            return data.size();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col >= COL_ROLE;
        }

        @Override
        public Class<?> getColumnClass(int col) {
            if (col == COL_YEAR) {
                return Integer.class;
            }
            return String.class;
        }

        @Override
        public Object getValueAt(int row, int col) {
            switch (col) {
                case COL_CHASSIS:
                    return data.get(row).getChassis();
                case COL_MODEL:
                    if (data.get(row).isClan()) {
                        return data.get(row).getModel() + "[C]";
                    } else if (data.get(row).isSL()) {
                        return data.get(row).getModel() + "[*]";
                    } 
                    return data.get(row).getModel();
                case COL_UNIT_TYPE:
                    if (data.get(row).getMechSummary() == null) {
                        System.err.println("Could not find mechsummary for " + data.get(row).getKey());
                    }
                    return data.get(row).getMechSummary().getUnitType();
                case COL_WEIGHT:
                    return data.get(row).getMechSummary().getTons();
                case COL_YEAR:
                    if (data.get(row).getMechSummary() == null) {
                        System.err.println("Could not find mechsummary for " + data.get(row).getKey());
                    }
                    return data.get(row).getMechSummary().getYear();
                    
                case COL_EXTINCT_RANGE:
                    if (data.get(row).getMechSummary() == null) {
                        System.err.println("Could not find mechsummary for " + data.get(row).getKey());
                    }
                    return data.get(row).getMechSummary().getExtinctRange();
                	
                case COL_ROLE:
                    return data.get(row).getRoles().stream().map(Object::toString).collect(Collectors.joining(","));
                case COL_CANON_ROLE:
                    return UnitRoleHandler.getRoleFor(data.get(row).getChassis() + " " + data.get(row).getModel()).toString();
                case COL_DEPLOYED_WITH:
                    StringJoiner sj = new StringJoiner(",");
                    data.get(row).getDeployedWith().forEach(sj::add);
                    data.get(row).getRequiredUnits().forEach(s -> sj.add("req:" + s));
                    return sj.toString();
                case COL_EXCLUDE_FACTIONS:
                    return String.join(",", data.get(row).getExcludedFactions());
                default:
                    return "?";
            }
        }

        @Override
        public void setValueAt(Object val, int row, int col) {
            if (col == COL_ROLE) {
                data.get(row).addRoles((String) val);
            } else if (col == COL_DEPLOYED_WITH) {
                data.get(row).getRequiredUnits().clear();
                data.get(row).getDeployedWith().clear();
                if (!((String) val).isBlank()) {
                    for (String unit : ((String) val).split(",")) {
                        if (unit.startsWith("req:")) {
                            data.get(row).getRequiredUnits().add(unit);             
                        } else {
                            data.get(row).getDeployedWith().add(unit);
                        }
                    }
                }
            } else if (col == COL_EXCLUDE_FACTIONS) {
                data.get(row).setExcludedFactions((String) val);
            }
        }

        public ModelRecord getUnitRecord(int row) {
            return data.get(row);
        }
    }

    private static class UnitEditorTableModel extends DefaultTableModel {

        public static final int MODE_MODEL = 0;
        public static final int MODE_CHASSIS = 1;
        public static final int MODE_SUMMARY = 2;

        ArrayList<String> factions;
        HashMap<String, List<String>> data;
        private int mode;
        private AbstractUnitRecord unitRecord;

        private String getUnitKey() {
            return (mode == MODE_CHASSIS) ? unitRecord.getChassisKey() : unitRecord.getKey();
        }

        public UnitEditorTableModel() {
            factions = new ArrayList<>();
            data = new HashMap<>();
            unitRecord = null;
        }

        public void clearData() {
            factions.clear();
            data.clear();
            unitRecord = null;
            fireTableDataChanged();
        }

        public void setData(AbstractUnitRecord unitRec, int mode) {
            factions.clear();
            data.clear();
            this.mode = mode;
            unitRecord = unitRec;
            final ArrayList<String> empty = new ArrayList<>();
            while (empty.size() < rg.getEraSet().size()) {
                empty.add("");
            }
            for (int i = 0; i < rg.getEraSet().size(); i++) {
                Collection<AvailabilityRating> recs = (mode == MODE_MODEL)
                        ? rg.getModelFactionRatings(ERAS[i], getUnitKey())
                        : rg.getChassisFactionRatings(ERAS[i], unitRec.getChassisKey());
                if (recs != null) {
                    for (AvailabilityRating rec : recs) {
                        String key = rec.getFactionCode();
                        if (!factions.contains(key)) {
                            factions.add(key);
                            data.put(key, new ArrayList<>(empty));
                        }
                        if (mode == MODE_SUMMARY) {
                            AvailabilityRating mar = rg.findModelAvailabilityRecord(ERAS[i],
                                    unitRec.getKey(), rec.getFaction());
                            if (mar != null) {
                                int weight = (int) ((rec.getWeight() * 10.0 * mar.getWeight()) /
                                        rg.getChassisRecord(unitRec.getChassisKey()).
                                        totalModelWeight(ERAS[i], rec.getFaction()) + 0.5);
                                if (weight > 0) {
                                    data.get(key).set(i, Integer.toString(weight));
                                }
                            }
                        } else if (rec.getEra() == rec.getStartYear()) {
                            data.get(key).set(i, rec.getAvailabilityCode());
                        } else {
                            data.get(key).set(i, rec.getAvailabilityCode() + ":" + rec.getStartYear());
                        }
                    }
                }
            }
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int col) {
            if (col == 0) {
                return "Faction";
            }
            return "<HTML><CENTER>" + ERAS[col - 1] + "<BR>" + getEra(ERAS[col - 1]) + "</HTML>";
        }

        @Override
        public int getColumnCount() {
            if (data == null) {
                return 0;
            }
            return ERAS.length + 1;
        }

        @Override
        public int getRowCount() {
            if (data == null) {
                return 0;
            }
            return data.size();
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return String.class;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (col == 0) {
                return factions.get(row);
            } else {
                return data.get(factions.get(row)).get(col - 1);
            } 
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return mode < MODE_SUMMARY && col > 0;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (!(value instanceof String)) {
                return;
            }
            String stringValue = (String) value;
            if (!stringValue.isBlank() && !stringValue.matches("\\d+[+\\-]?(:\\d+)?")) {
                return;
            }
            AvailabilityRating ar;
            int era = ERAS[col - 1];
            if (stringValue.isBlank()) {
                ar = null;
            } else {
                ar = new AvailabilityRating(getUnitKey(), era, factions.get(row) + ":" + stringValue);
            }
            data.get(factions.get(row)).set(col - 1, stringValue);
            if (rg.getChassisRecord(getUnitKey()) != null) {
                if (ar == null) {
                    rg.removeChassisFactionRating(era, getUnitKey(), factions.get(row));                
                } else {
                    rg.setChassisFactionRating(era, getUnitKey(), ar);
                }
            } else {
                if (ar == null) {
                    rg.removeModelFactionRating(era, getUnitKey(), factions.get(row));               
                } else {
                    rg.setModelFactionRating(era, getUnitKey(), ar);
                }
            }
        }

        public boolean addEntry(String faction) {
            ArrayList<String> list = new ArrayList<>();
            while (list.size() < ERAS.length) {
                list.add("");
            }
            return addEntry(faction, list);
        }

        public boolean addEntry(String faction, List<String> factionData) {
            if (unitRecord == null) {
                return false;
            }

            if (mode == MODE_MODEL) {
                boolean chassisRecordFound = false;
                for (int era : ERAS) {
                    if (rg.getChassisFactionRatings(era, unitRecord.getChassisKey()) != null) {
                        chassisRecordFound = true;
                        break;
                    }
                }

                if (!chassisRecordFound) {
                    return false;
                }
            }

            factions.add(faction);
            data.put(faction, factionData);
            fireTableDataChanged();
            return true;
        }

        public boolean addEntry(RowData rowData) {
            boolean rowAdded = addEntry(rowData.faction, rowData.eraData);
            if (rowAdded) {
                for (int i = 0; i < ERAS.length; i++) {
                    if (!rowData.eraData.get(i).isBlank()) {
                        AvailabilityRating ar = new AvailabilityRating(getUnitKey(), ERAS[i],
                                rowData.faction + ":" + rowData.eraData.get(i));
                        if (mode == MODE_MODEL) {
                            rg.setModelFactionRating(ERAS[i], getUnitKey(), ar);
                        } else {
                            rg.setChassisFactionRating(ERAS[i], getUnitKey(), ar);
                        }
                    }
                }
            }
            return rowAdded;
        }

        public void removeEntry(int row) {
            for (int era : ERAS) {
                if (mode == MODE_CHASSIS) {
                    rg.removeChassisFactionRating(era, getUnitKey(), factions.get(row));                
                } else {
                    rg.removeModelFactionRating(era, getUnitKey(), factions.get(row));                               
                }
            }
            data.remove(factions.get(row));
            factions.remove(row);
            fireTableDataChanged();
        }

        /** Returns the given row data as a List with the first entry being the faction. */
        public RowData getRow(int row) {
            return new RowData(factions.get(row), data.get(factions.get(row)));
        }

        public void copyRow(int row, String newFaction) {
            List<String> copyFrom = data.get(factions.get(row));
            factions.add(newFaction);
            List<String> copyTo = new ArrayList<>();
            for (int i = 0; i < ERAS.length; i++) {
                copyTo.add(copyFrom.get(i));
            }       
            data.put(newFaction, copyTo);
            for (int i = 0; i < ERAS.length; i++) {
                if (!copyFrom.get(i).isBlank()) {
                    AvailabilityRating ar = new AvailabilityRating(getUnitKey(), ERAS[i],
                            newFaction + ":" + copyFrom.get(i));
                    if (mode == MODE_MODEL) {
                        rg.setModelFactionRating(ERAS[i], getUnitKey(), ar);
                    } else {
                        rg.setChassisFactionRating(ERAS[i], getUnitKey(), ar);
                    }
                }
            }
            fireTableDataChanged();
        }

        public int getMode() {
            return mode;
        }
    }

    private static class RowData {
        String faction;
        List<String> eraData;

        public RowData(String faction, List<String> eraData) {
            this.faction = faction;
            this.eraData = eraData;
        }
    }

    TableCellRenderer unitListRenderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int realModelRow = tblUnitModelEditor.convertRowIndexToModel(row);
            String faction = (String) unitModelEditorModel.getValueAt(realModelRow, 0);
            if (column == 0) {
                if (!currentChassisFactions.contains(faction)) {
                    setForeground(Color.RED);
                } else {
                    setForeground(null);
                }
            }
            setBackground(null);
            if (tblUnitChassisEditor.getSelectedRow() > -1) {
                int realRow = tblUnitChassisEditor.convertRowIndexToModel(tblUnitChassisEditor.getSelectedRow());
                String chassisSelectedFaction = unitChassisEditorModel.factions.get(realRow);
                if (faction.equals(chassisSelectedFaction)) {
                    setBackground(Color.YELLOW);
                }
            }
            return this;
        }
    };

    TableCellRenderer chassisListRenderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int realChassisRow = tblUnitChassisEditor.convertRowIndexToModel(row);
            String faction = (String) unitChassisEditorModel.getValueAt(realChassisRow, 0);
            setBackground(null);
            if (tblUnitModelEditor.getSelectedRow() > -1) {
                int realRow = tblUnitModelEditor.convertRowIndexToModel(tblUnitModelEditor.getSelectedRow());
                String modelSelectedFaction = unitModelEditorModel.factions.get(realRow);
                if (faction.equals(modelSelectedFaction)) {
                    setBackground(Color.GREEN);
                }
            }
            return this;
        }
    };

    private static class UnitTypeComparator implements Comparator<String> {
        private final Map<String, Integer> keys;

        public UnitTypeComparator() {
            keys = new HashMap<>();
            for (int i = 0; i < UnitType.SIZE; i++) {
                keys.put(UnitType.getTypeName(i), i);
            }
        }
        @Override
        public int compare(String arg0, String arg1) {
            return keys.get(arg0) - keys.get(arg1);
        }

    }

    private class FactionListTableModel extends DefaultTableModel {
        public static final int COL_CODE = 0;
        public static final int COL_NAME = 1;
        public static final int COL_YEARS = 2;
        public static final int COL_MINOR = 3;
        public static final int COL_CLAN = 4;
        public static final int COL_PERIPHERY = 5;
        public static final int COL_RATINGS = 6;
        public static final int COL_USE_ALT_FACTION = 7;
        public static final int NUM_COLS = 8;
        
        public final String[] colNames = {"Code", "Name", "Years", "Minor", "Clan",
            "Periphery", "Ratings", "Use Alt"};
        
        private final ArrayList<FactionRecord> data;
        
        public FactionListTableModel(Collection<FactionRecord> factionList) {
            data = new ArrayList<>();
            data.addAll(factionList);
            fillFactionChoosers();
        }
        
        public void addRecord(FactionRecord rec) {
            data.add(rec);
            fireTableDataChanged();
            fillFactionChoosers();
        }
        
        public void delRecord(FactionRecord rec) {
            data.remove(rec);
            fireTableDataChanged();
            fillFactionChoosers();
        }
        
        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        @Override
        public int getColumnCount() {
            return NUM_COLS;
        }
        
        @Override
        public int getRowCount() {
            if (data == null) {
                return 0;
            }
            return data.size();
        }
        
        @Override
        public boolean isCellEditable(int row, int col) {
            return col > COL_CODE;
        }
        
        @Override
        public Class<?> getColumnClass(int col) {
            if (col == COL_MINOR || col == COL_CLAN || col == COL_PERIPHERY) {
                return Boolean.class;
            }
            return String.class;
        }
        
        @Override
        public Object getValueAt(int row, int col) {
            switch (col) {
                case COL_CODE:
                    return data.get(row).getKey();
                case COL_NAME:
                    return data.get(row).getNamesAsString();
                case COL_YEARS:
                    return data.get(row).getYearsAsString();
                case COL_MINOR:
                    return data.get(row).isMinor();
                case COL_CLAN:
                    return data.get(row).isClan();
                case COL_PERIPHERY:
                    return data.get(row).isPeriphery();
                case COL_RATINGS:
                    return String.join(",", data.get(row).getRatingLevels());
                case COL_USE_ALT_FACTION:
                    return String.join(",", data.get(row).getParentFactions());
                default:
                    return "?";
            }
        }
        
        @Override
        public void setValueAt(Object val, int row, int col) {
            switch (col) {
                case COL_CLAN:
                    data.get(row).setClan((Boolean) val);
                    break;
                case COL_PERIPHERY:
                    data.get(row).setPeriphery((Boolean) val);
                    break;
                case COL_NAME:
                    data.get(row).setNames((String) val);
                    break;
                case COL_YEARS:
                    try {
                        data.get(row).setYears((String) val);
                    } catch (Exception ex) {
                        //Illegal format; ignore new value
                    }
                    break;
                case COL_MINOR:
                    data.get(row).setMinor((Boolean) val);
                    break;
                case COL_RATINGS:
                    data.get(row).setRatings((String) val);
                    break;
                case COL_USE_ALT_FACTION:
                    data.get(row).setParentFactions((String) val);
            }
        }
        
        public FactionRecord getFactionRecord(int row) {
            return data.get(row);
        }
        
    }
    
    private static class FactionEditorTableModel extends DefaultTableModel {
        private static final int CAT_OMNI_PCT = 0;
        private static final int CAT_CLAN_PCT = 1;
        private static final int CAT_SL_PCT = 2;
        private static final int CAT_OMNI_AERO_PCT = 3;
        private static final int CAT_CLAN_AERO_PCT = 4;
        private static final int CAT_SL_AERO_PCT = 5;
        private static final int CAT_CLAN_VEE_PCT = 6;
        private static final int CAT_SL_VEE_PCT = 7;

        private static final String[] CATEGORIES = {
            "Omni %", "Clan %", "SL %",
            "Omni % (Aero)", "Clan % (Aero)", "SL % (Aero)",
            "Clan % (Vee)", "SL % (Vee)"
        };
        
        private static final int[] WEIGHT_DIST_UNIT_TYPES = {
                UnitType.MEK, UnitType.TANK, UnitType.AERO
        };
        
        private FactionRecord factionRec;
        
        public FactionEditorTableModel(FactionRecord rec) {
            factionRec = rec;
        }

        public void clearData() {
            setData(null);
        }
        
        public void setData(FactionRecord rec) {
            factionRec = rec;
            fireTableDataChanged();
        }
        
        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "";
            } else {
                return ERAS[column - 1] + " (" + getEra(ERAS[column - 1]) + ")";
            }
        }

        @Override
        public int getColumnCount() {
            return ERAS.length + 1;
        }
        
        @Override
        public int getRowCount() {
            if (factionRec == null) {
                return 0;
            }
            return 1 + CATEGORIES.length * factionRec.getRatingLevels().size()
                    + WEIGHT_DIST_UNIT_TYPES.length;
        }
        
        @Override
        public boolean isCellEditable(int row, int col) {
            return factionRec != null && col > 0;
        }
        
        @Override
        public Class<?> getColumnClass(int col) {
            return String.class;
        }
        
        @Override
        public Object getValueAt(int row, int col) {
            if (factionRec == null) {
                return "";
            }
            if (col == 0) {
                if (row == 0) {
                    return "Salvage %";
                } else if (row > factionRec.getRatingLevels().size() * CATEGORIES.length) {
                    return WEIGHT_DIST_UNIT_TYPES[row - 1 - factionRec.getRatingLevels().size() * CATEGORIES.length];
                } else {
                    return CATEGORIES[(row - 1) / factionRec.getRatingLevels().size()]
                            + " " + factionRec.getRatingLevels().get((row - 1) % factionRec.getRatingLevels().size());
                }
            }
            int era = ERAS[col - 1];
            
            if (row == 0) {
                Integer pct = factionRec.getPctSalvage(era);
                return (pct == null) ? "" : pct.toString();
            } else if (row > factionRec.getRatingLevels().size() * CATEGORIES.length) {
                int unitType = WEIGHT_DIST_UNIT_TYPES[row - 1 - factionRec.getRatingLevels().size() * CATEGORIES.length];
                return factionRec.getWeightDistributionAsString(era, unitType);
            }
            int rating = (row - 1) % factionRec.getRatingLevels().size();
            switch ((row - 1) / factionRec.getRatingLevels().size()) {
                case CAT_OMNI_PCT:
                    return factionRec.getPctTech(TechCategory.OMNI, era, rating);
                case CAT_CLAN_PCT:
                    return factionRec.getPctTech(TechCategory.CLAN, era, rating);
                case CAT_SL_PCT:
                    return factionRec.getPctTech(TechCategory.IS_ADVANCED, era, rating);
                case CAT_OMNI_AERO_PCT:
                    return factionRec.getPctTech(TechCategory.OMNI_AERO, era, rating);
                case CAT_CLAN_AERO_PCT:
                    return factionRec.getPctTech(TechCategory.CLAN_AERO, era, rating);
                case CAT_SL_AERO_PCT:
                    return factionRec.getPctTech(TechCategory.IS_ADVANCED_AERO, era, rating);
                case CAT_CLAN_VEE_PCT:
                    return factionRec.getPctTech(TechCategory.CLAN_VEE, era, rating);
                case CAT_SL_VEE_PCT:
                    return factionRec.getPctTech(TechCategory.IS_ADVANCED_VEE, era, rating);
                default:
                    return "?";
            }
        }
        
        @Override
        public void setValueAt(Object val, int row, int col) {
            int era = ERAS[col - 1];
            Integer value = null;
            if (!((String) val).isBlank()) {
                try {
                    value = Integer.parseInt((String) val);
                } catch (Exception ignored) {

                }
            }
            if (row == 0) {
                factionRec.setPctSalvage(era, value);
            } else if (row > factionRec.getRatingLevels().size() * CATEGORIES.length) {
                int unitType = WEIGHT_DIST_UNIT_TYPES[row - 1 - factionRec.getRatingLevels().size() * CATEGORIES.length];
                if (!((String) val).isBlank()) {
                    try {
                        factionRec.setWeightDistribution(era, unitType, (String) val);
                    } catch (Exception ignored) {

                    }
                } else {
                    factionRec.setWeightDistribution(era, unitType, null);
                }
            } else if (null != value) {
                int rating = (row - 1) % factionRec.getRatingLevels().size();
                switch ((row - 1) / factionRec.getRatingLevels().size()) {
                    case CAT_OMNI_PCT:
                        factionRec.setPctTech(TechCategory.OMNI, era, rating, value);
                        break;
                    case CAT_CLAN_PCT:
                        factionRec.setPctTech(TechCategory.CLAN, era, rating, value);
                        break;
                    case CAT_SL_PCT:
                        factionRec.setPctTech(TechCategory.IS_ADVANCED, era, rating, value);
                        break;
                    case CAT_OMNI_AERO_PCT:
                        factionRec.setPctTech(TechCategory.OMNI_AERO, era, rating, value);
                        break;
                    case CAT_CLAN_AERO_PCT:
                        factionRec.setPctTech(TechCategory.CLAN_AERO, era, rating, value);
                        break;
                    case CAT_SL_AERO_PCT:
                        factionRec.setPctTech(TechCategory.IS_ADVANCED_AERO, era, rating, value);
                        break;
                    case CAT_CLAN_VEE_PCT:
                        factionRec.setPctTech(TechCategory.CLAN_VEE, era, rating, value);
                        break;
                    case CAT_SL_VEE_PCT:
                        factionRec.setPctTech(TechCategory.IS_ADVANCED_VEE, era, rating, value);
                        break;
                }
            }
        }
        
    }

    private static class SalvageEditorTableModel extends DefaultTableModel {
        ArrayList<String> factions;
        HashMap<String, ArrayList<String>> data;
        private FactionRecord factionRec;
        
        public SalvageEditorTableModel() {
            factions = new ArrayList<>();
            data = new HashMap<>();
            factionRec = null;
        }

        public void clearData() {
            factionRec = null;
            factions.clear();
            data.clear();
            fireTableDataChanged();
        }

        public void setData(FactionRecord rec) {
            factionRec = rec;
            factions.clear();
            data.clear();
            final ArrayList<String> empty = new ArrayList<>();
            while (empty.size() < ERAS.length) {
                empty.add("");
            }
            for (int i = 0; i < ERAS.length; i++) {
                HashMap<String,Integer> recs = factionRec.getSalvage(ERAS[i]);
                if (recs != null) {
                    for (String faction : recs.keySet()) {
                        if (!factions.contains(faction)) {
                            factions.add(faction);
                            data.put(faction, new ArrayList<>(empty));
                        }
                        data.get(faction).set(i, recs.get(faction).toString());
                    }
                }
            }
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int col) {
            if (col == 0) {
                return "Faction";
            }
            return ERAS[col - 1] + " (" + getEra(ERAS[col - 1]) + ")";
        }
        
        @Override
        public int getColumnCount() {
            if (data == null) {
                return 0;
            }
            return ERAS.length + 1;
        }
        
        @Override
        public int getRowCount() {
            if (data == null) {
                return 0;
            }
            return data.size();
        }
        
        @Override
        public Class<?> getColumnClass(int col) {
            return String.class;
        }
        
        @Override
        public Object getValueAt(int row, int col) {
            if (col == 0) {
                return factions.get(row);
            } else {
                return data.get(factions.get(row)).get(col - 1);
            } 
        }
        
        @Override
        public boolean isCellEditable(int row, int col) {
            return col > 0;
        }
        
        @Override
        public void setValueAt(Object value, int row, int col) {
            Integer wt;
            int era = ERAS[col - 1];
            if ("".equals(value)) {
                wt = null;
            } else if (!((String) value).matches("\\d+")) {
                return;
            } else {
                wt = Integer.parseInt((String) value);
            }
            data.get(factions.get(row)).set(col - 1, (String) value);
            if (wt == null) {
                factionRec.removeSalvage(era, factions.get(row));
            } else {
                factionRec.setSalvage(era, factions.get(row), wt);
            }
        }
        
        public void addEntry(String faction) {
            factions.add(faction);
            ArrayList<String> list = new ArrayList<>();
            while (list.size() < ERAS.length) {
                list.add("");
            }
            data.put(faction, list);
            fireTableDataChanged();
        }
        
        public void removeEntry(int row) {
            for (int era : ERAS) {
                factionRec.removeSalvage(era, factions.get(row));
            }
            data.remove(factions.get(row));
            factions.remove(row);
            fireTableDataChanged();
        }
        
        public void copyRow(int row, String newFaction) {
            ArrayList<String> copyFrom = data.get(factions.get(row));
            factions.add(newFaction);
            ArrayList<String> copyTo = new ArrayList<>();
            for (int i = 0; i < ERAS.length; i++) {
                copyTo.add(copyFrom.get(i));
            }       
            data.put(newFaction, copyTo);
            for (int i = 0; i < ERAS.length; i++) {
                if (!copyFrom.get(i).isBlank()) {
                    factionRec.setSalvage(ERAS[i], newFaction, Integer.parseInt(copyFrom.get(i)));
                }
            }
            fireTableDataChanged();
        }
    }

    private static String getEra(int year) {
        return Eras.getEra(year).code();
    }

    /**
     * Runs the RATGeneratorEditor UI
     *
     * @param args The RATGenerator data will be loaded from the directory named as the first element
     *             of the arguments. If the {@code args} element is empty, or the first element
     *             is not a valid directory, loads from the default location.
     */
    public static void main(String... args) {
        SwingUtilities.invokeLater (() -> {
            RATGeneratorEditor ui;
            if (args.length > 0) {
                File dir = new File(args[0]);
                if (dir.exists() && dir.isDirectory()) {
                    ui = new RATGeneratorEditor(dir);
                } else {
                    LogManager.getLogger().info(args[0] + " is not a valid directory name");
                    ui = new RATGeneratorEditor();
                }
            } else {
                ui = new RATGeneratorEditor();
            }
            ui.setVisible(true);
        });
    }
}
