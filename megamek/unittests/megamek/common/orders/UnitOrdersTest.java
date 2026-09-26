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
package megamek.common.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Optional;

import megamek.common.OffBoardDirection;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import megamek.common.util.SerializationHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UnitOrders} and {@link UnitOrderAction}: what each order does, and that orders survive a savegame
 * and the network.
 */
class UnitOrdersTest {

    private static final Coords FIRST_HEX = Coords.parseHexNumber("1508");
    private static final Coords SECOND_HEX = Coords.parseHexNumber("1504");
    private static final Coords THIRD_HEX = Coords.parseHexNumber("1201");
    private static final int ROUND = 6;
    private static final int FACING_NORTH = 0;
    private static final int FACING_NORTHEAST = 1;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static UnitOrders apply(UnitOrderAction action, UnitOrders current, List<Coords> hexes) {
        return action.apply(current, hexes, OffBoardDirection.NONE, UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO,
              null, ROUND);
    }

    private static UnitOrders apply(UnitOrderAction action, UnitOrders current, List<Coords> hexes,
          List<WaypointOrder> waypointOrders) {
        return action.apply(current, hexes, waypointOrders, OffBoardDirection.NONE, UnitOrders.FACING_AUTO,
              UnitOrders.FACING_AUTO, null, ROUND, null);
    }

    @Test
    void aNewUnitHasNoOrders() {
        assertTrue(new BipedMek().getUnitOrders().isEmpty());
    }

    @Test
    void aRouteIsFollowedInOrderAndAddedTo() {
        UnitOrders orders = apply(UnitOrderAction.ROUTE, UnitOrders.NONE, List.of(FIRST_HEX, SECOND_HEX));
        orders = apply(UnitOrderAction.ADD, orders, List.of(THIRD_HEX));

        assertEquals(List.of(FIRST_HEX, SECOND_HEX, THIRD_HEX), orders.getRoute());
        assertEquals(Optional.of(FIRST_HEX), orders.getNextWaypoint());
        assertEquals(Optional.of(SECOND_HEX), orders.withNextWaypointReached().getNextWaypoint());
        assertEquals(List.of(FIRST_HEX, SECOND_HEX), apply(UnitOrderAction.REMOVE_LAST, orders, List.of()).getRoute());
    }

    @Test
    void skippingAnUnreachableLastWaypointDoesNotPauseTheUnit() {
        // a waypoint dropped because it cannot be reached is not an arrival: the unit must not park (test games)
        UnitOrders orders = apply(UnitOrderAction.ROUTE, UnitOrders.NONE, List.of(FIRST_HEX));

        UnitOrders skipped = apply(UnitOrderAction.SKIP, orders, List.of());

        assertFalse(skipped.hasRoute());
        assertFalse(skipped.isPaused());
    }

    @Test
    void theLastWaypointStaysSoTheUnitHoldsAndReturnsToIt() {
        UnitOrders orders = apply(UnitOrderAction.ROUTE, UnitOrders.NONE, List.of(FIRST_HEX, SECOND_HEX));

        UnitOrders afterFirst = apply(UnitOrderAction.REACHED, orders, List.of());
        UnitOrders afterLast = apply(UnitOrderAction.REACHED, afterFirst, List.of());

        assertEquals(List.of(SECOND_HEX), afterFirst.getRoute());
        assertEquals(List.of(SECOND_HEX), afterLast.getRoute());
        assertFalse(afterLast.isPaused());
    }

    @Test
    void ordersAreNeverChangedInPlace() {
        UnitOrders original = apply(UnitOrderAction.ROUTE, UnitOrders.NONE, List.of(FIRST_HEX));

        apply(UnitOrderAction.ADD, original, List.of(SECOND_HEX));

        assertEquals(List.of(FIRST_HEX), original.getRoute());
        assertThrows(UnsupportedOperationException.class, () -> original.getRoute().add(THIRD_HEX));
    }

