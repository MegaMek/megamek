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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;
import java.util.regex.PatternSyntaxException;

class WeaponSearchTab extends JPanel implements KeyListener, DocumentListener, FocusListener {

    final List<FilterToken> filterTokens = new ArrayList<>();

    final JButton btnLeftParen = new JButton("(");
    final JButton btnRightParen = new JButton(")");
    final JToggleButton btnLessThan = new JToggleButton("<");
    final JToggleButton btnAtLeast = new JToggleButton("\u2265");
    final JButton btnAdd = new JButton(Messages.getString("MekSelectorDialog.Search.add"));
    final JButton btnAddMultiOr = new JButton("Add [OR]");
    final JButton btnAddMultiAnd = new JButton("Add [AND]");
    final JButton btnAnd = new JButton(Messages.getString("MekSelectorDialog.Search.and"));
    final JButton btnOr = new JButton(Messages.getString("MekSelectorDialog.Search.or"));
    final JButton btnClear = new JButton(Messages.getString("MekSelectorDialog.Reset"));
    final JButton btnBack = new JButton("Back");
    final JLabel lblWEEqExpTxt = new JLabel(Messages.getString("MekSelectorDialog.Search.FilterExpression"));
    final JTextArea txtWEEqExp = new JTextArea("", 2, 40);
    final JScrollPane expWEScroller = new JScrollPane(txtWEEqExp,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    final JLabel lblUnitType = new JLabel(Messages.getString("MekSelectorDialog.Search.UnitType"));
    final JLabel lblTechClass = new JLabel(Messages.getString("MekSelectorDialog.Search.TechClass"));
    final JLabel lblTechLevelBase = new JLabel(Messages.getString("MekSelectorDialog.Search.TechLevel"));
    final JList<String> unitTypeSelector = new JList<>();
    final JList<String> techClassSelector = new JList<>();
    final JList<String> techLevelSelector = new JList<>();
    final JLabel tableFilterTextLabel = new JLabel(Messages.getString("MekSelectorDialog.Search.TableFilter"));
    final JTextField tableFilterText = new JTextField(10);

    final JLabel lblWeapons = new JLabel(Messages.getString("MekSelectorDialog.Search.Weapons"));
    final JScrollPane scrTableWeapons = new JScrollPane();
    final SearchableTable tblWeapons;
    final WeaponsTableModel weaponsModel;
    final TableRowSorter<WeaponsTableModel> weaponsSorter;
    final JLabel lblEquipment = new JLabel(Messages.getString("MekSelectorDialog.Search.Equipment"));
    final JScrollPane scrTableEquipment = new JScrollPane();
    final SearchableTable tblEquipment;
    final EquipmentTableModel equipmentModel;
    final TableRowSorter<EquipmentTableModel> equipmentSorter;

    final JLabel lblWeaponClass = new JLabel(Messages.getString("MekSelectorDialog.Search.WeaponClass"));
    final JSpinner weaponClassCount;
    final JComboBox<WeaponClass> weaponClassChooser;

    JComponent focusedSelector = null;

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

        unitTypeSelector.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        unitTypeSelector.setVisibleRowCount(1);
        var model = new DefaultListModel<String>();
        model.addElement("All");
        model.addElement(UnitType.getTypeDisplayableName(UnitType.MEK));
        model.addElement(UnitType.getTypeDisplayableName(UnitType.TANK));
        model.addElement(UnitType.getTypeDisplayableName(UnitType.BATTLE_ARMOR));
        model.addElement(UnitType.getTypeDisplayableName(UnitType.INFANTRY));
        model.addElement(UnitType.getTypeDisplayableName(UnitType.PROTOMEK));
        model.addElement(UnitType.getTypeDisplayableName(UnitType.AERO));
        unitTypeSelector.setModel(model);
        unitTypeSelector.setSelectedIndices(new int[]{0, 1, 2, 3, 4, 5});
        unitTypeSelector.addListSelectionListener(e -> filterTables());
        unitTypeSelector.setCellRenderer(new ChoiceRenderer());

        techLevelSelector.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        techLevelSelector.setVisibleRowCount(1);
        techLevelSelector.setListData(TechConstants.T_SIMPLE_NAMES);
        techLevelSelector.setSelectedIndices(new int[]{0, 1, 2, 3}); // all except unofficial as the default selection
        techLevelSelector.addListSelectionListener(e -> filterTables());
        techLevelSelector.setCellRenderer(new ChoiceRenderer());

        techClassSelector.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        techClassSelector.setVisibleRowCount(1);
        techClassSelector.setListData(new String[]{"Inner Sphere", "Clan"});
        techClassSelector.setSelectedIndices(new int[]{0, 1}); // all except unofficial as the default selection
        techClassSelector.addListSelectionListener(e -> filterTables());
        techClassSelector.setCellRenderer(new ChoiceRenderer());

        // Set up Weapon Class chooser
        weaponClassCount = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        weaponClassChooser = new JComboBox<>(WeaponClass.values());
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
        for (int column : List.of(1, 2, 3, 4, 5)) {
            tblWeapons.getColumnModel().getColumn(column).setCellRenderer(tableDataRenderer);
        }

        var techBaseRenderer = new TechBaseRenderer();
        tblWeapons.getColumnModel().getColumn(6).setCellRenderer(techBaseRenderer);

        for (int i = 0; i < weaponsModel.getColumnCount(); i++) {
            tblWeapons.getColumnModel().getColumn(i).setPreferredWidth(weaponsModel.getPreferredWidth(i));
        }

        scrTableWeapons.setViewportView(tblWeapons);

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

       var costRenderer = new EquipmentCostRenderer();
        tblEquipment.getColumnModel().getColumn(1).setCellRenderer(costRenderer);
        tblEquipment.getColumnModel().getColumn(2).setCellRenderer(techBaseRenderer);

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

        JPanel upperPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        upperPanel.add(lblTechClass, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        upperPanel.add(techClassSelector, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        upperPanel.add(lblUnitType, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        upperPanel.add(unitTypeSelector, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        upperPanel.add(lblTechLevelBase, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        upperPanel.add(techLevelSelector, gbc);

        JPanel filterPanel = new JPanel();
        filterPanel.add(tableFilterTextLabel);
        filterPanel.add(tableFilterText);
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        upperPanel.add(filterPanel, gbc);

        gbc.gridheight = 1;
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        upperPanel.add(lblWeapons, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
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
        gbc.gridwidth = GridBagConstraints.REMAINDER;
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
        JPanel weaponClassPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        weaponClassPanel.add(weaponClassChooser);
        upperPanel.add(weaponClassPanel, gbc);

        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAtLeast);
        btnPanel.add(btnLessThan);
        btnPanel.add(weaponClassCount);
        btnPanel.add(btnAdd);
        btnPanel.add(btnAddMultiAnd);
        btnPanel.add(btnAddMultiOr);
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
        final int[] techLevels = techLevelSelector.getSelectedIndices();
        final int[] techClass = techClassSelector.getSelectedIndices();
        final int[] unitTypes = unitTypeSelector.getSelectedIndices();
        // If current expression doesn't parse, don't update.
        try {
            weaponFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends WeaponsTableModel, ? extends Integer> entry) {
                    WeaponsTableModel weapModel = entry.getModel();
                    WeaponType wp = weapModel.getWeaponTypeAt(entry.getIdentifier());
                    String weaponTechClass = TechConstants.getTechName(wp.getTechLevel(parentPanel.gameYear));
                    boolean techLvlMatch = matchTechLvl(techLevels, wp.getTechLevel(parentPanel.gameYear));
                    boolean techClassMatch = matchTechClass(techClass, weaponTechClass);
                    boolean unitTypeMatch = matchUnitTypeToWeapon(unitTypes, wp);
                    boolean textFilterMatch = (tableFilterText.getText() == null) || (tableFilterText.getText().length() < 2)
                        || matchWeaponTextFilter(entry);
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
                    MiscType eq = eqModel.getEquipmentTypeAt(entry.getIdentifier());
                    String currTechClass = TechConstants.getTechName(eq.getTechLevel(parentPanel.gameYear));
                    boolean techLvlMatch = matchTechLvl(techLevels, eq.getTechLevel(parentPanel.gameYear));
                    boolean techClassMatch = matchTechClass(techClass, currTechClass);
                    boolean unitTypeMatch = matchUnitTypeToMisc(unitTypes, eq);
                    boolean textFilterMatch = (tableFilterText.getText() == null) || (tableFilterText.getText().length() < 2)
                        || matchEquipmentTextFilter(entry);
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

    private boolean matchTechClass(int[] selectedClasses, String t2) {
        if (t2.equals("IS/Clan") || (selectedClasses.length != 1)) {
            return true;
        } else {
            if (selectedClasses[0] == 0) {
                return t2.equals("Inner Sphere");
            } else {
                return t2.equals("Clan");
            }
        }
    }

    private boolean matchUnitTypeToWeapon(int[] selectedUnitTypes, EquipmentType eq) {
        // All or nothing is selected
        if (selectedUnitTypes.length == 0 || selectedUnitTypes.length >= 5) {
            return true;
        } else {
            List<Integer> selection = Arrays.stream(selectedUnitTypes).boxed().toList();
            // 5 is the index, not the unit type constant!
            return (selection.contains(5) && eq.hasFlag(WeaponType.F_AERO_WEAPON))
                || (selection.contains(UnitType.BATTLE_ARMOR) && eq.hasFlag(WeaponType.F_BA_WEAPON))
                || (selection.contains(UnitType.INFANTRY) && eq.hasFlag(WeaponType.F_INFANTRY))
                || (selection.contains(UnitType.MEK) && eq.hasFlag(WeaponType.F_MEK_WEAPON))
                || (selection.contains(UnitType.TANK) && eq.hasFlag(WeaponType.F_TANK_WEAPON))
                || (selection.contains(UnitType.PROTOMEK) && eq.hasFlag(WeaponType.F_PROTO_WEAPON));
        }
    }

    private boolean matchUnitTypeToMisc(int[] selectedUnitTypes, MiscType eq) {
        // All or nothing is selected
        if (selectedUnitTypes.length == 0 || selectedUnitTypes.length >= 5) {
            return true;
        } else {
            List<Integer> selection = Arrays.stream(selectedUnitTypes).boxed().toList();
            // 5 is the index, not the unit type constant!
            return (selection.contains(5) && eq.hasFlag(MiscType.F_FIGHTER_EQUIPMENT))
                || (selection.contains(UnitType.BATTLE_ARMOR) && eq.hasFlag(MiscType.F_BA_EQUIPMENT))
                || (selection.contains(UnitType.INFANTRY) && eq.hasFlag(MiscType.F_INF_EQUIPMENT))
                || (selection.contains(UnitType.MEK) && eq.hasFlag(MiscType.F_MEK_EQUIPMENT))
                || (selection.contains(UnitType.TANK) && eq.hasFlag(MiscType.F_TANK_EQUIPMENT))
                || (selection.contains(UnitType.PROTOMEK) && eq.hasFlag(MiscType.F_PROTOMEK_EQUIPMENT));
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

    private boolean matchTechLvl(int[] selectedTechLevels, int equipmentTechLevel) {
        int simpleEquipmentLevel = TechConstants.convertFromNormalToSimple(equipmentTechLevel);
        return Arrays.stream(selectedTechLevels).anyMatch(selectedLevel -> selectedLevel == simpleEquipmentLevel);
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
        int qty = (int) weaponClassCount.getValue();
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
            filterTokens.add(new WeaponClassFT((WeaponClass) weaponClassChooser.getSelectedItem(), qty, atleast));

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
        return (focusedSelector == weaponClassChooser) || (focusedSelector == tblEquipment) || (focusedSelector == tblWeapons);
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

    private static class ChoiceRenderer extends DefaultListCellRenderer {

        public ChoiceRenderer() {
            paddingPanel.add(this);
            paddingPanel.setBorder(new EmptyBorder(0, 2, 0, 2));
        }

        JPanel paddingPanel = new JPanel(new GridLayout(1, 1));


        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, false);
            return paddingPanel;
        }
    }
}
