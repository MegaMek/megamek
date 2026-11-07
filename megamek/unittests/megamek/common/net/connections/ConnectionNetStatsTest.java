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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.net.stats.NetStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the connection layer feeds {@link NetStats} at the right points with the right numbers: the send
 * side when a packet is marshalled and written, the receive side when a frame is unmarshalled, and the flush.
 */
class ConnectionNetStatsTest {

    /** 1 byte zipped flag + 4 bytes marshalling type + 4 bytes length. */
    private static final int HEADER_BYTES = 9;

    private boolean timingBefore;

    @BeforeEach
    void setUp() {
        timingBefore = NetStats.isTimingEnabled();
    }

    @AfterEach
    void tearDown() {
        NetStats.setTimingEnabled(timingBefore);
    }

    private static DataStreamConnection sender(ByteArrayOutputStream sink, NetStats stats) throws IOException {
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(sink);
        when(socket.getSendBufferSize()).thenReturn(1024);
        DataStreamConnection connection = new DataStreamConnection(socket, 11);
        connection.setNetStats(stats);
        return connection;
    }

    private static DataStreamConnection receiver(byte[] wire, NetStats stats) throws IOException {
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(wire));
        when(socket.getReceiveBufferSize()).thenReturn(1024);
        DataStreamConnection connection = new DataStreamConnection(socket, 12);
        connection.setNetStats(stats);
        return connection;
    }

    @Test
    void aNewConnectionReportsToTheDefaultInstanceUntilToldOtherwise() throws IOException {
        Socket socket = mock(Socket.class);
        DataStreamConnection connection = new DataStreamConnection(socket, 1);

        assertSame(NetStats.defaultInstance(), connection.getNetStats());

        NetStats own = new NetStats();
        connection.setNetStats(own);
        assertSame(own, connection.getNetStats());
    }

    @Test
    void sendingRecordsCountRawBytesAndWireBytes() throws IOException {
        NetStats.setTimingEnabled(false);
        NetStats stats = new NetStats();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        DataStreamConnection connection = sender(sink, stats);

        connection.send(new Packet(PacketCommand.CHAT, "hello"));
        connection.send(new Packet(PacketCommand.PLAYER_READY, true));

        assertEquals(1, stats.sent(PacketCommand.CHAT));
        assertEquals(1, stats.sent(PacketCommand.PLAYER_READY));
        assertEquals(0, stats.received(PacketCommand.CHAT));
        long wireTotal = stats.wireBytesSent(PacketCommand.CHAT) + stats.wireBytesSent(PacketCommand.PLAYER_READY);
        assertEquals(sink.size() - 2 * HEADER_BYTES, wireTotal, "wire bytes are the frame payloads actually written");
        assertEquals(connection.getBytesSent(), wireTotal, "and agree with the connection's own counter");
        assertTrue(stats.rawBytesSent(PacketCommand.CHAT) > 0, "raw bytes are the pre-compression size");
        assertEquals(0, stats.marshalNanos(PacketCommand.CHAT), "no timing while it is off");
        assertEquals(0, stats.writeNanos(PacketCommand.CHAT));
        assertEquals(2, stats.flushCount(), "one flush per send()");
        assertEquals(0, stats.maxFlushNanos());
    }

    @Test
    void rawBytesAreTheUncompressedSerializationSize() throws Exception {
        NetStats stats = new NetStats();
        DataStreamConnection connection = sender(new ByteArrayOutputStream(), stats);
        ByteArrayOutputStream reference = new ByteArrayOutputStream();
        connection.marshaller.marshall(new Packet(PacketCommand.CHAT, "hello"), reference);

        connection.send(new Packet(PacketCommand.CHAT, "hello"));

        assertEquals(reference.size(), stats.rawBytesSent(PacketCommand.CHAT));
    }

    @Test
    void receivingRecordsCountAndWireBytes() throws Exception {
        NetStats.setTimingEnabled(false);
        NetStats sendStats = new NetStats();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        DataStreamConnection sender = sender(sink, sendStats);
        sender.send(new Packet(PacketCommand.CHAT, "hello"));
        sender.send(new Packet(PacketCommand.PLAYER_READY, true));
        NetStats receiveStats = new NetStats();
        DataStreamConnection receiver = receiver(sink.toByteArray(), receiveStats);

        receiver.update();

        assertEquals(1, receiveStats.received(PacketCommand.CHAT));
        assertEquals(1, receiveStats.received(PacketCommand.PLAYER_READY));
        assertEquals(0, receiveStats.sent(PacketCommand.CHAT));
        assertEquals(sendStats.wireBytesSent(PacketCommand.CHAT), receiveStats.wireBytesReceived(PacketCommand.CHAT),
              "both ends count the same payload bytes");
        assertEquals(0, receiveStats.unmarshalNanos(PacketCommand.CHAT));
        assertEquals(0, sendStats.received(PacketCommand.CHAT), "each side only sees its own direction");
    }

    @Test
    void timersFillInWhenTimingIsOn() throws Exception {
        NetStats.setTimingEnabled(true);
        NetStats sendStats = new NetStats();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        DataStreamConnection sender = sender(sink, sendStats);
        // A payload big enough that marshalling and gzip take measurable time on any clock
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 20_000; i++) {
            big.append("round report line ").append(i).append('\n');
        }
        sender.send(new Packet(PacketCommand.SENDING_REPORTS, big.toString()));
        NetStats receiveStats = new NetStats();
        DataStreamConnection receiver = receiver(sink.toByteArray(), receiveStats);

        receiver.update();

        assertTrue(sendStats.marshalNanos(PacketCommand.SENDING_REPORTS) > 0, "marshal time recorded");
        assertTrue(receiveStats.unmarshalNanos(PacketCommand.SENDING_REPORTS) > 0, "unmarshal time recorded");
        assertEquals(1, sendStats.flushCount());
        assertTrue(sendStats.snapshot().timing());
    }

    @Test
    void aFailedUnmarshalRecordsNothing() throws Exception {
        NetStats stats = new NetStats();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        sender(sink, new NetStats()).send(new Packet(PacketCommand.CHAT, "hello"));
        byte[] wire = sink.toByteArray();
        wire[4] = 42; // unknown marshalling type
        DataStreamConnection receiver = receiver(wire, stats);

        receiver.update();

        assertEquals(0, stats.snapshot().totalPackets());
        assertTrue(receiver.isClosed());
    }
}
