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
package megamek.client.ui.dialogs.BotCommands;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.PhaseLine;

/**
 * The columns of the Move Order editor's waypoint table: each column's width and the dropdown, spinner or tick box
 * that edits it. Every editable cell draws as its control even when not being edited, so the player can see what can
 * be changed. Split out of {@link BotMoveOrderDialog}.
 */
final class WaypointColumns {

    private static final int NUMBER_COLUMN_WIDTH = 32;
    private static final int HEX_COLUMN_WIDTH = 56;
    private static final int SHAPE_COLUMN_WIDTH = 110;
    private static final int CHANGE_COLUMN_WIDTH = 112;
    private static final int SPACING_COLUMN_WIDTH = 80;
    private static final int PACE_COLUMN_WIDTH = 72;
    private static final int CONTACT_COLUMN_WIDTH = 160;
    private static final int TOGETHER_COLUMN_WIDTH = 90;
    private static final int FACING_COLUMN_WIDTH = 130;
    private static final int THEN_COLUMN_WIDTH = 136;
    private static final int TURNS_COLUMN_WIDTH = 56;
    private static final int PHASE_LINE_COLUMN_WIDTH = 110;

    private WaypointColumns() {}

    /**
     * Sets each column's width, and the dropdowns, spinner and tick box that edit it. Every editable cell draws as its
     * control even when not being edited, so the player can see what can be changed.
     *
     * @param table           the waypoint table
     * @param waypoints       its model
     * @param knownPhaseLines the phase lines a waypoint can join, asked each time a phase line is edited
     */
    static void setUp(JTable table, WaypointTableModel waypoints, Supplier<List<String>> knownPhaseLines) {
        TableColumnModel columns = table.getColumnModel();
        int[] widths = {NUMBER_COLUMN_WIDTH, HEX_COLUMN_WIDTH, SHAPE_COLUMN_WIDTH, CHANGE_COLUMN_WIDTH,
              SPACING_COLUMN_WIDTH, PACE_COLUMN_WIDTH, CONTACT_COLUMN_WIDTH, TOGETHER_COLUMN_WIDTH, FACING_COLUMN_WIDTH,
              THEN_COLUMN_WIDTH, TURNS_COLUMN_WIDTH, PHASE_LINE_COLUMN_WIDTH};
        for (int column = 0; column < widths.length; column++) {
            columns.getColumn(column).setPreferredWidth(UIUtil.scaleForGUI(widths[column]));
        }
        List<Integer> spacings = new ArrayList<>();
        for (int spacing = FormationOrder.MINIMUM_SPACING; spacing <= FormationOrder.MAXIMUM_SPACING; spacing++) {
            spacings.add(spacing);
        }
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_SHAPE),
              WaypointTableModel.shapeOptions().toArray(), String::valueOf);
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_CHANGE), WaypointTableModel.Change.values(),
              String::valueOf);
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_SPACING), spacings.toArray(),
              value -> Messages.getString("BotCommandPanel.Formations.spacing.hexes", value));
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_PACE), FormationPace.values(),
              value -> Messages.getString("BotCommandPanel.Formations.pace." + ((Enum<?>) value).name()));
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_CONTACT), ContactRule.values(),
              value -> Messages.getString("BotCommandPanel.Formations.contact." + ((Enum<?>) value).name()));
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_FACING),
              WaypointTableModel.facingOptions().toArray(), String::valueOf);
        columns.getColumn(WaypointTableModel.COLUMN_PHASE_LINE).setCellEditor(new PhaseLineCellEditor(knownPhaseLines));
        columns.getColumn(WaypointTableModel.COLUMN_PHASE_LINE).setCellRenderer(new ComboCellRenderer(String::valueOf));
        columns.getColumn(WaypointTableModel.COLUMN_THEN).setCellEditor(new ThenCellEditor(waypoints));
        columns.getColumn(WaypointTableModel.COLUMN_THEN).setCellRenderer(new ComboCellRenderer(String::valueOf));
        columns.getColumn(WaypointTableModel.COLUMN_TURNS).setCellEditor(new TurnsCellEditor(waypoints));
        columns.getColumn(WaypointTableModel.COLUMN_TURNS).setCellRenderer(new TurnsCellRenderer(waypoints));
    }

    /**
     * Edits a waypoint's phase line: none, one already used by any of the player's lances, or a new one, named with
     * the next ICAO name or one the player types.
     */
    private static final class PhaseLineCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<Object> combo = new JComboBox<>();
        private final Supplier<List<String>> knownPhaseLines;
        private boolean isFilling;

        private PhaseLineCellEditor(Supplier<List<String>> knownPhaseLines) {
            this.knownPhaseLines = knownPhaseLines;
            // the player may also type a name of their own
            combo.setEditable(true);
            // a pick, or Enter after typing, takes effect at once rather than when the player clicks elsewhere
            combo.addActionListener(event -> {
                if (!isFilling) {
                    stopCellEditing();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
              int column) {
            isFilling = true;
            combo.removeAllItems();
            combo.addItem(WaypointTableModel.PhaseLineOption.NONE);
            List<String> known = knownPhaseLines.get();
            for (String name : known) {
                combo.addItem(new WaypointTableModel.PhaseLineOption(name, false));
            }
            // the next free ICAO name, ready to pick: the first lance's order has nothing to join yet
            combo.addItem(new WaypointTableModel.PhaseLineOption(PhaseLine.nextName(known), true));
            combo.setSelectedItem(value);
            isFilling = false;
            return combo;
        }

        @Override
        public Object getCellEditorValue() {
            Object selected = combo.getSelectedItem();
            if (selected instanceof WaypointTableModel.PhaseLineOption option) {
                return option;
            }
            return WaypointTableModel.PhaseLineOption.typed((selected == null) ? "" : selected.toString());
        }
    }

    private static void setComboColumn(TableColumn column, Object[] choices, Function<Object, String> label) {
        JComboBox<Object> editor = new JComboBox<>(choices);
        editor.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean hasFocus) {
                return super.getListCellRendererComponent(list, (value == null) ? "" : label.apply(value), index,
                      isSelected, hasFocus);
            }
        });
        column.setCellEditor(new DefaultCellEditor(editor));
        column.setCellRenderer(new ComboCellRenderer(label));
    }

    /** Draws a cell as a dropdown showing its value, greyed out where the cell cannot be changed. */
    private static final class ComboCellRenderer implements TableCellRenderer {
        private final JComboBox<String> combo = new JComboBox<>();
        private final Function<Object, String> label;

        private ComboCellRenderer(Function<Object, String> label) {
            this.label = label;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            combo.removeAllItems();
            if (value != null) {
                combo.addItem(label.apply(value));
            }
            combo.setEnabled(table.getModel().isCellEditable(row, table.convertColumnIndexToModel(column)));
            return combo;
        }
    }

    /**
     * Draws the turns as a spinner on every row, greyed out where the waypoint neither holds nor waits to assemble.
     */
    private static final class TurnsCellRenderer implements TableCellRenderer {
        private final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0,
              WaypointTableModel.MAXIMUM_HOLD_TURNS, 1));
        private final WaypointTableModel waypoints;

        private TurnsCellRenderer(WaypointTableModel waypoints) {
            this.waypoints = waypoints;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            spinner.setValue(waypoints.getHoldTurns(row));
            spinner.setEnabled(waypoints.isCellEditable(row, WaypointTableModel.COLUMN_TURNS));
            return spinner;
        }
    }

    /** Edits the turns with a spinner, 0 to {@link WaypointTableModel#MAXIMUM_HOLD_TURNS}. */
    private static final class TurnsCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0,
              WaypointTableModel.MAXIMUM_HOLD_TURNS, 1));
        private final WaypointTableModel waypoints;

        private TurnsCellEditor(WaypointTableModel waypoints) {
            this.waypoints = waypoints;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
              int column) {
            spinner.setValue(waypoints.getHoldTurns(row));
            return spinner;
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }
    }

    /**
     * Edits what the units do on reaching a waypoint, offering pass, hold or assemble part-way along the route and
     * stay or exit at its end.
     */
    private static final class ThenCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<WaypointTableModel.Then> combo = new JComboBox<>();
        private final WaypointTableModel waypoints;

        private ThenCellEditor(WaypointTableModel waypoints) {
            this.waypoints = waypoints;
            combo.addActionListener(event -> stopCellEditing());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
              int column) {
            combo.removeAllItems();
            for (WaypointTableModel.Then then : waypoints.thenOptions(row)) {
                combo.addItem(then);
            }
            combo.setSelectedItem(waypoints.getThen(row));
            return combo;
        }

        @Override
        public Object getCellEditorValue() {
            return combo.getSelectedItem();
        }
    }
}
