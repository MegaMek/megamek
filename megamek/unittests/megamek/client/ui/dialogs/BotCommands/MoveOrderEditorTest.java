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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.board.Coords;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointFormation;
import megamek.common.orders.WaypointOrder;
import org.junit.jupiter.api.Test;

/**
 * Tests the Move Order editor's model: the waypoint table as the player edits it, and the commands a finished order
 * turns into.
 */
class MoveOrderEditorTest {

    private static final int NORTH = 0;
    private static final int NORTH_EAST = 1;
    private static final Coords FIRST_HEX = Coords.parseHexNumber("1709");
    private static final Coords SECOND_HEX = Coords.parseHexNumber("1706");
    private static final Coords LAST_HEX = Coords.parseHexNumber("2204");

    @Test
    void clickedHexesBecomeWaypointsThatCanBeReorderedAndRemoved() {
        WaypointTableModel waypoints = new WaypointTableModel();
        waypoints.addWaypoint(FIRST_HEX);
        waypoints.addWaypoint(LAST_HEX);
        waypoints.addWaypoint(SECOND_HEX);

        assertEquals(1, waypoints.moveUp(2));
        assertEquals(List.of(FIRST_HEX, SECOND_HEX, LAST_HEX), waypoints.getHexes());
        assertEquals(0, waypoints.moveUp(0));

        waypoints.removeWaypoint(0);
        assertEquals(List.of(SECOND_HEX, LAST_HEX), waypoints.getHexes());
    }

    @Test
    void theLastWaypointIsTheEndOfTheRouteAndHasNoHold() {
        WaypointTableModel waypoints = new WaypointTableModel();
        waypoints.setRoute(List.of(SECOND_HEX, LAST_HEX), List.of(), WaypointFormation.NONE);
        waypoints.setFacing(0, NORTH_EAST);
        waypoints.setHoldTurns(0, 2);
        waypoints.setFacing(1, NORTH);
        waypoints.setHoldTurns(1, 3);

        assertFalse(waypoints.isCellEditable(1, WaypointTableModel.COLUMN_HOLD));
        assertEquals(List.of(new WaypointOrder(NORTH_EAST, 2, WaypointFormation.NONE),
              new WaypointOrder(NORTH, 0, WaypointFormation.NONE)), waypoints.getWaypointOrders());
        // a hold beyond the editor's range is kept to it
        waypoints.setHoldTurns(0, 99);
        assertEquals(WaypointTableModel.MAXIMUM_HOLD_TURNS, waypoints.getHoldTurns(0));
    }

    @Test
    void aNewWaypointTravelsInTheFormationOfTheOneBefore() {
        // HammerGS: set the formation per waypoint; row 1 sets it and it carries on until a row changes it
        WaypointTableModel waypoints = new WaypointTableModel();
        waypoints.addWaypoint(FIRST_HEX);
        assertEquals(WaypointTableModel.DEFAULT_FORMATION, waypoints.getFormation(0));

        waypoints.setValueAt(new WaypointTableModel.ShapeOption(FormationShape.COLUMN), 0,
              WaypointTableModel.COLUMN_SHAPE);
        waypoints.addWaypoint(SECOND_HEX);
        assertEquals(FormationShape.COLUMN, waypoints.getFormation(1).getShape());

        waypoints.setValueAt(new WaypointTableModel.ShapeOption(null), 1, WaypointTableModel.COLUMN_SHAPE);
        assertTrue(waypoints.getFormation(1).isNone());
        assertFalse(waypoints.isCellEditable(1, WaypointTableModel.COLUMN_SPACING));
    }

    @Test
    void aSingleUnitTravelsOutOfFormation() {
        WaypointTableModel waypoints = new WaypointTableModel();
        waypoints.setCanForm(false);
        waypoints.addWaypoint(FIRST_HEX);

        assertTrue(waypoints.getFormation(0).isNone());
        assertFalse(waypoints.isCellEditable(0, WaypointTableModel.COLUMN_SHAPE));
    }

    @Test
    void aMoveOrderSendsEachUnitItsSlotThenTheRouteWithEachLegsFormation() {
        WaypointFormation column = new WaypointFormation(FormationShape.COLUMN, 2, FormationPace.RUN,
              ContactRule.BREAK, true);
        WaypointFormation wedge = new WaypointFormation(FormationShape.WEDGE, 3, FormationPace.WALK,
              ContactRule.HOLD, true);

        List<String> commands = MoveOrderCommands.commands(List.of(20, 21), 21, false, List.of(FIRST_HEX, SECOND_HEX),
              List.of(new WaypointOrder(UnitOrders.FACING_AUTO, 0, column), new WaypointOrder(NORTH_EAST, 2, wedge)),
              OrderPriority.NORMAL);

        String route = " ROUTE hexes=1709/F:COLUMN:2:RUN:BREAK:T-1706/NE/2/F:WEDGE:3:WALK:HOLD:T priority=NORMAL";
        assertEquals(List.of(
              "/unitOrder 20 FORMATION shape=COLUMN leader=21 spacing=2 slot=1 pace=RUN contact=BREAK together=true",
              "/unitOrder 20" + route,
              "/unitOrder 21 FORMATION shape=COLUMN leader=21 spacing=2 slot=0 pace=RUN contact=BREAK together=true",
              "/unitOrder 21" + route), commands);
    }

    @Test
    void aRouteWithNoFormationLeavesTheFormationAndOnlySetsThePriorityWhenEmpty() {
        List<String> commands = MoveOrderCommands.commands(List.of(20), 20, true, List.of(), List.of(),
              OrderPriority.IMPERATIVE);

        assertEquals(List.of("/unitOrder 20 FORMATION_OFF", "/unitOrder 20 PRIORITY priority=IMPERATIVE"), commands);
        assertEquals(UnitOrders.FACING_AUTO, new WaypointTableModel.FacingOption(UnitOrders.FACING_AUTO).facing());
    }
}
