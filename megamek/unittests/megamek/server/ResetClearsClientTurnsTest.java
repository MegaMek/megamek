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

package megamek.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Resetting a game to the lobby from a report phase left every client holding the last turn of the old game, often
 * another player's. The lobby's Done button refuses to act while another player's turn is pending, so the human
 * player could not start the next game. These tests reset the server and feed the turn list it sends into a client's
 * copy of the game, the way a client receives it.
 */
class ResetClearsClientTurnsTest {

    private static final int HUMAN_PLAYER_ID = 0;
    private static final int BOT_PLAYER_ID = 1;

    private MockedStatic<Server> mockedServer;
    private Server server;
    private TWGameManager gameManager;
    private Game clientGame;

    @BeforeEach
    void beforeEach() {
        // every packet leaves through the server, whichever helper class sends it, so it is recorded there
        server = mock(Server.class);
        mockedServer = mockStatic(Server.class);
        mockedServer.when(Server::getServerInstance).thenReturn(server);

        gameManager = new TWGameManager();

        Game serverGame = gameManager.getGame();
        serverGame.addPlayer(HUMAN_PLAYER_ID, new Player(HUMAN_PLAYER_ID, "Defenders"));
        serverGame.addPlayer(BOT_PLAYER_ID, new Player(BOT_PLAYER_ID, "Bot"));
        serverGame.setPhase(GamePhase.INITIATIVE_REPORT);

        // the client still holds the deployment turns it was last sent; report phases send no turn list
        clientGame = new Game();
        clientGame.addPlayer(HUMAN_PLAYER_ID, new Player(HUMAN_PLAYER_ID, "Defenders"));
        clientGame.addPlayer(BOT_PLAYER_ID, new Player(BOT_PLAYER_ID, "Bot"));
        clientGame.setTurnVector(List.of(new GameTurn(HUMAN_PLAYER_ID), new GameTurn(BOT_PLAYER_ID)));
        clientGame.setTurnIndex(1, HUMAN_PLAYER_ID);
    }

    @AfterEach
    void afterEach() {
        mockedServer.close();
    }

    /** @return every turn list packet the server sent to all clients, in the order sent */
    private List<Packet> sentTurnListPackets() {
        ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
        Mockito.verify(server, Mockito.atLeast(0)).send(packetCaptor.capture());
        List<Packet> turnListPackets = new ArrayList<>();
        for (Packet packet : packetCaptor.getAllValues()) {
            if (packet.command() == PacketCommand.SENDING_TURNS) {
                turnListPackets.add(packet);
            }
        }
        return turnListPackets;
    }

    @Test
    @DisplayName("Before the reset, the client is waiting on the bot's leftover turn")
    void clientStartsWithTheBotsLeftoverTurn() {
        GameTurn pendingTurn = clientGame.getTurn();

        assertNotNull(pendingTurn, "the client should start out holding the old game's turn");
        assertEquals(BOT_PLAYER_ID, pendingTurn.playerId(), "the leftover turn should be the bot's");
    }

    @Test
    @DisplayName("Resetting to the lobby sends the clients an empty turn list")
    void resetSendsAnEmptyTurnList() throws InvalidPacketDataException {
        gameManager.resetGame();

        List<Packet> turnListPackets = sentTurnListPackets();
        assertFalse(turnListPackets.isEmpty(), "the reset should send the clients a turn list");
        assertTrue(turnListPackets.getLast().getGameTurnList(0).isEmpty(),
              "the last turn list sent on entering the lobby should be empty");
    }

    @Test
    @DisplayName("After the reset, the client has no pending turn, so the lobby's Done button can act")
    void clientHasNoPendingTurnAfterReset() throws InvalidPacketDataException {
        gameManager.resetGame();

        Packet lastTurnList = sentTurnListPackets().getLast();
        clientGame.setTurnVector(lastTurnList.getGameTurnList(0));

        assertNull(clientGame.getTurn(), "the client should no longer be waiting on any turn in the lobby");
        assertTrue(gameManager.getGame().getPhase().isLounge(), "the server should be back in the lobby");
    }
}
