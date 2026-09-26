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

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import megamek.client.ui.Messages;
import megamek.common.board.Coords;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointOrder;

/**
 * The waypoints of a move order being set up in the Move Order editor, one row each: its number, its hex, the facing
 * on arrival and the turns to hold there. The last row is the end of the route, which the unit holds until given new
 * orders, so it has no hold count.
 */
class WaypointTableModel extends AbstractTableModel {

    static final int COLUMN_NUMBER = 0;
    static final int COLUMN_HEX = 1;
    static final int COLUMN_FACING = 2;
    static final int COLUMN_HOLD = 3;

    /** The longest hold the editor offers; a player wanting longer holds with Pause. */
    static final int MAXIMUM_HOLD_TURNS = 20;

    private static final int FACING_COUNT = 6;
    private static final String[] COLUMN_KEYS = {"number", "hex", "facing", "hold"};

    /**
     * A facing as the facing column shows it: 0-5, or {@link UnitOrders#FACING_AUTO} for the bot's choice.
     *
     * @param facing the facing
     */
    record FacingOption(int facing) {
        @Override
        public String toString() {
            String key = (facing == UnitOrders.FACING_AUTO) ? "auto" : String.valueOf(facing);
            return Messages.getString("BotCommandPanel.Orders.facing." + key);
        }
    }

    /**
     * @return every facing the column offers, Auto first
     */
    static List<FacingOption> facingOptions() {
        List<FacingOption> options = new ArrayList<>();
        for (int facing = UnitOrders.FACING_AUTO; facing < FACING_COUNT; facing++) {
            options.add(new FacingOption(facing));
        }
        return options;
    }

    /** One waypoint being edited. */
    private static final class Row {
        private final Coords hex;
        private int facing;
        private int holdTurns;

        private Row(Coords hex, int facing, int holdTurns) {
            this.hex = hex;
            this.facing = facing;
            this.holdTurns = holdTurns;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    /**
     * Replaces every row with a route and what to do at each of its waypoints.
     *
     * @param hexes          the route
     * @param waypointOrders what to do at each hex, in the same order
     */
    void setRoute(List<Coords> hexes, List<WaypointOrder> waypointOrders) {
        rows.clear();
        for (int index = 0; index < hexes.size(); index++) {
            WaypointOrder order = (index < waypointOrders.size()) ? waypointOrders.get(index)
                  : WaypointOrder.PASS_THROUGH;
            rows.add(new Row(hexes.get(index), order.getFacing(), order.getHoldTurns()));
        }
        fireTableDataChanged();
    }

    /**
     * @param hex a hex to add at the end of the route, passed through with the bot choosing the facing
     */
    void addWaypoint(Coords hex) {
        rows.add(new Row(hex, UnitOrders.FACING_AUTO, 0));
        fireTableDataChanged();
    }

    /**
     * @param index the row to remove
     */
    void removeWaypoint(int index) {
        if ((index >= 0) && (index < rows.size())) {
            rows.remove(index);
            fireTableDataChanged();
        }
    }

    /**
     * @param index the row to move one place earlier in the route
     *
     * @return the row's new index
     */
    int moveUp(int index) {
        if ((index <= 0) || (index >= rows.size())) {
            return index;
        }
        rows.add(index - 1, rows.remove(index));
        fireTableDataChanged();
        return index - 1;
    }

    /**
     * @param index the row to move one place later in the route
     *
     * @return the row's new index
     */
    int moveDown(int index) {
        if ((index < 0) || (index >= rows.size() - 1)) {
            return index;
        }
        rows.add(index + 1, rows.remove(index));
        fireTableDataChanged();
        return index + 1;
    }

    /** Removes every waypoint. */
    void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    /**
     * @param index a row
     *
     * @return {@code true} for the last waypoint, the end of the route
     */
    boolean isEndOfRoute(int index) {
        return index == rows.size() - 1;
    }

    Coords getHex(int index) {
        return rows.get(index).hex;
    }

    int getFacing(int index) {
        return rows.get(index).facing;
    }

    int getHoldTurns(int index) {
        return isEndOfRoute(index) ? 0 : rows.get(index).holdTurns;
    }

    void setFacing(int index, int facing) {
        rows.get(index).facing = facing;
        fireTableRowsUpdated(index, index);
    }

    void setHoldTurns(int index, int holdTurns) {
        rows.get(index).holdTurns = Math.max(0, Math.min(MAXIMUM_HOLD_TURNS, holdTurns));
        fireTableRowsUpdated(index, index);
    }

    /**
     * @return the route's hexes, in order
     */
    List<Coords> getHexes() {
        List<Coords> hexes = new ArrayList<>();
        for (Row row : rows) {
            hexes.add(row.hex);
        }
        return hexes;
    }

    /**
     * @return what to do at each waypoint, in route order; the end of the route has no hold
     */
    List<WaypointOrder> getWaypointOrders() {
        List<WaypointOrder> orders = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            orders.add(new WaypointOrder(rows.get(index).facing, getHoldTurns(index)));
        }
        return orders;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_KEYS.length;
    }

    @Override
    public String getColumnName(int column) {
        return Messages.getString("BotCommandPanel.MoveOrder.column." + COLUMN_KEYS[column]);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row row = rows.get(rowIndex);
        return switch (columnIndex) {
            case COLUMN_NUMBER -> rowIndex + 1;
            case COLUMN_HEX -> row.hex.getBoardNum();
            case COLUMN_FACING -> new FacingOption(row.facing);
            default -> isEndOfRoute(rowIndex) ? Messages.getString("BotCommandPanel.MoveOrder.endOfRoute")
                  : holdText(row.holdTurns);
        };
    }

    private static String holdText(int holdTurns) {
        return (holdTurns == 0) ? Messages.getString("BotCommandPanel.MoveOrder.passThrough")
              : Messages.getString("BotCommandPanel.MoveOrder.holdTurns", holdTurns);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return (columnIndex == COLUMN_FACING) || ((columnIndex == COLUMN_HOLD) && !isEndOfRoute(rowIndex));
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if ((columnIndex == COLUMN_FACING) && (value instanceof FacingOption option)) {
            setFacing(rowIndex, option.facing());
        } else if ((columnIndex == COLUMN_HOLD) && (value instanceof Integer turns)) {
            setHoldTurns(rowIndex, turns);
        }
    }
}
