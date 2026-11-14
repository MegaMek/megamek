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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Component;
import java.util.List;
import javax.swing.ListSelectionModel;

import megamek.common.board.Coords;
import megamek.common.orders.FormationPace;
import org.junit.jupiter.api.Test;

/**
 * Tests the Move Order editor's waypoint table as QA asked for it (#9449, 2026-11-07): a tooltip on every column, the
 * tick box drawn like the other cells, and one change made in a selected row going to every selected row.
 */
class WaypointTableTest {

    private static final Coords FIRST_HEX = Coords.parseHexNumber("1709");
    private static final Coords SECOND_HEX = Coords.parseHexNumber("1706");
    private static final Coords THIRD_HEX = Coords.parseHexNumber("1903");
    private static final Coords LAST_HEX = Coords.parseHexNumber("2204");

    private static WaypointTableModel fourWaypoints() {
        WaypointTableModel waypoints = new WaypointTableModel();
        waypoints.addWaypoint(FIRST_HEX);
        waypoints.addWaypoint(SECOND_HEX);
        waypoints.addWaypoint(THIRD_HEX);
        waypoints.addWaypoint(LAST_HEX);
        return waypoints;
    }

    private static WaypointColumns.WaypointTable tableFor(WaypointTableModel waypoints) {
        WaypointColumns.WaypointTable table = new WaypointColumns.WaypointTable(waypoints);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        WaypointColumns.setUp(table, waypoints, List::of);
        return table;
    }

    @Test
    void aPaceChosenInOneSelectedRowGoesToEverySelectedRow() {
        WaypointTableModel waypoints = fourWaypoints();

        waypoints.setValueAtRows(FormationPace.RUN, 1, new int[] {0, 1, 2}, WaypointTableModel.COLUMN_PACE);

        assertEquals(FormationPace.RUN, waypoints.getFormation(0).getPace());
        assertEquals(FormationPace.RUN, waypoints.getFormation(1).getPace());
        assertEquals(FormationPace.RUN, waypoints.getFormation(2).getPace());
        assertEquals(FormationPace.WALK, waypoints.getFormation(3).getPace(), "a row not selected keeps its pace");
    }

    @Test
    void aThenTheEndOfTheRouteDoesNotOfferIsLeftOffIt() {
        WaypointTableModel waypoints = fourWaypoints();
        WaypointTableModel.Then endOfRoute = waypoints.getThen(3);

        waypoints.setValueAtRows(WaypointTableModel.Then.HOLD, 0, new int[] {0, 1, 2, 3},
              WaypointTableModel.COLUMN_THEN);

        assertEquals(WaypointTableModel.Then.HOLD, waypoints.getThen(0));
        assertEquals(WaypointTableModel.Then.HOLD, waypoints.getThen(2));
        assertEquals(endOfRoute, waypoints.getThen(3), "the end of the route offers no Hold");
    }

    @Test
    void aPhaseLineStaysOnTheRowItWasSetOn() {
        WaypointTableModel waypoints = fourWaypoints();

        waypoints.setValueAtRows(new WaypointTableModel.PhaseLineOption("Bravo", true), 1, new int[] {0, 1, 2},
              WaypointTableModel.COLUMN_PHASE_LINE);

        assertEquals(List.of("Bravo"), waypoints.phaseLinesInUse());
        assertEquals(WaypointTableModel.PhaseLineOption.NONE,
              waypoints.getValueAt(0, WaypointTableModel.COLUMN_PHASE_LINE));
    }

    @Test
    void theTableSendsAnEditToEverySelectedRow() {
        WaypointTableModel waypoints = fourWaypoints();
        WaypointColumns.WaypointTable table = tableFor(waypoints);
        table.setRowSelectionInterval(1, 3);

        table.setValueAt(FormationPace.RUN, 2, WaypointTableModel.COLUMN_PACE);

        assertEquals(FormationPace.WALK, waypoints.getFormation(0).getPace());
        assertEquals(FormationPace.RUN, waypoints.getFormation(1).getPace());
        assertEquals(FormationPace.RUN, waypoints.getFormation(3).getPace());
    }

    @Test
    void clickingAControlInASelectedRowKeepsTheSelectionAndClickingTheNumberDoesNot() {
        WaypointColumns.WaypointTable table = tableFor(fourWaypoints());
        table.setRowSelectionInterval(0, 2);

        table.changeSelection(1, WaypointTableModel.COLUMN_PACE, false, false);
        assertEquals(3, table.getSelectedRowCount(), "opening a dropdown in a selected row keeps them all");

        table.changeSelection(1, WaypointTableModel.COLUMN_NUMBER, false, false);
        assertEquals(1, table.getSelectedRowCount(), "a click on the number selects that row alone");
    }

    @Test
    void everyColumnHeaderExplainsItsColumn() {
        WaypointTableModel waypoints = fourWaypoints();

        for (int column = 0; column < waypoints.getColumnCount(); column++) {
            String tip = waypoints.getColumnToolTip(column);
            assertNotNull(tip);
            assertFalse(tip.startsWith("!"), "no tooltip text for column " + waypoints.getColumnName(column));
        }
    }

    @Test
    void theTickBoxIsNotHighlightedOnTheSelectedRow() {
        WaypointColumns.WaypointTable table = tableFor(fourWaypoints());
        table.setRowSelectionInterval(0, 0);

        Component tickBox = table.prepareRenderer(table.getCellRenderer(0, WaypointTableModel.COLUMN_TOGETHER), 0,
              WaypointTableModel.COLUMN_TOGETHER);

        assertEquals(table.getBackground(), tickBox.getBackground());
    }
}
