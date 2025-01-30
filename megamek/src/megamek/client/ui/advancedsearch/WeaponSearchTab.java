/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * listener file is part of MegaMek.
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
package megamek.client.ui.advancedsearch;

import com.formdev.flatlaf.FlatClientProperties;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.FlatLafStyleBuilder;
import megamek.client.ui.swing.util.FontHandler;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.PatternSyntaxException;

class WeaponSearchTab extends JPanel implements KeyListener, DocumentListener, FocusListener {

    final List<FilterToken> filterTokens = new ArrayList<>();

    private final JButton btnLeftParen = new JButton("(");
    private final JButton btnRightParen = new JButton(")");
    private final JSpinner equipmentCount;
    private final JToggleButton btnLessThan = new JToggleButton("<");
    private final JToggleButton btnAtLeast = new JToggleButton("\u2265");
    private final JButton btnAdd = new JButton(Messages.getString("MekSelectorDialog.Search.add"));
    private final JButton btnAddMultiOr = new JButton(Messages.getString("MekSelectorDialog.Search.AddOR"));
    private final JButton btnAddMultiAnd = new JButton(Messages.getString("MekSelectorDialog.Search.AddAND"));
    private final JButton btnAnd = new JButton(Messages.getString("MekSelectorDialog.Search.and"));
    private final JButton btnOr = new JButton(Messages.getString("MekSelectorDialog.Search.or"));
    private final JButton btnClear = new JButton(Messages.getString("MekSelectorDialog.Search.Reset"));
    private final JButton btnBack = new JButton(Messages.getString("MekSelectorDialog.Search.Back"));
    private final JLabel lblWEEqExpTxt = new JLabel(Messages.getString("MekSelectorDialog.Search.FilterExpression"));
    final JTextArea txtWEEqExp = new JTextArea("", 2, 40);

    private final JButton btnUnitTypeAll = new JButton("All");
    private final JToggleButton btnUnitTypeMek = new JToggleButton(Messages.getString("MekSelectorDialog.Search.Mek"));
    private final JToggleButton btnUnitTypeVee = new JToggleButton(Messages.getString("MekSelectorDialog.Search.Vee"));
    private final JToggleButton btnUnitTypeBA = new JToggleButton(Messages.getString("MekSelectorDialog.Search.BA"));
    private final JToggleButton btnUnitTypeCI = new JToggleButton(Messages.getString("MekSelectorDialog.Search.CI"));
    private final JToggleButton btnUnitTypeAero = new JToggleButton(Messages.getString("MekSelectorDialog.Search.Aero"));
    private final JToggleButton btnUnitTypePM = new JToggleButton(Messages.getString("MekSelectorDialog.Search.PM"));
    private final List<JToggleButton> unitTypeButtons =
        List.of(btnUnitTypeMek, btnUnitTypePM, btnUnitTypeBA, btnUnitTypeCI, btnUnitTypeAero, btnUnitTypeVee);

    private final JToggleButton btnTechClassIS = new JToggleButton("Inner Sphere");
    private final JToggleButton btnTechClassClan = new JToggleButton("Clan");

    private final JButton btnTechLevelOfficial = new JButton(Messages.getString("MekSelectorDialog.Search.AllOfficial"));
    private final JToggleButton btnTechLevelIntro = new JToggleButton(Messages.getString("MekSelectorDialog.Search.Introductory"));
    private final JToggleButton btnTechLevelStd = new JToggleButton(Messages.getString("MekSelectorDialog.Search.Standard"));
    private final JToggleButton btnTechLevelAdv = new JToggleButton(Messages.getString("MekSelectorDialog.Search.Advanced"));
    private final JToggleButton btnTechLevelExp = new JToggleButton(Messages.getString("MekSelectorDialog.Search.Experimental"));
    private final JToggleButton btnTechLevelUnoff = new JToggleButton(Messages.getString("MekSelectorDialog.Search.Unofficial"));
    private final List<JToggleButton> techLevelButtons =
        List.of(btnTechLevelIntro, btnTechLevelStd, btnTechLevelAdv, btnTechLevelExp, btnTechLevelUnoff);

