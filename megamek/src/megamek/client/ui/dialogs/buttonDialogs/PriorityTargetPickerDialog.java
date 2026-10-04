/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs.buttonDialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Lets the player pick a bot's priority target units from a list of the units in the game, grouped by owner and by
 * force, instead of typing unit IDs. Each picked unit gets a priority from 1 (the most wanted) to 5. Ticking a force
 * picks the units in it now; units added to the force later are not included.
 */
public class PriorityTargetPickerDialog extends AbstractButtonDialog {

    private static final int PICK_COLUMN = 0;
    private static final int PRIORITY_COLUMN = 1;
    private static final int UNIT_COLUMN = 2;
    private static final Integer[] PRIORITY_CHOICES = { 1, 2, 3, 4, 5 };

    private final Game game;
    /** The bot the targets are for; null for a bot that has no player yet. */
    private final Player botPlayer;
    /** The picked units and their priorities, kept in the order they were picked. */
    private final Map<Integer, Integer> pickedPriorities = new LinkedHashMap<>();
    /** Every unit that can be picked, sorted by owner, then force, then ID. */
    private final List<Entity> candidateUnits = new ArrayList<>();
    private final List<PickerRow> shownRows = new ArrayList<>();

    private final JTextField searchField = new JTextField(20);
    private final JCheckBox enemiesOnlyCheck = new JCheckBox(Messages.getString("PriorityTargetPicker.enemiesOnly"));
    private final JLabel pickedCountLabel = new JLabel();
    private final PickerTableModel tableModel = new PickerTableModel();
    private final JTable targetTable = new JTable(tableModel);

    /** The kind of a row in the picker: a header for an owner, a force (or the units in no force), or a unit. */
    private enum RowKind {
        PLAYER, FORCE, UNIT
    }

    /**
     * One row of the picker. A force row lists the shown units below it, so ticking it ticks exactly what the player
     * sees.
     */
    private record PickerRow(RowKind kind, String label, @Nullable Entity unit, List<Entity> forceUnits) {}

