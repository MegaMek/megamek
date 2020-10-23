/*
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.utils;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import megamek.MegaMek;
import megamek.client.ratgenerator.AbstractUnitRecord;
import megamek.client.ratgenerator.AvailabilityRating;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.FactionRecord.TechCategory;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.Configuration;
import megamek.common.EntityMovementMode;
import megamek.common.UnitType;

/**
 * @author neoancient
 *
 */
public class RATGeneratorEditor extends JFrame {

    private static final long serialVersionUID = 6544418066071039219L;

    private static final String[] MOVEMENT_TYPE_NAMES = {
            "Leg", "Tracked", "Wheeled", "Hover", "WiGE", "VTOL",
            "Naval", "Underwater", "Jump", "Motorized", "Atmospheric",
            "Aerospace", "Space", "None"
    };

    private static RATGenerator rg;
    
    private static Integer[] ERAS;

    private final JComboBox<String> cbUnitType = new JComboBox<>();
    private final JComboBox<String> cbMovementType = new JComboBox<>();
    private final JRadioButton radioModel = new JRadioButton("Model");
    private final JRadioButton radioChassis = new JRadioButton("Chassis");

    private final JTabbedPane panMain = new JTabbedPane();

    private final JTextField txtSearch = new JTextField();
    private final JTable tblMasterUnitList = new JTable();
    private MasterUnitListTableModel masterUnitListModel;
    private TableRowSorter<MasterUnitListTableModel> masterUnitListSorter;

    private final JTextField txtFaction = new JTextField();
    private final JTable tblUnitEditor = new JTable();
    private final UnitEditorTableModel unitEditorModel = new UnitEditorTableModel();

    private final JTextField txtNewFaction = new JTextField();
    private final JCheckBox chkShowSubfactions = new JCheckBox();
    private final JCheckBox chkShowMinorFactions = new JCheckBox();
    private final JTable tblMasterFactionList = new JTable();
    private FactionListTableModel masterFactionListModel;
    private TableRowSorter<FactionListTableModel> masterFactionListSorter;
    private final JTable tblFactionEditor = new JTable();
    private FactionEditorTableModel factionEditorModel;
    private final JTextField txtSalvageFaction = new JTextField();
    private final JTable tblSalvageEditor = new JTable();
    private SalvageEditorTableModel salvageEditorModel;

    private File lastDir = Configuration.forceGeneratorDir();

