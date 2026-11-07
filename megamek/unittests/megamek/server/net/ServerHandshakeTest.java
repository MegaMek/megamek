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
package megamek.server.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import megamek.MMConstants;
import megamek.SuiteConstants;
import megamek.Version;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for the server side of the connection handshake and for how received packets are routed:
 * which ones the server answers immediately on the connection's receive thread and which ones go through the
 * packet pump. A {@link RawPacketClient} drives the handshake one packet at a time so each reply can be checked.
 */
class ServerHandshakeTest {

    private TWGameManager gameManager;
    private Game game;
    private Server server;

    /** The name of the thread that delivered each packet to the game manager, in arrival order. */
    private final LinkedBlockingQueue<String> handlerThreads = new LinkedBlockingQueue<>();

    @BeforeEach
    void setUp() throws IOException {
        TWGameManager realManager = new TWGameManager();
        // The manager's own game is a fully initialised lobby. Every test joins it in the LOUNGE phase: outside the
        // lounge a player with no units is made an observer on arrival, which changes what a disconnect does.
        game = realManager.getGame();
        game.setPhase(GamePhase.LOUNGE);
        // Spy after set-up so the manager's helpers, which keep a reference to the original instance, share its Game
        gameManager = spy(realManager);
        // Game logic is not under test: record who called and on which thread, then do nothing
        doAnswer(invocation -> {
            handlerThreads.add(Thread.currentThread().getName());
            return null;
        }).when(gameManager).handlePacket(anyInt(), any());
        server = ServerFactory.createServer(gameManager);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.die();
        }
    }

    private RawPacketClient connect() {
        return new RawPacketClient(MMConstants.LOCALHOST_IP, server.getPort());
    }

    @Test
    void theServerOpensWithAVersionCheck() throws Exception {
        try (RawPacketClient client = connect()) {
            assertEquals(PacketCommand.SERVER_VERSION_CHECK, client.next().command());
        }
    }

    @Test
    void aMatchingVersionIsGreeted() throws Exception {
        try (RawPacketClient client = connect()) {
            client.next();
            client.send(new Packet(PacketCommand.CLIENT_VERSIONS, SuiteConstants.VERSION));

            assertEquals(PacketCommand.SERVER_GREETING, client.next().command());
            assertFalse(client.isClosed());
        }
    }

    @Test
    void aMismatchedVersionIsToldWhyAndDropped() throws Exception {
        try (RawPacketClient client = connect()) {
            client.next();
            client.send(new Packet(PacketCommand.CLIENT_VERSIONS, new Version("0.1.0")));

            Packet reply = client.next();
            assertEquals(PacketCommand.ILLEGAL_CLIENT_VERSION, reply.command());
            assertEquals(SuiteConstants.VERSION, reply.getObject(0), "the server reports its own version");
            assertTrue(client.awaitDisconnect(10, TimeUnit.SECONDS), "the server closes the connection");
        }
    }

    @Test
    void versionsAreComparedForExactEquality() throws Exception {
        // A fork protocol version does not exist yet: the only compatibility check is equality of the suite version.
        // One patch level off is enough to be rejected.
        Version offByAPatch = new Version(SuiteConstants.VERSION.getMajor(), SuiteConstants.VERSION.getMinor(),
              SuiteConstants.VERSION.getPatch() + 1);
        try (RawPacketClient client = connect()) {
            client.next();
            client.send(new Packet(PacketCommand.CLIENT_VERSIONS, offByAPatch));

            assertEquals(PacketCommand.ILLEGAL_CLIENT_VERSION, client.next().command());
        }
    }

    @Test
    void aNewNameIsConfirmedThenGivenAPlayerNumber() throws Exception {
        try (RawPacketClient client = connect()) {
            client.next();
            client.send(new Packet(PacketCommand.CLIENT_VERSIONS, SuiteConstants.VERSION));
            client.next();
            client.send(new Packet(PacketCommand.CLIENT_NAME, "Alice", false));

            List<Packet> replies = client.drainUntil(PacketCommand.LOCAL_PN);
            List<PacketCommand> commands = RawPacketClient.commandsOf(replies);

            Packet correctName = replies.get(commands.indexOf(PacketCommand.SERVER_CORRECT_NAME));
            assertEquals("Alice", correctName.getStringValue(0), "an unused name is echoed back unchanged");
            int localPn = replies.get(replies.size() - 1).getIntValue(0);
            Player player = server.getPlayer(localPn);
            assertNotNull(player);
            assertEquals("Alice", player.getName());
            assertFalse(player.isGhost());
            assertTrue(commands.indexOf(PacketCommand.SERVER_CORRECT_NAME) < commands.indexOf(PacketCommand.LOCAL_PN),
                  "the name is corrected before the player number is assigned");
            assertTrue(commands.contains(PacketCommand.PLAYER_ADD), "the new player is announced");
        }
    }

    @Test
    void aDuplicateNameGetsANumericSuffix() throws Exception {
        game.addPlayer(0, new Player(0, "Alice"));
        try (RawPacketClient client = connect()) {
            client.next();
            client.send(new Packet(PacketCommand.CLIENT_VERSIONS, SuiteConstants.VERSION));
            client.next();
            client.send(new Packet(PacketCommand.CLIENT_NAME, "Alice", false));

            List<Packet> replies = client.drainUntil(PacketCommand.LOCAL_PN);
            Packet correctName = replies.get(RawPacketClient.commandsOf(replies)
                  .indexOf(PacketCommand.SERVER_CORRECT_NAME));
            assertEquals("Alice.2", correctName.getStringValue(0));
            int localPn = replies.get(replies.size() - 1).getIntValue(0);
            assertEquals("Alice.2", server.getPlayer(localPn).getName());
            assertEquals(2, game.getPlayersList().size());
        }
    }

    @Test
    void aNameMatchingAGhostPlayerTakesOverThatSeat() throws Exception {
        // This is the reconnect path that exists today: same name as a disconnected (ghost) player, same player id,
        // no name correction, followed by the full current-state resend.
        Player ghost = new Player(5, "Bob");
        ghost.setGhost(true);
        game.addPlayer(5, ghost);
        try (RawPacketClient client = connect()) {
            client.next();
            client.send(new Packet(PacketCommand.CLIENT_VERSIONS, SuiteConstants.VERSION));
            client.next();
            client.send(new Packet(PacketCommand.CLIENT_NAME, "Bob", false));

            List<Packet> replies = client.drainUntil(PacketCommand.LOCAL_PN);
            List<PacketCommand> commands = RawPacketClient.commandsOf(replies);
            assertFalse(commands.contains(PacketCommand.SERVER_CORRECT_NAME), "a returning player keeps the name");
            assertEquals(5, replies.get(replies.size() - 1).getIntValue(0), "and keeps the player id");
            assertFalse(ghost.isGhost());
            assertEquals(1, game.getPlayersList().size(), "no second player was created");
            assertNotNull(server.getConnection(5), "the connection was re-keyed to the player id");
        }
    }

    @Test
    void aNameSentBeforeTheVersionCheckIsAnsweredAnyway() throws Exception {
        // Nothing enforces handshake order: CLIENT_NAME from a pending connection is accepted even if the version
        // packet never came. Recorded because the lifecycle work will decide whether to keep that.
        try (RawPacketClient client = connect()) {
            client.next();
            client.send(new Packet(PacketCommand.CLIENT_NAME, "Eve", false));

            List<Packet> replies = client.drainUntil(PacketCommand.LOCAL_PN);
            assertEquals("Eve", server.getPlayer(replies.get(replies.size() - 1).getIntValue(0)).getName());
        }
    }

    @Test
    void aPendingConnectionThatNeverAnswersIsPolledByTheWatchdog() throws Exception {
        // The ConnectionWatchdog re-sends SERVER_VERSION_CHECK every 500 ms (and logs at ERROR each time) until the
        // client answers or two minutes pass. Two seconds of silence should show several repeats.
        try (RawPacketClient client = connect()) {
            int versionChecks = 0;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (System.nanoTime() < deadline) {
                Packet packet = client.poll(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (packet != null && packet.command() == PacketCommand.SERVER_VERSION_CHECK) {
                    versionChecks++;
                }
            }
            assertTrue(versionChecks >= 2, "expected repeated version checks, got " + versionChecks);
            assertFalse(client.isClosed(), "the watchdog does not give up this quickly");
        }
    }

    @Test
    void gamePacketsReachTheGameManagerOnThePacketPumpThread() throws Exception {
        try (RawPacketClient client = connect()) {
            client.handshake("Alice");

            client.send(new Packet(PacketCommand.PLAYER_READY, true));

            String thread = handlerThreads.poll(10, TimeUnit.SECONDS);
            assertEquals("Packet Pump", thread);
        }
    }

    @Test
    void chatIsAnsweredWithoutGoingThroughThePump() throws Exception {
        try (RawPacketClient client = connect()) {
            client.handshake("Alice");

            client.send(new Packet(PacketCommand.CHAT, "hello there"));

            Packet echo = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (echo == null && System.nanoTime() < deadline) {
                Packet packet = client.poll(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (packet != null && packet.command() == PacketCommand.CHAT
                      && packet.getStringValue(0).contains("hello there")) {
                    echo = packet;
                }
            }
            assertNotNull(echo, "chat is broadcast back to the sender");
            assertTrue(echo.getStringValue(0).startsWith("Alice"), "prefixed with the sender's name");
            assertNull(handlerThreads.poll(200, TimeUnit.MILLISECONDS), "chat never reaches the game manager");
        }
    }

    @Test
    void pauseHoldsGamePacketsUntilUnpause() throws Exception {
        try (RawPacketClient client = connect()) {
            client.handshake("Alice");

            client.send(new Packet(PacketCommand.PAUSE));
            client.send(new Packet(PacketCommand.PLAYER_READY, true));

            assertNull(handlerThreads.poll(500, TimeUnit.MILLISECONDS), "held while paused");

            client.send(new Packet(PacketCommand.UNPAUSE));

            assertEquals("Packet Pump", handlerThreads.poll(10, TimeUnit.SECONDS), "released on unpause");
        }
    }

    @Test
    void closeConnectionInTheLoungeRemovesThePlayerEntirely() throws Exception {
        // A seat is only kept as a ghost (the thing a reconnect latches onto) once the game is under way and the
        // player owns units. In the lobby the player is simply dropped, so a lobby drop has nothing to rejoin.
        try (RawPacketClient client = connect()) {
            int localPn = client.handshake("Alice");
            assertNotNull(server.getPlayer(localPn));

            client.send(new Packet(PacketCommand.CLOSE_CONNECTION));

            assertTrue(client.awaitDisconnect(10, TimeUnit.SECONDS));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (server.getPlayer(localPn) != null && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertNull(server.getConnection(localPn), "the connection is forgotten");
            assertNull(server.getPlayer(localPn), "and so is the player");
            assertTrue(game.getPlayersList().isEmpty());
        }
    }
}