    @Test
    void pauseKeepsTheRouteAndResumeCarriesOn() {
        UnitOrders routed = apply(UnitOrderAction.ROUTE, UnitOrders.NONE, List.of(FIRST_HEX));

        UnitOrders paused = apply(UnitOrderAction.PAUSE, routed, List.of());
        UnitOrders resumed = apply(UnitOrderAction.RESUME, paused, List.of());

        assertTrue(paused.isPaused());
        assertEquals(List.of(FIRST_HEX), paused.getRoute());
        assertEquals(routed, resumed);
    }

    @Test
    void stopClearsEverythingAndHoldsForThisRoundOnly() {
        UnitOrders routed = apply(UnitOrderAction.ROUTE, UnitOrders.NONE, List.of(FIRST_HEX));

        UnitOrders stopped = apply(UnitOrderAction.STOP, routed, List.of());

        assertFalse(stopped.hasRoute());
        assertTrue(stopped.isStoppedInRound(ROUND));
        assertFalse(stopped.isStoppedInRound(ROUND + 1));
    }

    @Test
    void anEdgeOrderReplacesTheRoute() {
        UnitOrders routed = apply(UnitOrderAction.ROUTE, UnitOrders.NONE, List.of(FIRST_HEX));

        UnitOrders exiting = UnitOrderAction.EXIT_BY_EDGE.apply(routed, List.of(), OffBoardDirection.NORTH,
              UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, ROUND);

        assertEquals(EdgeOrder.EXIT_BY, exiting.getEdgeOrder());
        assertEquals(OffBoardDirection.NORTH, exiting.getEdge());
        assertFalse(exiting.hasRoute());
    }

    @Test
    void ordersThatNeedDetailsRefuseWithoutThem() {
        assertThrows(IllegalArgumentException.class,
              () -> apply(UnitOrderAction.ROUTE, UnitOrders.NONE, List.of()));
        assertThrows(IllegalArgumentException.class,
              () -> apply(UnitOrderAction.MOVE_TO_EDGE, UnitOrders.NONE, List.of()));
        assertThrows(IllegalArgumentException.class,
              () -> apply(UnitOrderAction.PRIORITY, UnitOrders.NONE, List.of()));
        assertThrows(IllegalArgumentException.class, () -> UnitOrders.NONE.withFacings(6, 0));
    }

    @Test
    void aRouteCanBeImperative() {
        UnitOrders orders = UnitOrderAction.ROUTE.apply(UnitOrders.NONE, List.of(FIRST_HEX),
              OffBoardDirection.NONE, UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, OrderPriority.IMPERATIVE,
              ROUND);

        assertEquals(OrderPriority.IMPERATIVE, orders.getPriority());
    }

    @Test
    void ordersSurviveASaveAndLoad() {
        BipedMek mek = new BipedMek();
        UnitOrders orders = UnitOrders.NONE.withRoute(List.of(FIRST_HEX, SECOND_HEX))
              .withPriority(OrderPriority.IMPERATIVE)
              .withFacings(FACING_NORTH, FACING_NORTHEAST);
        mek.setUnitOrders(orders);

        String savedXml = SerializationHelper.getSaveGameXStream().toXML(mek);
        BipedMek restored = (BipedMek) SerializationHelper.getLoadSaveGameXStream().fromXML(savedXml);

        assertEquals(orders, restored.getUnitOrders());
    }