    public RATGeneratorEditor() {
        rg = RATGenerator.getInstance();
        while (!rg.isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
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

        masterUnitListModel = new MasterUnitListTableModel(rg.getModelList());

        setLayout(new GridBagLayout());

        buildOptionPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        add(panMain, gbc);

        JPanel panButtons = new JPanel();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(panButtons, gbc);

        JPanel panEditTab = createUnitPanel();
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        panMain.addTab("Edit", panEditTab);

        JPanel panFactionEditorTab = createFactionTab();
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        panMain.addTab("Edit Factions", panFactionEditorTab);

        JButton button = new JButton("Load");
        button.setToolTipText("Load data from alternate location");
        panButtons.add(button);
        button.addActionListener(ev -> loadAltDir());

        button = new JButton("Save");
        button.setToolTipText("Export data to a selected directory");
        panButtons.add(button);
        button.addActionListener(ev -> saveValues());

        pack();

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

    private void buildOptionPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);

        cbUnitType.addItem("All");
        for (int i = 0; i < UnitType.SIZE; i++) {
            cbUnitType.addItem(UnitType.getTypeName(i));
        }
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Unit Type:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        add(cbUnitType, gbc);
        cbUnitType.addActionListener(ev -> filterMasterUnitList());

        cbMovementType.addItem("All");
        for (String movementTypeName : MOVEMENT_TYPE_NAMES) {
            cbMovementType.addItem(movementTypeName);
        }
        gbc.gridx = 2;
        gbc.gridy = 0;
        add(new JLabel("Movement Type:"), gbc);
        gbc.gridx = 3;
        gbc.gridy = 0;
        add(cbMovementType, gbc);
        cbMovementType.addActionListener(arg0 -> filterMasterUnitList());

        radioModel.setSelected(true);
        gbc.gridx = 4;
        gbc.gridy = 0;
        add(radioModel, gbc);
        radioModel.addActionListener(ev -> {
            if (unitEditorModel.getMode() != UnitEditorTableModel.MODE_MODEL &&
                    tblMasterUnitList.getSelectedRow() >= 0) {
                ModelRecord model = masterUnitListModel.getUnitRecord(tblMasterUnitList.convertRowIndexToModel(tblMasterUnitList.getSelectedRow()));
                unitEditorModel.setData(model, UnitEditorTableModel.MODE_MODEL);
            } else {
                unitEditorModel.clearData();
            }
        });

        gbc.gridx = 5;
        gbc.gridy = 0;
        add(radioChassis, gbc);
        radioChassis.addActionListener(ev ->  {
            if (unitEditorModel.getMode() != UnitEditorTableModel.MODE_CHASSIS &&
                    tblMasterUnitList.getSelectedRow() >= 0) {
                ModelRecord model = masterUnitListModel.getUnitRecord(tblMasterUnitList.convertRowIndexToModel(tblMasterUnitList.getSelectedRow()));
                unitEditorModel.setData(model, UnitEditorTableModel.MODE_CHASSIS);
            } else {
                unitEditorModel.clearData();
            }
        });

        ButtonGroup group = new ButtonGroup();
        group.add(radioModel);
        group.add(radioChassis);
    }

    private JPanel createUnitPanel() {
        JPanel panEditTab = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);

