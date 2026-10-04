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

import megamek.common.Player;
import megamek.common.enums.ForcedWithdrawalOrder;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests the gamemaster's Forced Withdrawal order command, run from its typed form the way the Edit Damage dialog
 * sends it, so the argument parsing, the gamemaster check and the effect on the unit are all exercised.
 */
class ForcedWithdrawalOrderCommandTest {

    private static final int GAMEMASTER_CONNECTION = 1;
    private static final int PLAYER_CONNECTION = 2;
    private static final int BOT_CONNECTION = 3;
    private static final int BOT_UNIT_ID = 10;
    private static final int HUMAN_UNIT_ID = 11;

    private ForcedWithdrawalOrderCommand command;
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
        game.addPlayer(GAMEMASTER_CONNECTION, gamemaster);
        game.addPlayer(PLAYER_CONNECTION, new Player(PLAYER_CONNECTION, "Player"));
        Player bot = new Player(BOT_CONNECTION, "Princess");
        bot.setBot(true);
        game.addPlayer(BOT_CONNECTION, bot);

        botUnit = new BipedMek();
        botUnit.setId(BOT_UNIT_ID);
        botUnit.setOwner(bot);
        game.addEntity(botUnit);
        humanUnit = new BipedMek();
        humanUnit.setId(HUMAN_UNIT_ID);
        humanUnit.setOwner(game.getPlayer(PLAYER_CONNECTION));
        game.addEntity(humanUnit);

        TWGameManager gameManager = Mockito.mock(TWGameManager.class);
        Mockito.when(gameManager.getGame()).thenReturn(game);
        Server server = Mockito.mock(Server.class);
        Mockito.when(server.getGameManager()).thenReturn(gameManager);
        Mockito.when(server.getPlayer(Mockito.anyInt())).thenAnswer(
              invocation -> game.getPlayer(invocation.getArgument(0)));
        command = new ForcedWithdrawalOrderCommand(server, gameManager);
    }

    /** Runs the command as typed, {@code /withdrawOrder <unit> <order>}. */
    private void runAs(int connection, int unitId, ForcedWithdrawalOrder order) {
        command.run(connection, new String[] { "withdrawOrder", String.valueOf(unitId), order.name() });
    }

    @Test
    void aGamemasterCanOrderABotUnitToWithdraw() {
        runAs(GAMEMASTER_CONNECTION, BOT_UNIT_ID, ForcedWithdrawalOrder.WITHDRAW);

        assertEquals(ForcedWithdrawalOrder.WITHDRAW, botUnit.getForcedWithdrawalOrder());
    }

    @Test
    void aGamemasterCanOrderABotUnitToFightToTheDeath() {
        runAs(GAMEMASTER_CONNECTION, BOT_UNIT_ID, ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH);

        assertEquals(ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH, botUnit.getForcedWithdrawalOrder());
    }

    @Test
    void aGamemasterCanHandTheUnitBackToTheBotsRules() {
        botUnit.setForcedWithdrawalOrder(ForcedWithdrawalOrder.WITHDRAW);

        runAs(GAMEMASTER_CONNECTION, BOT_UNIT_ID, ForcedWithdrawalOrder.BOT_RULES);

        assertEquals(ForcedWithdrawalOrder.BOT_RULES, botUnit.getForcedWithdrawalOrder());
    }

    @Test
    void aPlayerWhoIsNotGamemasterCannot() {
        // Pulling an enemy unit off the field, or keeping it fighting, is not a player's call.
        runAs(PLAYER_CONNECTION, BOT_UNIT_ID, ForcedWithdrawalOrder.WITHDRAW);

        assertEquals(ForcedWithdrawalOrder.BOT_RULES, botUnit.getForcedWithdrawalOrder());
    }

    @Test
    void aHumanPlayersUnitIsLeftAlone() {
        // A human decides when their own units withdraw, so the order would do nothing; it is refused instead.
        runAs(GAMEMASTER_CONNECTION, HUMAN_UNIT_ID, ForcedWithdrawalOrder.WITHDRAW);

        assertEquals(ForcedWithdrawalOrder.BOT_RULES, humanUnit.getForcedWithdrawalOrder());
    }
}
