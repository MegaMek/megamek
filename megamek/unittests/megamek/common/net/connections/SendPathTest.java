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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for the send side of {@link AbstractConnection}: who does the marshalling, when the socket
 * is written, and what the {@link SendQueue} actually buffers. Today {@code send()} does everything on the calling
 * thread and the queue is drained in the same call that filled it, which is why a server broadcast stalls on the
 * slowest client. These tests record that so the per-connection writer work can show the change.
 */
class SendPathTest {

    /** Records which thread wrote each chunk and how many times flush was called. */
    private static final class ThreadRecordingStream extends OutputStream {
        final List<Thread> writers = new ArrayList<>();
        final AtomicInteger flushes = new AtomicInteger();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        @Override
        public void write(int value) {
            writers.add(Thread.currentThread());
            bytes.write(value);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) {
            writers.add(Thread.currentThread());
            bytes.write(buffer, offset, length);
        }

        @Override
        public void flush() {
            flushes.incrementAndGet();
        }
    }

    private static DataStreamConnection connectionOver(OutputStream stream, int sendBufferSize) throws IOException {
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(stream);
        when(socket.getSendBufferSize()).thenReturn(sendBufferSize);
        return new DataStreamConnection(socket, 3);
    }

    @Test
    void sendMarshalsAndWritesOnTheCallingThread() throws IOException {
        ThreadRecordingStream stream = new ThreadRecordingStream();
        DataStreamConnection connection = connectionOver(stream, 1024);

        connection.send(new Packet(PacketCommand.CHAT, "hello"));

        assertFalse(stream.writers.isEmpty(), "the socket was written during send()");
        for (Thread writer : stream.writers) {
            assertSame(Thread.currentThread(), writer, "no hand-off: the caller writes the socket itself");
        }
        assertEquals(1, stream.flushes.get(), "send() flushes the stream once per call");
    }

    @Test
    void sendLeavesNothingQueued() throws IOException {
        DataStreamConnection connection = connectionOver(new ThreadRecordingStream(), 1024);

        connection.send(new Packet(PacketCommand.CHAT, "hello"));

        assertFalse(connection.hasPending(), "the queue is drained inside send(), so it never buffers across calls");
    }

    @Test
    void flushOnAnIdleConnectionIsANoOp() throws IOException {
        ThreadRecordingStream stream = new ThreadRecordingStream();
        DataStreamConnection connection = connectionOver(stream, 1024);

        connection.flush();

        assertTrue(stream.writers.isEmpty());
        assertEquals(0, stream.flushes.get(), "no output stream has been opened yet, so nothing is flushed");
        assertFalse(connection.isClosed());
    }

    @Test
    void sendBeforeOpenStillWritesBecauseTheServerSideIsOpenOnConstruction() throws IOException {
        // A server-side connection is built around an accepted socket; open() only flips a flag. Sending without
        // calling open() therefore works, which is what Server.run() relies on (it calls open() but nothing checks it).
        ThreadRecordingStream stream = new ThreadRecordingStream();
        DataStreamConnection connection = connectionOver(stream, 1024);

        connection.send(new Packet(PacketCommand.SERVER_VERSION_CHECK));

        assertTrue(stream.bytes.size() > 0);
        assertTrue(connection.open(), "open() on an already-socketed connection reports success");
    }

    @Test
    void aWriteFailureClosesTheConnection() throws IOException {
        OutputStream broken = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new SocketException("Connection reset");
            }
        };
        // A tiny send buffer so the frame is pushed to the socket during the write, not only at flush
        DataStreamConnection connection = connectionOver(broken, 4);
        AtomicInteger disconnects = new AtomicInteger();
        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                disconnects.incrementAndGet();
            }
        });

        connection.send(new Packet(PacketCommand.CHAT, "a message long enough to exceed the buffer"));

        // sendNow() swallows the exception from the write itself; the failure only surfaces because the trailing
        // flush retries the buffered bytes and fails again. Either way the connection must end up closed exactly once.
        assertTrue(connection.isClosed());
        assertEquals(1, disconnects.get());
    }

    @Test
    void bytesSentGrowsByTheCompressedPayloadEvenIfTheSocketWriteFails() throws IOException {
        OutputStream broken = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new SocketException("Connection reset");
            }
        };
        DataStreamConnection connection = connectionOver(broken, 4);

        connection.send(new Packet(PacketCommand.CHAT, "hello"));

        // The counter is bumped when the SendPacket is built, before any socket I/O, so it counts attempted bytes.
        assertTrue(connection.getBytesSent() > 0);
    }

    @Test
    void closingTwiceFiresDisconnectTwice() throws IOException {
        // close() is not idempotent at the listener level: every call fires DisconnectedEvent. Server's listener
        // tolerates this today (removals of absent entries), which is worth knowing before reconnect work.
        DataStreamConnection connection = connectionOver(new ThreadRecordingStream(), 1024);
        AtomicInteger disconnects = new AtomicInteger();
        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                disconnects.incrementAndGet();
            }
        });

        connection.close();
        connection.close();

        assertEquals(2, disconnects.get());
        assertTrue(connection.isClosed());
    }

    @Test
    void sendPacketIsMarshalledOnConstruction() throws IOException {
        DataStreamConnection connection = connectionOver(new ThreadRecordingStream(), 1024);

        SendPacket sendPacket = new SendPacket(new Packet(PacketCommand.CHAT, "hello"), connection);

        assertEquals(PacketCommand.CHAT, sendPacket.getCommand());
        assertTrue(sendPacket.isCompressed());
        assertNotNull(sendPacket.data());
        assertTrue(sendPacket.data().length > 0);
        assertEquals(sendPacket.data().length, connection.getBytesSent());
    }

    @Test
    void sendQueueIsPlainFifoWithAFinishSwitch() {
        SendQueue queue = new SendQueue();
        assertFalse(queue.hasPending());
        assertNull(queue.getPacket());

        SendPacket first = mock(SendPacket.class);
        SendPacket second = mock(SendPacket.class);
        queue.addPacket(first);
        queue.addPacket(second);

        assertTrue(queue.hasPending());
        assertSame(first, queue.getPacket());
        assertSame(second, queue.getPacket());
        assertNull(queue.getPacket());
        assertFalse(queue.hasPending());
    }

    @Test
    void aFinishedSendQueueDropsItsContentsAndRefusesToHandOutMore() {
        SendQueue queue = new SendQueue();
        queue.addPacket(mock(SendPacket.class));

        queue.finish();

        assertFalse(queue.hasPending(), "finish() clears whatever was queued");
        queue.addPacket(mock(SendPacket.class));
        assertTrue(queue.hasPending(), "adding after finish still stores the packet...");
        assertNull(queue.getPacket(), "...but it can never be retrieved: the queue is dead");
    }
}
