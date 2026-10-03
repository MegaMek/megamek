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
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.PhaseLine;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointFormation;
import megamek.common.orders.WaypointOrder;

/**
 * The waypoints of a move order being set up in the Move Order editor, one row each. A row reads as one instruction:
 * travel to this hex in this formation, face this way on arrival, then pass on, hold a number of turns, or wait until
 * the formation has assembled. The last row is the end of the route: the units hold it until given new orders, or
 * leave the board by the edge nearest it.
 */
class WaypointTableModel extends AbstractTableModel {

    static final int COLUMN_NUMBER = 0;
    static final int COLUMN_HEX = 1;
    static final int COLUMN_SHAPE = 2;
    static final int COLUMN_CHANGE = 3;
    static final int COLUMN_SPACING = 4;
    static final int COLUMN_PACE = 5;
    static final int COLUMN_CONTACT = 6;
    static final int COLUMN_TOGETHER = 7;
    static final int COLUMN_FACING = 8;
    static final int COLUMN_THEN = 9;
    static final int COLUMN_TURNS = 10;
    static final int COLUMN_PHASE_LINE = 11;

    /** The longest hold the editor offers; a player wanting longer holds with Pause. */
    static final int MAXIMUM_HOLD_TURNS = 20;

    /** The formation a first waypoint gets for a group of two or more units: a Wedge moving as a block. */
    static final WaypointFormation DEFAULT_FORMATION = new WaypointFormation(FormationShape.WEDGE,
          FormationOrder.DEFAULT_SPACING, FormationPace.WALK, ContactRule.TURN_AND_FIRE, true);

    private static final int FACING_COUNT = 6;
    private static final String[] COLUMN_KEYS = {"number", "hex", "shape", "change", "spacing", "pace", "contact",
          "together", "facing", "then", "turns", "phase"};

    /**
     * The turns a new hold waits: a two-turn delay; or, waiting until in position, at most eight turns - the leader's
     * own estimate of when the last unit arrives normally ends the wait long before.
     */
    private static final int DEFAULT_HOLD_TURNS = 2;
    private static final int DEFAULT_ASSEMBLE_TURNS = 8;

    /**
     * What the units do on reaching a waypoint: part-way along the route pass on, hold or wait to assemble; at the end
     * of the route stay or leave the board.
     */
    enum Then {
        PASS,
        HOLD,
        ASSEMBLE,
        STAY,
        EXIT;

        @Override
        public String toString() {
            return Messages.getString("BotCommandPanel.MoveOrder.then." + name());
        }
    }

    /**
     * Where the units take a waypoint's formation: on the way to it, or on reaching it, keeping their old shape until
     * then and re-forming there before moving on.
     */
    enum Change {
        ON_THE_WAY,
        AT_WAYPOINT;

        @Override
        public String toString() {
            return Messages.getString("BotCommandPanel.MoveOrder.change." + name());
        }
    }

    /**
     * A facing as the facing column shows it: 0-5, or {@link UnitOrders#FACING_AUTO} for the bot's choice.
     *
     * @param facing the facing
     */
    record FacingOption(int facing) {
        @Override
        public String toString() {
            // a waypoint left without a facing faces on toward the next flag (HammerGS, 2026-09-27)
            return (facing == UnitOrders.FACING_AUTO) ? Messages.getString("BotCommandPanel.MoveOrder.facing.next")
                  : Messages.getString("BotCommandPanel.Orders.facing." + facing);
        }
    }

    /**
     * A phase line as the phase line column shows it: none, one already in use, or a new one offered by its next free
     * name, e.g. "PL Alfa (new)". The first lance's order has no line to join yet, so the new one is in the list
     * itself, ready to pick (HammerGS's playtest, 2026-10-02: behind a separate "New phase line..." prompt, the list
     * read as empty).
     *
     * @param name  the phase line's name, or {@code null} for none
     * @param isNew {@code true} for a line not yet on any route
     */
    record PhaseLineOption(@Nullable String name, boolean isNew) {
        static final PhaseLineOption NONE = new PhaseLineOption(null, false);

