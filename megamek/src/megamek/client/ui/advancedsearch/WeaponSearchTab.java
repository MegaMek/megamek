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

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.table.MegaMekTable;
import megamek.common.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class WeaponSearchTab extends JPanel implements KeyListener, ListSelectionListener {

    final List<FilterTokens> filterToks = new ArrayList<>();

    // Weapons / Equipment
    JButton btnWELeftParen = new JButton("(");
    JButton btnWERightParen = new JButton(")");
    JButton btnWEAdd = new JButton(Messages.getString("MekSelectorDialog.Search.add"));
    JButton btnWEAnd = new JButton(Messages.getString("MekSelectorDialog.Search.and"));
    JButton btnWEOr = new JButton(Messages.getString("MekSelectorDialog.Search.or"));
    JButton btnWEClear = new JButton(Messages.getString("MekSelectorDialog.Reset"));
    JButton btnWEBack = new JButton("Back");
    JLabel  lblWEEqExpTxt = new JLabel(Messages.getString("MekSelectorDialog.Search.FilterExpression"));
    JTextArea  txtWEEqExp = new JTextArea("");
    JScrollPane expWEScroller = new JScrollPane(txtWEEqExp,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    JLabel lblUnitType = new JLabel(Messages.getString("MekSelectorDialog.Search.UnitType"));
    JLabel lblTechClass = new JLabel(Messages.getString("MekSelectorDialog.Search.TechClass"));
    JLabel lblTechLevelBase = new JLabel(Messages.getString("MekSelectorDialog.Search.TechLevel"));
    JComboBox<String> cboUnitType = new JComboBox<>();
    JComboBox<String> cboTechClass = new JComboBox<>();
    JComboBox<String> cboTechLevel = new JComboBox<>();
    JLabel lblWeaponClass = new JLabel(Messages.getString("MekSelectorDialog.Search.WeaponClass"));
    JScrollPane scrTableWeaponType = new JScrollPane();
    MegaMekTable tblWeaponType;
    WeaponClassTableModel weaponTypesModel;
    TableRowSorter<WeaponClassTableModel> weaponTypesSorter;
    JLabel lblWeapons = new JLabel(Messages.getString("MekSelectorDialog.Search.Weapons"));
    JScrollPane scrTableWeapons = new JScrollPane();
    MegaMekTable tblWeapons;
    WeaponsTableModel weaponsModel;
    TableRowSorter<WeaponsTableModel> weaponsSorter;
    JLabel lblEquipment = new JLabel(Messages.getString("MekSelectorDialog.Search.Equipment"));
    JScrollPane scrTableEquipment = new JScrollPane();
    MegaMekTable tblEquipment;
    EquipmentTableModel equipmentModel;
    TableRowSorter<EquipmentTableModel> equipmentSorter;
    JComboBox<String> cboQty = new JComboBox<>();

    private final TWAdvancedSearchPanel parentPanel;

    WeaponSearchTab(TWAdvancedSearchPanel parentPanel) {
        this.parentPanel = parentPanel;
        // Initialize Items
        btnWEAnd.addActionListener(parentPanel);
        btnWEAdd.addActionListener(parentPanel);
        btnWELeftParen.addActionListener(parentPanel);
        btnWERightParen.addActionListener(parentPanel);
        btnWEOr.addActionListener(parentPanel);
        btnWEClear.addActionListener(parentPanel);
        btnWEBack.addActionListener(parentPanel);

        btnWEBack.setEnabled(false);
        btnWEAdd.setEnabled(false);

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
        cboUnitType.addActionListener(parentPanel);

        DefaultComboBoxModel<String> techLevelModel = new DefaultComboBoxModel<>();

        for (int i = 0; i < TechConstants.SIZE; i++) {
            techLevelModel.addElement(TechConstants.getLevelDisplayableName(i));
        }

        techLevelModel.setSelectedItem(TechConstants.getLevelDisplayableName(TechConstants.SIZE - 1));
        cboTechLevel.setModel(techLevelModel);
        cboTechLevel.addActionListener(parentPanel);

        DefaultComboBoxModel<String> techClassModel = new DefaultComboBoxModel<>();
        techClassModel.addElement("All");
        techClassModel.addElement("Inner Sphere");
        techClassModel.addElement("Clan");
        techClassModel.addElement("IS/Clan");
        techClassModel.addElement("(Unknown Technology Base)");
        techClassModel.setSelectedItem("All");
        cboTechClass.setModel(techClassModel);
        cboTechClass.addActionListener(parentPanel);

        // Set up Weapon Class table
        weaponTypesModel = new WeaponClassTableModel();
        tblWeaponType = new MegaMekTable(weaponTypesModel, WeaponClassTableModel.COL_NAME);
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
        weaponsModel = new WeaponsTableModel(parentPanel);
        tblWeapons = new MegaMekTable(weaponsModel, WeaponsTableModel.COL_NAME);
        TableColumn wpsCol = tblWeapons.getColumnModel().getColumn(WeaponsTableModel.COL_QTY);
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
        equipmentModel = new EquipmentTableModel(parentPanel);
        tblEquipment = new MegaMekTable(equipmentModel, EquipmentTableModel.COL_NAME);
        TableColumn eqCol = tblEquipment.getColumnModel().getColumn(EquipmentTableModel.COL_QTY);
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

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        add(lblTableFilters, c);
        c.gridx = 0; c.gridy++;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 40, 0, 0);
        JPanel cboPanel = new JPanel();
        cboPanel.add(lblUnitType);
        cboPanel.add(cboUnitType);
        cboPanel.add(lblTechClass);
        cboPanel.add(cboTechClass);
        cboPanel.add(lblTechLevelBase, c);
        cboPanel.add(cboTechLevel, c);
        add(cboPanel, c);
        c.gridwidth = 1;

        c.gridx = 0; c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 0);
        add(lblWeaponClass, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 5;
        c.gridx = 0; c.gridy++;
        add(scrTableWeaponType, c);
        c.gridwidth = 1;

        c.fill = GridBagConstraints.NONE;
        c.gridx = 0; c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        add(lblWeapons, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 5;
        c.gridx = 0; c.gridy++;
        add(scrTableWeapons, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.gridx = 0; c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        add(lblEquipment, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 5;
        c.gridx = 0; c.gridy++;
        add(scrTableEquipment, c);

        c.insets = new Insets(0, 50, 0, 0);
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0; c.gridy++;
        c.gridwidth = 4;
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnWEAdd, c);
        btnPanel.add(btnWELeftParen, c);
        btnPanel.add(btnWERightParen, c);
        btnPanel.add(btnWEAnd, c);
        btnPanel.add(btnWEOr, c);
        btnPanel.add(btnWEBack, c);
        btnPanel.add(btnWEClear, c);
        add(btnPanel, c);

        // Filter Expression
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        add(lblWEEqExpTxt, c);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridwidth = 3;
        add(expWEScroller, c);
        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        add(blankPanel, c);
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
                    String currTechClass = TechConstants.getTechName(wp.getTechLevel(parentPanel.gameYear));

                    boolean techLvlMatch = matchTechLvl(techLevel, wp.getTechLevel(parentPanel.gameYear));
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
                    String currTechClass = TechConstants.getTechName(eq.getTechLevel(parentPanel.gameYear));
                    boolean techLvlMatch = matchTechLvl(techLevel, eq.getTechLevel(parentPanel.gameYear));
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


    void clearWeaponsEquipment() {
        filterToks.clear();
        tblWeapons.clearSelection();
        tblEquipment.clearSelection();
        txtWEEqExp.setText("");
        btnWEBack.setEnabled(false);
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


    @Override
    public void keyPressed(KeyEvent evt) { }

    @Override
    public void keyReleased(KeyEvent evt) { }

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
                if (eq.hasFlag(WeaponType.F_MEK_WEAPON)
                    || eq.hasFlag(MiscType.F_MEK_EQUIPMENT)) {
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
                    || eq.hasFlag(MiscType.F_PROTOMEK_EQUIPMENT)) {
                    return true;
                }
                break;
            default:
                return false;
        }
        return false;
    }

    @Override
    public void valueChanged(ListSelectionEvent evt) {
        boolean lastTokIsOperation;
        int tokSize = filterToks.size();
        lastTokIsOperation = ((tokSize == 0) ||
            (filterToks.get(tokSize - 1) instanceof OperationFT));
        if (evt.getSource().equals(tblWeapons.getSelectionModel())) {
            if ((tblWeapons.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblEquipment.clearSelection();
                tblWeaponType.clearSelection();
                btnWEAdd.setEnabled(true);
            } else if (tblWeapons.getSelectedRow() >= 0) {
                tblEquipment.clearSelection();
                tblWeaponType.clearSelection();
            }
        } else if (evt.getSource().equals(tblEquipment.getSelectionModel())) {
            if ((tblEquipment.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblWeapons.clearSelection();
                tblWeaponType.clearSelection();
                btnWEAdd.setEnabled(true);
            } else if (tblEquipment.getSelectedRow() >= 0) {
                tblWeapons.clearSelection();
                tblWeaponType.clearSelection();
            }
        } else if (evt.getSource().equals(tblWeaponType.getSelectionModel())) {
            if ((tblWeaponType.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblWeapons.clearSelection();
                tblEquipment.clearSelection();
                btnWEAdd.setEnabled(true);
            } else if (tblWeaponType.getSelectedRow() >= 0) {
                tblWeapons.clearSelection();
                tblEquipment.clearSelection();
            }
        }
    }

    String filterExpressionString() {
        // Build the string representation of the new expression
        StringBuilder filterExp = new StringBuilder();
        for (int i = 0; i < filterToks.size(); i++) {
            filterExp.append(" ").append(filterToks.get(i).toString()).append(" ");
        }
        return filterExp.toString();
    }

    /**
     * Convenience method for enabling the buttons related to weapon/equipment
     * selection for filtering (btnAddEquipment, btnAddWeapon, etc)
     */
    void enableSelectionButtons() {
        if ((tblWeapons.getSelectedRow() != -1) ||
            (tblEquipment.getSelectedRow() != -1) ||
            (tblWeaponType.getSelectedRow() != -1)) {
            btnWEAdd.setEnabled(true);
        }
        btnWELeftParen.setEnabled(true);
    }

    /**
     * Convenience method for disabling the buttons related to weapon/equipment
     * selection for filtering (btnAddEquipment, btnAddWeapon, etc)
     */
    void disableSelectionButtons() {
        btnWEAdd.setEnabled(false);
        btnWELeftParen.setEnabled(false);
    }

    /**
     * Convenience method for enabling the buttons related to filter operations
     * for filtering (btnAnd, btnOr, etc)
     */
    void enableOperationButtons() {
        btnWEOr.setEnabled(true);
        btnWEAnd.setEnabled(true);
        btnWERightParen.setEnabled(true);
    }

    /**
     * Convenience method for disabling the buttons related to filter operations
     * for filtering (btnAnd, btnOr, etc)
     */
    void disableOperationButtons() {
        btnWEOr.setEnabled(false);
        btnWEAnd.setEnabled(false);
        btnWERightParen.setEnabled(false);
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
}
