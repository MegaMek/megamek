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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that a packet handler which throws cannot take down the server thread that dispatched the packet.
 * {@link Server#handle(int, Packet)} runs on the packet pump thread and on the connection receive thread; an
 * exception escaping it would end the pump loop, after which the server ignores every further packet from every
 * client without any indication that it has stopped listening.
 */
class ServerPacketHandlingTest {

    private static final int TEST_CONNECTION_ID = 0;

    private Server server;
    private TWGameManager gameManager;

    @BeforeEach
    void setUp() throws IOException {
        gameManager = spy(new TWGameManager());
        Game game = new Game();
        Player player = new Player(TEST_CONNECTION_ID, "Test Player");
        game.addPlayer(TEST_CONNECTION_ID, player);
        gameManager.setGame(game);

        // Port 0 binds an ephemeral port, so the test never collides with a running server
        server = new Server(null, 0, gameManager);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.die();
        }
    }

    @Test
    void anExceptionFromAPacketHandlerDoesNotEscapeTheDispatchingThread() {
        doThrow(new IllegalArgumentException("board is the wrong size, expected 2x2, got 0x0"))
              .when(gameManager).handlePacket(anyInt(), any());

        assertDoesNotThrow(() -> server.handle(TEST_CONNECTION_ID,
              new Packet(PacketCommand.LOBBY_GENERATE_BOARD)));
    }

    @Test
    void theServerKeepsHandlingPacketsAfterAHandlerThrew() {
        doThrow(new IllegalStateException("first packet fails"))
              .doNothing()
              .when(gameManager).handlePacket(anyInt(), any());

        server.handle(TEST_CONNECTION_ID, new Packet(PacketCommand.LOBBY_GENERATE_BOARD));

        // The dispatching thread survived the first packet, so the next one is still processed
        assertDoesNotThrow(() -> server.handle(TEST_CONNECTION_ID,
              new Packet(PacketCommand.LOBBY_GENERATE_BOARD)));

        // Not throwing is not enough on its own: an early return inside handle() would be just as silent.
        // Assert the second packet actually reached the handler.
        verify(gameManager, times(2)).handlePacket(anyInt(), any());
    }
}
