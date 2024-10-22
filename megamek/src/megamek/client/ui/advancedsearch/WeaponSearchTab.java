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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.table.MegaMekTable;
import megamek.common.*;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class WeaponSearchTab extends JPanel implements KeyListener, DocumentListener, FocusListener {

    final List<FilterToken> filterTokens = new ArrayList<>();

    final JButton btnLeftParen = new JButton("(");
    final JButton btnRightParen = new JButton(")");
    final JButton btnAdd = new JButton(Messages.getString("MekSelectorDialog.Search.add"));
    final JButton btnAnd = new JButton(Messages.getString("MekSelectorDialog.Search.and"));
    final JButton btnOr = new JButton(Messages.getString("MekSelectorDialog.Search.or"));
    final JButton btnClear = new JButton(Messages.getString("MekSelectorDialog.Reset"));
    final JButton btnBack = new JButton("Back");
    final JLabel lblWEEqExpTxt = new JLabel(Messages.getString("MekSelectorDialog.Search.FilterExpression"));
    final JTextArea txtWEEqExp = new JTextArea("", 2, 40);
    final JScrollPane expWEScroller = new JScrollPane(txtWEEqExp,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    final JLabel lblTableFilters = new JLabel(Messages.getString("MekSelectorDialog.Search.TableFilters"));
    final JLabel lblUnitType = new JLabel(Messages.getString("MekSelectorDialog.Search.UnitType"));
    final JLabel lblTechClass = new JLabel(Messages.getString("MekSelectorDialog.Search.TechClass"));
    final JLabel lblTechLevelBase = new JLabel(Messages.getString("MekSelectorDialog.Search.TechLevel"));
    final JComboBox<String> cboUnitType = new JComboBox<>();
    final JComboBox<String> cboTechClass = new JComboBox<>();
    final JComboBox<String> cboTechLevel = new JComboBox<>();
    final JLabel tableFilterTextLabel = new JLabel(Messages.getString("MekSelectorDialog.Search.TableFilter"));
    final JTextField tableFilterText = new JTextField(20);

    final JLabel lblWeapons = new JLabel(Messages.getString("MekSelectorDialog.Search.Weapons"));
    final JScrollPane scrTableWeapons = new JScrollPane();
    final MegaMekTable tblWeapons;
    final WeaponsTableModel weaponsModel;
    final TableRowSorter<WeaponsTableModel> weaponsSorter;
    final JLabel lblEquipment = new JLabel(Messages.getString("MekSelectorDialog.Search.Equipment"));
    final JScrollPane scrTableEquipment = new JScrollPane();
    final MegaMekTable tblEquipment;
    final EquipmentTableModel equipmentModel;
    final TableRowSorter<EquipmentTableModel> equipmentSorter;
    final JComboBox<String> cboQty = new JComboBox<>();

    final JLabel lblWeaponClass = new JLabel(Messages.getString("MekSelectorDialog.Search.WeaponClass"));
    final JSpinner weaponClassCount;
    final JComboBox<WeaponClass> weaponClassChooser;

    JComponent focusedSelector = null;

    private final TWAdvancedSearchPanel parentPanel;

    WeaponSearchTab(TWAdvancedSearchPanel parentPanel) {
        this.parentPanel = parentPanel;

        btnAnd.addActionListener(e -> addFilterToken(new AndFilterToken()));
        btnAdd.addActionListener(e -> addButtonPressed());
        btnLeftParen.addActionListener(e -> addFilterToken(new LeftParensFilterToken()));
        btnRightParen.addActionListener(e -> addFilterToken(new RightParensFilterToken()));
        btnOr.addActionListener(e -> addFilterToken(new OrFilterToken()));
        btnClear.addActionListener(e -> clear());
        btnBack.addActionListener(e -> backOperation());
        adaptTokenButtons();

        for (int i = 1; i <= 20; i++) {
            cboQty.addItem(Integer.toString(i));
        }
        cboQty.setSelectedIndex(0);

        // Setup table filter combo boxes
        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<>();
        unitTypeModel.addElement(Messages.getString("MekSelectorDialog.All"));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.MEK));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.TANK));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.BATTLE_ARMOR));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.INFANTRY));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.PROTOMEK));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.AERO));
        unitTypeModel.setSelectedItem(Messages.getString("MekSelectorDialog.All"));

        cboUnitType.setModel(unitTypeModel);
        cboUnitType.addActionListener(e -> filterTables());

        DefaultComboBoxModel<String> techLevelModel = new DefaultComboBoxModel<>();

        for (int i = 0; i < TechConstants.SIZE; i++) {
            techLevelModel.addElement(TechConstants.getLevelDisplayableName(i));
        }

        techLevelModel.setSelectedItem(TechConstants.getLevelDisplayableName(TechConstants.SIZE - 1));
        cboTechLevel.setModel(techLevelModel);
        cboTechLevel.addActionListener(e -> filterTables());

        DefaultComboBoxModel<String> techClassModel = new DefaultComboBoxModel<>();
        techClassModel.addElement("All");
        techClassModel.addElement("Inner Sphere");
        techClassModel.addElement("Clan");
        techClassModel.addElement("IS/Clan");
        techClassModel.addElement("(Unknown Technology Base)");
        techClassModel.setSelectedItem("All");
        cboTechClass.setModel(techClassModel);
        cboTechClass.addActionListener(e -> filterTables());

        // Set up Weapon Class chooser
        weaponClassCount = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        weaponClassCount.addChangeListener(e->spinnerChange());
        weaponClassChooser = new JComboBox<>(WeaponClass.values());
        weaponClassChooser.addFocusListener(this);

        // Setup Weapons Table
        weaponsModel = new WeaponsTableModel(parentPanel);
        tblWeapons = new MegaMekTable(weaponsModel, WeaponsTableModel.COL_NAME) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                Dimension standardSize = super.getPreferredScrollableViewportSize();
                return new Dimension(standardSize.width, getRowHeight() * 6);
            }
        };
        TableColumn wpsCol = tblWeapons.getColumnModel().getColumn(WeaponsTableModel.COL_QTY);
        wpsCol.setCellEditor(new DefaultCellEditor(cboQty));
        tblWeapons.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        weaponsSorter = new TableRowSorter<>(weaponsModel);
        tblWeapons.setRowSorter(weaponsSorter);
        tblWeapons.addKeyListener(this);
        tblWeapons.addFocusListener(this);

        var tableDataRenderer = new EquipmentDataRenderer();
        for (int column : List.of(0, 2, 3, 4, 5, 6)) {
            tblWeapons.getColumnModel().getColumn(column).setCellRenderer(tableDataRenderer);
        }

        var techBaseRenderer = new TechBaseRenderer();
        tblWeapons.getColumnModel().getColumn(7).setCellRenderer(techBaseRenderer);

        for (int i = 0; i < weaponsModel.getColumnCount(); i++) {
            tblWeapons.getColumnModel().getColumn(i).setPreferredWidth(weaponsModel.getPreferredWidth(i));
        }

        scrTableWeapons.setViewportView(tblWeapons);

        // Setup Equipment Table
        equipmentModel = new EquipmentTableModel(parentPanel);
        tblEquipment = new MegaMekTable(equipmentModel, EquipmentTableModel.COL_NAME) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                Dimension standardSize = super.getPreferredScrollableViewportSize();
                return new Dimension(standardSize.width, getRowHeight() * 6);
            }
        };
        TableColumn eqCol = tblEquipment.getColumnModel().getColumn(EquipmentTableModel.COL_QTY);
        eqCol.setCellEditor(new DefaultCellEditor(cboQty));
        tblEquipment.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        equipmentSorter = new TableRowSorter<>(equipmentModel);
        tblEquipment.setRowSorter(equipmentSorter);
        tblEquipment.addKeyListener(this);
        tblEquipment.addFocusListener(this);

        for (int i = 0; i < equipmentModel.getColumnCount(); i++) {
            tblEquipment.getColumnModel().getColumn(i).setPreferredWidth(equipmentModel.getPreferredWidth(i));
        }

        tblEquipment.getColumnModel().getColumn(0).setCellRenderer(tableDataRenderer);
        var costRenderer = new EquipmentCostRenderer();
        tblEquipment.getColumnModel().getColumn(2).setCellRenderer(costRenderer);
        tblEquipment.getColumnModel().getColumn(3).setCellRenderer(techBaseRenderer);

        scrTableEquipment.setViewportView(tblEquipment);

        // Populate Tables
        populateWeaponsAndEquipmentChoices();

        // initialize with the weapons sorted alphabetically by name
        ArrayList<RowSorter.SortKey> sortlist = new ArrayList<>();
        sortlist.add(new RowSorter.SortKey(WeaponsTableModel.COL_NAME, SortOrder.ASCENDING));
        tblWeapons.getRowSorter().setSortKeys(sortlist);
        ((DefaultRowSorter<?, ?>) tblWeapons.getRowSorter()).sort();
        tblWeapons.invalidate(); // force re-layout of window

        // initialize with the equipment sorted alphabetically by chassis
        sortlist = new ArrayList<>();
        sortlist.add(new RowSorter.SortKey(EquipmentTableModel.COL_NAME, SortOrder.ASCENDING));
        tblEquipment.getRowSorter().setSortKeys(sortlist);
        ((DefaultRowSorter<?, ?>) tblEquipment.getRowSorter()).sort();
        tblEquipment.invalidate(); // force re-layout of window

        txtWEEqExp.setEditable(false);
        txtWEEqExp.setLineWrap(true);
        txtWEEqExp.setWrapStyleWord(true);

        tableFilterText.getDocument().addDocumentListener(this);

        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy = 0;
        upperPanel.add(lblTableFilters, gbc);
        gbc.gridy++;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel tableTechFilterPanel = new JPanel();
        tableTechFilterPanel.add(lblUnitType);
        tableTechFilterPanel.add(cboUnitType);
        tableTechFilterPanel.add(lblTechClass);
        tableTechFilterPanel.add(cboTechClass);
        tableTechFilterPanel.add(lblTechLevelBase);
        tableTechFilterPanel.add(cboTechLevel);
        upperPanel.add(tableTechFilterPanel, gbc);

        gbc.gridy++;
        JPanel tableTextFilterPanel = new JPanel();
        tableTextFilterPanel.add(tableFilterTextLabel);
        tableTextFilterPanel.add(tableFilterText);
        upperPanel.add(tableTextFilterPanel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        upperPanel.add(lblWeapons, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = 5;
        gbc.gridy++;
        upperPanel.add(scrTableWeapons, gbc);

        gbc.gridy++;
        upperPanel.add(Box.createVerticalStrut(20), gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        upperPanel.add(lblEquipment, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = 5;
        gbc.gridy++;
        upperPanel.add(scrTableEquipment, gbc);

        gbc.gridy++;
        upperPanel.add(Box.createVerticalStrut(20), gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 20);
        upperPanel.add(lblWeaponClass, gbc);
        gbc.gridy++;
        upperPanel.add(weaponClassCount, gbc);
        upperPanel.add(weaponClassChooser, gbc);

        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAdd);
        btnPanel.add(btnLeftParen);
        btnPanel.add(btnRightParen);
        btnPanel.add(btnAnd);
        btnPanel.add(btnOr);
        btnPanel.add(btnBack);
        btnPanel.add(btnClear);

        Box filterExpressionPanel = Box.createHorizontalBox();
        filterExpressionPanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        filterExpressionPanel.add(lblWEEqExpTxt);
        filterExpressionPanel.add(Box.createHorizontalStrut(20));
        filterExpressionPanel.add(expWEScroller);

        Box filterAssemblyPanel = Box.createVerticalBox();
        filterAssemblyPanel.add(Box.createVerticalStrut(10));
        filterAssemblyPanel.add(btnPanel);
        filterAssemblyPanel.add(filterExpressionPanel);
        filterAssemblyPanel.add(Box.createVerticalStrut(10));

        setLayout(new BorderLayout());
        add(new TWAdvancedSearchPanel.StandardScrollPane(upperPanel), BorderLayout.CENTER);
        add(filterAssemblyPanel, BorderLayout.PAGE_END);
    }

    void filterTables() {
        RowFilter<WeaponsTableModel, Integer> weaponFilter;
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
                    String currTechClass = TechConstants.getTechName(wp.getTechLevel(parentPanel.gameYear));

                    boolean techLvlMatch = matchTechLvl(techLevel, wp.getTechLevel(parentPanel.gameYear));
                    boolean techClassMatch = matchTechClass(techClass, currTechClass);
                    boolean unitTypeMatch = matchUnitType(unitType, wp);
                    boolean textFilterMatch = (tableFilterText.getText() == null) || (tableFilterText.getText().length() < 2)
                        || matchWeaponTextFilter(entry, WeaponsTableModel.COL_NAME);
                    return techLvlMatch && techClassMatch && unitTypeMatch && textFilterMatch;
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
                    EquipmentType eq = eqModel.getEquipmentTypeAt(entry.getIdentifier());
                    String currTechClass = TechConstants.getTechName(eq.getTechLevel(parentPanel.gameYear));
                    boolean techLvlMatch = matchTechLvl(techLevel, eq.getTechLevel(parentPanel.gameYear));
                    boolean techClassMatch = matchTechClass(techClass, currTechClass);
                    boolean unitTypeMatch = matchUnitType(unitType, eq);
                    boolean textFilterMatch = (tableFilterText.getText() == null) || (tableFilterText.getText().length() < 2)
                        || matchEquipmentTextFilter(entry, EquipmentTableModel.COL_NAME);
                    return techLvlMatch && techClassMatch && unitTypeMatch && textFilterMatch;
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
     * Creates collections for all the possible <code>WeaponType</code>s and
     * <code>EquipmentType</code>s. These are used to populate the weapons
     * and equipment tables.
     */
    private void populateWeaponsAndEquipmentChoices() {
        List<WeaponType> weapons = new ArrayList<>();
        List<EquipmentType> equipment = new ArrayList<>();

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
                if (eq.hasFlag(WeaponType.F_AERO_WEAPON) || eq.hasFlag(MiscType.F_FIGHTER_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.BATTLE_ARMOR:
                if (eq.hasFlag(WeaponType.F_BA_WEAPON) || eq.hasFlag(MiscType.F_BA_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.INFANTRY:
                if (eq.hasFlag(WeaponType.F_INFANTRY)) {
                    return true;
                }
                break;
            case UnitType.MEK:
                if (eq.hasFlag(WeaponType.F_MEK_WEAPON) || eq.hasFlag(MiscType.F_MEK_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.TANK:
                if (eq.hasFlag(WeaponType.F_TANK_WEAPON) || eq.hasFlag(MiscType.F_TANK_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.PROTOMEK:
                if (eq.hasFlag(WeaponType.F_PROTO_WEAPON) || eq.hasFlag(MiscType.F_PROTOMEK_EQUIPMENT)) {
                    return true;
                }
                break;
            default:
                return false;
        }
        return false;
    }

    // Build the string representation of the new expression
    String filterExpressionString() {
        StringBuilder filterExp = new StringBuilder();
        for (FilterToken filterTok : filterTokens) {
            filterExp.append(" ").append(filterTok.toString()).append(" ");
        }
        return filterExp.toString();
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

    private void addButtonPressed() {
        if ((focusedSelector == tblEquipment) && (tblEquipment.getSelectedRow() != -1)) {
            int row = tblEquipment.getSelectedRow();
            String internalName = (String) tblEquipment.getModel().getValueAt(
                tblEquipment.convertRowIndexToModel(row),
                EquipmentTableModel.COL_INTERNAL_NAME);
            String fullName = (String) tblEquipment.getValueAt(row, EquipmentTableModel.COL_NAME);
            int qty = Integer.parseInt((String) tblEquipment.getValueAt(row, EquipmentTableModel.COL_QTY));
            filterTokens.add(new EquipmentTypeFT(internalName, fullName, qty));

        } else if ((focusedSelector == tblWeapons) && (tblWeapons.getSelectedRow() != -1)) {
            int row = tblWeapons.getSelectedRow();
            String internalName = (String) tblWeapons.getModel().getValueAt(
                tblWeapons.convertRowIndexToModel(row),
                WeaponsTableModel.COL_INTERNAL_NAME);
            String fullName = (String) tblWeapons.getValueAt(row, WeaponsTableModel.COL_NAME);
            int qty = Integer.parseInt((String) tblWeapons.getValueAt(row, WeaponsTableModel.COL_QTY));
            filterTokens.add(new EquipmentTypeFT(internalName, fullName, qty));

        } else if ((focusedSelector == weaponClassChooser) && (weaponClassChooser.getSelectedItem() != null)) {
            int qty = (int) weaponClassCount.getValue();
            filterTokens.add(new WeaponClassFT((WeaponClass) weaponClassChooser.getSelectedItem(), qty));

        } else {
            // if something else is focused, do nothing
            return;
        }
        txtWEEqExp.setText(filterExpressionString());
        adaptTokenButtons();
    }

    private boolean matchWeaponTextFilter(RowFilter.Entry<? extends WeaponsTableModel, ? extends Integer> entry, int column) {
        String wp = entry.getModel().getValueAt(entry.getIdentifier(), column).toString();
        return matchTextFilter(wp);
    }

    private boolean matchEquipmentTextFilter(RowFilter.Entry<? extends EquipmentTableModel, ? extends Integer> entry, int column) {
        String wp = entry.getModel().getValueAt(entry.getIdentifier(), column).toString();
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

    private void spinnerChange() {
        focusWeaponClasschooser();
    }

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
        return (focusedSelector == weaponClassChooser) || (focusedSelector == tblEquipment) || (focusedSelector == tblWeapons);
    }

    void adaptTokenButtons() {
        btnBack.setEnabled(!filterTokens.isEmpty());
        btnClear.setEnabled(!filterTokens.isEmpty());

        boolean canAddEquipment = filterTokens.isEmpty() || (lastToken() instanceof OperatorFT)
            || (lastToken() instanceof LeftParensFilterToken);
        btnAdd.setEnabled(hasFocusedSelector() && canAddEquipment);
        btnLeftParen.setEnabled(canAddEquipment);

        boolean canAddOperator = (lastToken() instanceof EquipmentFilterToken) || (lastToken() instanceof RightParensFilterToken);
        btnAnd.setEnabled(canAddOperator);
        btnOr.setEnabled(canAddOperator);
        btnRightParen.setEnabled(canAddOperator);
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
}
