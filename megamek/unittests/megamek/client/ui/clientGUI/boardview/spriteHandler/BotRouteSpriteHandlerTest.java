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
package megamek.client.ui.clientGUI.boardview.spriteHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.clientGUI.boardview.spriteHandler.BotRouteSpriteHandler.RouteFlag;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointFormation;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests which bot routes get waypoint flags and how the flags are grouped, colored and labelled.
 */
class BotRouteSpriteHandlerTest {

    private static final int OUR_TEAM = 1;
    private static final int AUTO = UnitOrders.FACING_AUTO;
    private static final int NORTH = 0;
    private static final int NORTH_EAST = 1;
    private static final int ENEMY_TEAM = 2;
    private static final Coords FIRST_WAYPOINT = Coords.parseHexNumber("1623");
    private static final Coords SECOND_WAYPOINT = Coords.parseHexNumber("1615");

    private Game game;
    private Player human;
    private Player ourBot;
    private Player enemyBot;
    private final List<Entity> units = new ArrayList<>();

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        human = new Player(0, "Defenders");
        human.setTeam(OUR_TEAM);
        game.addPlayer(0, human);
        ourBot = new Player(1, "Princess");
        ourBot.setBot(true);
        ourBot.setTeam(OUR_TEAM);
        game.addPlayer(1, ourBot);
        enemyBot = new Player(2, "Clan Wolf");
        enemyBot.setBot(true);
        enemyBot.setTeam(ENEMY_TEAM);
        game.addPlayer(2, enemyBot);
    }

    private BipedMek unit(int unitId, String model, Player owner, UnitOrders orders) {
        BipedMek mek = new BipedMek();
        mek.setId(unitId);
        mek.setChassis("Test");
        mek.setModel(model);
        mek.setOwner(owner);
        game.addEntity(mek);
        mek.setPosition(new Coords(unitId, 30));
        mek.setUnitOrders(orders);
        units.add(mek);
        return mek;
    }

    private static UnitOrders inFormation(int slot) {
        return UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT, SECOND_WAYPOINT)).withFormation(
              new FormationOrder(FormationShape.COLUMN, 1, 2, slot, FormationPace.WALK, ContactRule.BREAK));
    }

    @Test
    void aFormationGetsOneNumberedFlagPerWaypointNamedForItsLeader() {
        unit(1, "GHR-5H", ourBot, inFormation(0));
        unit(2, "CN9-A", ourBot, inFormation(1));
        unit(3, "LGB-7Q", ourBot, inFormation(2));

        List<RouteFlag> flags = BotRouteSpriteHandler.routeFlags(units, human);

        assertEquals(List.of(new RouteFlag(FIRST_WAYPOINT, 0, 0, "GHR-5H +2", "1", AUTO, 0, false, false, null),
              new RouteFlag(SECOND_WAYPOINT, 0, 0, "GHR-5H +2", "2", AUTO, 0, false, false, null)), flags);
    }

    @Test
    void separateRoutesGetSeparateColors() {
        unit(1, "GHR-5H", ourBot, UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT)));
        unit(2, "CN9-A", ourBot, UnitOrders.NONE.withRoute(List.of(SECOND_WAYPOINT)));

        List<RouteFlag> flags = BotRouteSpriteHandler.routeFlags(units, human);

        assertEquals(List.of(new RouteFlag(FIRST_WAYPOINT, 0, 0, "Test GHR-5H", "1", AUTO, 0, false, false, null),
              new RouteFlag(SECOND_WAYPOINT, 0, 1, "Test CN9-A", "1", AUTO, 0, false, false, null)), flags);
    }

    @Test
    void enemyBotRoutesAndHumanUnitsAreNeverFlagged() {
        unit(1, "AGT-1A", human, UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT)));
        unit(2, "TBR-A", enemyBot, UnitOrders.NONE.withRoute(List.of(SECOND_WAYPOINT)));

        assertTrue(BotRouteSpriteHandler.routeFlags(units, human).isEmpty());
    }

    @Test
    void aFlagShowsTheWaypointsHoldAndFacing() {
        unit(1, "GHR-5H", ourBot, UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT, SECOND_WAYPOINT),
              List.of(new WaypointOrder(NORTH_EAST, 2), new WaypointOrder(NORTH, 0))));

        List<RouteFlag> flags = BotRouteSpriteHandler.routeFlags(units, human);

        assertEquals(List.of(new RouteFlag(FIRST_WAYPOINT, 0, 0, "Test GHR-5H", "1", NORTH_EAST, 2, false, false, null),
              new RouteFlag(SECOND_WAYPOINT, 0, 0, "Test GHR-5H", "2", NORTH, 0, false, false, null)), flags);
        assertEquals("1 hold 2", flags.get(0).progressText());
        assertEquals("2", flags.get(1).progressText());
    }

    @Test
    void hoveringOverAFlagSummarisesTheOrdersAtThatWaypoint() {
        // HammerGS: hovering on a waypoint flag gives a summary of the orders there
        unit(1, "GHR-5H", ourBot, UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT, SECOND_WAYPOINT),
              List.of(new WaypointOrder(NORTH_EAST, 2).withNavNumber(1), WaypointOrder.PASS_THROUGH.withNavNumber(2))));

        String summary = BotRouteSpriteHandler.tooltipFor(units, human, FIRST_WAYPOINT, 0);

        assertTrue(summary.contains("Nav Point Alpha (" + FIRST_WAYPOINT.getBoardNum() + ") - Test GHR-5H"), summary);
        assertTrue(summary.contains("Facing on arrival: Northeast"), summary);
        assertTrue(summary.contains("Then: Hold 2 turns"), summary);
        assertTrue(summary.contains("Priority: Normal"), summary);
        assertTrue(BotRouteSpriteHandler.tooltipFor(units, human, FIRST_WAYPOINT.translated(0, 3), 0).isEmpty());
    }

    @Test
    void aLancesFlagsCarryTheLancesName() {
        // HammerGS: moving a force called Alpha Lance, the flag says Alpha Lance; one unit alone, its own name
        int lanceId = game.getForces().addTopLevelForce(Force.createToplevelForce("Alpha Lance", ourBot), ourBot);
        UnitOrders route = UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT));
        game.getForces().addEntity(unit(1, "GHR-5H", ourBot, route), lanceId);
        game.getForces().addEntity(unit(2, "CN9-A", ourBot, route), lanceId);
        unit(3, "WHM-6R", ourBot, UnitOrders.NONE.withRoute(List.of(SECOND_WAYPOINT)));

        List<RouteFlag> flags = BotRouteSpriteHandler.routeFlags(units, human);

        assertEquals("Alpha Lance", flags.get(0).label());
        assertEquals("Test WHM-6R", flags.get(1).label());
    }

    @Test
    void theSummarySaysWhichFormationTheLanceMovesInAndWhereItChanges() {
        // HammerGS: "moving to the flag in formation X", and a note where the formation changes
        WaypointFormation column = new WaypointFormation(FormationShape.COLUMN, 1, FormationPace.WALK,
              ContactRule.TURN_AND_FIRE, true);
        WaypointFormation line = new WaypointFormation(FormationShape.LINE, 1, FormationPace.WALK,
              ContactRule.TURN_AND_FIRE, true);
        unit(1, "GHR-5H", ourBot, UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT, SECOND_WAYPOINT),
              List.of(new WaypointOrder(UnitOrders.FACING_AUTO, 0, column).withNavNumber(1),
                    new WaypointOrder(UnitOrders.FACING_AUTO, 0, line).withNavNumber(2))));

        String summary = BotRouteSpriteHandler.tooltipFor(units, human, FIRST_WAYPOINT, 0);

        assertTrue(summary.contains("Moving to Nav Point Alpha in: Column (1 hex"), summary);
        assertTrue(summary.contains("Then changes on the way to Nav Point Beta, to: Line (1 hex"), summary);
    }

    @Test
    void aPhaseLineIsNamedOnItsFlag() {
        RouteFlag onBravo = new RouteFlag(FIRST_WAYPOINT, 0, 0, "GHR-5H", "Beta", AUTO, 0, false, false, "Bravo");
        RouteFlag holdingOnBravo = new RouteFlag(FIRST_WAYPOINT, 0, 0, "GHR-5H", "Beta", AUTO, 2, false, false,
              "Bravo");

        assertEquals("Beta - PL Bravo", onBravo.progressText());
        assertEquals("Beta hold 2 - PL Bravo", holdingOnBravo.progressText());
    }

    @Test
    void aRouteReadsAsALineOfDotsBetweenItsPoints() {
        BipedMek mek = unit(1, "GHR-5H", ourBot, UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT, SECOND_WAYPOINT)));
        mek.setDeployed(true);
        mek.setPosition(Coords.parseHexNumber("1630"));

        List<BotRouteSpriteHandler.TrailDot> dots = BotRouteSpriteHandler.routeTrails(units, human);

        // 1630 to 1623 and 1623 to 1615: the hexes between, not the unit's hex or the waypoints
        assertEquals(6 + 7, dots.size());
        for (BotRouteSpriteHandler.TrailDot dot : dots) {
            assertTrue(dot.hex().getX() == 15, "a straight north line stays in column 16: " + dot.hex());
            assertTrue(!dot.hex().equals(FIRST_WAYPOINT) && !dot.hex().equals(SECOND_WAYPOINT));
        }
    }

    @Test
    void aRouteAlongTheEdgeBetweenTwoColumnsReadsAsOneLine() {
        // HammerGS's playtest, 2026-10-04: a convoy at 0222 heading for 0103 drew two columns of dots side by side
        Coords from = Coords.parseHexNumber("0222");
        Coords to = Coords.parseHexNumber("0103");

        List<Coords> steps = BotRouteSpriteHandler.stepsBetween(from, to);

        assertEquals(from.distance(to) + 1, steps.size(), "one hex a step: " + steps);
        int columnChanges = 0;
        for (int index = 1; index < steps.size(); index++) {
            assertEquals(1, steps.get(index - 1).distance(steps.get(index)), "steps are neighbours: " + steps);
            if (steps.get(index - 1).getX() != steps.get(index).getX()) {
                columnChanges++;
            }
        }
        assertEquals(1, columnChanges, "one jog from column 02 to column 01: " + steps);
    }

    @Test
    void aPlannedTurnIsADotAndALeadersFlagCarriesItsLancesName() {
        int lanceId = game.getForces().addTopLevelForce(Force.createToplevelForce("Convoy", ourBot), ourBot);
        WaypointOrder turn = WaypointOrder.PASS_THROUGH.withRoutePlan(WaypointOrder.RoutePlan.TURN_POINT);
        BipedMek leader = unit(1, "Sherpa", ourBot, UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT, SECOND_WAYPOINT),
              List.of(turn, WaypointOrder.PASS_THROUGH)).withFormation(new FormationOrder(FormationShape.COLUMN, 1, 1,
              0, FormationPace.WALK, ContactRule.HOLD)));
        game.getForces().addEntity(leader, lanceId);

        List<RouteFlag> flags = BotRouteSpriteHandler.routeFlags(units, human);

        assertEquals("Convoy", flags.get(0).label(), "the lance's name, not the truck's");
        assertTrue(flags.get(0).isPlanned());
        assertTrue(!flags.get(1).isPlanned());
    }
}
