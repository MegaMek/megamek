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

import java.util.List;

import megamek.common.board.Coords;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.UnitOrders;
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
        waypoints.setRoute(List.of(SECOND_HEX, LAST_HEX), List.of());
        waypoints.setFacing(0, NORTH_EAST);
        waypoints.setHoldTurns(0, 2);
        waypoints.setFacing(1, NORTH);
        waypoints.setHoldTurns(1, 3);

        assertFalse(waypoints.isCellEditable(1, WaypointTableModel.COLUMN_HOLD));
        assertEquals(List.of(new WaypointOrder(NORTH_EAST, 2), new WaypointOrder(NORTH, 0)),
              waypoints.getWaypointOrders());
        // a hold beyond the editor's range is kept to it
        waypoints.setHoldTurns(0, 99);
        assertEquals(WaypointTableModel.MAXIMUM_HOLD_TURNS, waypoints.getHoldTurns(0));
    }

    @Test
    void aMoveOrderInFormationSendsEachUnitItsSlotThenTheRoute() {
        MoveOrderCommands.FormationChoice wedge = new MoveOrderCommands.FormationChoice(FormationShape.WEDGE, 21, 2,
              FormationPace.WALK, ContactRule.BREAK, true);

        List<String> commands = MoveOrderCommands.commands(List.of(20, 21), wedge, false,
              List.of(FIRST_HEX, SECOND_HEX, LAST_HEX),
              List.of(WaypointOrder.PASS_THROUGH, new WaypointOrder(NORTH_EAST, 2), new WaypointOrder(NORTH, 0)),
              OrderPriority.NORMAL);

        assertEquals(List.of(
              "/unitOrder 20 FORMATION shape=WEDGE leader=21 spacing=2 slot=1 pace=WALK contact=BREAK together=true",
              "/unitOrder 20 ROUTE hexes=1709-1706/NE/2-2204/N priority=NORMAL",
              "/unitOrder 21 FORMATION shape=WEDGE leader=21 spacing=2 slot=0 pace=WALK contact=BREAK together=true",
              "/unitOrder 21 ROUTE hexes=1709-1706/NE/2-2204/N priority=NORMAL"), commands);
    }

    @Test
    void leavingAFormationWithNoRouteOnlySetsThePriority() {
        List<String> commands = MoveOrderCommands.commands(List.of(20), null, true, List.of(), List.of(),
              OrderPriority.IMPERATIVE);

        assertEquals(List.of("/unitOrder 20 FORMATION_OFF", "/unitOrder 20 PRIORITY priority=IMPERATIVE"), commands);
        assertEquals(UnitOrders.FACING_AUTO, new WaypointTableModel.FacingOption(UnitOrders.FACING_AUTO).facing());
    }
}
