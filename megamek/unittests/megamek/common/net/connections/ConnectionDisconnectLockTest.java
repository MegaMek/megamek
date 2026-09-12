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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.marshalling.PacketMarshallerFactory;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConnectionDisconnectLockTest {
    private static final int TIMEOUT_SECONDS = 5;

    @ParameterizedTest
    @CsvSource({ "true, true", "true, false", "false, true", "false, false" })
    void streamFailureNotifiesOutsideConnectionAndOutputLocks(boolean immediate, boolean socketFailure)
          throws Exception {
        OutputStream output = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw socketFailure ? new SocketException("Disconnected test socket")
                      : new IOException("Failed test output");
            }
        };
        var connection = new DeferredStreamConnection(output, immediate);
        Field outputField = DataStreamConnection.class.getDeclaredField("out");
        outputField.setAccessible(true);
        Supplier<Object> outputMonitor = () -> {
            try {
                return outputField.get(connection);
            } catch (IllegalAccessException ex) {
                throw new AssertionError(ex);
            }
        };
        Runnable operation = () -> {
            connection.send(new Packet(PacketCommand.SERVER_GREETING, "test"));
            if (!immediate) {
                connection.flushEnabled = true;
                connection.flush();
            }
        };

        assertUnlockedDisconnect(connection, outputMonitor, operation);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void failedQueueProcessingNotifiesOutsideConnectionLock(boolean immediate) throws Exception {
        var connection = new FailedProcessorConnection(immediate);
        Runnable operation = () -> {
            connection.send(new Packet(PacketCommand.SERVER_GREETING));
            if (!immediate) {
                connection.flushEnabled = true;
                connection.flush();
            }
        };

        assertUnlockedDisconnect(connection, () -> null, operation);
    }

    @Test
    void drainingQueuedAndImmediatePacketsPreservesWireOrder() throws Exception {
        var output = new ByteArrayOutputStream();
        var connection = new DeferredStreamConnection(output, false);
        for (int index = 0; index < 12; index++) {
            connection.send(new Packet(PacketCommand.SERVER_GREETING, index));
        }
        assertTrue(connection.hasPending());
        assertEquals(0, output.size());

        connection.flushEnabled = true;
        connection.flush();
        connection.send(new Packet(PacketCommand.SERVER_GREETING, 12));
        assertFalse(connection.hasPending());
        try (var input = new DataInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            for (int index = 0; index < 13; index++) {
                boolean zipped = input.readBoolean();
                int encoding = input.readInt();
                byte[] payload = input.readNBytes(input.readInt());
                InputStream packetInput = new ByteArrayInputStream(payload);
                if (zipped) {
                    packetInput = new GZIPInputStream(packetInput);
                }
                try (InputStream decoded = packetInput) {
                    Packet packet = PacketMarshallerFactory.getInstance().getMarshaller(encoding).unmarshall(decoded);
                    assertEquals(PacketCommand.SERVER_GREETING, packet.command());
                    assertEquals(index, packet.getIntValue(0));
                }
            }
            assertEquals(-1, input.read());
        }
    }

    /**
     * Reproduces the server's game-lock/connection-lock ordering without leaving a deadlocked thread when regressed.
     * The listener must acquire a lock whose owner is waiting for the connection monitor. A bounded tryLock lets a
     * broken callback return and release its monitor, so both test threads can still be joined.
     */
    private void assertUnlockedDisconnect(AbstractConnection connection, Supplier<Object> outputMonitor,
          Runnable operation) throws Exception {
        var gameLock = new ReentrantLock();
        var gameLocked = new CountDownLatch(1);
        var listenerEntered = new CountDownLatch(1);
        var connectionAcquired = new AtomicBoolean();
        var callbackAcquiredGame = new AtomicBoolean();
        var callbackHeldConnection = new AtomicBoolean();
        var callbackHeldOutput = new AtomicBoolean();
        var callbackCount = new AtomicInteger();
        var threadFailure = new AtomicReference<Throwable>();
        Thread gameThread = new Thread(() -> {
            gameLock.lock();
            try {
                gameLocked.countDown();
                if (!listenerEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new AssertionError("Disconnect listener was not called");
                }
                synchronized (connection) {
                    connectionAcquired.set(true);
                }
            } catch (Throwable failure) {
                threadFailure.set(failure);
            } finally {
                gameLock.unlock();
            }
        }, "connection-disconnect-game-lock");
        gameThread.setDaemon(true);
        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                callbackCount.incrementAndGet();
                callbackHeldConnection.set(Thread.holdsLock(connection));
                Object monitor = outputMonitor.get();
                callbackHeldOutput.set(monitor != null && Thread.holdsLock(monitor));
                listenerEntered.countDown();
                try {
                    if (gameLock.tryLock(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        callbackAcquiredGame.set(true);
                        gameLock.unlock();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    threadFailure.set(ex);
                }
            }
        });
        gameThread.start();
        try {
            assertTrue(gameLocked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            operation.run();
        } finally {
            listenerEntered.countDown();
            gameThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        }

        assertFalse(gameThread.isAlive(), "Game-lock owner must finish after disconnect");
        assertNull(threadFailure.get());
        assertEquals(1, callbackCount.get());
        assertFalse(callbackHeldConnection.get(), "Disconnect listener retained the connection monitor");
        assertFalse(callbackHeldOutput.get(), "Disconnect listener retained the output monitor");
        assertTrue(connectionAcquired.get());
        assertTrue(callbackAcquiredGame.get(), "Disconnect listener could not acquire the game lock");
        assertTrue(connection.isClosed());
        assertFalse(connection.hasPending());
    }

    private static class DeferredStreamConnection extends DataStreamConnection {
        private final OutputStream output;
        private boolean flushEnabled;

        DeferredStreamConnection(OutputStream output, boolean flushEnabled) {
            super(new Socket(), 1);
            this.output = output;
            this.flushEnabled = flushEnabled;
        }

        @Override
        protected OutputStream getOutputStream() {
            return output;
        }

        @Override
        protected int getSendBufferSize() {
            // Keep complete test packets buffered so the failure happens in DataStreamConnection.flush.
            return 65536;
        }

        @Override
        public void flush() {
            if (flushEnabled) {
                super.flush();
            }
        }
    }

    private static class FailedProcessorConnection extends AbstractConnection {
        private boolean flushEnabled;

        FailedProcessorConnection(boolean flushEnabled) {
            super(new Socket(), 1);
            this.flushEnabled = flushEnabled;
        }

        @Override
        protected INetworkPacket readNetworkPacket() {
            return null;
        }

        @Override
        protected void sendNetworkPacket(byte[] data, boolean zipped) {
            throw new AssertionError("The failing queue processor must not write to the network");
        }

        @Override
        protected void processPacket(SendPacket packet) {
            // sendNow swallows writer errors; fail processing itself to reach AbstractConnection.flush's catch.
            throw new IllegalStateException("Failed test packet processor");
        }

        @Override
        public void flush() {
            if (flushEnabled) {
                super.flush();
            }
        }
    }
}
