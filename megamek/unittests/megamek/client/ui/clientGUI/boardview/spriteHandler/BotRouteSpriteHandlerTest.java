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
import megamek.common.game.Game;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.UnitOrders;
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

        assertEquals(List.of(new RouteFlag(FIRST_WAYPOINT, 0, 0, "GHR-5H +2", 1, AUTO, 0),
              new RouteFlag(SECOND_WAYPOINT, 0, 0, "GHR-5H +2", 2, AUTO, 0)), flags);
    }

    @Test
    void separateRoutesGetSeparateColors() {
        unit(1, "GHR-5H", ourBot, UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT)));
        unit(2, "CN9-A", ourBot, UnitOrders.NONE.withRoute(List.of(SECOND_WAYPOINT)));

        List<RouteFlag> flags = BotRouteSpriteHandler.routeFlags(units, human);

        assertEquals(List.of(new RouteFlag(FIRST_WAYPOINT, 0, 0, "GHR-5H", 1, AUTO, 0),
              new RouteFlag(SECOND_WAYPOINT, 0, 1, "CN9-A", 1, AUTO, 0)), flags);
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

        assertEquals(List.of(new RouteFlag(FIRST_WAYPOINT, 0, 0, "GHR-5H", 1, NORTH_EAST, 2),
              new RouteFlag(SECOND_WAYPOINT, 0, 0, "GHR-5H", 2, NORTH, 0)), flags);
        assertEquals("1 hold 2", flags.get(0).progressText());
        assertEquals("2", flags.get(1).progressText());
    }
}