        @Override
        public String toString() {
            if (name == null) {
                return Messages.getString("BotCommandPanel.MoveOrder.phaseLine.none");
            }
            return isNew ? Messages.getString("BotCommandPanel.MoveOrder.phaseLine.newNamed", PhaseLine.display(name))
                  : PhaseLine.display(name);
        }

        /**
         * @param typed what the player typed into the column, e.g. {@code Hill 312} or {@code PL Bravo}
         *
         * @return the option for it: the phase line named, or none for a blank entry
         */
        static PhaseLineOption typed(String typed) {
            String text = typed.trim();
            String newSuffix = Messages.getString("BotCommandPanel.MoveOrder.phaseLine.newNamed", "").trim();
            if (!newSuffix.isEmpty() && text.endsWith(newSuffix)) {
                text = text.substring(0, text.length() - newSuffix.length()).trim();
            }
            if (text.regionMatches(true, 0, "PL ", 0, 3)) {
                text = text.substring(3);
            }
            String name = PhaseLine.cleanName(text);
            return (name == null) ? NONE : new PhaseLineOption(name, false);
        }
    }

    /**
     * A shape as the shape column shows it, or no formation at all.
     *
     * @param shape the shape, or {@code null} to travel out of formation
     */
    record ShapeOption(@Nullable FormationShape shape) {
        @Override
        public String toString() {
            return (shape == null) ? Messages.getString("BotCommandPanel.MoveOrder.noFormation")
                  : Messages.getString("BotCommandPanel.Formations.shape." + shape);
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

    /**
     * @return every shape the column offers, no formation first
     */
    static List<ShapeOption> shapeOptions() {
        List<ShapeOption> options = new ArrayList<>();
        options.add(new ShapeOption(null));
        for (FormationShape shape : FormationShape.values()) {
            options.add(new ShapeOption(shape));
        }
        return options;
    }

    /** One waypoint being edited. */
    private static final class Row {
        private final Coords hex;
        private int facing;
        private WaypointOrder.HoldMode holdMode;
        private int holdTurns;
        private boolean exitBoard;
        private WaypointFormation formation;
        private boolean isChangeAtWaypoint;
        private String phaseLine;
        // a turning point the bot planned, not one the player set
        private boolean isPlanned;

        private Row(Coords hex, int facing, WaypointOrder.HoldMode holdMode, int holdTurns, boolean exitBoard,
              WaypointFormation formation) {
            this.hex = hex;
            this.facing = facing;
            this.holdMode = holdMode;
            this.holdTurns = holdTurns;
            this.exitBoard = exitBoard;
            this.formation = formation;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private boolean canForm = true;
    // the units' formation when the order was loaded: the shape they keep to a first waypoint that changes it there
    private WaypointFormation unitsFormation = WaypointFormation.NONE;

    /**
     * @param canFormNow {@code false} for a group of one unit, which travels out of formation on every leg
     */
    void setCanForm(boolean canFormNow) {
        canForm = canFormNow;
        if (!canForm) {
            for (Row row : rows) {
                row.formation = WaypointFormation.NONE;
            }
        }
        fireTableDataChanged();
    }

    /**
     * Replaces every row with a route and what to do at each of its waypoints.
     *
     * @param hexes          the route
     * @param waypointOrders what to do at each hex, in the same order
     * @param unitsFormation the formation of a leg that sets none of its own: the units' own formation
     */
    void setRoute(List<Coords> hexes, List<WaypointOrder> waypointOrders, WaypointFormation unitsFormation) {
        rows.clear();
        this.unitsFormation = unitsFormation;
        for (int index = 0; index < hexes.size(); index++) {
            WaypointOrder order = (index < waypointOrders.size()) ? waypointOrders.get(index)
                  : WaypointOrder.PASS_THROUGH;
            // a waypoint that changes shape there shows the shape it changes to
            boolean isChangeAtWaypoint = order.getArrivalFormation() != null;
            WaypointFormation formation = isChangeAtWaypoint ? order.getArrivalFormation()
                  : ((order.getFormation() == null) ? unitsFormation : order.getFormation());
            Row row = new Row(hexes.get(index), order.getFacing(), order.getHoldMode(), order.getHoldTurns(),
                  order.isExitBoard(), canForm ? formation : WaypointFormation.NONE);
            row.isChangeAtWaypoint = canForm && isChangeAtWaypoint;
            row.phaseLine = order.getPhaseLine();
            row.isPlanned = order.isPlannedTurn();
            rows.add(row);
        }
        fireTableDataChanged();
    }

    /**
     * Adds a hex at the end of the route, passed through with the bot choosing the facing, in the formation of the
     * waypoint before it - so a formation set on the first waypoint carries on until changed.
     *
     * @param hex the hex
     */
    void addWaypoint(Coords hex) {
        addWaypoint(hex, false);
    }

    /**
     * @param hex       the hex
     * @param isPlanned {@code true} for a turning point the bot planned, shown as one and sent as one
     */
    void addWaypoint(Coords hex, boolean isPlanned) {
        WaypointFormation formation;
        if (!canForm) {
            formation = WaypointFormation.NONE;
        } else if (rows.isEmpty()) {
            formation = DEFAULT_FORMATION;
        } else {
            formation = rows.get(rows.size() - 1).formation;
        }
        Row row = new Row(hex, UnitOrders.FACING_AUTO, WaypointOrder.HoldMode.PASS, 0, false, formation);
        row.isPlanned = isPlanned;
        rows.add(row);
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
        return (isEndOfRoute(index) || (rows.get(index).holdMode == WaypointOrder.HoldMode.PASS)) ? 0
              : rows.get(index).holdTurns;
    }

    /**
     * @param index a row
     *
     * @return what the units do on reaching that waypoint
     */
    Then getThen(int index) {
        Row row = rows.get(index);
        if (isEndOfRoute(index)) {
            return row.exitBoard ? Then.EXIT : Then.STAY;
        }
        return switch (row.holdMode) {
            case HOLD -> Then.HOLD;
            case ASSEMBLE -> Then.ASSEMBLE;
            default -> Then.PASS;
        };
    }

    /**
     * @param index a row
     *
     * @return the choices for that waypoint: stay or exit at the end of the route, pass, hold or assemble before it
     */
    List<Then> thenOptions(int index) {
        return isEndOfRoute(index) ? List.of(Then.STAY, Then.EXIT) : List.of(Then.PASS, Then.HOLD, Then.ASSEMBLE);
    }

    void setThen(int index, Then then) {
        Row row = rows.get(index);
        switch (then) {
            case STAY -> row.exitBoard = false;
            case EXIT -> row.exitBoard = true;
            case PASS -> row.holdMode = WaypointOrder.HoldMode.PASS;
            case HOLD -> {
                row.holdMode = WaypointOrder.HoldMode.HOLD;
                row.holdTurns = (row.holdTurns > 0) ? row.holdTurns : DEFAULT_HOLD_TURNS;
            }
            case ASSEMBLE -> {
                row.holdMode = WaypointOrder.HoldMode.ASSEMBLE;
                row.holdTurns = (row.holdTurns > 0) ? row.holdTurns : DEFAULT_ASSEMBLE_TURNS;
            }
        }
        fireTableRowsUpdated(index, index);
    }

    WaypointFormation getFormation(int index) {
        return rows.get(index).formation;
    }

    /**
     * @param index a row
     *
     * @return where the units take that waypoint's formation
     */
    Change getChange(int index) {
        return rows.get(index).isChangeAtWaypoint ? Change.AT_WAYPOINT : Change.ON_THE_WAY;
    }

    void setChange(int index, Change change) {
        rows.get(index).isChangeAtWaypoint = canForm && (change == Change.AT_WAYPOINT);
        fireTableRowsUpdated(index, index);
    }

    /**
     * @param index a row
     *
     * @return one line on what the units do at that waypoint, for under the table: how they take its formation and
     *       what they do there, e.g. "Waypoint 2 - hex 1617: The lance takes the Wedge on the way here. Hold 2: ..."
     */
    /**
     * @return the order, marked as a turning point the bot planned when the row is one
     */
    private static WaypointOrder planned(Row row, WaypointOrder order) {
        return row.isPlanned ? order.withRoutePlan(WaypointOrder.RoutePlan.TURN_POINT) : order;
    }

    /**
     * @return {@code true} if the row is a turning point the bot planned
     */
    boolean isPlanned(int index) {
        return rows.get(index).isPlanned;
    }

    String describe(int index) {
        Row row = rows.get(index);
        StringBuilder text = new StringBuilder(Messages.getString("BotCommandPanel.MoveOrder.help.selected",
              index + 1, row.hex.getBoardNum()));
        if (row.isPlanned) {
            text.append(' ').append(Messages.getString("BotCommandPanel.MoveOrder.help.planned"));
        }
        text.append(' ');
        if (row.formation.isNone()) {
            text.append(Messages.getString("BotCommandPanel.MoveOrder.help.noFormation"));
        } else {
            String shape = new ShapeOption(row.formation.getShape()).toString();
            text.append(Messages.getString(row.isChangeAtWaypoint ? "BotCommandPanel.MoveOrder.help.shapeAtWaypoint"
                  : "BotCommandPanel.MoveOrder.help.shapeOnTheWay", shape));
        }
        text.append(' ').append(Messages.getString("BotCommandPanel.MoveOrder.help." + getThen(index).name(),
              row.holdTurns));
        if (row.phaseLine != null) {
            text.append(' ').append(Messages.getString("BotCommandPanel.MoveOrder.help.phaseLine",
                  PhaseLine.display(row.phaseLine)));
        }
        return text.toString();
    }

    /**
     * @param index     a row
     * @param phaseLine the phase line that waypoint is on, or {@code null} for none
     */
    void setPhaseLine(int index, @Nullable String phaseLine) {
        rows.get(index).phaseLine = PhaseLine.cleanName(phaseLine);
        fireTableRowsUpdated(index, index);
    }

    @Nullable String getPhaseLine(int index) {
        return rows.get(index).phaseLine;
    }

    /**
     * @return the phase lines this route's waypoints are on
     */
    List<String> phaseLinesInUse() {
        List<String> names = new ArrayList<>();
        for (Row row : rows) {
            if ((row.phaseLine != null) && !names.contains(row.phaseLine)) {
                names.add(row.phaseLine);
            }
        }
        return names;
    }

    void setFacing(int index, int facing) {
        rows.get(index).facing = facing;
        fireTableRowsUpdated(index, index);
    }

    void setHoldTurns(int index, int holdTurns) {
        Row row = rows.get(index);
        row.holdTurns = Math.clamp(holdTurns, 0, MAXIMUM_HOLD_TURNS);
        if (row.holdTurns == 0) {
            row.holdMode = WaypointOrder.HoldMode.PASS;
        } else if (row.holdMode == WaypointOrder.HoldMode.PASS) {
            row.holdMode = WaypointOrder.HoldMode.HOLD;
        }
        fireTableRowsUpdated(index, index);
    }

    void setFormation(int index, WaypointFormation formation) {
        rows.get(index).formation = canForm ? formation : WaypointFormation.NONE;
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
     * @return what to do at each waypoint, in route order, each with the formation for the leg ending there; the end
     *       of the route has no hold, and leaves the board if set to. Each is named as a nav point in order, Alpha
     *       first, which the radio calls and the map flags use
     */
    List<WaypointOrder> getWaypointOrders() {
        List<WaypointOrder> orders = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            int holdTurns = getHoldTurns(index);
            WaypointOrder.HoldMode holdMode = (holdTurns == 0) ? WaypointOrder.HoldMode.PASS : row.holdMode;
            boolean isExit = isEndOfRoute(index) && row.exitBoard;
            if (row.isChangeAtWaypoint && !row.formation.isNone()) {
                // the leg keeps the shape the units had before, and they re-form in this one on arrival
                WaypointFormation shapeBefore = (index == 0) ? unitsFormation : rows.get(index - 1).formation;
                orders.add(planned(row, new WaypointOrder(row.facing, holdMode, holdTurns, shapeBefore, isExit,
                      row.formation).withNavNumber(index + 1).withPhaseLine(row.phaseLine)));
            } else {
                orders.add(planned(row, new WaypointOrder(row.facing, holdMode, holdTurns, row.formation, isExit)
                      .withNavNumber(index + 1).withPhaseLine(row.phaseLine)));
            }
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
    public Class<?> getColumnClass(int columnIndex) {
        return (columnIndex == COLUMN_TOGETHER) ? Boolean.class : Object.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row row = rows.get(rowIndex);
        WaypointFormation formation = row.formation;
        return switch (columnIndex) {
            case COLUMN_NUMBER -> rowIndex + 1;
            case COLUMN_HEX -> row.isPlanned ? Messages.getString("BotCommandPanel.MoveOrder.plannedHex",
                  row.hex.getBoardNum()) : row.hex.getBoardNum();
            case COLUMN_SHAPE -> new ShapeOption(formation.getShape());
            case COLUMN_CHANGE -> getChange(rowIndex);
            case COLUMN_SPACING -> formation.getSpacing();
            case COLUMN_PACE -> formation.getPace();
            case COLUMN_CONTACT -> formation.getContactRule();
            case COLUMN_TOGETHER -> formation.isKeepTogether();
            case COLUMN_FACING -> new FacingOption(row.facing);
            case COLUMN_THEN -> getThen(rowIndex);
            case COLUMN_PHASE_LINE -> (row.phaseLine == null) ? PhaseLineOption.NONE
                  : new PhaseLineOption(row.phaseLine, false);
            default -> getHoldTurns(rowIndex);
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        boolean hasShape = !rows.get(rowIndex).formation.isNone();
        return switch (columnIndex) {
            case COLUMN_SHAPE -> canForm;
            case COLUMN_CHANGE, COLUMN_SPACING, COLUMN_PACE, COLUMN_CONTACT, COLUMN_TOGETHER -> hasShape;
            case COLUMN_FACING, COLUMN_THEN, COLUMN_PHASE_LINE -> true;
            case COLUMN_TURNS -> !isEndOfRoute(rowIndex) && (rows.get(rowIndex).holdMode != WaypointOrder.HoldMode.PASS);
            default -> false;
        };
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        WaypointFormation formation = rows.get(rowIndex).formation;
        switch (columnIndex) {
            case COLUMN_SHAPE -> {
                if (value instanceof ShapeOption option) {
                    setFormation(rowIndex, (option.shape() == null) ? WaypointFormation.NONE
                          : new WaypointFormation(option.shape(), formation.getSpacing(), formation.getPace(),
                          formation.getContactRule(), formation.isNone() || formation.isKeepTogether()));
                }
            }
            case COLUMN_SPACING -> {
                if (value instanceof Integer spacing) {
                    setFormation(rowIndex, new WaypointFormation(formation.getShape(), spacing, formation.getPace(),
                          formation.getContactRule(), formation.isKeepTogether()));
                }
            }
            case COLUMN_PACE -> {
                if (value instanceof FormationPace pace) {
                    setFormation(rowIndex, new WaypointFormation(formation.getShape(), formation.getSpacing(), pace,
                          formation.getContactRule(), formation.isKeepTogether()));
                }
            }
            case COLUMN_CONTACT -> {
                if (value instanceof ContactRule contactRule) {
                    setFormation(rowIndex, new WaypointFormation(formation.getShape(), formation.getSpacing(),
                          formation.getPace(), contactRule, formation.isKeepTogether()));
                }
            }
            case COLUMN_TOGETHER -> {
                if (value instanceof Boolean keepTogether) {
                    setFormation(rowIndex, new WaypointFormation(formation.getShape(), formation.getSpacing(),
                          formation.getPace(), formation.getContactRule(), keepTogether));
                }
            }
            case COLUMN_CHANGE -> {
                if (value instanceof Change change) {
                    setChange(rowIndex, change);
                }
            }
            case COLUMN_FACING -> {
                if (value instanceof FacingOption option) {
                    setFacing(rowIndex, option.facing());
                }
            }
            case COLUMN_THEN -> {
                if (value instanceof Then then) {
                    setThen(rowIndex, then);
                }
            }
            case COLUMN_TURNS -> {
                if (value instanceof Integer turns) {
                    setHoldTurns(rowIndex, turns);
                }
            }
            case COLUMN_PHASE_LINE -> {
                if (value instanceof PhaseLineOption option) {
                    setPhaseLine(rowIndex, option.name());
                } else if (value instanceof String typed) {
                    setPhaseLine(rowIndex, PhaseLineOption.typed(typed).name());
                }
            }
            default -> {
            }
        }
    }
}
