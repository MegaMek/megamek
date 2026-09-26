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
package megamek.server.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.orders.EdgeOrder;
import megamek.common.orders.OrderPriority;
import megamek.common.units.BipedMek;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests the unit order command, run from its typed form the way the Bot Commands panel sends it, so the argument
 * parsing, the permission check and the effect on the unit are all exercised.
 */
class UnitOrderCommandTest {

    private static final int GAMEMASTER_CONNECTION = 1;
    private static final int TEAMMATE_CONNECTION = 2;
    private static final int OPPONENT_CONNECTION = 3;
    private static final int BOT_CONNECTION = 4;
    private static final int ALLIED_TEAM = 1;
    private static final int ENEMY_TEAM = 2;
    private static final int BOT_UNIT_ID = 10;
    private static final int HUMAN_UNIT_ID = 11;

    private UnitOrderCommand command;
    private BipedMek botUnit;
    private BipedMek humanUnit;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        Game game = new Game();
        Player gamemaster = new Player(GAMEMASTER_CONNECTION, "Gamemaster");
        gamemaster.setGameMaster(true);
        gamemaster.setTeam(ENEMY_TEAM);
        game.addPlayer(GAMEMASTER_CONNECTION, gamemaster);
        Player teammate = new Player(TEAMMATE_CONNECTION, "Billion Brigade");
        teammate.setTeam(ALLIED_TEAM);
        game.addPlayer(TEAMMATE_CONNECTION, teammate);
        Player opponent = new Player(OPPONENT_CONNECTION, "Clan Wolf");
        opponent.setTeam(ENEMY_TEAM);
        game.addPlayer(OPPONENT_CONNECTION, opponent);
        Player bot = new Player(BOT_CONNECTION, "Lyran Allies");
        bot.setBot(true);
        bot.setTeam(ALLIED_TEAM);
        game.addPlayer(BOT_CONNECTION, bot);

        botUnit = new BipedMek();
        botUnit.setId(BOT_UNIT_ID);
        botUnit.setOwner(bot);
        game.addEntity(botUnit);
        humanUnit = new BipedMek();
        humanUnit.setId(HUMAN_UNIT_ID);
        humanUnit.setOwner(teammate);
        game.addEntity(humanUnit);

        TWGameManager gameManager = Mockito.mock(TWGameManager.class);
        Mockito.when(gameManager.getGame()).thenReturn(game);
        Server server = Mockito.mock(Server.class);
        Mockito.when(server.getGameManager()).thenReturn(gameManager);
        Mockito.when(server.getPlayer(Mockito.anyInt())).thenAnswer(
              invocation -> game.getPlayer(invocation.getArgument(0)));
        command = new UnitOrderCommand(server, gameManager);
    }

    /** Runs the command as typed, {@code /unitOrder <unit> <action> [name=value ...]}. */
    private void runAs(int connection, int unitId, String... actionAndOptions) {
        String[] args = new String[actionAndOptions.length + 2];
        args[0] = UnitOrderCommand.COMMAND_NAME;
        args[1] = String.valueOf(unitId);
        System.arraycopy(actionAndOptions, 0, args, 2, actionAndOptions.length);
        command.run(connection, args);
    }

    @Test
    void aTeammateCanGiveABotUnitARoute() {
        runAs(TEAMMATE_CONNECTION, BOT_UNIT_ID, "ROUTE", "hexes=1508-1504", "priority=IMPERATIVE");

        assertEquals(List.of(Coords.parseHexNumber("1508"), Coords.parseHexNumber("1504")),
              botUnit.getUnitOrders().getRoute());
        assertEquals(OrderPriority.IMPERATIVE, botUnit.getUnitOrders().getPriority());
    }

    @Test
    void aGamemasterOnTheOtherTeamCanGiveOrders() {
        runAs(GAMEMASTER_CONNECTION, BOT_UNIT_ID, "EXIT_BY_EDGE", "edge=NORTH");

        assertEquals(EdgeOrder.EXIT_BY, botUnit.getUnitOrders().getEdgeOrder());
        assertEquals(OffBoardDirection.NORTH, botUnit.getUnitOrders().getEdge());
    }

    @Test
    void anOpponentCannot() {
        runAs(OPPONENT_CONNECTION, BOT_UNIT_ID, "ROUTE", "hexes=1508");

        assertTrue(botUnit.getUnitOrders().isEmpty());
    }

    @Test
    void aHumanPlayersUnitIsLeftAlone() {
        // A human moves their own units, so the orders would do nothing; they are refused instead.
        runAs(TEAMMATE_CONNECTION, HUMAN_UNIT_ID, "ROUTE", "hexes=1508");

        assertTrue(humanUnit.getUnitOrders().isEmpty());
    }

    @Test
    void facingIsSetForMovingAndStopped() {
        runAs(TEAMMATE_CONNECTION, BOT_UNIT_ID, "FACING", "moving=0", "stopped=1");

        assertEquals(0, botUnit.getUnitOrders().getFacingWhileMoving());
        assertEquals(1, botUnit.getUnitOrders().getFacingWhenStopped());
    }

    @Test
    void aRouteWithoutHexesIsRefusedAndChangesNothing() {
        runAs(TEAMMATE_CONNECTION, BOT_UNIT_ID, "ROUTE");

        assertTrue(botUnit.getUnitOrders().isEmpty());
    }
}
