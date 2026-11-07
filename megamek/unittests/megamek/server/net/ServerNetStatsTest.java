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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import megamek.MMConstants;
import megamek.common.enums.GamePhase;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.net.stats.NetStats;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The server's own {@link NetStats}: accepted connections report into it, the packet pump records what it dispatches,
 * {@code /netstats} reads it back over chat, and the phase-change summary runs without touching game state.
 */
class ServerNetStatsTest {

    private TWGameManager gameManager;
    private Server server;

    @BeforeEach
    void setUp() throws IOException {
        TWGameManager realManager = new TWGameManager();
        realManager.getGame().setPhase(GamePhase.LOUNGE);
        gameManager = spy(realManager);
        doNothing().when(gameManager).handlePacket(anyInt(), any());
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
    void theServerHasItsOwnRegistry() {
        assertNotSame(NetStats.defaultInstance(), server.getNetStats());
    }

    @Test
    void acceptedConnectionsReportIntoTheServersRegistry() throws Exception {
        NetStats stats = server.getNetStats();
        long versionChecksBefore = stats.sent(PacketCommand.SERVER_VERSION_CHECK);
        try (RawPacketClient client = connect()) {
            client.handshake("Alice");

            assertTrue(stats.sent(PacketCommand.SERVER_VERSION_CHECK) > versionChecksBefore);
            assertEquals(1, stats.received(PacketCommand.CLIENT_VERSIONS));
            assertEquals(1, stats.received(PacketCommand.CLIENT_NAME));
            assertEquals(1, stats.sent(PacketCommand.SERVER_GREETING));
            assertEquals(1, stats.sent(PacketCommand.LOCAL_PN));
            assertTrue(stats.wireBytesSent(PacketCommand.LOCAL_PN) > 0);
            assertEquals(0, stats.sent(PacketCommand.CLIENT_NAME), "the server never sends client commands");
        }
    }

    @Test
    void thePumpRecordsWhatItDispatches() throws Exception {
        NetStats stats = server.getNetStats();
        try (RawPacketClient client = connect()) {
            client.handshake("Alice");
            assertEquals(0, stats.handled(PacketCommand.PLAYER_READY));

            client.send(new Packet(PacketCommand.PLAYER_READY, true));

            InProcessHarness.await(() -> stats.handled(PacketCommand.PLAYER_READY) == 1,
                  "the pump to dispatch PLAYER_READY", 10, TimeUnit.SECONDS);
            assertTrue(stats.pumpDepthMax() >= 1, "the packet was seen in the queue");
            assertEquals(0, stats.handled(PacketCommand.CHAT), "immediate commands bypass the pump");
        }
    }

    @Test
    void netstatsCommandAnswersOverChat() throws Exception {
        try (RawPacketClient client = connect()) {
            client.handshake("Alice");

            client.send(new Packet(PacketCommand.CHAT, "/netstats"));

            List<String> lines = chatLinesUntil(client, line -> line.startsWith("pump:"));
            assertTrue(lines.stream().anyMatch(line -> line.startsWith("command")), lines.toString());
            // The lobby state sent on join dwarfs the handshake packets, so those are what the top rows show
            assertTrue(lines.stream().anyMatch(line -> line.startsWith("PLAYER_ADD")), lines.toString());
            assertTrue(lines.stream().anyMatch(line -> line.startsWith("CHAT ")), "the /netstats chat itself counts");
            assertTrue(lines.stream().anyMatch(line -> line.startsWith("other")), "small commands fold into other");
            assertTrue(lines.stream().anyMatch(line -> line.startsWith("total:")), lines.toString());
        }
    }

    @Test
    void netstatsConnListsThePlayers() throws Exception {
        try (RawPacketClient client = connect()) {
            client.handshake("Alice");

            client.send(new Packet(PacketCommand.CHAT, "/netstats conn"));

            List<String> lines = chatLinesUntil(client, line -> line.contains("Alice"));
            assertTrue(lines.stream().anyMatch(line -> line.startsWith("id")), lines.toString());
        }
    }

    @Test
    void thePhaseSummaryRunsOnAnyThreadWithoutTouchingTheGame() {
        // Exercised for real by every changePhase; here just prove it is safe to call cold and repeatedly
        server.logNetStatsSummary(GamePhase.LOUNGE, GamePhase.EXCHANGE);
        server.logNetStatsSummary(GamePhase.EXCHANGE, GamePhase.INITIATIVE);
        assertEquals(GamePhase.LOUNGE, server.getGame().getPhase(), "the summary does not change the phase");
    }

    private static List<String> chatLinesUntil(RawPacketClient client, java.util.function.Predicate<String> last)
          throws Exception {
        List<String> lines = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Packet packet = client.poll(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (packet == null) {
                break;
            }
            if (packet.command() == PacketCommand.CHAT) {
                // Server chat arrives as "***Server: <text>"; keep only the text
                String line = packet.getStringValue(0);
                String prefix = Server.ORIGIN + ": ";
                if (line.startsWith(prefix)) {
                    line = line.substring(prefix.length());
                }
                lines.add(line);
                if (last.test(line)) {
                    return lines;
                }
            }
        }
        throw new AssertionError("Expected chat line never arrived; saw " + lines);
    }
}
