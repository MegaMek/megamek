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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.BipedMek;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the radio relay: a bot's call about its unit reaches its own side as a toast and a chat line, never the enemy.
 */
class RadioCommandTest {

    private static final int TEAMMATE_CONNECTION = 2;
    private static final int OPPONENT_CONNECTION = 3;
    private static final int BOT_CONNECTION = 4;
    private static final int ALLIED_TEAM = 1;
    private static final int ENEMY_TEAM = 2;
    private static final int BOT_UNIT_ID = 10;
    private static final String CALL = "Command Two: Set at 1504, holding.";

    private RadioCommand command;
    private TWGameManager gameManager;
    private Server server;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        Game game = new Game();
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

        BipedMek botUnit = new BipedMek();
        botUnit.setId(BOT_UNIT_ID);
        botUnit.setOwner(bot);
        game.addEntity(botUnit);

        gameManager = mock(TWGameManager.class);
        when(gameManager.getGame()).thenReturn(game);
        server = mock(Server.class);
        when(server.getGameManager()).thenReturn(gameManager);
        command = new RadioCommand(server, gameManager);
    }

    private void runAs(int connection, String commandText) {
        command.run(connection, commandText.substring(1).split(" "));
    }

    private static boolean isToastOf(Packet packet, String message) {
        return (packet.command() == PacketCommand.SEND_TOAST) && message.equals(packet.getObject(1))
              && Integer.valueOf(BOT_UNIT_ID).equals(packet.getObject(2));
    }

    @Test
    void aBotsCallReachesItsOwnSideAsAToastAndAChatLine() {
        runAs(BOT_CONNECTION, RadioCommand.commandText(BOT_UNIT_ID, CALL));

        verify(gameManager).send(eq(TEAMMATE_CONNECTION), argThat(packet -> isToastOf(packet, CALL)));
        verify(gameManager).send(eq(BOT_CONNECTION), argThat(packet -> isToastOf(packet, CALL)));
        verify(server).sendChat(TEAMMATE_CONNECTION, "Radio[Lyran Allies]", CALL);
    }

    @Test
    void theEnemyNeverHearsTheCall() {
        runAs(BOT_CONNECTION, RadioCommand.commandText(BOT_UNIT_ID, CALL));

        verify(gameManager, never()).send(eq(OPPONENT_CONNECTION), any(Packet.class));
        verify(server, never()).sendChat(eq(OPPONENT_CONNECTION), anyString(), anyString());
    }

    @Test
    void aPlayerCannotMakeACallAboutAUnitTheyDoNotOwn() {
        runAs(TEAMMATE_CONNECTION, RadioCommand.commandText(BOT_UNIT_ID, CALL));

        verify(gameManager, never()).send(anyInt(), any(Packet.class));
    }
}
