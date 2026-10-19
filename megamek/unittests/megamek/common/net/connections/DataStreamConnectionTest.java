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
package megamek.common.net.connections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #8668: a connection whose send fails must not fire its disconnect listeners while still
 * holding its own lock, because the server's listener takes the game lock and another thread may hold the game lock
 * while broadcasting to this same connection.
 */
class DataStreamConnectionTest {

    private static final long DEADLOCK_TIMEOUT_SECONDS = 10;

    /** An output stream that accepts every write and runs the given action on flush. */
    private static final class FlushHookStream extends OutputStream {
        private final FlushAction flushAction;

        FlushHookStream(FlushAction flushAction) {
            this.flushAction = flushAction;
        }

        @Override
        public void write(int value) {
            // Discard the data, only the flush matters to these tests
        }

        @Override
        public void flush() throws IOException {
            flushAction.run();
        }
    }

    @FunctionalInterface
    private interface FlushAction {
        void run() throws IOException;
    }

    private static DataStreamConnection connectionWithStream(OutputStream stream) throws IOException {
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(stream);
        when(socket.getSendBufferSize()).thenReturn(1024);
        return new DataStreamConnection(socket, 1);
    }

    @Test
    void sendFailureFiresDisconnectWithoutHoldingConnectionLock() throws IOException {
        DataStreamConnection connection = connectionWithStream(new FlushHookStream(() -> {
            throw new SocketException("Broken pipe");
        }));
        AtomicInteger disconnectCount = new AtomicInteger();
        AtomicBoolean heldLockDuringDisconnect = new AtomicBoolean();
        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                disconnectCount.incrementAndGet();
                heldLockDuringDisconnect.set(Thread.holdsLock(event.getConnection()));
            }
        });

        connection.send(new Packet(PacketCommand.SERVER_GREETING));

        assertEquals(1, disconnectCount.get(), "A failed send should close the connection once");
        assertFalse(heldLockDuringDisconnect.get(),
              "The disconnect listener must not run while the connection's lock is held");
    }

    @Test
    void flushFailureFiresDisconnectWithoutHoldingConnectionLock() throws IOException {
        AtomicBoolean failFlush = new AtomicBoolean(false);
        DataStreamConnection connection = connectionWithStream(new FlushHookStream(() -> {
            if (failFlush.get()) {
                throw new SocketException("Broken pipe");
            }
        }));
        AtomicBoolean heldLockDuringDisconnect = new AtomicBoolean();
        AtomicInteger disconnectCount = new AtomicInteger();
        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                disconnectCount.incrementAndGet();
                heldLockDuringDisconnect.set(Thread.holdsLock(event.getConnection()));
            }
        });
        // Open the output stream with a good send, then break it
        connection.send(new Packet(PacketCommand.SERVER_GREETING));
        failFlush.set(true);

        connection.flush();

        assertEquals(1, disconnectCount.get(), "A failed flush should close the connection once");
        assertFalse(heldLockDuringDisconnect.get(),
              "The disconnect listener must not run while the connection's lock is held");
    }

    /**
     * Reproduces the cycle captured in the issue's jstack dumps. The shutdown thread sends to a connection whose stream
     * has broken, and its disconnect listener needs the game lock. Meanwhile, a second thread holds the game lock and
     * broadcasts to the same connection. Before the fix, both threads block forever.
     */
    @Test
    void brokenSendDoesNotDeadlockAgainstGameLockBroadcast() throws Exception {
        ReentrantLock gameLock = new ReentrantLock();
        CountDownLatch shutdownThreadInSend = new CountDownLatch(1);
        CountDownLatch gameLockHeld = new CountDownLatch(1);

        DataStreamConnection connection = connectionWithStream(new FlushHookStream(() -> {
            shutdownThreadInSend.countDown();
            try {
                gameLockHeld.await(DEADLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            throw new SocketException("Broken pipe");
        }));
        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                // Stands in for Server.disconnected(Player), which takes the game lock
                gameLock.lock();
                gameLock.unlock();
            }
        });

        Thread shutdownThread = new Thread(() -> connection.send(new Packet(PacketCommand.SERVER_GREETING)),
              "shutdown-sender");
        Thread connectionThread = new Thread(() -> {
            try {
                shutdownThreadInSend.await(DEADLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            // Stands in for a connection thread handling a disconnect: game lock held, then a broadcast
            gameLock.lock();
            try {
                gameLockHeld.countDown();
                connection.send(new Packet(PacketCommand.CHAT));
            } finally {
                gameLock.unlock();
            }
        }, "game-lock-broadcaster");
        // Daemon, so a regression leaves the test failing rather than the JVM hung
        shutdownThread.setDaemon(true);
        connectionThread.setDaemon(true);

        shutdownThread.start();
        connectionThread.start();
        shutdownThread.join(TimeUnit.SECONDS.toMillis(DEADLOCK_TIMEOUT_SECONDS));
        connectionThread.join(TimeUnit.SECONDS.toMillis(DEADLOCK_TIMEOUT_SECONDS));

        assertFalse(shutdownThread.isAlive() || connectionThread.isAlive(),
              "Sender and broadcaster deadlocked on the connection lock and the game lock");
        assertTrue(connection.isClosed(), "The broken connection should end up closed");
    }
}
