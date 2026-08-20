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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Regression tests for ordered packet admission and the server's blocking packet pump. */
@Timeout(5)
class ServerPacketPumpTest {

    private static final int CONNECTION_ID = 0;

    private IGameManager gameManager;
    private Server server;

    @BeforeEach
    void setUp() throws IOException {
        Game game = new Game();
        game.addPlayer(CONNECTION_ID, new Player(CONNECTION_ID, "Test Player"));

        gameManager = mock(IGameManager.class);
        when(gameManager.getGame()).thenReturn(game);
        when(gameManager.getCommandList(any())).thenReturn(List.of());
        server = new Server(null, 0, gameManager);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.die();
        }
    }

    @Test
    void idlePumpProcessesASinglePacketWithoutAnotherEnqueue() throws InterruptedException {
        CountDownLatch handled = new CountDownLatch(1);
        doAnswer(invocation -> {
            handled.countDown();
            return null;
        }).when(gameManager).handlePacket(eq(CONNECTION_ID), any());

        server.receivePacket(CONNECTION_ID, packet(1));

        assertTrue(handled.await(1, TimeUnit.SECONDS));
    }

    @Test
    void pumpProcessesPacketsExactlyOnceInFifoOrder() throws InterruptedException {
        int packetCount = 5;
        CountDownLatch handled = new CountDownLatch(packetCount);
        List<Integer> values = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            Packet packet = invocation.getArgument(1);
            values.add(packet.getIntValue(0));
            handled.countDown();
            return null;
        }).when(gameManager).handlePacket(eq(CONNECTION_ID), any());

        for (int value = 0; value < packetCount; value++) {
            server.receivePacket(CONNECTION_ID, packet(value));
        }

        assertTrue(handled.await(1, TimeUnit.SECONDS));
        assertEquals(List.of(0, 1, 2, 3, 4), values);
    }

    @Test
    void pausedPacketsAreRetainedAndDrainedBeforeLaterPackets() throws InterruptedException, IOException {
        CountDownLatch pauseHandled = new CountDownLatch(1);
        CountDownLatch handled = new CountDownLatch(3);
        List<Integer> values = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            Packet packet = invocation.getArgument(1);
            values.add(packet.getIntValue(0));
            handled.countDown();
            return null;
        }).when(gameManager).handlePacket(eq(CONNECTION_ID), any());
        Server orderingServer = new Server(null, 0, gameManager) {
            @Override
            public void sendServerChat(String message) {
                if ("Game is paused.".equals(message)) {
                    pauseHandled.countDown();
                }
            }
        };
        server.die();
        server = orderingServer;

        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.PAUSE));
        assertTrue(pauseHandled.await(1, TimeUnit.SECONDS));
        server.receivePacket(CONNECTION_ID, packet(1));
        server.receivePacket(CONNECTION_ID, packet(2));

        assertFalse(handled.await(100, TimeUnit.MILLISECONDS));
        assertEquals(List.of(), values);
        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.UNPAUSE));
        server.receivePacket(CONNECTION_ID, packet(3));

        assertTrue(handled.await(1, TimeUnit.SECONDS));
        assertEquals(List.of(1, 2, 3), values);
    }

    @Test
    void repeatedPauseAndUnpauseDoesNotDuplicatePackets() throws InterruptedException {
        CountDownLatch handled = new CountDownLatch(1);
        List<Integer> values = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            Packet packet = invocation.getArgument(1);
            values.add(packet.getIntValue(0));
            handled.countDown();
            return null;
        }).when(gameManager).handlePacket(eq(CONNECTION_ID), any());

        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.PAUSE));
        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.PAUSE));
        server.receivePacket(CONNECTION_ID, packet(7));
        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.UNPAUSE));
        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.UNPAUSE));

        assertTrue(handled.await(1, TimeUnit.SECONDS));
        assertEquals(List.of(7), values);
    }

    @Test
    void cfrResponsesBypassPauseAndNormalPacketDispatch() throws Exception {
        CountDownLatch pauseHandled = new CountDownLatch(1);
        Server orderingServer = new Server(null, 0, gameManager) {
            @Override
            public void sendServerChat(String message) {
                if ("Game is paused.".equals(message)) {
                    pauseHandled.countDown();
                }
            }
        };
        server.die();
        server = orderingServer;
        Packet cfrPacket = new Packet(PacketCommand.CLIENT_FEEDBACK_REQUEST,
              PacketCommand.CFR_HIDDEN_PBS);
        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.PAUSE));
        assertTrue(pauseHandled.await(1, TimeUnit.SECONDS));

        server.receivePacket(CONNECTION_ID, cfrPacket);

        verify(gameManager).handleCfrPacket(any(Server.ReceivedPacket.class));
        verify(gameManager, never()).handlePacket(eq(CONNECTION_ID), eq(cfrPacket));
    }

    @Test
    void pauseAnnouncementWaitsForPreviouslyQueuedGameplay() throws InterruptedException, IOException {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondHandled = new CountDownLatch(1);
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            Packet packet = invocation.getArgument(1);
            int value = packet.getIntValue(0);
            if (value == 1) {
                firstStarted.countDown();
                if (!releaseFirst.await(1, TimeUnit.SECONDS)) {
                    return null;
                }
            }
            events.add("packet-" + value);
            if (value == 2) {
                secondHandled.countDown();
            }
            return null;
        }).when(gameManager).handlePacket(eq(CONNECTION_ID), any());
        Server orderingServer = new Server(null, 0, gameManager) {
            @Override
            public void sendServerChat(String message) {
                events.add(message);
            }
        };
        server.die();
        server = orderingServer;

        server.receivePacket(CONNECTION_ID, packet(1));
        assertTrue(firstStarted.await(1, TimeUnit.SECONDS));
        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.PAUSE));
        server.receivePacket(CONNECTION_ID, packet(2));
        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.UNPAUSE));
        releaseFirst.countDown();

        assertTrue(secondHandled.await(1, TimeUnit.SECONDS));
        assertEquals(List.of("packet-1", "Game is paused.", "Game is resumed.", "packet-2"), events);
    }

    @Test
    void handlerExceptionDoesNotStopPacketPump() throws InterruptedException {
        Packet failingPacket = packet(1);
        Packet succeedingPacket = packet(2);
        CountDownLatch handledAfterFailure = new CountDownLatch(1);
        doThrow(new IllegalStateException("test failure"))
              .when(gameManager).handlePacket(CONNECTION_ID, failingPacket);
        doAnswer(invocation -> {
            handledAfterFailure.countDown();
            return null;
        }).when(gameManager).handlePacket(CONNECTION_ID, succeedingPacket);

        server.receivePacket(CONNECTION_ID, failingPacket);
        server.receivePacket(CONNECTION_ID, succeedingPacket);

        assertTrue(handledAfterFailure.await(1, TimeUnit.SECONDS));
    }

    @Test
    void chatAndGameplayShareReceiveOrder() throws Exception {
        CountDownLatch handled = new CountDownLatch(3);
        List<PacketCommand> commands = Collections.synchronizedList(new ArrayList<>());
        Server orderingServer = new Server(null, 0, gameManager) {
            @Override
            protected void handle(int connId, Packet packet) {
                commands.add(packet.command());
                handled.countDown();
            }
        };
        server.die();
        server = orderingServer;

        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.CHAT, "first"));
        server.receivePacket(CONNECTION_ID, packet(1));
        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.CHAT, "last"));

        assertTrue(handled.await(1, TimeUnit.SECONDS));
        assertEquals(List.of(PacketCommand.CHAT, PacketCommand.LOBBY_GENERATE_BOARD, PacketCommand.CHAT), commands);
    }

    @Test
    void concurrentProducersDoNotLoseOrDuplicatePackets() throws Exception {
        int packetsPerProducer = 20;
        CountDownLatch handled = new CountDownLatch((packetsPerProducer * 2) + 1);
        List<Integer> values = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            Packet packet = invocation.getArgument(1);
            values.add(packet.getIntValue(0));
            handled.countDown();
            return null;
        }).when(gameManager).handlePacket(eq(CONNECTION_ID), any());

        Thread first = new Thread(() -> enqueueRange(0, packetsPerProducer), "first producer");
        Thread second = new Thread(() -> enqueueRange(100, packetsPerProducer), "second producer");
        first.start();
        second.start();
        first.join();
        second.join();
        server.receivePacket(CONNECTION_ID, packet(999));

        assertTrue(handled.await(1, TimeUnit.SECONDS));
        assertEquals((packetsPerProducer * 2) + 1, values.size());
        assertEquals((packetsPerProducer * 2) + 1, new HashSet<>(values).size());
        assertTrue(isOrderedSubsequence(values, 0, packetsPerProducer));
        assertTrue(isOrderedSubsequence(values, 100, packetsPerProducer));
        assertEquals(999, values.get(values.size() - 1));
    }

    @Test
    void enqueueDuringUnpauseDoesNotLoseOrDuplicatePackets() throws Exception {
        CountDownLatch pauseHandled = new CountDownLatch(1);
        CountDownLatch handled = new CountDownLatch(2);
        List<Integer> values = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            Packet packet = invocation.getArgument(1);
            values.add(packet.getIntValue(0));
            handled.countDown();
            return null;
        }).when(gameManager).handlePacket(eq(CONNECTION_ID), any());
        Server orderingServer = new Server(null, 0, gameManager) {
            @Override
            public void sendServerChat(String message) {
                if ("Game is paused.".equals(message)) {
                    pauseHandled.countDown();
                }
            }
        };
        server.die();
        server = orderingServer;
        server.receivePacket(CONNECTION_ID, new Packet(PacketCommand.PAUSE));
        assertTrue(pauseHandled.await(1, TimeUnit.SECONDS));
        server.receivePacket(CONNECTION_ID, packet(1));

        CountDownLatch start = new CountDownLatch(1);
        Thread unpause = new Thread(() -> awaitAndReceive(start, new Packet(PacketCommand.UNPAUSE)));
        Thread producer = new Thread(() -> awaitAndReceive(start, packet(2)));
        unpause.start();
        producer.start();
        start.countDown();
        unpause.join();
        producer.join();

        assertTrue(handled.await(1, TimeUnit.SECONDS));
        assertEquals(List.of(1, 2), values);
    }

    @Test
    void shutdownStopsPacketDispatch() throws Exception {
        Server stoppedServer = server;
        stoppedServer.die();
        server = null;
        stoppedServer.receivePacket(CONNECTION_ID, packet(1));

        Thread.sleep(100);
        verify(gameManager, never()).handlePacket(eq(CONNECTION_ID), any());
    }

    private void enqueueRange(int start, int count) {
        for (int value = start; value < start + count; value++) {
            server.receivePacket(CONNECTION_ID, packet(value));
        }
    }

    private void awaitAndReceive(CountDownLatch start, Packet packet) {
        try {
            assertTrue(start.await(1, TimeUnit.SECONDS));
            server.receivePacket(CONNECTION_ID, packet);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private boolean isOrderedSubsequence(List<Integer> values, int start, int count) {
        int previousIndex = -1;
        for (int value = start; value < start + count; value++) {
            int index = values.indexOf(value);
            if (index <= previousIndex) {
                return false;
            }
            previousIndex = index;
        }
        return true;
    }

    private Packet packet(int value) {
        return new Packet(PacketCommand.LOBBY_GENERATE_BOARD, value);
    }
}