    /**
     * @param frame           the parent frame
     * @param game            the game whose units are listed
     * @param botPlayer       the bot the targets are for, or {@code null} for a bot that has no player yet; without
     *                        it every unit is listed, since enemies cannot be told apart
     * @param currentTargets  the bot's current priority targets, unit ID to priority; they start ticked
     */
    public PriorityTargetPickerDialog(JFrame frame, Game game, @Nullable Player botPlayer,
          Map<Integer, Integer> currentTargets) {
        super(frame, "PriorityTargetPickerDialog", "PriorityTargetPicker.title");
        this.game = game;
        this.botPlayer = botPlayer;
        pickedPriorities.putAll(currentTargets);
        collectCandidateUnits();
        initialize();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                searchField.requestFocusInWindow();
            }
        });
    }

    /** @return the picked units and their priorities, unit ID to priority, 1 the most wanted */
    public Map<Integer, Integer> getPickedTargets() {
        return new LinkedHashMap<>(pickedPriorities);
    }

    @Override
    protected Container createCenterPane() {
        int gap = UIUtil.scaleForGUI(8);
        JPanel result = new JPanel(new BorderLayout(gap, gap));
        result.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        JLabel searchLabel = new JLabel(Messages.getString("PriorityTargetPicker.search"));
        searchLabel.setLabelFor(searchField);
        searchField.setToolTipText(Messages.getString("PriorityTargetPicker.searchTip"));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshRows();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshRows();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshRows();
            }
        });
        enemiesOnlyCheck.setToolTipText(Messages.getString("PriorityTargetPicker.enemiesOnlyTip"));
        enemiesOnlyCheck.setSelected(botPlayer != null);
        enemiesOnlyCheck.setEnabled(botPlayer != null);
        enemiesOnlyCheck.addActionListener(event -> refreshRows());
        filterPanel.add(searchLabel);
        filterPanel.add(searchField);
        filterPanel.add(enemiesOnlyCheck);

        setUpTable();
        JScrollPane tableScroller = new JScrollPane(targetTable);
        tableScroller.setPreferredSize(UIUtil.scaleForGUI(560, 340));
        tableScroller.getVerticalScrollBar().setUnitIncrement(UIUtil.scaleForGUI(16));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        JButton pickShownButton = new JButton(Messages.getString("PriorityTargetPicker.pickShown"));
        pickShownButton.setToolTipText(Messages.getString("PriorityTargetPicker.pickShownTip"));
        pickShownButton.addActionListener(event -> pickShownUnits());
        JButton clearButton = new JButton(Messages.getString("PriorityTargetPicker.clear"));
        clearButton.setToolTipText(Messages.getString("PriorityTargetPicker.clearTip"));
        clearButton.addActionListener(event -> clearPicks());
        bottomPanel.add(pickShownButton);
        bottomPanel.add(clearButton);
        bottomPanel.add(pickedCountLabel);

        result.add(filterPanel, BorderLayout.PAGE_START);
        result.add(tableScroller, BorderLayout.CENTER);
        result.add(bottomPanel, BorderLayout.PAGE_END);
        refreshRows();
        return result;
    }

    private void setUpTable() {
        targetTable.setFillsViewportHeight(true);
        targetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        targetTable.getTableHeader().setReorderingAllowed(false);
        int textHeight = targetTable.getFontMetrics(targetTable.getFont()).getHeight();
        targetTable.setRowHeight(textHeight + UIUtil.scaleForGUI(6));

        TableColumn pickColumn = targetTable.getColumnModel().getColumn(PICK_COLUMN);
        pickColumn.setPreferredWidth(UIUtil.scaleForGUI(40));
        pickColumn.setMaxWidth(UIUtil.scaleForGUI(60));
        pickColumn.setCellRenderer(new PickRenderer(targetTable.getDefaultRenderer(Boolean.class)));

        TableColumn priorityColumn = targetTable.getColumnModel().getColumn(PRIORITY_COLUMN);
        priorityColumn.setPreferredWidth(UIUtil.scaleForGUI(70));
        priorityColumn.setMaxWidth(UIUtil.scaleForGUI(90));
        priorityColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(PRIORITY_CHOICES)));
        DefaultTableCellRenderer priorityRenderer = new DefaultTableCellRenderer();
        priorityRenderer.setHorizontalAlignment(JLabel.CENTER);
        priorityColumn.setCellRenderer(priorityRenderer);

        TableColumn unitColumn = targetTable.getColumnModel().getColumn(UNIT_COLUMN);
        unitColumn.setPreferredWidth(UIUtil.scaleForGUI(440));
        unitColumn.setCellRenderer(new UnitRenderer());
    }

    /** Lists the units that can be picked: every unit in the game but the bot's own, sorted for grouping. */
    private void collectCandidateUnits() {
        Forces forces = game.getForces();
        for (Entity unit : game.getEntitiesVector()) {
            if ((botPlayer != null) && (unit.getOwnerId() == botPlayer.getId())) {
                continue;
            }
            candidateUnits.add(unit);
        }
        candidateUnits.sort(Comparator.comparing((Entity unit) -> ownerName(unit).toLowerCase(Locale.ROOT))
              .thenComparing(unit -> forceLabel(forces, unit).isEmpty())
              .thenComparing(unit -> forceLabel(forces, unit).toLowerCase(Locale.ROOT))
              .thenComparingInt(Entity::getId));
    }

    /** Rebuilds the shown rows from the search text and the enemies filter. */
    private void refreshRows() {
        shownRows.clear();
        Forces forces = game.getForces();
        String searchText = searchField.getText().strip().toLowerCase(Locale.ROOT);
        String currentOwner = null;
        String currentForce = null;
        PickerRow currentForceRow = null;
        for (Entity unit : candidateUnits) {
            if (!isShown(unit, searchText)) {
                continue;
            }
            String owner = ownerName(unit);
            String force = forceLabel(forces, unit);
            if (!owner.equals(currentOwner)) {
                shownRows.add(new PickerRow(RowKind.PLAYER, ownerHeading(unit), null, List.of()));
                currentOwner = owner;
                currentForce = null;
            }
            if (!force.equals(currentForce)) {
                String forceText = force.isEmpty() ? Messages.getString("PriorityTargetPicker.noForce") : force;
                currentForceRow = new PickerRow(RowKind.FORCE, forceText, null, new ArrayList<>());
                shownRows.add(currentForceRow);
                currentForce = force;
            }
            currentForceRow.forceUnits().add(unit);
            shownRows.add(new PickerRow(RowKind.UNIT, unitLabel(unit), unit, List.of()));
        }
        tableModel.fireTableDataChanged();
        updatePickedCount();
    }

    private boolean isShown(Entity unit, String searchText) {
        if (enemiesOnlyCheck.isSelected() && !isEnemy(unit)) {
            return false;
        }
        if (searchText.isEmpty()) {
            return true;
        }
        return unitLabel(unit).toLowerCase(Locale.ROOT).contains(searchText)
              || ownerName(unit).toLowerCase(Locale.ROOT).contains(searchText)
              || forceLabel(game.getForces(), unit).toLowerCase(Locale.ROOT).contains(searchText);
    }

    private boolean isEnemy(Entity unit) {
        if (botPlayer == null) {
            return true;
        }
        Player owner = game.getPlayer(unit.getOwnerId());
        return (owner != null) && owner.isEnemyOf(botPlayer);
    }

    private String ownerName(Entity unit) {
        Player owner = game.getPlayer(unit.getOwnerId());
        return (owner == null) ? "" : owner.getName();
    }

    private String ownerHeading(Entity unit) {
        Player owner = game.getPlayer(unit.getOwnerId());
        if (owner == null) {
            return Messages.getString("PriorityTargetPicker.unknownOwner");
        }
        String key = isEnemy(unit) && (botPlayer != null) ? "PriorityTargetPicker.enemyOwner"
              : "PriorityTargetPicker.owner";
        return Messages.getString(key, owner.getName(), owner.getTeam());
    }

    /** The unit's force and the forces above it, top first, such as "1st Company / Supply Column". */
    private static String forceLabel(Forces forces, Entity unit) {
        StringBuilder label = new StringBuilder();
        for (Force force : forces.forceChain(unit)) {
            if (!label.isEmpty()) {
                label.append(" / ");
            }
            label.append(force.getName());
        }
        return label.toString();
    }

    private static String unitLabel(Entity unit) {
        return Messages.getString("PriorityTargetPicker.unit", unit.getId(), unit.getShortNameRaw());
    }

    private void pickShownUnits() {
        for (PickerRow row : shownRows) {
            if (row.kind() == RowKind.UNIT) {
                pickedPriorities.putIfAbsent(row.unit().getId(), BehaviorSettings.DEFAULT_TARGET_PRIORITY);
            }
        }
        tableModel.fireTableDataChanged();
        updatePickedCount();
    }

    private void clearPicks() {
        pickedPriorities.clear();
        tableModel.fireTableDataChanged();
        updatePickedCount();
    }

    private void updatePickedCount() {
        pickedCountLabel.setText(Messages.getString("PriorityTargetPicker.pickedCount", pickedPriorities.size()));
    }

    private boolean allPicked(Collection<Entity> units) {
        for (Entity unit : units) {
            if (!pickedPriorities.containsKey(unit.getId())) {
                return false;
            }
        }
        return true;
    }

    private boolean anyPicked(Collection<Entity> units) {
        for (Entity unit : units) {
            if (pickedPriorities.containsKey(unit.getId())) {
                return true;
            }
        }
        return false;
    }

    /** @return the priority all picked units of the force share, or {@code null} when none are picked or they differ */
    private @Nullable Integer sharedPriority(Collection<Entity> units) {
        Integer shared = null;
        for (Entity unit : units) {
            Integer priority = pickedPriorities.get(unit.getId());
            if (priority == null) {
                continue;
            }
            if ((shared != null) && !shared.equals(priority)) {
                return null;
            }
            shared = priority;
        }
        return shared;
    }

    private class PickerTableModel extends AbstractTableModel {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public int getRowCount() {
            return shownRows.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case PICK_COLUMN -> Messages.getString("PriorityTargetPicker.pickColumn");
                case PRIORITY_COLUMN -> Messages.getString("PriorityTargetPicker.priorityColumn");
                default -> Messages.getString("PriorityTargetPicker.unitColumn");
            };
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case PICK_COLUMN -> Boolean.class;
                case PRIORITY_COLUMN -> Integer.class;
                default -> String.class;
            };
        }

        @Override
        public @Nullable Object getValueAt(int rowIndex, int column) {
            PickerRow row = shownRows.get(rowIndex);
            return switch (column) {
                case PICK_COLUMN -> switch (row.kind()) {
                    case PLAYER -> null;
                    case FORCE -> allPicked(row.forceUnits());
                    case UNIT -> pickedPriorities.containsKey(row.unit().getId());
                };
                case PRIORITY_COLUMN -> switch (row.kind()) {
                    case PLAYER -> null;
                    case FORCE -> sharedPriority(row.forceUnits());
                    case UNIT -> pickedPriorities.get(row.unit().getId());
                };
                default -> row.label();
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int column) {
            PickerRow row = shownRows.get(rowIndex);
            return switch (column) {
                case PICK_COLUMN -> row.kind() != RowKind.PLAYER;
                case PRIORITY_COLUMN -> switch (row.kind()) {
                    case PLAYER -> false;
                    case FORCE -> anyPicked(row.forceUnits());
                    case UNIT -> pickedPriorities.containsKey(row.unit().getId());
                };
                default -> false;
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int column) {
            PickerRow row = shownRows.get(rowIndex);
            List<Entity> affectedUnits = (row.kind() == RowKind.UNIT) ? List.of(row.unit()) : row.forceUnits();
            if ((column == PICK_COLUMN) && (value instanceof Boolean picked)) {
                for (Entity unit : affectedUnits) {
                    if (picked) {
                        pickedPriorities.putIfAbsent(unit.getId(), BehaviorSettings.DEFAULT_TARGET_PRIORITY);
                    } else {
                        pickedPriorities.remove(unit.getId());
                    }
                }
            } else if ((column == PRIORITY_COLUMN) && (value instanceof Integer priority)) {
                for (Entity unit : affectedUnits) {
                    if (pickedPriorities.containsKey(unit.getId())) {
                        pickedPriorities.put(unit.getId(), priority);
                    }
                }
            }
            // A change to one row can change the force row above it or the unit rows below it
            fireTableDataChanged();
            updatePickedCount();
        }
    }

    /** Shows no checkbox on owner rows. */
    private class PickRenderer implements TableCellRenderer {
        private final TableCellRenderer checkboxRenderer;
        private final DefaultTableCellRenderer blankRenderer = new DefaultTableCellRenderer();

        PickRenderer(TableCellRenderer checkboxRenderer) {
            this.checkboxRenderer = checkboxRenderer;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            if (value == null) {
                return blankRenderer.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            }
            return checkboxRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    /** Indents forces under owners and units under forces, and sets the headings in bold. */
    private class UnitRenderer extends DefaultTableCellRenderer {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            RowKind kind = shownRows.get(row).kind();
            int indentSteps = switch (kind) {
                case PLAYER -> 0;
                case FORCE -> 1;
                case UNIT -> 2;
            };
            int indent = UIUtil.scaleForGUI(16) * indentSteps + UIUtil.scaleForGUI(4);
            setBorder(BorderFactory.createEmptyBorder(0, indent, 0, 0));
            Font baseFont = table.getFont();
            setFont((kind == RowKind.UNIT) ? baseFont : baseFont.deriveFont(Font.BOLD));
            return this;
        }
    }
}
