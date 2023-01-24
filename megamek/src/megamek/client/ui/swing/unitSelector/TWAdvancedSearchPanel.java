/*
 * Copyright (c) 2002, 2003 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.unitSelector;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.table.MegamekTable;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;

import javax.swing.*;
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
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Panel that allows the user to create a unit filter.
 *
 * @author Arlith
 * @author Jay Lawson
 * @author Simon (Juliez)
 */
public class TWAdvancedSearchPanel extends JPanel implements ActionListener, ItemListener,
        KeyListener, ListSelectionListener {

    private boolean isCanceled = true;
    public MechSearchFilter mechFilter;
    private Vector<FilterTokens> filterToks;

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
    private JComboBox<String> cWalk = new JComboBox<>();
    private JTextField tWalk = new JTextField(2);

    private JLabel lblJump = new JLabel(Messages.getString("MechSelectorDialog.Search.Jump"));
    private JComboBox<String> cJump = new JComboBox<>();
    private JTextField tJump = new JTextField(2);

    private JLabel lblArmor = new JLabel(Messages.getString("MechSelectorDialog.Search.Armor"));
    private JComboBox<String> cArmor = new JComboBox<>();
    private JLabel lblOmni = new JLabel(Messages.getString("MechSelectorDialog.Search.Omni"));
    private JComboBox<String> cOmni = new JComboBox<>();

    private JLabel lblOfficial = new JLabel(Messages.getString("MechSelectorDialog.Search.Official"));
    private JComboBox<String> cOfficial = new JComboBox<>();
    private JLabel lblClanEngine = new JLabel(Messages.getString("MechSelectorDialog.Search.ClanEngine"));
    private JComboBox<String> cClanEngine = new JComboBox<>();
    private JLabel lblTableFilters = new JLabel(Messages.getString("MechSelectorDialog.Search.TableFilters"));
    private JLabel lblUnitType = new JLabel(Messages.getString("MechSelectorDialog.Search.UnitType"));
    private JLabel lblTechClass = new JLabel(Messages.getString("MechSelectorDialog.Search.TechClass"));
    private JLabel lblTechLevel = new JLabel(Messages.getString("MechSelectorDialog.Search.TechLevel"));
    private JComboBox<String> cboUnitType = new JComboBox<>();
    private JComboBox<String> cboTechClass = new JComboBox<>();
    private JComboBox<String> cboTechLevel = new JComboBox<>();

    private JLabel lblWeaponClass = new JLabel(Messages.getString("MechSelectorDialog.Search.WeaponClass"));
    private JScrollPane scrTableWeaponType = new JScrollPane();
    private MegamekTable tblWeaponType;
    private WeaponClassTableModel weaponTypesModel;
    private TableRowSorter<WeaponClassTableModel> weaponTypesSorter;

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

    private JLabel lblTons = new JLabel(Messages.getString("MechSelectorDialog.Search.Tons"));
    private JTextField tStartTons = new JTextField(4);
    private JTextField tEndTons = new JTextField(4);

    private JLabel lblBV = new JLabel(Messages.getString("MechSelectorDialog.Search.BV"));
    private JTextField tStartBV = new JTextField(4);
    private JTextField tEndBV = new JTextField(4);

    private JCheckBox cbxEnableCockpitSearch = new JCheckBox(Messages.getString("MechSelectorDialog.Search.Enable"));
    private JLabel lblCockpitType = new JLabel(Messages.getString("MechSelectorDialog.Search.CockpitType"));
    private JComboBox<String> cboCockpitType = new JComboBox<>();

    private JCheckBox cbxEnableInternalsSearch = new JCheckBox(Messages.getString("MechSelectorDialog.Search.Enable"));
    private JLabel lblInternalsType = new JLabel(Messages.getString("MechSelectorDialog.Search.InternalsType"));
    private JComboBox<String> cboInternalsType = new JComboBox<>();

    private JCheckBox cbxEnableArmorSearch = new JCheckBox(Messages.getString("MechSelectorDialog.Search.Enable"));
    private JLabel lblArmorType = new JLabel(Messages.getString("MechSelectorDialog.Search.ArmorType"));
    private JComboBox<String> cboArmorType = new JComboBox<>();

    private JCheckBox cbxEnableQuirkSearch = new JCheckBox(Messages.getString("MechSelectorDialog.Search.Enable"));
    private JLabel lblQuirkType = new JLabel(Messages.getString("MechSelectorDialog.Search.Quirk"));
    private JComboBox<String> cboQuirkType = new JComboBox<>();

    private JCheckBox cbxEnableEngineSearch = new JCheckBox(Messages.getString("MechSelectorDialog.Search.Enable"));
    private JLabel lblEngineType = new JLabel(Messages.getString("MechSelectorDialog.Search.Engine"));
    private JComboBox<String> cboEngineType = new JComboBox<>();

    private JCheckBox cbxFilterLAM = new JCheckBox(Messages.getString("MechSelectorDialog.Search.LAM"));
    private JCheckBox cbxfilterQuad= new JCheckBox(Messages.getString("MechSelectorDialog.Search.Quad"));
    private JCheckBox cbxFilterTripod = new JCheckBox(Messages.getString("MechSelectorDialog.Search.Tripod"));
    private JCheckBox cbxfilterQuadVee = new JCheckBox(Messages.getString("MechSelectorDialog.Search.QuadVee"));
    private JCheckBox cbxfilterSupportVTOL = new JCheckBox(Messages.getString("MechSelectorDialog.Search.SupportVTOL"));
    private JCheckBox cbxfilterFixedWingSupport = new JCheckBox(Messages.getString("MechSelectorDialog.Search.FixedWingSupport"));
    private JCheckBox cbxFilterSuperHeavyTank = new JCheckBox(Messages.getString("MechSelectorDialog.Search.SuperHeavyTank"));
    private JCheckBox cbxFilterSupportTank = new JCheckBox(Messages.getString("MechSelectorDialog.Search.SupportTank"));
    private JCheckBox cbxFilterLargeSupportTank = new JCheckBox(Messages.getString("MechSelectorDialog.Search.LargeSupportTank"));

    private JComboBox<String> cboQty = new JComboBox<>();

    /** The game's current year. */
    private int gameYear;

    /**
     * Constructs a new advanced search panel for Total Warfare values
     */
    public TWAdvancedSearchPanel(int year) {
        mechFilter = new MechSearchFilter();
        gameYear = year;

        filterToks = new Vector<>(30);

        // Initialize Items
        btnAnd.addActionListener(this);
        btnAdd.addActionListener(this);
        btnLeftParen.addActionListener(this);
        btnRightParen.addActionListener(this);
        btnOr.addActionListener(this);
        btnClear.addActionListener(this);
        btnBack.addActionListener(this);

        btnBack.setEnabled(false);
        btnAdd.setEnabled(false);

        cWalk.addItem(Messages.getString("MechSelectorDialog.Search.AtLeast"));
        cWalk.addItem(Messages.getString("MechSelectorDialog.Search.EqualTo"));
        cWalk.addItem(Messages.getString("MechSelectorDialog.Search.NoMoreThan"));

        cJump.addItem(Messages.getString("MechSelectorDialog.Search.AtLeast"));
        cJump.addItem(Messages.getString("MechSelectorDialog.Search.EqualTo"));
        cJump.addItem(Messages.getString("MechSelectorDialog.Search.NoMoreThan"));

        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor25"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor50"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor75"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor90"));

        cOmni.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cOmni.addItem(Messages.getString("MechSelectorDialog.Search.Yes"));
        cOmni.addItem(Messages.getString("MechSelectorDialog.Search.No"));

        cOfficial.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cOfficial.addItem(Messages.getString("MechSelectorDialog.Search.Yes"));
        cOfficial.addItem(Messages.getString("MechSelectorDialog.Search.No"));

        cClanEngine.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cClanEngine.addItem(Messages.getString("MechSelectorDialog.Search.Yes"));
        cClanEngine.addItem(Messages.getString("MechSelectorDialog.Search.No"));

        for (int i = 0; i < EquipmentType.armorNames.length; i++) {
            cboArmorType.addItem(EquipmentType.armorNames[i]);
        }
        cboArmorType.setEnabled(false);
        lblArmorType.setEnabled(false);

        Quirks quirks = new Quirks();
        List<String> qs = new ArrayList<>();
        for (final Enumeration<IOptionGroup> optionGroups = quirks.getGroups(); optionGroups.hasMoreElements();) {
            final IOptionGroup group = optionGroups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                final IOption option = options.nextElement();
                if (option != null) {
                    qs.add(option.getDisplayableNameWithValue());
                }
            }
        }
        WeaponQuirks weaponquirks = new WeaponQuirks();
        for (final Enumeration<IOptionGroup> optionGroups = weaponquirks.getGroups(); optionGroups.hasMoreElements();) {
            final IOptionGroup group = optionGroups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                final IOption option = options.nextElement();
                if (option != null) {
                    qs.add(option.getDisplayableNameWithValue());
                }
            }
        }
        qs = qs.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());

        for (String q : qs) {
            cboQuirkType.addItem(q);
        }

        cboQuirkType.setEnabled(false);
        lblQuirkType.setEnabled(false);

        for (int i = 0; i < Engine.NUM_ENGINE_TYPES; i++) {
            cboEngineType.addItem(Engine.getEngineType(i));
        }

        cboEngineType.setEnabled(false);
        lblEngineType.setEnabled(false);

        for (int i = 0; i < EquipmentType.structureNames.length; i++) {
            cboInternalsType.addItem(EquipmentType.structureNames[i]);
        }
        cboInternalsType.setEnabled(false);
        lblInternalsType.setEnabled(false);

        for (int i = 0; i < Mech.COCKPIT_STRING.length; i++) {
            cboCockpitType.addItem(Mech.COCKPIT_STRING[i]);
        }
        cboCockpitType.setEnabled(false);
        lblCockpitType.setEnabled(false);

        cbxEnableCockpitSearch.setHorizontalTextPosition(SwingConstants.LEFT);
        cbxEnableCockpitSearch.addItemListener(this);
        cbxEnableInternalsSearch.setHorizontalTextPosition(SwingConstants.LEFT);
        cbxEnableInternalsSearch.addItemListener(this);
        cbxEnableArmorSearch.setHorizontalTextPosition(SwingConstants.LEFT);
        cbxEnableArmorSearch.addItemListener(this);
        cbxEnableQuirkSearch.setHorizontalTextPosition(SwingConstants.LEFT);
        cbxEnableQuirkSearch.addItemListener(this);
        cbxEnableEngineSearch.setHorizontalTextPosition(SwingConstants.LEFT);
        cbxEnableEngineSearch.addItemListener(this);

        for (int i = 1; i <= 20; i++) {
            cboQty.addItem(Integer.toString(i));
        }
        cboQty.setSelectedIndex(0);

        cbxFilterLAM.setHorizontalTextPosition(SwingConstants.RIGHT);
        cbxFilterLAM.addItemListener(this);
        cbxfilterQuad.setHorizontalTextPosition(SwingConstants.RIGHT);
        cbxfilterQuad.addItemListener(this);
        cbxFilterTripod.setHorizontalTextPosition(SwingConstants.RIGHT);
        cbxFilterTripod.addItemListener(this);
        cbxfilterQuadVee.setHorizontalTextPosition(SwingConstants.RIGHT);
        cbxfilterQuadVee.addItemListener(this);
        cbxfilterSupportVTOL.setHorizontalTextPosition(SwingConstants.RIGHT);
        cbxfilterSupportVTOL.addItemListener(this);
        cbxfilterFixedWingSupport.setHorizontalTextPosition(SwingConstants.RIGHT);
        cbxfilterFixedWingSupport.addItemListener(this);
        cbxFilterSuperHeavyTank.setHorizontalTextPosition(SwingConstants.RIGHT);
        cbxFilterSuperHeavyTank.addItemListener(this);
        cbxFilterSupportTank.setHorizontalTextPosition(SwingConstants.RIGHT);
        cbxFilterSupportTank.addItemListener(this);
        cbxFilterLargeSupportTank.setHorizontalTextPosition(SwingConstants.RIGHT);
        cbxFilterLargeSupportTank.addItemListener(this);

        // Setup table filter combo boxes
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


        // Set up Weapon Class table
        weaponTypesModel = new WeaponClassTableModel();
        tblWeaponType = new MegamekTable(weaponTypesModel,WeaponClassTableModel.COL_NAME);
        TableColumn wpsTypeCol = tblWeaponType.getColumnModel().getColumn(WeaponClassTableModel.COL_QTY);
        wpsTypeCol.setCellEditor(new DefaultCellEditor(cboQty));
        tblWeaponType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        weaponTypesSorter = new TableRowSorter<>(weaponTypesModel);
        tblWeaponType.setRowSorter(weaponTypesSorter);
        tblWeaponType.addKeyListener(this);
        tblWeaponType.setFont(new Font(MMConstants.FONT_MONOSPACED, Font.PLAIN, 12));
        tblWeaponType.getSelectionModel().addListSelectionListener(this);
        for (int i = 0; i < weaponTypesModel.getColumnCount(); i++) {
            tblWeaponType.getColumnModel().getColumn(i).setPreferredWidth(weaponTypesModel.getPreferredWidth(i));
        }
        scrTableWeaponType.setViewportView(tblWeaponType);

        // Setup Weapons Table
        weaponsModel = new WeaponsTableModel();
        tblWeapons = new MegamekTable(weaponsModel, WeaponsTableModel.COL_NAME);
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

        // Setup Equipment Table
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
        for (int i = 0; i < equipmentModel.getColumnCount(); i++) {
            tblEquipment.getColumnModel().getColumn(i).setPreferredWidth(equipmentModel.getPreferredWidth(i));
        }
        scrTableEquipment.setViewportView(tblEquipment);

        // Populate Tables
        populateWeaponsAndEquipmentChoices();

        // initialize with the weapons sorted alphabetically by name
        ArrayList<RowSorter.SortKey> sortlist = new ArrayList<>();
        sortlist.add(new RowSorter.SortKey(WeaponsTableModel.COL_NAME,SortOrder.ASCENDING));
        tblWeapons.getRowSorter().setSortKeys(sortlist);
        ((DefaultRowSorter<?, ?>) tblWeapons.getRowSorter()).sort();
        tblWeapons.invalidate(); // force re-layout of window

        // initialize with the equipment sorted alphabetically by chassis
        sortlist = new ArrayList<>();
        sortlist.add(new RowSorter.SortKey(EquipmentTableModel.COL_NAME,SortOrder.ASCENDING));
        tblEquipment.getRowSorter().setSortKeys(sortlist);
        ((DefaultRowSorter<?, ?>) tblEquipment.getRowSorter()).sort();
        tblEquipment.invalidate(); // force re-layout of window

        tWalk.setText("");
        tJump.setText("");

        txtEqExp.setEditable(false);
        txtEqExp.setLineWrap(true);
        txtEqExp.setWrapStyleWord(true);

        // Layout
        GridBagConstraints c = new GridBagConstraints();
        setLayout(new GridBagLayout());



        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0;
        c.insets = new Insets(0, 10, 0, 0);

        c.gridwidth  = 4;
        JPanel p0Panel = new JPanel();
        p0Panel.add(lblOfficial);
        p0Panel.add(cOfficial);
        p0Panel.add(lblYear);
        p0Panel.add(tStartYear);
        p0Panel.add(new Label("-"));
        p0Panel.add(tEndYear);
        p0Panel.add(lblTons);
        p0Panel.add(tStartTons);
        p0Panel.add(new Label("-"));
        p0Panel.add(tEndTons);
        p0Panel.add(lblBV);
        p0Panel.add(tStartBV);
        p0Panel.add(new Label("-"));
        p0Panel.add(tEndBV);
        this.add(p0Panel, c);

        c.gridx = 0; c.gridy++;
        JPanel p1Panel = new JPanel();
        p1Panel.add(lblWalk);
        p1Panel.add(cWalk);
        p1Panel.add(tWalk);
        p1Panel.add(lblJump);
        p1Panel.add(cJump);
        p1Panel.add(tJump);
        p1Panel.add(lblArmor);
        p1Panel.add(cArmor);
        p1Panel.add(lblOmni);
        p1Panel.add(cOmni);
        this.add(p1Panel, c);

        c.gridx = 0; c.gridy++;;
        c.gridwidth = 1;
        c.insets = new Insets(0, 10, 0, 0);
        c.anchor = GridBagConstraints.WEST;
        JPanel cockpitPanel = new JPanel();
        cockpitPanel.add(cbxEnableCockpitSearch,BorderLayout.WEST);
        cockpitPanel.add(lblCockpitType,BorderLayout.WEST);
        cockpitPanel.add(cboCockpitType,BorderLayout.EAST);
        this.add(cockpitPanel, c);
        c.gridx = 1;
        JPanel internalsPanel = new JPanel();
        internalsPanel.add(cbxEnableInternalsSearch);
        internalsPanel.add(lblInternalsType);
        internalsPanel.add(cboInternalsType,BorderLayout.EAST);
        this.add(internalsPanel, c);

        c.gridx = 0; c.gridy++;;
        JPanel armorPanel = new JPanel();
        armorPanel.add(cbxEnableArmorSearch);
        armorPanel.add(lblArmorType);
        armorPanel.add(cboArmorType,BorderLayout.EAST);
        this.add(armorPanel, c);
        c.gridx = 1;
        JPanel enginePanel = new JPanel();
        enginePanel.add(cbxEnableEngineSearch);
        enginePanel.add(lblEngineType);
        enginePanel.add(cboEngineType,BorderLayout.EAST);
        enginePanel.add(lblClanEngine);
        enginePanel.add(cClanEngine);
        this.add(enginePanel, c);

        c.gridx = 0; c.gridy++;;
        JPanel quirkPanel = new JPanel();
        quirkPanel.add(cbxEnableQuirkSearch);
        quirkPanel.add(lblQuirkType);
        quirkPanel.add(cboQuirkType,BorderLayout.EAST);
        this.add(quirkPanel, c);

        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy++;
        c.gridwidth = 4;
        c.insets = new Insets(0, 10, 0, 0);
        JPanel filterEntityPanel = new JPanel();
        filterEntityPanel.add(cbxFilterLAM);
        filterEntityPanel.add(cbxFilterTripod);
        filterEntityPanel.add(cbxfilterQuad);
        filterEntityPanel.add(cbxfilterQuadVee);
        filterEntityPanel.add(cbxfilterSupportVTOL);
        filterEntityPanel.add(cbxfilterFixedWingSupport);
        filterEntityPanel.add(cbxFilterSupportTank);
        filterEntityPanel.add(cbxFilterLargeSupportTank);
        filterEntityPanel.add(cbxFilterSuperHeavyTank);
        this.add(filterEntityPanel, c);

        // table
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        this.add(lblTableFilters, c);
        c.gridx = 0; c.gridy++;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.CENTER;
        JPanel cboPanel = new JPanel();
        cboPanel.add(lblUnitType);
        cboPanel.add(cboUnitType);
        cboPanel.add(lblTechClass);
        cboPanel.add(cboTechClass);
        cboPanel.add(lblTechLevel, c);
        cboPanel.add(cboTechLevel, c);
        this.add(cboPanel, c);
        c.gridwidth = 1;

        c.gridx = 0; c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        this.add(lblWeaponClass, c);

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 5;
        c.gridx = 0; c.gridy++;
        this.add(scrTableWeaponType, c);
        c.gridwidth = 1;

        c.fill = GridBagConstraints.NONE;
        c.gridx = 0; c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        this.add(lblWeapons, c);

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 5;
        c.gridx = 0; c.gridy++;
        this.add(scrTableWeapons, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.gridx = 0; c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        this.add(lblEquipment, c);

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 5;
        c.gridx = 0; c.gridy++;
        this.add(scrTableEquipment, c);

        c.insets = new Insets(0, 50, 0, 0);
        c.fill = GridBagConstraints.NONE;
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
        this.add(btnPanel, c);

        // Filter Expression
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        this.add(lblEqExpTxt, c);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        this.add(expScroller, c);
    }

    /**
     * Listener for check box state changes
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource().equals(cbxEnableCockpitSearch)) {
            cboCockpitType.setEnabled(!cboCockpitType.isEnabled());
            lblCockpitType.setEnabled(!lblCockpitType.isEnabled());
        } else if (e.getSource().equals(cbxEnableInternalsSearch)) {
            cboInternalsType.setEnabled(!cboInternalsType.isEnabled());
            lblInternalsType.setEnabled(!lblInternalsType.isEnabled());
        } else if (e.getSource().equals(cbxEnableArmorSearch)) {
            cboArmorType.setEnabled(!cboArmorType.isEnabled());
            lblArmorType.setEnabled(!lblArmorType.isEnabled());
        } else if (e.getSource().equals(cbxEnableQuirkSearch)) {
            cboQuirkType.setEnabled(!cboQuirkType.isEnabled());
            lblQuirkType.setEnabled(!lblQuirkType.isEnabled());
        } else if (e.getSource().equals(cbxEnableEngineSearch)) {
            cboEngineType.setEnabled(!cboEngineType.isEnabled());
            lblEngineType.setEnabled(!lblEngineType.isEnabled());
        }
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
                (filterToks.elementAt(tokSize - 1) instanceof OperationFT));
        if (evt.getSource().equals(tblWeapons.getSelectionModel())) {
            if ((tblWeapons.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblEquipment.clearSelection();
                tblWeaponType.clearSelection();
                btnAdd.setEnabled(true);
            } else if (tblWeapons.getSelectedRow() >= 0) {
                tblEquipment.clearSelection();
                tblWeaponType.clearSelection();
            }
        } else if (evt.getSource().equals(tblEquipment.getSelectionModel())) {
            if ((tblEquipment.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblWeapons.clearSelection();
                tblWeaponType.clearSelection();
                btnAdd.setEnabled(true);
            } else if (tblEquipment.getSelectedRow() >= 0) {
                tblWeapons.clearSelection();
                tblWeaponType.clearSelection();
            }
        } else if (evt.getSource().equals(tblWeaponType.getSelectionModel())) {
            if ((tblWeaponType.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblWeapons.clearSelection();
                tblEquipment.clearSelection();
                btnAdd.setEnabled(true);
            } else if (tblWeaponType.getSelectedRow() >= 0) {
                tblWeapons.clearSelection();
                tblEquipment.clearSelection();
            }
        }
    }

    /**
     * Convenience method for enabling the buttons related to weapon/equipment
     * selection for filtering (btnAddEquipment, btnAddWeapon, etc)
     */
    private void enableSelectionButtons() {
        if ((tblWeapons.getSelectedRow() != -1) ||
                (tblEquipment.getSelectedRow() != -1) ||
                (tblWeaponType.getSelectedRow() != -1)) {
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

    public void prepareFilter() {
        try {
            mechFilter = new MechSearchFilter(mechFilter);
            mechFilter.createFilterExpressionFromTokens(filterToks);
            updateMechSearchFilter();
        } catch (MechSearchFilter.FilterParsingException e) {
            JOptionPane.showMessageDialog(this,
                    "Error parsing filter expression!\n\n" + e.msg,
                    "Filter Expression Parsing Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *  Listener for button presses.
     */
    @Override
    public void actionPerformed(java.awt.event.ActionEvent ev) {
        if (ev.getSource().equals(cboUnitType)
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
                filterToks.add(new EquipmentFT(internalName, fullName, qty));
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
                filterToks.add(new EquipmentFT(internalName, fullName, qty));
                txtEqExp.setText(filterExpressionString());
                btnBack.setEnabled(true);
                enableOperationButtons();
                disableSelectionButtons();
            }
            row = tblWeaponType.getSelectedRow();
            if (row >= 0) {
                int qty = Integer.parseInt((String)tblWeaponType.getValueAt(row, WeaponClassTableModel.COL_QTY));
                filterToks.add(new WeaponClassFT((WeaponClass)tblWeaponType.getModel().getValueAt(tblWeaponType.convertRowIndexToModel(row), WeaponClassTableModel.COL_VAL), qty));
                txtEqExp.setText(filterExpressionString());
                btnBack.setEnabled(true);
                enableOperationButtons();
                disableSelectionButtons();
            }
        } else if (ev.getSource().equals(btnLeftParen)) {
            filterToks.add(new ParensFT("("));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
            btnLeftParen.setEnabled(false);
            btnRightParen.setEnabled(false);
        } else if (ev.getSource().equals(btnRightParen)) {
            filterToks.add(new ParensFT(")"));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            enableOperationButtons();
            disableSelectionButtons();
            btnLeftParen.setEnabled(false);
            btnRightParen.setEnabled(false);
        } else if (ev.getSource().equals(btnAnd)) {
            filterToks.add(new OperationFT(MechSearchFilter.BoolOp.AND));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
        } else if (ev.getSource().equals(btnOr)) {
            filterToks.add(new OperationFT(MechSearchFilter.BoolOp.OR));
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

                if ((filterToks.isEmpty()) || (filterToks.lastElement() instanceof OperationFT)) {
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
        return ((t1 == TechConstants.T_ALL) || (t1 == t2)
                || ((t1 == TechConstants.T_IS_TW_ALL) && (t2 <= TechConstants.T_IS_TW_NON_BOX)))

                || ((t1 == TechConstants.T_TW_ALL) && (t2 <= TechConstants.T_CLAN_TW))

                || ((t1 == TechConstants.T_ALL_IS) && ((t2 <= TechConstants.T_IS_TW_NON_BOX)
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
            case 5:
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
        // If current expression doesn't parse, don't update.
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
        } catch (java.util.regex.PatternSyntaxException ignored) {
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
        } catch (java.util.regex.PatternSyntaxException ignored) {
            return;
        }
        equipmentSorter.setRowFilter(equipmentFilter);
    }

    private String filterExpressionString() {
        // Build the string representation of the new expression
        StringBuilder filterExp = new StringBuilder();
        for (int i = 0; i < filterToks.size(); i++) {
            filterExp.append(" ").append(filterToks.elementAt(i).toString()).append(" ");
        }
        return filterExp.toString();
    }

    /**
     * Show the dialog. setVisible(true) blocks until setVisible(false).
     *
     * @return Return the filter that was created with this dialog.
     */
    public MechSearchFilter showDialog() {
        // We need to save a copy since the user can alter the filter state
        // and then click on the cancel button. We want to make sure the
        // original filter state is saved.
        MechSearchFilter currFilter = mechFilter;
        mechFilter = new MechSearchFilter(currFilter);
        txtEqExp.setText(mechFilter.getEquipmentExpression());
        if ((filterToks == null) || filterToks.isEmpty()
                || (filterToks.lastElement() instanceof OperationFT)) {
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
        cWalk.setSelectedIndex(0);
        tWalk.setText("");
        cJump.setSelectedIndex(0);
        tJump.setText("");
        cArmor.setSelectedIndex(0);
        cOfficial.setSelectedIndex(0);
        cClanEngine.setSelectedIndex(0);
        cOmni.setSelectedIndex(0);
        tblWeapons.clearSelection();
        tblEquipment.clearSelection();
        txtEqExp.setText("");
        cbxEnableArmorSearch.setSelected(false);
        cbxEnableQuirkSearch.setSelected(false);
        cbxEnableEngineSearch.setSelected(false);
        cbxEnableCockpitSearch.setSelected(false);
        cbxEnableInternalsSearch.setSelected(false);
        cboArmorType.setSelectedIndex(0);
        cboQuirkType.setSelectedIndex(0);
        cboEngineType.setSelectedIndex(0);
        cbxFilterLAM.setSelected(false);
        cbxfilterQuad.setSelected(false);
        cbxFilterTripod.setSelected(false);
        cbxfilterQuadVee.setSelected(false);
        cbxfilterSupportVTOL.setSelected(false);
        cbxfilterFixedWingSupport.setSelected(false);
        cbxFilterSuperHeavyTank.setSelected(false);
        cbxFilterSupportTank.setSelected(false);
        cbxFilterLargeSupportTank.setSelected(false);
        cboCockpitType.setSelectedIndex(0);
        cboInternalsType.setSelectedIndex(0);
        tStartYear.setText("");
        tEndYear.setText("");
        tStartTons.setText("");
        tEndTons.setText("");
        tStartBV.setText("");
        tEndBV.setText("");
        mechFilter = null;
        filterToks.clear();
        btnBack.setEnabled(false);
        disableOperationButtons();
        enableSelectionButtons();
    }

    /**
     * Creates collections for all the possible <code>WeaponType</code>s and
     * <code>EquipmentType</code>s. These are used to populate the weapons
     * and equipment tables.
     */
    private void populateWeaponsAndEquipmentChoices() {
        Vector<WeaponType> weapons = new Vector<>();
        Vector<EquipmentType> equipment = new Vector<>();

        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if (et instanceof WeaponType) {
                weapons.add((WeaponType) et);
                // Check for C3+Tag and C3 Master Booster
                if (et.hasFlag(WeaponType.F_C3M) || et.hasFlag(WeaponType.F_C3MBS)) {
                    equipment.add(et);
                }
            } else if (et instanceof MiscType) {
                equipment.add(et);
            }
        }
        weaponsModel.setData(weapons);
        equipmentModel.setData(equipment);
    }

    public MechSearchFilter getMechSearchFilter() {
        return mechFilter;
    }

    /**
     * Update the search fields that aren't automatically updated.
     */
    protected void updateMechSearchFilter() {
        mechFilter.isDisabled = false;
        mechFilter.sWalk = tWalk.getText();
        mechFilter.iWalk = cWalk.getSelectedIndex();

        mechFilter.sJump = tJump.getText();
        mechFilter.iJump = cJump.getSelectedIndex();

        mechFilter.iArmor = cArmor.getSelectedIndex();
        mechFilter.iOmni = cOmni.getSelectedIndex();
        mechFilter.iOfficial = cOfficial.getSelectedIndex();
        mechFilter.iClanEngine = cClanEngine.getSelectedIndex();

        mechFilter.sStartYear = tStartYear.getText();
        mechFilter.sEndYear = tEndYear.getText();

        mechFilter.sStartTons = tStartTons.getText();
        mechFilter.sEndTons = tEndTons.getText();

        mechFilter.sStartBV = tStartBV.getText();
        mechFilter.sEndBV = tEndBV.getText();

        mechFilter.checkArmorType = cbxEnableArmorSearch.isSelected();
        if (cbxEnableArmorSearch.isSelected()) {
            mechFilter.armorType = cboArmorType.getSelectedIndex();
        }

        mechFilter.checkQuirkType = cbxEnableQuirkSearch.isSelected();
        if (cbxEnableQuirkSearch.isSelected()) {
            mechFilter.quirkType = cboQuirkType.getSelectedItem().toString();
        }

        mechFilter.checkEngineType = cbxEnableEngineSearch.isSelected();
        if (cbxEnableEngineSearch.isSelected()) {
            mechFilter.engineType = cboEngineType.getSelectedItem().toString();
        }

        mechFilter.checkInternalsType = cbxEnableInternalsSearch.isSelected();
        if (cbxEnableInternalsSearch.isSelected()) {
            mechFilter.internalsType = cboInternalsType.getSelectedIndex();
        }

        mechFilter.checkCockpitType = cbxEnableCockpitSearch.isSelected();
        if (cbxEnableCockpitSearch.isSelected()) {
            mechFilter.cockpitType = cboCockpitType.getSelectedIndex();
        }

        mechFilter.filterLAM = cbxFilterLAM.isSelected();
        mechFilter.filterQuad = cbxfilterQuad.isSelected();
        mechFilter.filterTripod = cbxFilterTripod.isSelected();
        mechFilter.filterQuadVee = cbxfilterQuadVee.isSelected();
        mechFilter.filterSupportVTOL = cbxfilterSupportVTOL.isSelected();
        mechFilter.filterFixedWingSupport = cbxfilterFixedWingSupport.isSelected();
        mechFilter.filterSuperHeavyTank = cbxFilterSuperHeavyTank.isSelected();
        mechFilter.filterSupportTank = cbxFilterSupportTank.isSelected();
        mechFilter.filterLargeSupportTank = cbxFilterLargeSupportTank.isSelected();
    }

    public class WeaponClassTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        private static final int COL_QTY = 0;
        private static final int COL_NAME = 1;
        private static final int N_COL = 2;
        private static final int COL_VAL = 2;


        private int[] qty;

        private Vector<WeaponClass> weaponClasses = new Vector<>();

        public WeaponClassTableModel() {
            for (WeaponClass cl : WeaponClass.values())
            {
                weaponClasses.add(cl);
            }
            qty = new int[weaponClasses.size()];
            Arrays.fill(qty, 1);
        }

        @Override
        public int getRowCount() {
            return weaponClasses.size();
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
                    return "Weapon Class";
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
        // public void setData(Vector<Integer> wps) {
        //     weaponClasses = wps;
        //     qty = new int[wps.size()];
        //     Arrays.fill(qty, 1);
        //     fireTableDataChanged();
        // }

        public WeaponClass getWeaponTypeAt(int row) {
            return weaponClasses.elementAt(row);
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= weaponClasses.size()) {
                return null;
            }
            
            switch (col) {
                case COL_QTY:
                    return qty[row] + "";
                case COL_NAME:
                    return weaponClasses.elementAt(row).toString();
                case COL_VAL:
                    return weaponClasses.elementAt(row);
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
     * A table model for displaying weapons
     */
    public class WeaponsTableModel extends AbstractTableModel {

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
     * A table model for displaying equipment
     */
    public class EquipmentTableModel extends AbstractTableModel {

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

        public int getPreferredWidth(int col) {
            switch (col) {
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
        // Ensure we've got a number or letter pressed
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
    public static class FilterTokens {

    }

    /**
     * FilterTokens subclass that represents parenthesis.
     * @author Arlith
     */
    public static class ParensFT extends FilterTokens {
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
    public static class EquipmentFT extends FilterTokens {
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
            return qty + " " + fullName + ((qty != 1) ? "s" : "");
        }
    }

    /**
     * FilterTokens subclass that represents a boolean operation.
     * @author Arlith
     */
    public static class OperationFT extends FilterTokens {
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

    public class WeaponClassFT extends FilterTokens {
        public WeaponClass weaponClass;
        public int qty;

        public WeaponClassFT(WeaponClass in_class, int in_qty) {
            weaponClass = in_class;
            qty = in_qty;
        }

        @Override
        public String toString() {
            if (qty == 1) {
                return qty + " " + weaponClass.toString();
            } else {
                return qty + " " + weaponClass.toString() + "s";
            }
        }
    }

    public static enum WeaponClass {
        AUTOCANNON {
            public String toString() {
                return "Autocannon";
            }
        },
        RAC,
        ULTRA {
            public String toString() {
                return "Ultra A/C";
            }
        },
        LIGHT {
            public String toString() {
                return "Light A/C";
            }
        },
        MACHINE_GUN {
            public String toString() {
                return "Machine Gun";
            }
        },
        GAUSS {
            public String toString() {
                return "Gauss";
            }
        },
        BALLISTIC {
            public String toString() {
                return "Ballistic";
            }
        },
        PLASMA {
            public String toString() {
                return "Plasma";
            }
        },
        ENERGY {
            public String toString() {
                return "Energy";
            }
        },
        LASER {
            public String toString() {
                return "Laser";
            }
        },
        PULSE {
            public String toString() {
                return "Pulse Laser";
            }
        },
        RE_ENGINEERED {
            public String toString() {
                return "Re-Engineered Laser";
            }
        },
        PPC {
            public String toString() {
                return "PPC";
            }
        },
        TASER {
            public String toString() {
                return "Taser";
            }
        },
        FLAMER {
            public String toString() {
                return "Flamer";
            }
        },
        MISSILE {
            public String toString() {
                return "Missile";
            }
        },
        LRM,
        MRM,
        SRM,
        PHYSICAL {
            public String toString() {
                return "Physical (inc. industrial equipment)";
            }
        },
        AMS,
        PRACTICAL_PHYSICAL {
            public String toString() {
                return "Physical (weapons only)";
            }
        };

        public boolean matches(String name) {
            if (name.toLowerCase().contains("ammo")) {
                return false;
            }
            if (this == PHYSICAL) {
                String lName = name.toLowerCase();

                if (lName.contains("backhoe") || 
                    lName.contains("saw") ||
                    lName.contains("whip") ||
                    lName.contains("claw") ||
                    lName.contains("combine") ||
                    lName.contains("flail") ||
                    lName.contains("hatchet") ||
                    lName.contains("driver") ||
                    lName.contains("lance") ||
                    lName.contains("mace") ||
                    lName.contains("drill") ||
                    lName.contains("ram") ||
                    lName.contains("blade") ||
                    lName.contains("cutter") ||
                    lName.contains("shield") ||
                    lName.contains("welder") ||
                    lName.contains("sword") ||
                    lName.contains("talons") ||
                    lName.contains("wrecking")) {
                    return true;
                }
            } else if (this == PRACTICAL_PHYSICAL) {
                String lName = name.toLowerCase();

                if (lName.contains("claw") ||
                    lName.contains("flail") ||
                    lName.contains("hatchet") ||
                    lName.contains("lance") ||
                    lName.contains("mace") ||
                    lName.contains("blade") ||
                    lName.contains("shield") ||
                    lName.contains("sword") ||
                    lName.contains("talons")) {
                    return true;
                }
            } else if (this == MISSILE) {
                if ((name.toLowerCase().contains("lrm") ||
                    name.toLowerCase().contains("mrm") || 
                    name.toLowerCase().contains("srm")) && 
                    !name.toLowerCase().contains("ammo")) {
                    return true;
                }
            } else if (this == RE_ENGINEERED) { 
                if (name.toLowerCase().contains("engineered")) {
                    return true;
                }
            } else if (this == ENERGY) {
                if (WeaponClass.LASER.matches(name) || WeaponClass.PPC.matches(name) || WeaponClass.FLAMER.matches(name)) {
                    return true;
                }
            } else if (this == MACHINE_GUN) {
                if ((name.toLowerCase().contains("mg") || name.toLowerCase().contains("machine")) && !name.toLowerCase().contains("ammo")) {
                    return true;
                }
            } else if (this == BALLISTIC) {
                return WeaponClass.AUTOCANNON.matches(name) || 
                    WeaponClass.GAUSS.matches(name) || 
                    WeaponClass.MISSILE.matches(name) || 
                    WeaponClass.MACHINE_GUN.matches(name);
            } else if (this == RAC) {
                if (name.toLowerCase().contains("rotary")) {
                    return true;
                }
            } else if (this == ULTRA) {
                if (name.toLowerCase().contains("ultraa")) {
                    return true;
                }
            } else if (name.toLowerCase().contains(this.name().toLowerCase()) && !name.toLowerCase().contains("ammo")) {
                return true;
            }
            return false;
        }
    }

    public void adaptToGUIScale() {
        scrTableWeaponType.setMinimumSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(150)));
        scrTableWeaponType.setPreferredSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(150)));
        scrTableWeapons.setMinimumSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(350)));
        scrTableWeapons.setPreferredSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(150)));
        scrTableEquipment.setMinimumSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(350)));
        scrTableEquipment.setPreferredSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(150)));
    }
}