    @Test
    void aFormationSurvivesASaveAndLoadAndLeavesWithStop() {
        BipedMek mek = new BipedMek();
        FormationOrder formation = new FormationOrder(FormationShape.WEDGE, 12, 3, 2, FormationPace.RUN,
              ContactRule.HOLD);
        UnitOrders orders = UnitOrders.NONE.withRoute(List.of(FIRST_HEX)).withFormation(formation);
        mek.setUnitOrders(orders);

        String savedXml = SerializationHelper.getSaveGameXStream().toXML(mek);
        BipedMek restored = (BipedMek) SerializationHelper.getLoadSaveGameXStream().fromXML(savedXml);

        assertEquals(Optional.of(formation), restored.getUnitOrders().getFormation());
        assertEquals(Optional.of(formation), orders.withRoute(List.of(SECOND_HEX)).getFormation());
        assertTrue(apply(UnitOrderAction.STOP, orders, List.of()).getFormation().isEmpty());
        assertTrue(apply(UnitOrderAction.FORMATION_OFF, orders, List.of()).getFormation().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new FormationOrder(FormationShape.LINE, 12, 7, 0,
              FormationPace.WALK, ContactRule.BREAK));
    }

    @Test
    void aUnitFromASaveMadeBeforeOrdersExistedHasNoOrders() {
        // Loading skips field initialisers, so a save without the element restores the field as null.
        BipedMek mek = new BipedMek();
        mek.setUnitOrders(UnitOrders.NONE.withRoute(List.of(FIRST_HEX)));
        String savedXml = SerializationHelper.getSaveGameXStream().toXML(mek);
        assertTrue(savedXml.contains("<unitOrders"), "the save should hold the orders before we strip them");
        String legacyXml = savedXml.replaceAll("(?s)<unitOrders[^>]*/>|<unitOrders[^>]*>.*?</unitOrders>", "");

        BipedMek restored = (BipedMek) SerializationHelper.getLoadSaveGameXStream().fromXML(legacyXml);

        assertTrue(restored.getUnitOrders().isEmpty());
    }

    @Test
    void ordersSurviveTheNetwork() throws IOException, ClassNotFoundException {
        // Units reach clients as Java-serialized packets, so every field has to serialize.
        UnitOrders orders = UnitOrders.NONE.withRoute(List.of(FIRST_HEX)).withPaused(true);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(orders);
        }