    private final JTextField tableFilterText = new JTextField(10);
    private final JButton filterClearButton = new JButton("X");
    private final JComboBox<AdvancedSearchEquipmentClass> weaponClassFilter = new JComboBox<>(AdvancedSearchEquipmentClass.values());

    private final SearchableTable tblWeapons;
    private final WeaponsTableModel weaponsModel;
    private final TableRowSorter<WeaponsTableModel> weaponsSorter;

    private final SearchableTable tblEquipment;
    private final EquipmentTableModel equipmentModel;
    private final TableRowSorter<EquipmentTableModel> equipmentSorter;

    private final JComboBox<AdvancedSearchEquipmentClass> weaponClassChooser = new JComboBox<>(AdvancedSearchEquipmentClass.values());

    private JComponent focusedSelector = null;
    private final TWAdvancedSearchPanel parentPanel;

    WeaponSearchTab(TWAdvancedSearchPanel parentPanel) {
        this.parentPanel = parentPanel;

        ButtonGroup atleastGroup = new ButtonGroup();
        atleastGroup.add(btnAtLeast);
        atleastGroup.add(btnLessThan);
        var btnStyle = new FlatLafStyleBuilder(FontHandler.notoFont());
        btnStyle.apply(btnAtLeast);
        btnStyle.apply(btnLessThan);

        btnAnd.addActionListener(e -> addFilterToken(new AndFilterToken()));
        btnAdd.addActionListener(e -> addFilter(true));
        btnAddMultiAnd.addActionListener(e -> addFilter(true));
        btnAddMultiOr.addActionListener(e -> addFilter(false));
        btnLeftParen.addActionListener(e -> addFilterToken(new LeftParensFilterToken()));
        btnRightParen.addActionListener(e -> addFilterToken(new RightParensFilterToken()));
        btnOr.addActionListener(e -> addFilterToken(new OrFilterToken()));
        btnClear.addActionListener(e -> clear());
        btnBack.addActionListener(e -> backOperation());
        btnAtLeast.setSelected(true);
        adaptTokenButtons();

        btnUnitTypeAll.addActionListener(e -> allUnitTypesClicked());
        unitTypeButtons.forEach(button -> button.setSelected(true));

        btnTechClassClan.setSelected(true);
        btnTechClassIS.setSelected(true);

        btnTechLevelOfficial.addActionListener(e -> officialTechLevelClicked());
        techLevelButtons.forEach(button -> button.setSelected(button != btnTechLevelUnoff));
        addToggleActionListeners();

        equipmentCount = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        weaponClassChooser.addFocusListener(this);

        // Setup Weapons Table
        weaponsModel = new WeaponsTableModel(parentPanel);
        tblWeapons = new SearchableTable(weaponsModel, WeaponsTableModel.COL_NAME) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(UIUtil.scaleForGUI(600), getRowHeight() * 6);
            }
        };
        tblWeapons.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        weaponsSorter = new TableRowSorter<>(weaponsModel);
        tblWeapons.setRowSorter(weaponsSorter);
        tblWeapons.addKeyListener(this);
        tblWeapons.addFocusListener(this);
        tblWeapons.getSelectionModel().addListSelectionListener(e -> adaptTokenButtons());

        var tableDataRenderer = new EquipmentDataRenderer();
        List<Integer> numberColumns = List.of(WeaponsTableModel.COL_DMG, WeaponsTableModel.COL_HEAT, WeaponsTableModel.COL_MIN,
            WeaponsTableModel.COL_SHORT, WeaponsTableModel.COL_MED, WeaponsTableModel.COL_LONG);
        for (int column : numberColumns) {
            tblWeapons.getColumnModel().getColumn(column).setCellRenderer(tableDataRenderer);
        }

        var techBaseRenderer = new TechBaseRenderer();
        tblWeapons.getColumnModel().getColumn(WeaponsTableModel.COL_IS_CLAN).setCellRenderer(techBaseRenderer);
        tblWeapons.getColumnModel().getColumn(WeaponsTableModel.COL_LEVEL).setCellRenderer(techBaseRenderer);

        for (int i = 0; i < weaponsModel.getColumnCount(); i++) {
            tblWeapons.getColumnModel().getColumn(i).setPreferredWidth(weaponsModel.getPreferredWidth(i));
        }

        // Setup Equipment Table
        equipmentModel = new EquipmentTableModel(parentPanel);
        tblEquipment = new SearchableTable(equipmentModel, EquipmentTableModel.COL_NAME) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                Dimension standardSize = super.getPreferredScrollableViewportSize();
                return new Dimension(standardSize.width, getRowHeight() * 6);
            }
        };
        tblEquipment.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        equipmentSorter = new TableRowSorter<>(equipmentModel);
        tblEquipment.setRowSorter(equipmentSorter);
        tblEquipment.addKeyListener(this);
        tblEquipment.addFocusListener(this);
        tblEquipment.getSelectionModel().addListSelectionListener(e -> adaptTokenButtons());

        for (int i = 0; i < equipmentModel.getColumnCount(); i++) {
            tblEquipment.getColumnModel().getColumn(i).setPreferredWidth(equipmentModel.getPreferredWidth(i));
        }
        tblEquipment.getColumnModel().getColumn(EquipmentTableModel.COL_LEVEL).setCellRenderer(techBaseRenderer);
        var costRenderer = new EquipmentCostRenderer();
        tblEquipment.getColumnModel().getColumn(1).setCellRenderer(costRenderer);
        tblEquipment.getColumnModel().getColumn(2).setCellRenderer(techBaseRenderer);

        // Populate Tables
        populateWeaponsAndEquipmentChoices();

        // initialize with the weapons sorted alphabetically by name
        ArrayList<RowSorter.SortKey> sortlist = new ArrayList<>();
        sortlist.add(new RowSorter.SortKey(WeaponsTableModel.COL_NAME, SortOrder.ASCENDING));
        tblWeapons.getRowSorter().setSortKeys(sortlist);
        ((DefaultRowSorter<?, ?>) tblWeapons.getRowSorter()).sort();
        tblWeapons.invalidate();

        // initialize with the equipment sorted alphabetically by chassis
        sortlist = new ArrayList<>();
        sortlist.add(new RowSorter.SortKey(EquipmentTableModel.COL_NAME, SortOrder.ASCENDING));
        tblEquipment.getRowSorter().setSortKeys(sortlist);
        ((DefaultRowSorter<?, ?>) tblEquipment.getRowSorter()).sort();
        tblEquipment.invalidate();

        txtWEEqExp.setEditable(false);
        txtWEEqExp.setLineWrap(true);
        txtWEEqExp.setWrapStyleWord(true);

        tableFilterText.getDocument().addDocumentListener(this);
        filterClearButton.addActionListener(e -> tableFilterText.setText(""));
        filterClearButton.setToolTipText(Messages.getString("MekSelectorDialog.Search.TableFilterClear.TT"));
        filterClearButton.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
        weaponClassFilter.addActionListener(e -> filterTables());

        JPanel upperPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(2, 0, 2, 0);
        upperPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.TechClass")), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JPanel techClassButtonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        techClassButtonPanel.setBorder(new EmptyBorder(0, 20, 0, 0));
        gbc.weightx = 0;
        techClassButtonPanel.add(btnTechClassIS);
        techClassButtonPanel.add(btnTechClassClan);
        upperPanel.add(techClassButtonPanel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        upperPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.UnitType")), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JPanel unitTypeButtonPanel = new JPanel(new GridLayout(1, 7, 10, 0));
        unitTypeButtonPanel.setBorder(new EmptyBorder(0, 20, 0, 0));
        unitTypeButtonPanel.add(btnUnitTypeAll);
        unitTypeButtonPanel.add(btnUnitTypeMek);
        unitTypeButtonPanel.add(btnUnitTypeVee);
        unitTypeButtonPanel.add(btnUnitTypePM);
        unitTypeButtonPanel.add(btnUnitTypeCI);
        unitTypeButtonPanel.add(btnUnitTypeBA);
        unitTypeButtonPanel.add(btnUnitTypeAero);
        upperPanel.add(unitTypeButtonPanel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        upperPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.TechLevel")), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JPanel techLevelButtonPanel = new JPanel(new GridLayout(1, 6, 10, 0));
        techLevelButtonPanel.setBorder(new EmptyBorder(0, 20, 0, 0));
        techLevelButtonPanel.add(btnTechLevelOfficial);
        techLevelButtonPanel.add(btnTechLevelIntro);
        techLevelButtonPanel.add(btnTechLevelStd);
        techLevelButtonPanel.add(btnTechLevelAdv);
        techLevelButtonPanel.add(btnTechLevelExp);
        techLevelButtonPanel.add(btnTechLevelUnoff);
        upperPanel.add(techLevelButtonPanel, gbc);
        gbc.weightx = 0;

        JPanel filterPanel = new JPanel();
        filterPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.TableFilter")));
        filterPanel.add(tableFilterText);
        filterPanel.add(filterClearButton);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.EquipmentGroup")));
        filterPanel.add(weaponClassFilter);
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        upperPanel.add(filterPanel, gbc);

        gbc.gridheight = 1;
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        upperPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Weapons")), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridy++;
        upperPanel.add(new JScrollPane(tblWeapons), gbc);

        gbc.gridy++;
        upperPanel.add(Box.createVerticalStrut(10), gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        upperPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Equipment")), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridy++;
        upperPanel.add(new JScrollPane(tblEquipment), gbc);

        gbc.gridy++;
        upperPanel.add(Box.createVerticalStrut(10), gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 20);
        upperPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.WeaponClass")), gbc);
        gbc.gridy++;
        JPanel weaponClassPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        weaponClassPanel.add(weaponClassChooser);
        upperPanel.add(weaponClassPanel, gbc);

        JPanel addBtnPanel = new JPanel();
        addBtnPanel.add(equipmentCount);
        addBtnPanel.add(btnAtLeast);
        addBtnPanel.add(btnLessThan);
        addBtnPanel.add(btnAdd);
        addBtnPanel.add(btnAddMultiAnd);
        addBtnPanel.add(btnAddMultiOr);

        JPanel controlBtnPanel = new JPanel();
        controlBtnPanel.add(btnLeftParen);
        controlBtnPanel.add(btnRightParen);
        controlBtnPanel.add(btnAnd);
        controlBtnPanel.add(btnOr);
        controlBtnPanel.add(btnBack);
        controlBtnPanel.add(btnClear);

        Box filterExpressionPanel = Box.createHorizontalBox();
        filterExpressionPanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        filterExpressionPanel.add(lblWEEqExpTxt);
        filterExpressionPanel.add(Box.createHorizontalStrut(20));
        filterExpressionPanel.add(new JScrollPane(txtWEEqExp));

        Box filterAssemblyPanel = Box.createVerticalBox();
        filterAssemblyPanel.add(Box.createVerticalStrut(10));
        filterAssemblyPanel.add(addBtnPanel);
        filterAssemblyPanel.add(controlBtnPanel);
        filterAssemblyPanel.add(filterExpressionPanel);
        filterAssemblyPanel.add(Box.createVerticalStrut(10));

        setLayout(new BorderLayout());
        add(new TWAdvancedSearchPanel.StandardScrollPane(upperPanel), BorderLayout.CENTER);
        add(filterAssemblyPanel, BorderLayout.PAGE_END);
    }

    void filterTables() {
        RowFilter<WeaponsTableModel, Integer> weaponFilter;
        // If current expression doesn't parse, don't update.
        try {
            weaponFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends WeaponsTableModel, ? extends Integer> entry) {
                    WeaponsTableModel weapModel = entry.getModel();
                    WeaponType wp = weapModel.getWeaponTypeAt(entry.getIdentifier());
                    String weaponTechClass = TechConstants.getTechName(wp.getTechLevel(parentPanel.gameYear));
                    boolean techLvlMatch = matchTechLvl(wp.getTechLevel(parentPanel.gameYear));
                    boolean techClassMatch = matchTechClass(weaponTechClass);
                    boolean unitTypeMatch = matchUnitTypeToWeapon(wp);
                    boolean textFilterMatch = (tableFilterText.getText() == null) || (tableFilterText.getText().length() < 2)
                        || matchWeaponTextFilter(entry);
                    boolean equipmentClassMatch = (weaponClassFilter.getSelectedItem() instanceof AdvancedSearchEquipmentClass equipmentclass)
                        && equipmentclass.matches(wp.getInternalName());
                    return techLvlMatch && techClassMatch && unitTypeMatch && textFilterMatch && equipmentClassMatch;
                }
            };
        } catch (PatternSyntaxException ignored) {
            return;
        }
        weaponsSorter.setRowFilter(weaponFilter);

        RowFilter<EquipmentTableModel, Integer> equipmentFilter;
        try {
            equipmentFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends EquipmentTableModel, ? extends Integer> entry) {
                    EquipmentTableModel eqModel = entry.getModel();
                    MiscType eq = eqModel.getEquipmentTypeAt(entry.getIdentifier());
                    String currTechClass = TechConstants.getTechName(eq.getTechLevel(parentPanel.gameYear));
                    boolean techLvlMatch = matchTechLvl(eq.getTechLevel(parentPanel.gameYear));
                    boolean techClassMatch = matchTechClass(currTechClass);
                    boolean unitTypeMatch = matchUnitTypeToMisc(eq);
                    boolean textFilterMatch = (tableFilterText.getText() == null) || (tableFilterText.getText().length() < 2)
                        || matchEquipmentTextFilter(entry);
                    boolean equipmentClassMatch = (weaponClassFilter.getSelectedItem() instanceof AdvancedSearchEquipmentClass equipmentclass)
                        && equipmentclass.matches(eq.getInternalName());
                    return techLvlMatch && techClassMatch && unitTypeMatch && textFilterMatch && equipmentClassMatch;
                }
            };
        } catch (PatternSyntaxException ignored) {
            return;
        }
        equipmentSorter.setRowFilter(equipmentFilter);
    }

    void clear() {
        filterTokens.clear();
        tblWeapons.clearSelection();
        tblEquipment.clearSelection();
        txtWEEqExp.setText("");
        adaptTokenButtons();
    }

    /**
     * Creates collections for all the possible WeaponTypes and MiscTypes. These are used to populate the weapons and equipment tables.
     */
    private void populateWeaponsAndEquipmentChoices() {
        List<WeaponType> weapons = new ArrayList<>();
        List<MiscType> equipment = new ArrayList<>();

        for (EquipmentType et : EquipmentType.allTypes()) {
            if (et instanceof WeaponType) {
                weapons.add((WeaponType) et);
            } else if (et instanceof MiscType) {
                equipment.add((MiscType) et);
            }
        }

        weaponsModel.setData(weapons);
        equipmentModel.setData(equipment);
    }


    @Override
    public void keyPressed(KeyEvent evt) { }

    @Override
    public void keyReleased(KeyEvent evt) { }

    @Override
    public void keyTyped(KeyEvent evt) {
        char keyChar = evt.getKeyChar();
        // Ensure we've got a number or letter pressed
        if (!(((keyChar >= '0') && (keyChar <= '9')) ||
            ((keyChar >= 'a') && (keyChar <= 'z')) || (keyChar == ' '))) {
            return;
        }

        if (evt.getComponent().equals(tblWeapons)) {
            tblWeapons.keyTyped(evt);
        } else if (evt.getComponent().equals(tblEquipment)) {
            tblEquipment.keyTyped(evt);
        }
    }

    private boolean matchTechClass(String equipmentTechClass) {
        if (equipmentTechClass.equals("IS/Clan")
            || (btnTechClassClan.isSelected() && btnTechClassIS.isSelected())
            || (!btnTechClassClan.isSelected() && !btnTechClassIS.isSelected())) {
            return true;
        } else {
            if (btnTechClassIS.isSelected()) {
                return equipmentTechClass.equals("Inner Sphere");
            } else {
                return equipmentTechClass.equals("Clan");
            }
        }
    }

    private boolean matchUnitTypeToWeapon(WeaponType eq) {
        if (areAllUnitTypesSelected()) {
            return true;
        } else {
            return (btnUnitTypeMek.isSelected() && eq.hasFlag(WeaponTypeFlag.F_MEK_WEAPON))
                || (btnUnitTypeVee.isSelected() && eq.hasFlag(WeaponTypeFlag.F_TANK_WEAPON))
                || (btnUnitTypePM.isSelected() && eq.hasFlag(WeaponTypeFlag.F_PROTO_WEAPON))
                || (btnUnitTypeBA.isSelected() && eq.hasFlag(WeaponTypeFlag.F_BA_WEAPON))
                || (btnUnitTypeCI.isSelected() && eq.hasFlag(WeaponTypeFlag.F_INFANTRY))
                || (btnUnitTypeAero.isSelected() && eq.hasFlag(WeaponTypeFlag.F_AERO_WEAPON));
        }
    }

    private boolean matchUnitTypeToMisc(MiscType eq) {
        if (areAllUnitTypesSelected()) {
            return true;
        } else {
            return (btnUnitTypeMek.isSelected() && eq.hasFlag(EquipmentFlag.F_MEK_EQUIPMENT))
                || (btnUnitTypeVee.isSelected() && eq.hasFlag(EquipmentFlag.F_TANK_EQUIPMENT))
                || (btnUnitTypePM.isSelected() && eq.hasFlag(EquipmentFlag.F_PROTOMEK_EQUIPMENT))
                || (btnUnitTypeBA.isSelected() && eq.hasFlag(EquipmentFlag.F_BA_EQUIPMENT))
                || (btnUnitTypeCI.isSelected() && eq.hasFlag(EquipmentFlag.F_INF_EQUIPMENT))
                || (btnUnitTypeAero.isSelected() && eq.hasFlag(EquipmentFlag.F_FIGHTER_EQUIPMENT));
        }
    }

    // Build the string representation of the new expression
    String filterExpressionString() {
        StringBuilder filterExp = new StringBuilder();
        for (FilterToken filterTok : filterTokens) {
            filterExp.append(" ").append(filterTok.toString()).append(" ");
        }
        return filterExp.toString();
    }

    private boolean matchTechLvl(int equipmentTechLevel) {
        int simpleEquipmentLevel = TechConstants.convertFromNormalToSimple(equipmentTechLevel);
        return switch (simpleEquipmentLevel) {
            case TechConstants.T_SIMPLE_INTRO -> btnTechLevelIntro.isSelected();
            case TechConstants.T_SIMPLE_STANDARD -> btnTechLevelStd.isSelected();
            case TechConstants.T_SIMPLE_ADVANCED -> btnTechLevelAdv.isSelected();
            case TechConstants.T_SIMPLE_EXPERIMENTAL -> btnTechLevelExp.isSelected();
            default -> btnTechLevelUnoff.isSelected();
        };
    }

    private void addEquipmentFilter(int row, int qty, boolean atleast) {
        String internalName = (String) tblEquipment.getModel().getValueAt(
            tblEquipment.convertRowIndexToModel(row),
            EquipmentTableModel.COL_INTERNAL_NAME);
        String fullName = (String) tblEquipment.getValueAt(row, EquipmentTableModel.COL_NAME);
        filterTokens.add(new EquipmentTypeFT(internalName, fullName, qty, atleast));
    }

    private void addWeaponFilter(int row, int qty, boolean atleast) {
        String internalName = (String) tblWeapons.getModel().getValueAt(
            tblWeapons.convertRowIndexToModel(row),
            WeaponsTableModel.COL_INTERNAL_NAME);
        String fullName = (String) tblWeapons.getValueAt(row, WeaponsTableModel.COL_NAME);
        filterTokens.add(new EquipmentTypeFT(internalName, fullName, qty, atleast));
    }

    private void addFilter(boolean and) {
        boolean atleast = btnAtLeast.isSelected();
        int qty = (int) equipmentCount.getValue();
        if (focusedSelector == tblEquipment) {
            int[] rows = tblEquipment.getSelectedRows();
            if (rows.length == 1) {
                addEquipmentFilter(rows[0], qty, atleast);
            } else if (rows.length > 1) {
                addFilterToken(new LeftParensFilterToken());
                for (int row : rows) {
                    if (row != rows[0]) {
                        addFilterToken(and ? new AndFilterToken() : new OrFilterToken());
                    }
                    addEquipmentFilter(row, qty, atleast);
                }
                addFilterToken(new RightParensFilterToken());
            }

        } else if (focusedSelector == tblWeapons) {
            int[] rows = tblWeapons.getSelectedRows();
            if (rows.length == 1) {
                addWeaponFilter(rows[0], qty, atleast);
            } else if (rows.length > 1) {
                addFilterToken(new LeftParensFilterToken());
                for (int row : rows) {
                    if (row != rows[0]) {
                        addFilterToken(and ? new AndFilterToken() : new OrFilterToken());
                    }
                    addWeaponFilter(row, qty, atleast);
                }
                addFilterToken(new RightParensFilterToken());
            }
        } else if ((focusedSelector == weaponClassChooser) && (weaponClassChooser.getSelectedItem() != null)) {
            filterTokens.add(new WeaponClassFT((AdvancedSearchEquipmentClass) weaponClassChooser.getSelectedItem(), qty, atleast));

        } else {
            // if something else is focused, do nothing
            return;
        }
        txtWEEqExp.setText(filterExpressionString());
        adaptTokenButtons();
    }

    private boolean matchWeaponTextFilter(RowFilter.Entry<? extends WeaponsTableModel, ? extends Integer> entry) {
        String wp = entry.getModel().getValueAt(entry.getIdentifier(), WeaponsTableModel.COL_NAME).toString();
        return matchTextFilter(wp);
    }

    private boolean matchEquipmentTextFilter(RowFilter.Entry<? extends EquipmentTableModel, ? extends Integer> entry) {
        String wp = entry.getModel().getValueAt(entry.getIdentifier(), EquipmentTableModel.COL_NAME).toString();
        return matchTextFilter(wp);
    }

    private boolean matchTextFilter(String tableText) {
        return tableText.toLowerCase(Locale.ROOT).contains(tableFilterText.getText().toLowerCase(Locale.ROOT));
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        filterTables();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        filterTables();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        filterTables();
    }

    private void allUnitTypesClicked() {
        removeToggleActionListeners();
        unitTypeButtons.forEach(button -> button.setSelected(true));
        addToggleActionListeners();
        filterTables();
    }

    private boolean areAllUnitTypesSelected() {
        return unitTypeButtons.stream().allMatch(JToggleButton::isSelected);
    }

    private void removeToggleActionListeners() {
        unitTypeButtons.forEach(button -> button.removeActionListener(toggleButtonListener));
        btnTechClassIS.removeActionListener(toggleButtonListener);
        btnTechClassClan.removeActionListener(toggleButtonListener);
        techLevelButtons.forEach(button -> button.removeActionListener(toggleButtonListener));
    }

    private void addToggleActionListeners() {
        unitTypeButtons.forEach(button -> button.addActionListener(toggleButtonListener));
        btnTechClassIS.addActionListener(toggleButtonListener);
        btnTechClassClan.addActionListener(toggleButtonListener);
        techLevelButtons.forEach(button -> button.addActionListener(toggleButtonListener));
    }

    private void officialTechLevelClicked() {
        removeToggleActionListeners();
        techLevelButtons.forEach(button -> button.setSelected(button != btnTechLevelUnoff));
        addToggleActionListeners();
        filterTables();
    }

    @Override
    public void focusGained(FocusEvent e) {
        if ((e.getSource() == tblEquipment) || (e.getSource() == tblWeapons)) {
            focusedSelector = (JComponent) e.getSource();
            adaptTokenButtons();
        } else if (e.getSource() == weaponClassChooser) {
            focusWeaponClasschooser();
        }
    }

    @Override
    public void focusLost(FocusEvent e) { }

    private void focusWeaponClasschooser() {
        focusedSelector = weaponClassChooser;
        tblWeapons.clearSelection();
        tblEquipment.clearSelection();
        adaptTokenButtons();
    }

    private @Nullable FilterToken lastToken() {
        return filterTokens.isEmpty() ? null : filterTokens.get(filterTokens.size() - 1);
    }

    private boolean hasFocusedSelector() {
        return (focusedSelector != null)
            && ((focusedSelector == weaponClassChooser) || (focusedSelector == tblEquipment) || (focusedSelector == tblWeapons));
    }

    void adaptTokenButtons() {
        btnBack.setEnabled(!filterTokens.isEmpty());
        btnClear.setEnabled(!filterTokens.isEmpty());

        boolean canAddEquipment = filterTokens.isEmpty() || (lastToken() instanceof OperatorFT)
            || (lastToken() instanceof LeftParensFilterToken);
        btnAdd.setEnabled(hasFocusedSelector() && canAddEquipment && !isMultiSelection());
        btnAddMultiOr.setEnabled(isMultiSelection() && canAddEquipment);
        btnAddMultiAnd.setEnabled(isMultiSelection() && canAddEquipment);
        btnLeftParen.setEnabled(canAddEquipment);

        boolean canAddOperator = (lastToken() instanceof EquipmentFilterToken) || (lastToken() instanceof RightParensFilterToken);
        btnAnd.setEnabled(canAddOperator);
        btnOr.setEnabled(canAddOperator);
        btnRightParen.setEnabled(canAddOperator);
    }

    private boolean isMultiSelection() {
        return ((focusedSelector == tblEquipment) && (tblEquipment != null) && (tblEquipment.getSelectedRows().length > 1))
            || ((focusedSelector == tblWeapons) && (tblWeapons != null) && (tblWeapons.getSelectedRows().length > 1));
    }

    private void addFilterToken(FilterToken token) {
        filterTokens.add(token);
        txtWEEqExp.setText(filterExpressionString());
        adaptTokenButtons();
    }

    private void backOperation() {
        if (!filterTokens.isEmpty()) {
            filterTokens.remove(filterTokens.size() - 1);
            txtWEEqExp.setText(filterExpressionString());
            adaptTokenButtons();
        }
    }

    ActionListener toggleButtonListener = e -> {
        if (!(e.getSource() instanceof JToggleButton source)) {
            return;
        } else if (unitTypeButtons.contains(source) && ((e.getModifiers() & Event.SHIFT_MASK) == 0)) {
            removeToggleActionListeners();
            unitTypeButtons.forEach(button -> button.setSelected(e.getSource() == button));
            addToggleActionListeners();
        } else if (techLevelButtons.contains(source) && ((e.getModifiers() & Event.SHIFT_MASK) == 0)) {
            removeToggleActionListeners();
            techLevelButtons.forEach(button -> button.setSelected(e.getSource() == button));
            addToggleActionListeners();
        } else if ((source == btnTechClassClan || source == btnTechClassIS) && ((e.getModifiers() & Event.SHIFT_MASK) == 0)) {
            removeToggleActionListeners();
            btnTechClassClan.setSelected(source == btnTechClassClan);
            btnTechClassIS.setSelected(source == btnTechClassIS);
            addToggleActionListeners();
        }
        filterTables();
    };
}
