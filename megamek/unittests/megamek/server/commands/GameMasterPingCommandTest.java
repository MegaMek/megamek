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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@code /gmping}: only the Game Master may limit pings to themselves, and every player is told and updated.
 */
class GameMasterPingCommandTest {

    private static final int GAME_MASTER_CONNECTION = 0;
    private static final int PLAYER_CONNECTION = 1;

    private Server server;
    private TWGameManager gameManager;
    private GameMasterPingCommand command;
    private Player gameMaster;
    private Player player;

    @BeforeEach
    void setUp() {
        Game game = new Game();
        gameMaster = new Player(GAME_MASTER_CONNECTION, "Referee");
        gameMaster.setGameMaster(true);
        game.addPlayer(GAME_MASTER_CONNECTION, gameMaster);
        player = new Player(PLAYER_CONNECTION, "Clan Wolf");
        game.addPlayer(PLAYER_CONNECTION, player);

        gameManager = mock(TWGameManager.class);
        server = mock(Server.class);
        when(server.getGame()).thenReturn(game);
        when(server.getGameManager()).thenReturn(gameManager);
        command = new GameMasterPingCommand(server);
    }

    private void runAs(int connection, String commandText) {
        command.run(connection, commandText.substring(1).split(" "));
    }

    @Test
    void theGameMasterCanLimitPingsAndEveryoneIsTold() {
        runAs(GAME_MASTER_CONNECTION, GameMasterPingCommand.commandText(true));

        assertTrue(gameMaster.isPingGameMasterOnly());
        verify(gameManager).transmitAllPlayerUpdates();
        verify(server).sendServerChat(Messages.getString("GameMasterPingCommand.limited"));
    }

    @Test
    void theGameMasterCanLiftTheLimit() {
        gameMaster.setPingGameMasterOnly(true);

        runAs(GAME_MASTER_CONNECTION, GameMasterPingCommand.commandText(false));

        assertFalse(gameMaster.isPingGameMasterOnly());
        verify(server).sendServerChat(Messages.getString("GameMasterPingCommand.lifted"));
    }

    @Test
    void withoutAnArgumentTheLimitIsSwitched() {
        runAs(GAME_MASTER_CONNECTION, "/gmping");
        assertTrue(gameMaster.isPingGameMasterOnly());

        runAs(GAME_MASTER_CONNECTION, "/gmping");
        assertFalse(gameMaster.isPingGameMasterOnly());
    }

    @Test
    void aPlayerWhoIsNotTheGameMasterCannotLimitPings() {
        runAs(PLAYER_CONNECTION, GameMasterPingCommand.commandText(true));

        assertFalse(player.isPingGameMasterOnly());
        verify(gameManager, never()).transmitAllPlayerUpdates();
        verify(server).sendServerChat(PLAYER_CONNECTION, Messages.getString("GameMasterPingCommand.notGameMaster"));
    }

    @Test
    void theLimitTravelsInThePlayerCopySentToOtherClients() {
        gameMaster.setPingGameMasterOnly(true);

        assertTrue(gameMaster.copy().isPingGameMasterOnly());
    }

    @Test
    void theMenuBuildsTheSameCommandAGameMasterWouldType() {
        assertEquals("/gmping on", GameMasterPingCommand.commandText(true));
        assertEquals("/gmping off", GameMasterPingCommand.commandText(false));
    }
}