        txtSearch.setMinimumSize(new Dimension(200, 25));
        txtSearch.setPreferredSize(new Dimension(200, 25));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panEditTab.add(new JLabel("Search:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panEditTab.add(txtSearch, gbc);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void changedUpdate(DocumentEvent arg0) {
                filterMasterUnitList();
            }

            @Override
            public void insertUpdate(DocumentEvent arg0) {
                filterMasterUnitList();
            }

            @Override
            public void removeUpdate(DocumentEvent arg0) {
                filterMasterUnitList();
            }

        });

        tblMasterUnitList.setModel(masterUnitListModel);
        masterUnitListSorter =
                new TableRowSorter<>(masterUnitListModel);
        masterUnitListSorter.setComparator(MasterUnitListTableModel.COL_UNIT_TYPE, new UnitTypeComparator());
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(MasterUnitListTableModel.COL_UNIT_TYPE, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(MasterUnitListTableModel.COL_CHASSIS, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(MasterUnitListTableModel.COL_MODEL, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(MasterUnitListTableModel.COL_YEAR, SortOrder.ASCENDING));
        masterUnitListSorter.setSortKeys(sortKeys);
        tblMasterUnitList.setRowSorter(masterUnitListSorter);
        tblMasterUnitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMasterUnitList.getSelectionModel().addListSelectionListener(arg0 -> {
            if (tblMasterUnitList.getSelectedRow() >= 0) {
                ModelRecord rec = masterUnitListModel.
                        getUnitRecord(tblMasterUnitList.
                                convertRowIndexToModel(tblMasterUnitList.
                                        getSelectedRow()));
                unitEditorModel.setData(rec, radioModel.isSelected() ? UnitEditorTableModel.MODE_MODEL : UnitEditorTableModel.MODE_CHASSIS);
            } else {
                unitEditorModel.clearData();
            }
        });

        JScrollPane scroll = new JScrollPane();
        scroll.setMinimumSize(new Dimension(600, 400));
        scroll.setPreferredSize(new Dimension(600, 400));
        scroll.setViewportView(tblMasterUnitList);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panEditTab.add(scroll, gbc);

        txtFaction.setMinimumSize(new Dimension(60,25));
        txtFaction.setPreferredSize(new Dimension(60,25));
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        panEditTab.add(txtFaction, gbc);

        JButton button = new JButton("Add Row");
        gbc.gridx = 3;
        gbc.gridy = 0;
        panEditTab.add(button, gbc);
        button.addActionListener(ev -> {
            
            if(!unitEditorModel.addEntry(txtFaction.getText())) {
                JOptionPane.showMessageDialog(this, 
                        "Unable to add model or chassis entry. Please select a unit model. " +
                        "If adding a model entry, make sure you already have a chassis entry defined.");
            }
        });

        button = new JButton("Delete Row");
        gbc.gridx = 4;
        gbc.gridy = 0;
        panEditTab.add(button, gbc);
        button.addActionListener(ev -> {
            if (tblUnitEditor.getSelectedRow() >= 0) {
                unitEditorModel.removeEntry(tblUnitEditor.convertRowIndexToModel(tblUnitEditor.getSelectedRow()));
            }
        });

        button = new JButton("Copy Row");
        gbc.gridx = 5;
        gbc.gridy = 0;
        panEditTab.add(button, gbc);
        button.addActionListener(ev -> {
            if (tblUnitEditor.getSelectedRow() >= 0) {
                unitEditorModel.copyRow(tblUnitEditor.convertRowIndexToModel(tblUnitEditor.getSelectedRow()),
                        txtFaction.getText());
            }
        });

        tblUnitEditor.setModel(unitEditorModel);
        tblUnitEditor.createDefaultColumnsFromModel();
        tblUnitEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        scroll = new JScrollPane(tblUnitEditor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setMinimumSize(new Dimension(600, 400));
        scroll.setPreferredSize(new Dimension(600, 400));
        scroll.setViewportView(tblUnitEditor);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 8;
        gbc.weightx = 1.0;
        panEditTab.add(scroll, gbc);

        return panEditTab;
    }

    private JPanel createFactionTab() {
        JPanel panFactionEditorTab = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);

        txtNewFaction.setMinimumSize(new Dimension(100, 25));
        txtNewFaction.setPreferredSize(new Dimension(100, 25));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panFactionEditorTab.add(txtNewFaction, gbc);

        JButton button = new JButton("New");
        gbc.gridx = 1;
        gbc.gridy = 0;
        panFactionEditorTab.add(button, gbc);
        button.addActionListener(ev -> {
            if (txtNewFaction.getText().length() > 0) {
                FactionRecord rec = new FactionRecord(txtNewFaction.getText());
                rg.addFaction(rec);
                masterFactionListModel.addRecord(rec);
            }
        });

        button = new JButton("Delete");
        gbc.gridx = 2;
        gbc.gridy = 0;
        panFactionEditorTab.add(button, gbc);
        button.addActionListener(ev -> {
            if (tblMasterFactionList.getSelectedRow() >= 0) {
                FactionRecord rec = masterFactionListModel.getFactionRecord(
                        tblMasterFactionList.convertRowIndexToModel(
                                tblMasterFactionList.getSelectedRow()));
                masterFactionListModel.delRecord(rec);
                rg.removeFaction(rec);
            }
        });

        button = new JButton("Copy");
        gbc.gridx = 3;
        gbc.gridy = 0;
        panFactionEditorTab.add(button, gbc);
        button.addActionListener(ev -> {
            if (txtNewFaction.getText().length() > 0
                    && tblMasterFactionList.getSelectedRow() >= 0) {
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
        gbc.gridx = 4;
        gbc.gridy = 0;
        panFactionEditorTab.add(chkShowSubfactions, gbc);
        chkShowSubfactions.addActionListener(ev -> filterFactionList());

        chkShowMinorFactions.setText("Show Minor Factions");
        chkShowMinorFactions.setSelected(true);
        gbc.gridx = 5;
        gbc.gridy = 0;
        panFactionEditorTab.add(chkShowMinorFactions, gbc);
        chkShowMinorFactions.addActionListener(ev -> filterFactionList());

        masterFactionListModel = new FactionListTableModel(rg.getFactionList());
        tblMasterFactionList.setModel(masterFactionListModel);
        masterFactionListSorter =
                new TableRowSorter<>(masterFactionListModel);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(FactionListTableModel.COL_CODE, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(FactionListTableModel.COL_NAME, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(FactionListTableModel.COL_CLAN, SortOrder.ASCENDING));
        masterFactionListSorter.setSortKeys(sortKeys);
        tblMasterFactionList.setRowSorter(masterFactionListSorter);
        tblMasterFactionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMasterFactionList.getSelectionModel().addListSelectionListener(arg0 -> {
            if (tblMasterFactionList.getSelectedRow() >= 0) {
                FactionRecord rec = masterFactionListModel.
                        getFactionRecord(tblMasterFactionList.
                                convertRowIndexToModel(tblMasterFactionList.
                                        getSelectedRow()));
                factionEditorModel.setData(rec);
                salvageEditorModel.setData(rec);
            } else {
                factionEditorModel.clearData();
                salvageEditorModel.clearData();
            }
        });

        JScrollPane scroll = new JScrollPane();
        scroll.setMinimumSize(new Dimension(600, 400));
        scroll.setPreferredSize(new Dimension(600, 400));
        scroll.setViewportView(tblMasterFactionList);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 6;
        gbc.gridheight = 4;
        panFactionEditorTab.add(scroll, gbc);

        factionEditorModel = new FactionEditorTableModel(null);
        tblFactionEditor.setModel(factionEditorModel);
        tblFactionEditor.createDefaultColumnsFromModel();
        tblFactionEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        scroll = new JScrollPane(tblFactionEditor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setMinimumSize(new Dimension(600, 200));
        scroll.setPreferredSize(new Dimension(600, 200));
        scroll.setViewportView(tblFactionEditor);
        gbc.gridx = 6;
        gbc.gridy = 1;
        gbc.gridwidth = 8;
        gbc.gridheight = 1;
        panFactionEditorTab.add(scroll, gbc);

        txtSalvageFaction.setMinimumSize(new Dimension(60,25));
        txtSalvageFaction.setPreferredSize(new Dimension(60,25));
        gbc.gridx = 6;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panFactionEditorTab.add(txtSalvageFaction, gbc);

        button = new JButton("Add Row");
        gbc.gridx = 7;
        gbc.gridy = 2;
        panFactionEditorTab.add(button, gbc);
        button.addActionListener(ev -> salvageEditorModel.addEntry(txtSalvageFaction.getText()));

        button = new JButton("Delete Row");
        gbc.gridx = 8;
        gbc.gridy = 2;
        panFactionEditorTab.add(button, gbc);
        button.addActionListener(ev -> {
                if (tblSalvageEditor.getSelectedRow() >= 0) {
                    salvageEditorModel.removeEntry(tblSalvageEditor.convertRowIndexToModel(tblSalvageEditor.getSelectedRow()));
                }
        });

        button = new JButton("Copy Row");
        gbc.gridx = 9;
        gbc.gridy = 2;
        panFactionEditorTab.add(button, gbc);
        button.addActionListener(ev -> {
                if (tblSalvageEditor.getSelectedRow() >= 0) {
                    salvageEditorModel.copyRow(tblSalvageEditor.convertRowIndexToModel(tblSalvageEditor.getSelectedRow()),
                            txtSalvageFaction.getText());
                }
        });

        salvageEditorModel = new SalvageEditorTableModel();
        tblSalvageEditor.setModel(salvageEditorModel);
        tblSalvageEditor.createDefaultColumnsFromModel();
        tblSalvageEditor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        scroll = new JScrollPane(tblSalvageEditor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setMinimumSize(new Dimension(600, 200));
        scroll.setPreferredSize(new Dimension(600, 200));
        scroll.setViewportView(tblSalvageEditor);
        gbc.gridx = 6;
        gbc.gridy = 3;
        gbc.gridwidth = 8;
        gbc.weightx = 1.0;
        panFactionEditorTab.add(scroll, gbc);

        return panFactionEditorTab;
    }

    private void filterFactionList() {
        RowFilter<FactionListTableModel, Integer> rf;
        rf = new RowFilter<FactionListTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends FactionListTableModel,
                    ? extends Integer> entry) {
                FactionListTableModel model = entry.getModel();
                FactionRecord rec = model.getFactionRecord(entry.getIdentifier());
                if (!chkShowSubfactions.isSelected() &&
                        rec.getKey().contains(".")) {
                    return false;
                }

                return chkShowMinorFactions.isSelected() ||
                        rec.getParentFactions() == null;
            }
        };
        masterFactionListSorter.setRowFilter(rf);
    }

    private void filterMasterUnitList() {
        RowFilter<MasterUnitListTableModel, Integer> rf;
        rf = new RowFilter<MasterUnitListTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends MasterUnitListTableModel,
                    ? extends Integer> entry) {
                MasterUnitListTableModel model = entry.getModel();
                ModelRecord rec = model.getUnitRecord(entry.getIdentifier());
                if (cbUnitType.getSelectedIndex() > 0 &&
                        !UnitType.getTypeName(rec.getUnitType()).equals(cbUnitType.getSelectedItem())) {
                    return false;
                }
                if (cbMovementType.getSelectedIndex() > 0 &&
                        (rec.getMovementMode() != EntityMovementMode.getMode((String) cbMovementType.getSelectedItem()))) {
                    return false;
                }
                if (txtSearch.getText().length() > 0) {
                    return rec.getKey().toLowerCase().contains(txtSearch.getText().toLowerCase());
                }
                return true;
            }
        };
        masterUnitListSorter.setRowFilter(rf);
    }

    private static class MasterUnitListTableModel extends DefaultTableModel {

        /**
         * 
         */
        private static final long serialVersionUID = 2792332961159226169L;

        public static final int COL_CHASSIS = 0;
        public static final int COL_MODEL = 1;
        public static final int COL_UNIT_TYPE = 2;
        public static final int COL_YEAR = 3;
        public static final int COL_ROLE = 4;
        public static final int COL_DEPLOYED_WITH = 5;
        public static final int COL_EXCLUDE_FACTIONS = 6;
        public static final int NUM_COLS = 7;
        public static final String[] colNames = {
                "Chassis", "Model", "Unit Type", "Year", "Role", "Deployed With", "Exclude Factions"
        };

        private ArrayList<ModelRecord> data;

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
                case COL_YEAR:
                    if (data.get(row).getMechSummary() == null) {
                        System.err.println("Could not find mechsummary for " + data.get(row).getKey());
                    }
                    return data.get(row).getMechSummary().getYear();
                case COL_ROLE:
                    return data.get(row).getRoles().stream().map(Object::toString).collect(Collectors.joining(","));
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
                data.get(row).addRoles((String)val);
            } else if (col == COL_DEPLOYED_WITH) {
                data.get(row).getRequiredUnits().clear();
                data.get(row).getDeployedWith().clear();
                if (((String)val).length() > 0) {
                    for (String unit : ((String)val).split(",")) {
                        if (unit.startsWith("req:")) {
                            data.get(row).getRequiredUnits().add(unit);             
                        } else {
                            data.get(row).getDeployedWith().add(unit);
                        }
                    }
                }
            } else if (col == COL_EXCLUDE_FACTIONS) {
                data.get(row).setExcludedFactions((String)val);
            }
        }

        public ModelRecord getUnitRecord(int row) {
            return data.get(row);
        }
    }

    private static class UnitEditorTableModel extends DefaultTableModel {

        private static final long serialVersionUID = 1323721840252090355L;

        public static final int MODE_MODEL = 0;
        public static final int MODE_CHASSIS = 1;
        public static final int MODE_SUMMARY = 2;

        ArrayList<String> factions;
        HashMap<String, ArrayList<String>> data;
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
                Collection<AvailabilityRating> recs = (mode == MODE_MODEL)?
                        rg.getModelFactionRatings(ERAS[i], getUnitKey()):
                            rg.getChassisFactionRatings(ERAS[i], unitRec.getChassisKey());
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
                                        int weight = (int)((rec.getWeight() * 10.0 * mar.getWeight()) /
                                                rg.getChassisRecord(unitRec.getChassisKey()).
                                                totalModelWeight(ERAS[i], rec.getFaction()) + 0.5);
                                        if (weight > 0) {
                                            data.get(key).set(i, Integer.toString(weight));
                                        }
                                    } 
                                } else if (rec.getEra() == rec.getStartYear()){
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
            return Integer.toString(ERAS[col - 1]); 
        }

        @Override
        public int getColumnCount() {
            if (data == null) {
                return 0;
            }
            return ERAS.length + 1;
        }

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
            AvailabilityRating ar;
            int era = ERAS[col - 1];
            if ((value).equals("")) {
                ar = null;
            } else if (!((String)value).matches("\\d+[+\\-]?(:\\d+)?")) {
                return;
            } else {
                ar = new AvailabilityRating(getUnitKey(), era,
                        factions.get(row) + ":" + value);
            }
            data.get(factions.get(row)).set(col - 1, (String)value);
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
            if(unitRecord == null) {                 
                return false;
            }
            
            if(mode == MODE_MODEL) {
                boolean chassisRecordFound = false;
                for(int era : ERAS) {
                    if(rg.getChassisFactionRatings(era, unitRecord.getChassisKey()) != null) {
                        chassisRecordFound = true;
                        break;
                    }
                }
                
                if(!chassisRecordFound) {
                    return false;
                }
            }                    
            
            factions.add(faction);
            ArrayList<String> list = new ArrayList<>();
            while (list.size() < ERAS.length) {
                list.add("");
            }
            data.put(faction, list);
            fireTableDataChanged();
            return true;
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

        public void copyRow(int row, String newFaction) {
            ArrayList<String> copyFrom = data.get(factions.get(row));
            factions.add(newFaction);
            ArrayList<String> copyTo = new ArrayList<>();
            for (int i = 0; i < ERAS.length; i++) {
                copyTo.add(copyFrom.get(i));
            }       
            data.put(newFaction, copyTo);
            for (int i = 0; i < ERAS.length; i++) {
                if (!copyFrom.get(i).equals("")) {
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

    private static class UnitTypeComparator implements Comparator<String> {
        private Map<String, Integer> keys;

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

    private static class FactionListTableModel extends DefaultTableModel {

        /**
         * 
         */
        private static final long serialVersionUID = -2719685611810784836L;

        public static final int COL_CODE = 0;
        public static final int COL_NAME = 1;
        public static final int COL_YEARS = 2;
        public static final int COL_MINOR = 3;
        public static final int COL_CLAN = 4;
        public static final int COL_PERIPHERY = 5;
        public static final int COL_RATINGS = 6;
        public static final int COL_USE_ALT_FACTION = 7;
        public static final int NUM_COLS = 8;
        
        public static final String[] colNames = {"Code", "Name", "Years", "Minor", "Clan",
            "Periphery", "Ratings", "Use Alt"};
        
        private ArrayList<FactionRecord> data;
        
        public FactionListTableModel(Collection<FactionRecord> factionList) {
            data = new ArrayList<>();
            data.addAll(factionList);
        }
        
        public void addRecord(FactionRecord rec) {
            data.add(rec);
            fireTableDataChanged();
        }
        
        public void delRecord(FactionRecord rec) {
            data.remove(rec);
            fireTableDataChanged();
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
                data.get(row).setClan((Boolean)val);
                break;
            case COL_PERIPHERY:
                data.get(row).setPeriphery((Boolean)val);
                break;
            case COL_NAME:
                data.get(row).setNames((String)val);
                break;
            case COL_YEARS:
                try {
                    data.get(row).setYears((String)val);
                } catch (Exception ex) {
                    //Illegal format; ignore new value
                }
                break;
            case COL_MINOR:
                data.get(row).setMinor((Boolean)val);
                break;
            case COL_RATINGS:
                data.get(row).setRatings((String)val);
                break;
            case COL_USE_ALT_FACTION:
                data.get(row).setParentFactions((String)val);
            }
        }
        
        public FactionRecord getFactionRecord(int row) {
            return data.get(row);
        }
        
    }
    
    private static class FactionEditorTableModel extends DefaultTableModel {
        
        private static final long serialVersionUID = 5324520609209690560L;
        
        private static final int CAT_OMNI_PCT = 0;
        private static final int CAT_CLAN_PCT = 1;
        private static final int CAT_SL_PCT = 2;
        private static final int CAT_OMNI_AERO_PCT = 3;
        private static final int CAT_CLAN_AERO_PCT = 4;
        private static final int CAT_SL_AERO_PCT = 5;
        private static final int CAT_CLAN_VEE_PCT = 6;
        private static final int CAT_SL_VEE_PCT = 7;
        private static final String [] CATEGORIES = {
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
                return Integer.toString(ERAS[column - 1]);
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
                return (pct == null)?"":pct.toString();
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
            if (((String)val).length() > 0) {
                try {
                    value = Integer.parseInt((String)val);
                } catch (NumberFormatException ex) {
                    //leave null
                }
            }
            if (row == 0) {
                factionRec.setPctSalvage(era, value);
            } else if (row > factionRec.getRatingLevels().size() * CATEGORIES.length) {
                int unitType = WEIGHT_DIST_UNIT_TYPES[row - 1 - factionRec.getRatingLevels().size() * CATEGORIES.length];
                if (((String)val).length() > 0) {
                    try {
                        factionRec.setWeightDistribution(era, unitType, (String)val);
                    } catch (Exception ex) {
                        //ignore
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
        
        /**
         * 
         */
        private static final long serialVersionUID = -3155497417382584025L;
        
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
            return Integer.toString(ERAS[col - 1]); 
        }
        
        @Override
        public int getColumnCount() {
            if (data == null) {
                return 0;
            }
            return ERAS.length + 1;
        }
        
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
            } else if (!((String)value).matches("\\d+")) {
                return;
            } else {
                wt = Integer.parseInt((String)value);
            }
            data.get(factions.get(row)).set(col - 1, (String)value);
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
                if (!copyFrom.get(i).equals("")) {
                    factionRec.setSalvage(ERAS[i], newFaction, Integer.parseInt(copyFrom.get(i)));
                }
            }
            fireTableDataChanged();
        }
        
    }

    /**
     * Runs the RATGeneratorEditor UI
     *
     * @param args The RATGenerator data will be loaded from the directory named as the first element
     *             of the arguments. If the {@code args} element is empty, or the first element
     *             is not a valid directory, loads from the default location.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater (() -> {
            RATGeneratorEditor ui;
            if (args.length > 0) {
                File dir = new File(args[0]);
                if (dir.exists() && dir.isDirectory()) {
                    ui = new RATGeneratorEditor(dir);
                } else {
                    MegaMek.getLogger().info(args[0] + " is not a valid directory name");
                    ui = new RATGeneratorEditor();
                }
            } else {
                ui = new RATGeneratorEditor();
            }
            ui.setVisible(true);
        });
    }
}