        Object restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = input.readObject();
        }

        assertEquals(orders, restored);
    }

    @Test
    void eachWaypointKeepsItsFacingAndHoldAsTheRouteChanges() {
        WaypointOrder holdNortheast = new WaypointOrder(FACING_NORTHEAST, 2);
        UnitOrders orders = UnitOrders.NONE.withRoute(List.of(FIRST_HEX, SECOND_HEX),
              List.of(WaypointOrder.PASS_THROUGH, holdNortheast));

        assertEquals(List.of(WaypointOrder.PASS_THROUGH, holdNortheast), orders.getWaypointOrders());
        assertEquals(holdNortheast, orders.withNextWaypointReached().getWaypointOrder(0));
        assertEquals(List.of(WaypointOrder.PASS_THROUGH), orders.withLastWaypointRemoved().getWaypointOrders());
        // a plain route order, and waypoints added without settings, pass through
        assertEquals(WaypointOrder.PASS_THROUGH, orders.withWaypointsAdded(List.of(FIRST_HEX)).getWaypointOrder(2));
        assertEquals(List.of(WaypointOrder.PASS_THROUGH), UnitOrders.NONE.withRoute(List.of(FIRST_HEX))
              .getWaypointOrders());
    }

    @Test
    void aTwoTurnHoldReachedInRoundThreeHoldsRoundsFourAndFive() {
        // HammerGS: "hold 2 turns" means two full turns after the turn the unit arrives in.
        UnitOrders orders = UnitOrders.NONE.withRoute(List.of(FIRST_HEX, SECOND_HEX),
              List.of(new WaypointOrder(FACING_NORTHEAST, 2))).withHoldStarted(3);

        assertFalse(orders.isHoldingAtWaypoint(3));
        assertTrue(orders.isHoldingAtWaypoint(4));
        assertTrue(orders.isHoldingAtWaypoint(5));
        assertFalse(orders.isHoldingAtWaypoint(6));
        assertFalse(orders.isHoldDone(4));
        assertTrue(orders.isHoldDone(5));
        // moving on clears the hold
        assertEquals(UnitOrders.NO_ROUND, orders.withNextWaypointReached().getHoldSinceRound());
    }

    @Test
    void waypointFacingsAndHoldsSurviveASaveAndLoad() {
        BipedMek mek = new BipedMek();
        UnitOrders orders = UnitOrders.NONE.withRoute(List.of(FIRST_HEX, SECOND_HEX),
              List.of(new WaypointOrder(FACING_NORTHEAST, 2), new WaypointOrder(FACING_NORTH, 0))).withHoldStarted(4);
        mek.setUnitOrders(orders);

        String savedXml = SerializationHelper.getSaveGameXStream().toXML(mek);
        BipedMek restored = (BipedMek) SerializationHelper.getLoadSaveGameXStream().fromXML(savedXml);

        assertEquals(orders, restored.getUnitOrders());
    }

    @Test
    void aRouteFromASaveMadeBeforeWaypointSettingsPassesThroughEveryWaypoint() {
        BipedMek mek = new BipedMek();
        mek.setUnitOrders(UnitOrders.NONE.withRoute(List.of(FIRST_HEX, SECOND_HEX)));
        String savedXml = SerializationHelper.getSaveGameXStream().toXML(mek);
        assertTrue(savedXml.contains("<waypointOrders"), "the save should hold the settings before we strip them");
        String legacyXml = savedXml
              .replaceAll("(?s)<waypointOrders[^>]*/>|<waypointOrders[^>]*>.*?</waypointOrders>", "")
              .replaceAll("(?s)<holdSinceRound>.*?</holdSinceRound>", "");

        UnitOrders restored = ((BipedMek) SerializationHelper.getLoadSaveGameXStream().fromXML(legacyXml))
              .getUnitOrders();

        assertEquals(List.of(FIRST_HEX, SECOND_HEX), restored.getRoute());
        assertEquals(List.of(WaypointOrder.PASS_THROUGH, WaypointOrder.PASS_THROUGH), restored.getWaypointOrders());
        assertFalse(restored.isHoldDone(10));
    }

    @Test
    void editingARouteInPlaceKeepsAPauseAndAHoldUnderWay() {
        UnitOrders holding = UnitOrders.NONE.withRoute(List.of(FIRST_HEX, SECOND_HEX),
              List.of(new WaypointOrder(FACING_NORTHEAST, 2))).withHoldStarted(3).withPaused(true);

        // a later waypoint's facing changed from the map: the hold at the first carries on
        UnitOrders edited = apply(UnitOrderAction.EDIT_ROUTE, holding, List.of(FIRST_HEX, SECOND_HEX),
              List.of(new WaypointOrder(FACING_NORTHEAST, 2), new WaypointOrder(FACING_NORTH, 0)));
        assertEquals(3, edited.getHoldSinceRound());
        assertTrue(edited.isPaused());
        assertEquals(new WaypointOrder(FACING_NORTH, 0), edited.getWaypointOrder(1));

        // the waypoint it holds at taken off: the hold ends
        UnitOrders removed = apply(UnitOrderAction.EDIT_ROUTE, holding, List.of(SECOND_HEX), List.of());
        assertEquals(UnitOrders.NO_ROUND, removed.getHoldSinceRound());
    }

    @Test
    void aWaypointReadsItsFacingAndHoldInEitherOrder() {
        assertEquals(new WaypointOrder(FACING_NORTHEAST, 2), WaypointOrder.parse(List.of("NE", "2")));
        assertEquals(new WaypointOrder(4, 3), WaypointOrder.parse(List.of("3", "sw")));
        assertEquals(WaypointOrder.PASS_THROUGH, WaypointOrder.parse(List.of("A")));
        assertEquals("/NE/2", new WaypointOrder(FACING_NORTHEAST, 2).toCommandSuffix());
        assertEquals("", WaypointOrder.PASS_THROUGH.toCommandSuffix());
        assertThrows(IllegalArgumentException.class, () -> WaypointOrder.parse(List.of("NNE")));
    }
}
