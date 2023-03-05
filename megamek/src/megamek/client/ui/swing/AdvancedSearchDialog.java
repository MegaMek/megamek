/*
 * MegaMek - Copyright (C) 2002, 2003 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ui.swing;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.table.MegamekTable;
import megamek.client.ui.swing.unitSelector.TWAdvancedSearchPanel;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

/**
 * JDialog that allows the user to create a unit filter.
 *
 * @author Arlith
 * @author Jay Lawson
 */
public class AdvancedSearchDialog extends JDialog implements ActionListener, ItemListener,
        KeyListener, ListSelectionListener {
    private static final long serialVersionUID = 1L;
    private boolean isCanceled = true;
    public MechSearchFilter mechFilter = null;
    private Vector<TWAdvancedSearchPanel.FilterTokens> filterToks;
    private JButton btnOkay = new JButton(Messages.getString("Okay"));
    private JButton btnCancel = new JButton(Messages.getString("Cancel"));

    private JButton btnLeftParen = new JButton("(");
    private JButton btnRightParen = new JButton(")");
    private JButton btnAdd = new JButton(Messages.getString("MechSelectorDialog.Search.add"));
    private JButton btnAnd = new JButton(Messages.getString("MechSelectorDialog.Search.and"));
    private JButton btnOr = new JButton(Messages.getString("MechSelectorDialog.Search.or"));
    private JButton btnClear = new JButton(Messages.getString("MechSelectorDialog.Reset"));
    private JButton btnBack = new JButton("Back");

    private JLabel  lblEqExpTxt = new JLabel(Messages.getString("MechSelectorDialog.Search.FilterExpression"));
    private JTextArea  txtEqExp = new JTextArea("");
    private JScrollPane expScroller = new JScrollPane(txtEqExp,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private JLabel lblWalk = new JLabel(Messages.getString("MechSelectorDialog.Search.Walk"));
    private JTextField tStartWalk = new JTextField(4);
    private JTextField tEndWalk = new JTextField(4);

    private JLabel lblJump = new JLabel(Messages.getString("MechSelectorDialog.Search.Jump"));
    private JTextField tStartJump = new JTextField(4);
    private JTextField tEndJump = new JTextField(4);

    private JLabel lblArmor = new JLabel(Messages.getString("MechSelectorDialog.Search.Armor"));
    private JComboBox<String> cArmor = new JComboBox<>();

    private JLabel lblTableFilters = new JLabel(Messages.getString("MechSelectorDialog.Search.TableFilters"));
    private JLabel lblUnitType = new JLabel(Messages.getString("MechSelectorDialog.Search.UnitType"));
    private JLabel lblTechClass = new JLabel(Messages.getString("MechSelectorDialog.Search.TechClass"));
    private JLabel lblTechLevel = new JLabel(Messages.getString("MechSelectorDialog.Search.TechLevel"));
    private JComboBox<String> cboUnitType = new JComboBox<>();
    private JComboBox<String> cboTechClass = new JComboBox<>();
    private JComboBox<String> cboTechLevel = new JComboBox<>();

    private JLabel lblWeapons = new JLabel(Messages.getString("MechSelectorDialog.Search.Weapons"));
    private JScrollPane scrTableWeapons = new JScrollPane();
    private MegamekTable tblWeapons;
    private WeaponsTableModel weaponsModel;
    private TableRowSorter<WeaponsTableModel> weaponsSorter;

    private JLabel lblEquipment = new JLabel(Messages.getString("MechSelectorDialog.Search.Equipment"));
    private JScrollPane scrTableEquipment = new JScrollPane();
    private MegamekTable tblEquipment;
    private EquipmentTableModel equipmentModel;
    private TableRowSorter<EquipmentTableModel> equipmentSorter;

    private JLabel lblYear = new JLabel(Messages.getString("MechSelectorDialog.Search.Year"));
    private JTextField tStartYear = new JTextField(4);
    private JTextField tEndYear = new JTextField(4);
    private JLabel lblCockpitType  = new JLabel(Messages.getString("MechSelectorDialog.Search.CockpitType"));
    private JList<String> listCockpitType  = new JList<>(new DefaultListModel<String>());
    private JScrollPane spCockpitType = new JScrollPane(listCockpitType);
    private JLabel lblArmorType  = new JLabel(Messages.getString("MechSelectorDialog.Search.ArmorType"));
    private JList<String> listArmorType  = new JList<>(new DefaultListModel<String>());
    private JScrollPane spArmorType = new JScrollPane(listArmorType);
    private JLabel lblInternalsType  = new JLabel(Messages.getString("MechSelectorDialog.Search.InternalsType"));
    private JList<String> listInternalsType  = new JList<>(new DefaultListModel<String>());
    private JScrollPane spInternalsType = new JScrollPane(listInternalsType);
    private JComboBox<String> cboQty = new JComboBox<>();

    /**
     * Stores the games current year.
     */
    private int gameYear;

    private static class NoSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setAnchorSelectionIndex(final int anchorIndex) {}

        @Override
        public void setLeadAnchorNotificationEnabled(final boolean flag) {}

        @Override
        public void setLeadSelectionIndex(final int leadIndex) {}

        @Override
        public void setSelectionInterval(final int index0, final int index1) {}
    }

    private void toggleText(JList list, int index) {
        ListModel<String> m = list.getModel();
        DefaultListModel dlm  = new DefaultListModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);

            if (index == i) {
                if (ms.contains("\u2610")) {
                    dlm.addElement("\u2611" + ms.substring(1, ms.length()));
                } else if (ms.contains("\u2611")) {
                    dlm.addElement("\u2612" + ms.substring(1, ms.length()));
                } else if (ms.contains("\u2612")) {
                    dlm.addElement("\u2610" + ms.substring(1, ms.length()));
                }
            } else {
                dlm.addElement(ms);
            }
        }

        list.setModel(dlm);
    }

    /**
     * Constructs a new AdvancedSearchDialog.
     *
     * @param frame  Parent frame
     */
    public AdvancedSearchDialog(Frame frame, int yr) {
        super(frame, Messages.getString("AdvancedSearchDialog.title"), true);

        gameYear = yr;

        filterToks = new Vector<>(30);

        //Initialize Items
        btnOkay.addActionListener(this);
        btnCancel.addActionListener(this);
        btnAnd.addActionListener(this);
        btnAdd.addActionListener(this);
        btnLeftParen.addActionListener(this);
        btnRightParen.addActionListener(this);
        btnOr.addActionListener(this);
        btnClear.addActionListener(this);
        btnBack.addActionListener(this);

        btnBack.setEnabled(false);
        btnAdd.setEnabled(false);

        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor25"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor50"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor75"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor90"));

        DefaultListModel dlma  = new DefaultListModel();

        for (int i = 0; i < EquipmentType.armorNames.length; i++) {
            dlma.addElement("\u2610 " + EquipmentType.armorNames[i]);
        }

        listArmorType.setModel(dlma);

        listArmorType.setVisibleRowCount(7);
        listArmorType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listArmorType.setSelectionModel(new NoSelectionModel());
        listArmorType.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList list = (JList) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });

        DefaultListModel dlmc  = new DefaultListModel();

        for (int i = 0; i < Mech.COCKPIT_STRING.length; i++) {
            dlmc.addElement("\u2610 " + Mech.COCKPIT_STRING[i]);
        }

        listCockpitType.setModel(dlmc);

        listCockpitType.setVisibleRowCount(5);
        listCockpitType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listCockpitType.setSelectionModel(new NoSelectionModel());
        listCockpitType.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList list = (JList) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });

        DefaultListModel dlmi  = new DefaultListModel();

        for (int i = 0; i < EquipmentType.structureNames.length; i++) {
            dlmi.addElement("\u2610 " + EquipmentType.structureNames[i]);
        }

        listInternalsType.setModel(dlmi);

        listInternalsType.setVisibleRowCount(5);
        listInternalsType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listInternalsType.setSelectionModel(new NoSelectionModel());
        listInternalsType.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList list = (JList) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });

        for (int i = 0; i <= 20; i++) {
            cboQty.addItem(Integer.toString(i));
        }
        cboQty.setSelectedIndex(0);

        //Setup table filter combo boxes
        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<>();
        unitTypeModel.addElement(Messages.getString("MechSelectorDialog.All"));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.MEK));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.TANK));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.BATTLE_ARMOR));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.INFANTRY));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.PROTOMEK));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.AERO));
        unitTypeModel.setSelectedItem(Messages.getString("MechSelectorDialog.All"));

        cboUnitType.setModel(unitTypeModel);
        cboUnitType.addActionListener(this);

        DefaultComboBoxModel<String> techLevelModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < TechConstants.SIZE; i++) {
            techLevelModel.addElement(TechConstants.getLevelDisplayableName(i));
        }
        techLevelModel.setSelectedItem(TechConstants.getLevelDisplayableName(TechConstants.SIZE-1));
        cboTechLevel.setModel(techLevelModel);
        cboTechLevel.addActionListener(this);

        DefaultComboBoxModel<String> techClassModel = new DefaultComboBoxModel<>();
        techClassModel.addElement("All");
        techClassModel.addElement("Inner Sphere");
        techClassModel.addElement("Clan");
        techClassModel.addElement("IS/Clan");
        techClassModel.addElement("(Unknown Technology Base)");
        techClassModel.setSelectedItem("All");
        cboTechClass.setModel(techClassModel);
        cboTechClass.addActionListener(this);

        //Setup Weapons Table
        weaponsModel = new WeaponsTableModel();
        tblWeapons = new MegamekTable(weaponsModel,WeaponsTableModel.COL_NAME);
        TableColumn wpsCol = tblWeapons.getColumnModel().getColumn(
                WeaponsTableModel.COL_QTY);
        wpsCol.setCellEditor(new DefaultCellEditor(cboQty));
        tblWeapons.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        weaponsSorter = new TableRowSorter<>(weaponsModel);
        tblWeapons.setRowSorter(weaponsSorter);
        tblWeapons.addKeyListener(this);
        tblWeapons.setFont(new Font(MMConstants.FONT_MONOSPACED, Font.PLAIN, 12));
        tblWeapons.getSelectionModel().addListSelectionListener(this);
        for (int i = 0; i < weaponsModel.getColumnCount(); i++) {
            tblWeapons.getColumnModel().getColumn(i).setPreferredWidth(weaponsModel.getPreferredWidth(i));
        }
        scrTableWeapons.setViewportView(tblWeapons);

        //Setup Equipment Table
        equipmentModel = new EquipmentTableModel();
        tblEquipment = new MegamekTable(equipmentModel,
                EquipmentTableModel.COL_NAME);
        TableColumn eqCol = tblEquipment.getColumnModel().getColumn(
                EquipmentTableModel.COL_QTY);
        eqCol.setCellEditor(new DefaultCellEditor(cboQty));
        tblEquipment.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        equipmentSorter = new TableRowSorter<>(equipmentModel);
        tblEquipment.setRowSorter(equipmentSorter);
        tblEquipment.addKeyListener(this);
        tblEquipment.setFont(new Font(MMConstants.FONT_MONOSPACED, Font.PLAIN, 12));
        tblEquipment.getSelectionModel().addListSelectionListener(this);
        for (int i = 0; i < tblEquipment.getColumnCount(); i++) {
            tblEquipment.getColumnModel().getColumn(i).setPreferredWidth(equipmentModel.getPreferredWidth(i));
        }
        scrTableEquipment.setViewportView(tblEquipment);

        //Populate Tables
        populateWeaponsAndEquipmentChoices();

        //initialize with the weapons sorted alphabetically by name
        ArrayList<SortKey> sortlist = new ArrayList<>();
        sortlist.add(new SortKey(WeaponsTableModel.COL_NAME,SortOrder.ASCENDING));
        tblWeapons.getRowSorter().setSortKeys(sortlist);
        ((DefaultRowSorter<?, ?>) tblWeapons.getRowSorter()).sort();
        tblWeapons.invalidate(); // force re-layout of window

        //initialize with the equipment sorted alphabetically by chassis
        sortlist = new ArrayList<>();
        sortlist.add(new SortKey(EquipmentTableModel.COL_NAME,SortOrder.ASCENDING));
        tblEquipment.getRowSorter().setSortKeys(sortlist);
        ((DefaultRowSorter<?, ?>) tblEquipment.getRowSorter()).sort();
        tblEquipment.invalidate(); // force re-layout of window

        txtEqExp.setEditable(false);
        txtEqExp.setLineWrap(true);
        txtEqExp.setWrapStyleWord(true);

        // Layout
        GridBagConstraints c = new GridBagConstraints();
        JPanel mainPanel = new JPanel(new GridBagLayout());

        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 10, 0, 0);
        c.gridx = 0; c.gridy = 0;
        mainPanel.add(lblWalk, c);
        c.gridx = 1; c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.EAST;
        JPanel panWalk = new JPanel();
        panWalk.add(tStartWalk);
        panWalk.add(new Label("-"));
        panWalk.add(tEndWalk);
        mainPanel.add(panWalk, c);
        c.gridx = 3; c.gridy = 0;
        c.insets = new Insets(0, 40, 0, 0);
        c.anchor = GridBagConstraints.WEST;
        JPanel cockpitPanel = new JPanel(new BorderLayout());
        cockpitPanel.add(lblCockpitType,BorderLayout.NORTH);
        cockpitPanel.add(spCockpitType,BorderLayout.SOUTH);
        mainPanel.add(cockpitPanel, c);

        c.gridx = 0; c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 10, 0, 0);
        mainPanel.add(lblJump, c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1; c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        JPanel panJump = new JPanel();
        panJump.add(tStartJump);
        panJump.add(new Label("-"));
        panJump.add(tEndJump);
        mainPanel.add(panJump, c);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 3; c.gridy = 1;
        c.insets = new Insets(0, 40, 0, 0);
        JPanel internalsPanel = new JPanel(new BorderLayout());
        internalsPanel.add(lblInternalsType,BorderLayout.NORTH);
        internalsPanel.add(spInternalsType,BorderLayout.EAST);
        mainPanel.add(internalsPanel, c);

        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy++;
        c.insets = new Insets(0, 10, 0, 0);
        mainPanel.add(lblArmor, c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        mainPanel.add(cArmor, c);
        c.gridx = 3;
        c.insets = new Insets(0, 40, 0, 0);
        JPanel armorPanel = new JPanel(new BorderLayout());
        armorPanel.add(lblArmorType,BorderLayout.NORTH);
        armorPanel.add(spArmorType,BorderLayout.EAST);
        mainPanel.add(armorPanel, c);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(16, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        mainPanel.add(lblTableFilters, c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        c.gridwidth = 4;
        JPanel cboPanel = new JPanel();
        cboPanel.add(lblUnitType);
        cboPanel.add(cboUnitType);
        cboPanel.add(lblTechClass);
        cboPanel.add(cboTechClass);
        cboPanel.add(lblTechLevel, c);
        cboPanel.add(cboTechLevel, c);
        mainPanel.add(cboPanel, c);
        c.gridwidth = 1;

        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        mainPanel.add(lblWeapons, c);


        c.insets = new Insets(0, 0, 0, 0);
        c.gridwidth = 4;
        c.gridx = 0; c.gridy++;
        mainPanel.add(scrTableWeapons, c);

        c.gridwidth = 1;
        c.insets = new Insets(16, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        mainPanel.add(lblEquipment, c);


        c.insets = new Insets(0, 0, 0, 0);
        c.gridwidth = 4;
        c.gridx = 0; c.gridy++;
        mainPanel.add(scrTableEquipment, c);

        c.gridx = 0; c.gridy++;
        c.gridwidth = 4;
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAdd, c);
        btnPanel.add(btnLeftParen, c);
        btnPanel.add(btnRightParen, c);
        btnPanel.add(btnAnd, c);
        btnPanel.add(btnOr, c);
        btnPanel.add(btnBack, c);
        btnPanel.add(btnClear, c);
        mainPanel.add(btnPanel, c);
        c.gridwidth = 1;

        // Filter Expression
        c.gridx = 0; c.gridy++;
        mainPanel.add(lblEqExpTxt, c);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 4;
        c.gridx = 1;
        mainPanel.add(expScroller, c);
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);

        c.gridwidth  = 1;
        c.gridx = 0; c.gridy++;
        mainPanel.add(lblYear, c);
        c.gridx = 1;
        JPanel designYearPanel = new JPanel();
        designYearPanel.add(tStartYear);
        designYearPanel.add(new Label("-"));
        designYearPanel.add(tEndYear);
        mainPanel.add(designYearPanel, c);

        c.gridwidth = 1;
        c.gridx = 2; c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 20, 10, 0);
        mainPanel.add(btnOkay, c);
        c.gridx = 3;
        c.insets = new Insets(0, 20, 10, 0);
        c.anchor = GridBagConstraints.WEST;
        mainPanel.add(btnCancel, c);

        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        add(mainScrollPane);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                setVisible(false);
            }
        });

        pack();
        int x = Math.max(0,
                (frame.getLocation().x + (frame.getSize().width / 2)) -
                (getSize().width / 2));
        int y = Math.max(0,
                (frame.getLocation().y + (frame.getSize().height / 2)) -
                (getSize().height / 2));
        setLocation(x, y);
    }

    @Override
    public void setVisible(boolean show) {
        if (show) {
            adaptToGUIScale();
        }
        super.setVisible(show);
    }

    /**
     * Listener for check box state changes
     */
    @Override
    public void itemStateChanged(ItemEvent e) {

    }

    /**
     * Selection Listener for Weapons and Equipment tables. Checks to see if
     * a row is selected and if it is, enables the corresponding the add button.
     */
    @Override
    public void valueChanged(ListSelectionEvent evt) {
        boolean lastTokIsOperation;
        int tokSize = filterToks.size();
        lastTokIsOperation = ((tokSize == 0) ||
                (filterToks.elementAt(tokSize-1) instanceof TWAdvancedSearchPanel.OperationFT));
        if (evt.getSource().equals(tblWeapons.getSelectionModel())) {
            if ((tblWeapons.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblEquipment.clearSelection();
                btnAdd.setEnabled(true);
            } else if (tblWeapons.getSelectedRow() >= 0) {
                tblEquipment.clearSelection();
            }
        } else if (evt.getSource().equals(tblEquipment.getSelectionModel())) {
            if ((tblEquipment.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblWeapons.clearSelection();
                btnAdd.setEnabled(true);
            } else if (tblEquipment.getSelectedRow() >= 0) {
                tblWeapons.clearSelection();
            }
        }
    }

    /**
     * Convenience method for enabling the buttons related to weapon/equipment
     * selection for filtering (btnAddEquipment, btnAddWeapon, etc)
     */
    private void enableSelectionButtons() {
        if ((tblWeapons.getSelectedRow() != -1) ||
                (tblEquipment.getSelectedRow() != -1)) {
            btnAdd.setEnabled(true);
        }
        btnLeftParen.setEnabled(true);
    }

    /**
     * Convenience method for disabling the buttons related to weapon/equipment
     * selection for filtering (btnAddEquipment, btnAddWeapon, etc)
     */
    private void disableSelectionButtons() {
        btnAdd.setEnabled(false);
        btnLeftParen.setEnabled(false);
    }

    /**
     * Convenience method for enabling the buttons related to filter operations
     * for filtering (btnAnd, btnOr, etc)
     */
    private void enableOperationButtons() {
        btnOr.setEnabled(true);
        btnAnd.setEnabled(true);
        btnRightParen.setEnabled(true);
    }

    /**
     * Convenience method for disabling the buttons related to filter operations
     * for filtering (btnAnd, btnOr, etc)
     */
    private void disableOperationButtons() {
        btnOr.setEnabled(false);
        btnAnd.setEnabled(false);
        btnRightParen.setEnabled(false);
    }

    /**
     *  Listener for button presses.
     */
    @Override
    public void actionPerformed(java.awt.event.ActionEvent ev) {
        if (ev.getSource().equals(btnOkay)) {
            isCanceled = false;
            try {
                mechFilter.createFilterExpressionFromTokens(filterToks);
                setVisible(false);
            } catch (MechSearchFilter.FilterParsingException e) {
                JOptionPane.showMessageDialog(this,
                        "Error parsing filter expression!\n\n" + e.msg,
                        "Filter Expression Parsing Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else if (ev.getSource().equals(btnCancel)) {
            isCanceled = true;
            setVisible(false);
         } else if (ev.getSource().equals(cboUnitType)
                || ev.getSource().equals(cboTechLevel)
                || ev.getSource().equals(cboTechClass)) {
            filterTables();
        } else if (ev.getSource().equals(btnAdd)) {
            int row = tblEquipment.getSelectedRow();
            if (row >= 0) {
                String internalName = (String)
                        tblEquipment.getModel().getValueAt(
                                tblEquipment.convertRowIndexToModel(row),
                                EquipmentTableModel.COL_INTERNAL_NAME);
                String fullName = (String) tblEquipment.getValueAt(row, EquipmentTableModel.COL_NAME);
                int qty = Integer.parseInt((String)
                    tblEquipment.getValueAt(row, EquipmentTableModel.COL_QTY));
                filterToks.add(new TWAdvancedSearchPanel.EquipmentFT(internalName, fullName, qty));
                txtEqExp.setText(filterExpressionString());
                btnBack.setEnabled(true);
                enableOperationButtons();
                disableSelectionButtons();
            }
            row = tblWeapons.getSelectedRow();
            if (row >= 0) {
                String internalName = (String)
                        tblWeapons.getModel().getValueAt(
                                tblWeapons.convertRowIndexToModel(row),
                                WeaponsTableModel.COL_INTERNAL_NAME);
                String fullName = (String) tblWeapons.getValueAt(row, WeaponsTableModel.COL_NAME);
                int qty = Integer.parseInt((String)
                    tblWeapons.getValueAt(row, WeaponsTableModel.COL_QTY));
                filterToks.add(new TWAdvancedSearchPanel.EquipmentFT(internalName, fullName, qty));
                txtEqExp.setText(filterExpressionString());
                btnBack.setEnabled(true);
                enableOperationButtons();
                disableSelectionButtons();
            }
        } else if (ev.getSource().equals(btnLeftParen)) {
            filterToks.add(new TWAdvancedSearchPanel.ParensFT("("));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
            btnLeftParen.setEnabled(false);
            btnRightParen.setEnabled(false);
        } else if (ev.getSource().equals(btnRightParen)) {
            filterToks.add(new TWAdvancedSearchPanel.ParensFT(")"));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            enableOperationButtons();
            disableSelectionButtons();
            btnLeftParen.setEnabled(false);
            btnRightParen.setEnabled(false);
        } else if (ev.getSource().equals(btnAnd)) {
            filterToks.add(new TWAdvancedSearchPanel.OperationFT(MechSearchFilter.BoolOp.AND));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
        } else if (ev.getSource().equals(btnOr)) {
            filterToks.add(new TWAdvancedSearchPanel.OperationFT(MechSearchFilter.BoolOp.OR));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
        } else if (ev.getSource().equals(btnBack)) {
            if (!filterToks.isEmpty()) {
                filterToks.remove(filterToks.size() - 1);
                txtEqExp.setText(filterExpressionString());
                if (filterToks.isEmpty()) {
                    btnBack.setEnabled(false);
                }

                if ((filterToks.isEmpty()) || (filterToks.lastElement() instanceof TWAdvancedSearchPanel.OperationFT)) {
                    disableOperationButtons();
                    enableSelectionButtons();
                } else {
                    enableOperationButtons();
                    disableSelectionButtons();
                }
            }
        } else if (ev.getSource().equals(btnClear)) {
            filterToks.clear();
            txtEqExp.setText("");
            btnBack.setEnabled(false);
            disableOperationButtons();
            enableSelectionButtons();
        }
    }

    private boolean matchTechLvl(int t1, int t2) {
        return ((t1 == TechConstants.T_ALL)
                || (t1 == t2)
                || ((t1 == TechConstants.T_IS_TW_ALL)
                    && ((t2 <= TechConstants.T_IS_TW_NON_BOX)
                     || ((t2) == TechConstants.T_INTRO_BOXSET))))
                || ((t1 == TechConstants.T_TW_ALL)
                    && ((t2 <= TechConstants.T_IS_TW_NON_BOX)
                     || (t2 <= TechConstants.T_INTRO_BOXSET)
                     || (t2 <= TechConstants.T_CLAN_TW)))
                || ((t1 == TechConstants.T_ALL_IS)
                    && ((t2 <= TechConstants.T_IS_TW_NON_BOX)
                     || (t2 == TechConstants.T_INTRO_BOXSET)
                     || (t2 == TechConstants.T_IS_ADVANCED)
                     || (t2 == TechConstants.T_IS_EXPERIMENTAL)
                     || (t2 == TechConstants.T_IS_UNOFFICIAL)))
                || ((t1 == TechConstants.T_ALL_CLAN)
                    && ((t2 == TechConstants.T_CLAN_TW)
                     || (t2 == TechConstants.T_CLAN_ADVANCED)
                     || (t2 == TechConstants.T_CLAN_EXPERIMENTAL)
                     || (t2 == TechConstants.T_CLAN_UNOFFICIAL)));
    }

    private boolean matchTechClass(String t1, String t2) {
        if (t1.equals("All")) {
            return true;
        } else if (t1.equals("IS/Clan")) {
            return t2.equals("Inner Sphere") || t2.equals("Clan") || t1.equals(t2);
        } else {
            return t1.equals(t2);
        }
    }

    private boolean matchUnitType(int unitTypeFilter, EquipmentType eq) {
        // All is selected
        if (unitTypeFilter < 0) {
            return true;
        }

        switch (unitTypeFilter) {
            case 5: //UnitType.AERO: the aero index is out of order
                if (eq.hasFlag(WeaponType.F_AERO_WEAPON)
                        || eq.hasFlag(MiscType.F_FIGHTER_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.BATTLE_ARMOR:
                if (eq.hasFlag(WeaponType.F_BA_WEAPON)
                        || eq.hasFlag(MiscType.F_BA_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.INFANTRY:
                if (eq.hasFlag(WeaponType.F_INFANTRY)) {
                    return true;
                }
                break;
            case UnitType.MEK:
                if (eq.hasFlag(WeaponType.F_MECH_WEAPON)
                        || eq.hasFlag(MiscType.F_MECH_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.TANK:
                if (eq.hasFlag(WeaponType.F_TANK_WEAPON)
                        || eq.hasFlag(MiscType.F_TANK_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.PROTOMEK:
                if (eq.hasFlag(WeaponType.F_PROTO_WEAPON)
                        || eq.hasFlag(MiscType.F_PROTOMECH_EQUIPMENT)) {
                    return true;
                }
                break;
            default:
                return false;
        }
        return false;
    }

    void filterTables() {
        RowFilter<WeaponsTableModel, Integer> weaponFilter;
        RowFilter<EquipmentTableModel, Integer> equipmentFilter;
        final int techLevel = cboTechLevel.getSelectedIndex();
        final String techClass = (String) cboTechClass.getSelectedItem();
        final int unitType = cboUnitType.getSelectedIndex() - 1;
        //If current expression doesn't parse, don't update.
        try {
            weaponFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends WeaponsTableModel, ? extends Integer> entry) {
                    WeaponsTableModel weapModel = entry.getModel();
                    WeaponType wp = weapModel.getWeaponTypeAt(entry.getIdentifier());
                    String currTechClass = TechConstants.getTechName(wp.getTechLevel(gameYear));

                    boolean techLvlMatch = matchTechLvl(techLevel, wp.getTechLevel(gameYear));
                    boolean techClassMatch = matchTechClass(techClass, currTechClass);
                    boolean unitTypeMatch = matchUnitType(unitType, wp);
                    return techLvlMatch && techClassMatch && unitTypeMatch;
                }
            };
        } catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        weaponsSorter.setRowFilter(weaponFilter);

        try {
            equipmentFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends EquipmentTableModel, ? extends Integer> entry) {
                    EquipmentTableModel eqModel = entry.getModel();
                    EquipmentType eq = eqModel.getEquipmentTypeAt(entry.getIdentifier());
                    String currTechClass = TechConstants.getTechName(eq.getTechLevel(gameYear));
                    boolean techLvlMatch = matchTechLvl(techLevel, eq.getTechLevel(gameYear));
                    boolean techClassMatch = matchTechClass(techClass, currTechClass);
                    boolean unitTypeMatch = matchUnitType(unitType, eq);
                    return techLvlMatch && techClassMatch && unitTypeMatch;
                }
            };
        } catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        equipmentSorter.setRowFilter(equipmentFilter);
    }

    private String filterExpressionString() {
        //Build the string representation of the new expression
        StringBuilder filterExp = new StringBuilder();
        for (int i = 0; i < filterToks.size(); i++) {
            filterExp.append(" ").append(filterToks.elementAt(i).toString()).append(" ");
        }
        return filterExp.toString();
    }

    /**
     * Show the dialog.  setVisible(true) blocks until setVisible(false).
     *
     * @return Return the filter that was created with this dialog.
     */
    public MechSearchFilter showDialog() {
        //We need to save a copy since the user can alter the filter state
        // and then click on the cancel button.  We want to make sure the
        // original filter state is saved.
        MechSearchFilter currFilter = mechFilter;
        mechFilter = new MechSearchFilter(currFilter);
        txtEqExp.setText(mechFilter.getEquipmentExpression());
        if ((filterToks == null) || filterToks.isEmpty()
                || (filterToks.lastElement() instanceof TWAdvancedSearchPanel.OperationFT)) {
            disableOperationButtons();
            enableSelectionButtons();
        } else {
            enableOperationButtons();
            disableSelectionButtons();
        }
        setVisible(true);
        if (isCanceled) {
            mechFilter = currFilter;
        } else {
            updateMechSearchFilter();
        }

        return mechFilter;
    }

    /**
     *  Clear the filter.
     */
    public void clearValues() {
        tStartWalk.setText("");
        tEndWalk.setText("");
        tStartJump.setText("");
        tEndJump.setText("");
        cArmor.setSelectedIndex(0);
        tblWeapons.clearSelection();
        tblEquipment.clearSelection();
        txtEqExp.setText("");
        mechFilter = null;
        filterToks.clear();
        btnBack.setEnabled(false);

        DefaultListModel dlmwa  = new DefaultListModel();
        ListModel<String> m = listArmorType.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            dlmwa.addElement("\u2610 " + ms.substring(2, ms.length()));
        }

        listArmorType.setModel(dlmwa);

        m = listCockpitType.getModel();

        DefaultListModel dlmc  = new DefaultListModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            dlmc.addElement("\u2610 " + ms.substring(2, ms.length()));
        }

        listCockpitType.setModel(dlmc);

        m = listInternalsType.getModel();

        DefaultListModel dlmi  = new DefaultListModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            dlmi.addElement("\u2610 " + ms.substring(2, ms.length()));
        }

        listInternalsType.setModel(dlmi);

        disableOperationButtons();
        enableSelectionButtons();
    }

    /**
     * Creates collections for all the possible <code>WeaponType</code>s and
     * <code>EquipmentType</code>s.  These are used to populate the weapons
     * and equipment tables.
     */
    private void populateWeaponsAndEquipmentChoices() {
        Vector<WeaponType> weapons = new Vector<>();
        Vector<EquipmentType> equipment = new Vector<>();

        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if ((et instanceof WeaponType)) {
                weapons.add((WeaponType) et);
                //Check for C3+Tag and C3 Master Booster
                if (et.hasFlag(WeaponType.F_C3M) || et.hasFlag(WeaponType.F_C3MBS)) {
                    equipment.add(et);
                }
            }
            if ((et instanceof MiscType)) {
                equipment.add(et);
            }
        }
        weaponsModel.setData(weapons);
        equipmentModel.setData(equipment);
    }


    public MechSearchFilter getMechSearchFilter()
    {
        return mechFilter;
    }

    /**
     * Update the search fields that aren't automatically updated.
     */
    protected void updateMechSearchFilter() {
        mechFilter.isDisabled = false;
        mechFilter.sStartWalk = tStartWalk.getText();
        mechFilter.sEndWalk = tEndWalk.getText();

        mechFilter.sStartJump = tStartJump.getText();
        mechFilter.sEndJump = tEndJump.getText();

        mechFilter.iArmor = cArmor.getSelectedIndex();

        mechFilter.sStartYear = tStartYear.getText();
        mechFilter.sEndYear = tEndYear.getText();


        ListModel<String> m = listArmorType.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            if (ms.contains("\u2611")) {
                mechFilter.armorType.add(i);
            } else if (ms.contains("\u2612")) {
                mechFilter.armorTypeExclude.add(i);
            }
        }

        m = listCockpitType.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            if (ms.contains("\u2611")) {
                mechFilter.cockpitType.add(i);
            } else if (ms.contains("\u2612")) {
                mechFilter.cockpitTypeExclude.add(i);
            }
        }

        m = listInternalsType.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            if (ms.contains("\u2611")) {
                mechFilter.internalsType.add(i);
            } else if (ms.contains("\u2612")) {
                mechFilter.internalsTypeExclude.add(i);
            }
        }
    }

    /**
     * A table model for displaying weapons
     */
    public class WeaponsTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        private static final int COL_QTY = 0;
        private static final int COL_NAME = 1;
        private static final int COL_DMG = 2;
        private static final int COL_HEAT = 3;
        private static final int COL_SHORT = 4;
        private static final int COL_MED = 5;
        private static final int COL_LONG = 6;
        private static final int COL_IS_CLAN = 7;
        private static final int COL_LEVEL = 8;
        private static final int N_COL = 9;
        private static final int COL_INTERNAL_NAME = 9;


        private int[] qty;

        private Vector<WeaponType> weapons = new Vector<>();

        @Override
        public int getRowCount() {
            return weapons.size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        public int getPreferredWidth(int col) {
            switch (col) {
                case COL_QTY:
                    return 40;
                case COL_NAME:
                    return 310;
                case COL_IS_CLAN:
                    return 75;
                case COL_DMG:
                    return 50;
                case COL_HEAT:
                    return 50;
                case COL_SHORT:
                    return 50;
                case COL_MED:
                    return 50;
                case COL_LONG:
                    return 50;
                case COL_LEVEL:
                    return 100;
                default:
                    return 0;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_QTY:
                    return "Qty";
                case COL_NAME:
                    return "Weapon Name";
                case COL_IS_CLAN:
                    return "IS/Clan";
                case COL_DMG:
                    return "DMG";
                case COL_HEAT:
                    return "Heat";
                case COL_SHORT:
                    return "Short";
                case COL_MED:
                    return "Med";
                case COL_LONG:
                    return "Long";
                case COL_LEVEL:
                    return "Lvl";
                default:
                    return "?";
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            switch (col) {
                case COL_QTY:
                    return true;
                default:
                    return false;
            }
        }

        // fill table with values
        public void setData(Vector<WeaponType> wps) {
            weapons = wps;
            qty = new int[wps.size()];
            Arrays.fill(qty, 1);
            fireTableDataChanged();
        }

        public WeaponType getWeaponTypeAt(int row) {
            return weapons.elementAt(row);
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= weapons.size()) {
                return null;
            }
            WeaponType wp = weapons.elementAt(row);
            switch (col) {
                case COL_QTY:
                    return qty[row] + "";
                case COL_NAME:
                    return wp.getName();
                case COL_IS_CLAN:
                    return TechConstants.getTechName(wp.getTechLevel(gameYear));
                case COL_DMG:
                    return wp.getDamage();
                case COL_HEAT:
                    return wp.getHeat();
                case COL_SHORT:
                    return wp.getShortRange();
                case COL_MED:
                    return wp.getMediumRange();
                case COL_LONG:
                    return wp.getLongRange();
                case COL_LEVEL:
                        return TechConstants.getSimpleLevelName(TechConstants
                                .convertFromNormalToSimple(wp
                                        .getTechLevel(gameYear)));
                case COL_INTERNAL_NAME:
                    return wp.getInternalName();
                default:
                    return "?";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            switch (col) {
                case COL_QTY:
                    qty[row] = Integer.parseInt((String) value);
                    fireTableCellUpdated(row, col);
                    break;
                default:
                    break;
            }
        }

    }

            /**
     * A table model for displaying weapon types
     */
    
    /**
     * A table model for displaying equipment
     */
    public class EquipmentTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        private static final int COL_QTY = 0;
        private static final int COL_NAME = 1;
        private static final int COL_COST = 2;
        private static final int COL_IS_CLAN = 3;
        private static final int COL_LEVEL = 4;
        private static final int N_COL = 5;
        private static final int COL_INTERNAL_NAME = 5;

        private int[] qty;
        private Vector<EquipmentType> equipment = new Vector<>();

        @Override
        public int getRowCount() {
            return equipment.size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        public int getPreferredWidth(int column) {
            switch (column) {
                case COL_QTY:
                    return 40;
                case COL_NAME:
                    return 400;
                case COL_IS_CLAN:
                    return 75;
                case COL_COST:
                    return 175;
                case COL_LEVEL:
                    return 100;
                default:
                    return 0;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_QTY:
                    return "Qty";
                case COL_NAME:
                    return "Name";
                case COL_IS_CLAN:
                    return "IS/Clan";
                case COL_COST:
                    return "Cost";
                case COL_LEVEL:
                    return "Lvl";
                default:
                    return "?";
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            switch (col) {
                case COL_QTY:
                    return true;
                default:
                    return false;
            }
        }

        // fill table with values
        public void setData(Vector<EquipmentType> eq) {
            equipment = eq;
            qty = new int[eq.size()];
            Arrays.fill(qty, 1);
            fireTableDataChanged();
        }

        public EquipmentType getEquipmentTypeAt(int row) {
            return equipment.elementAt(row);
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= equipment.size()) {
                return null;
            }
            EquipmentType eq = equipment.elementAt(row);
            switch (col) {
                case COL_QTY:
                    return qty[row] + "";
                case COL_NAME:
                    return eq.getName();
                case COL_IS_CLAN:
                    return TechConstants.getTechName(eq.getTechLevel(gameYear));
                case COL_COST:
                    return eq.getRawCost();
                case COL_LEVEL:
                        return TechConstants.getSimpleLevelName(TechConstants
                                .convertFromNormalToSimple(eq
                                        .getTechLevel(gameYear)));
                case COL_INTERNAL_NAME:
                    return eq.getInternalName();
                default:
                    return "?";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            switch (col) {
                case COL_QTY:
                    qty[row] = Integer.parseInt((String) value);
                    fireTableCellUpdated(row, col);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent evt) {

    }

    @Override
    public void keyReleased(KeyEvent evt) {

    }

    @Override
    public void keyTyped(KeyEvent evt) {
        char keyChar = evt.getKeyChar();
        //Ensure we've got a number or letter pressed
        if (!(((keyChar >= '0') && (keyChar <= '9')) ||
             ((keyChar >= 'a') && (keyChar <='z')) || (keyChar == ' '))) {
            return;
        }

        if (evt.getComponent().equals(tblWeapons)) {
            tblWeapons.keyTyped(evt);
        } else if (evt.getComponent().equals(tblEquipment)) {
            tblEquipment.keyTyped(evt);
        }
    }


    /**
     * Base class for different tokens that can be in a filter expression.
     * @author Arlith
     */
    public class FilterTokens {

    }

    /**
     * FilterTokens subclass that represents parenthesis.
     * @author Arlith
     */
    public class ParensFT extends FilterTokens {
        public String parens;

        public ParensFT(String p) {
            parens = p;
        }

        @Override
        public String toString() {
            return parens;
        }
    }

    /**
     * FilterTokens subclass that represents equipment.
     * @author Arlith
     */
    public class EquipmentFT extends FilterTokens {
        public String internalName;
        public String fullName;
        public int qty;

        public EquipmentFT(String in, String fn, int q) {
            internalName = in;
            fullName = fn;
            qty = q;
        }

        @Override
        public String toString() {
            if (qty == 1) {
                return qty + " " + fullName;
            } else {
                return qty + " " + fullName + "s";
            }
        }
    }

    /**
     * FilterTokens subclass that represents a boolean operation.
     * @author Arlith
     *
     */
    public class OperationFT extends FilterTokens {
        public MechSearchFilter.BoolOp op;

        public OperationFT(MechSearchFilter.BoolOp o) {
            op = o;
        }

        @Override
        public String toString() {
            if (op == MechSearchFilter.BoolOp.AND) {
                return "And";
            } else if (op == MechSearchFilter.BoolOp.OR) {
                return "Or";
            } else {
                return "";
            }
        }
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
        scrTableWeapons.setMinimumSize(new Dimension(UIUtil.scaleForGUI(850), UIUtil.scaleForGUI(150)));
        scrTableWeapons.setPreferredSize(new Dimension(UIUtil.scaleForGUI(850), UIUtil.scaleForGUI(150)));
        scrTableEquipment.setMinimumSize(new Dimension(UIUtil.scaleForGUI(850), UIUtil.scaleForGUI(150)));
        scrTableEquipment.setPreferredSize(new Dimension(UIUtil.scaleForGUI(850), UIUtil.scaleForGUI(150)));
    }
}
