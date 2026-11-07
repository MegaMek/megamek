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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectStreamConstants;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.marshalling.PacketMarshaller;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for the frame layout that {@link DataStreamConnection} puts on the wire and for how it reads
 * frames back. Every frame is {@code boolean zipped, int marshallingType, int length, byte[length]}; the payload is
 * the marshalled packet, gzipped because compression is on by default and cannot be switched off any more.
 */
class DataStreamConnectionFramingTest {

    /** Size of the fixed frame header: 1 byte zipped flag + 4 bytes marshalling type + 4 bytes length. */
    private static final int HEADER_BYTES = 1 + 4 + 4;

    private static DataStreamConnection sendingConnection(ByteArrayOutputStream sink) throws IOException {
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(sink);
        when(socket.getSendBufferSize()).thenReturn(1024);
        return new DataStreamConnection(socket, 7);
    }

    private static DataStreamConnection receivingConnection(byte[] wire) throws IOException {
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(wire));
        when(socket.getReceiveBufferSize()).thenReturn(1024);
        return new DataStreamConnection(socket, 8);
    }

    /** A listener that records every packet and disconnect it sees. */
    private static final class Recorder extends ConnectionListener {
        final List<Packet> packets = new ArrayList<>();
        int disconnects;

        @Override
        public void packetReceived(PacketReceivedEvent event) {
            packets.add(event.getPacket());
        }

        @Override
        public void disconnected(DisconnectedEvent event) {
            disconnects++;
        }
    }

    private static byte[] wireBytesOf(Packet... packets) throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        DataStreamConnection connection = sendingConnection(sink);
        for (Packet packet : packets) {
            connection.send(packet);
        }
        return sink.toByteArray();
    }

    @Test
    void compressionIsOnByDefault() throws IOException {
        assertTrue(sendingConnection(new ByteArrayOutputStream()).isCompressed());
    }

    @Test
    void aFrameIsZippedFlagThenTypeThenLengthThenPayload() throws Exception {
        byte[] wire = wireBytesOf(new Packet(PacketCommand.CHAT, "hello"));
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(wire));

        assertTrue(in.readBoolean(), "zipped flag");
        assertEquals(PacketMarshaller.NATIVE_SERIALIZATION_MARSHALING, in.readInt(), "marshalling type");
        int length = in.readInt();
        assertEquals(wire.length - HEADER_BYTES, length, "length field covers exactly the rest of the frame");

        byte[] payload = in.readNBytes(length);
        assertEquals(0, in.available(), "nothing trails the payload");
        // The payload is a gzip member whose content is a Java serialization stream
        byte[] inflated = new GZIPInputStream(new ByteArrayInputStream(payload)).readAllBytes();
        assertEquals((byte) (ObjectStreamConstants.STREAM_MAGIC >> 8), inflated[0]);
        assertEquals((byte) ObjectStreamConstants.STREAM_MAGIC, inflated[1]);
    }

    @Test
    void consecutivePacketsAreConsecutiveFrames() throws Exception {
        byte[] wire = wireBytesOf(new Packet(PacketCommand.CHAT, "one"), new Packet(PacketCommand.PLAYER_READY, 3));
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(wire));

        for (int frame = 0; frame < 2; frame++) {
            in.readBoolean();
            in.readInt();
            in.skipNBytes(in.readInt());
        }
        assertEquals(0, in.available(), "two packets, two frames, nothing else");
    }

    @Test
    void bytesSentCountsPayloadsNotHeaders() throws Exception {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        DataStreamConnection connection = sendingConnection(sink);

        connection.send(new Packet(PacketCommand.CHAT, "one"));
        connection.send(new Packet(PacketCommand.CHAT, "two"));

        // The counter is incremented with the compressed payload size at marshalling time, so it omits the two
        // 9-byte frame headers. It is documented as "very approximate"; this pins what the approximation is.
        assertEquals(sink.size() - 2 * HEADER_BYTES, connection.getBytesSent());
    }

    @Test
    void sentFramesAreReadBackAsTheSamePackets() throws Exception {
        byte[] wire = wireBytesOf(new Packet(PacketCommand.CHAT, "one"), new Packet(PacketCommand.PLAYER_READY, 3));
        DataStreamConnection receiver = receivingConnection(wire);
        Recorder recorder = new Recorder();
        receiver.addConnectionListener(recorder);

        // update() reads until the stream ends; end of stream counts as the peer closing, so the connection closes.
        receiver.update();

        assertEquals(2, recorder.packets.size());
        assertEquals(PacketCommand.CHAT, recorder.packets.get(0).command());
        assertEquals("one", recorder.packets.get(0).getStringValue(0));
        assertEquals(PacketCommand.PLAYER_READY, recorder.packets.get(1).command());
        assertEquals(3, recorder.packets.get(1).getIntValue(0));
        assertEquals(wire.length - 2 * HEADER_BYTES, receiver.getBytesReceived());
        assertEquals(1, recorder.disconnects, "end of stream closes the connection once");
        assertTrue(receiver.isClosed());
    }

    @Test
    void aTruncatedFrameClosesTheConnectionWithoutDeliveringAPacket() throws Exception {
        byte[] wire = wireBytesOf(new Packet(PacketCommand.CHAT, "hello"));
        byte[] truncated = new byte[wire.length - 5];
        System.arraycopy(wire, 0, truncated, 0, truncated.length);
        DataStreamConnection receiver = receivingConnection(truncated);
        Recorder recorder = new Recorder();
        receiver.addConnectionListener(recorder);

        receiver.update();

        assertTrue(recorder.packets.isEmpty(), "a partial frame must never surface as a packet");
        assertEquals(1, recorder.disconnects);
        assertTrue(receiver.isClosed());
    }

    @Test
    void anUnknownMarshallingTypeClosesTheConnection() throws Exception {
        byte[] wire = wireBytesOf(new Packet(PacketCommand.CHAT, "hello"));
        // Bytes 1..4 are the marshalling type; nothing but 0 exists
        wire[4] = 42;
        DataStreamConnection receiver = receivingConnection(wire);
        Recorder recorder = new Recorder();
        receiver.addConnectionListener(recorder);

        receiver.update();

        assertTrue(recorder.packets.isEmpty());
        assertEquals(1, recorder.disconnects);
        assertTrue(receiver.isClosed());
    }

    @Test
    void anUncompressedFrameIsAcceptedWhenFlagged() throws Exception {
        // The reader honours the per-frame zipped flag, so a peer may send raw serialization data. Nothing sends such
        // frames today (compression is always on), but per-command compression later will rely on this.
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        megamek.common.net.marshalling.PacketMarshallerFactory.getInstance()
              .getMarshaller(PacketMarshaller.NATIVE_SERIALIZATION_MARSHALING)
              .marshall(new Packet(PacketCommand.CHAT, "plain"), raw);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(frame);
        out.writeBoolean(false);
        out.writeInt(PacketMarshaller.NATIVE_SERIALIZATION_MARSHALING);
        out.writeInt(raw.size());
        out.write(raw.toByteArray());
        DataStreamConnection receiver = receivingConnection(frame.toByteArray());
        Recorder recorder = new Recorder();
        receiver.addConnectionListener(recorder);

        receiver.update();

        assertEquals(1, recorder.packets.size());
        assertEquals("plain", recorder.packets.get(0).getStringValue(0));
    }

    @Test
    void theLengthFieldIsTrustedAsIs() throws Exception {
        // The reader allocates new byte[length] before checking that many bytes exist. With a length larger than the
        // data a clean EOF follows, but there is no upper bound at all; a hostile length is a 2 GB allocation. The
        // bound belongs to lifecycle hardening; this test pins today's behaviour for the benign case.
        byte[] wire = wireBytesOf(new Packet(PacketCommand.CHAT, "hello"));
        int declared = wire.length - HEADER_BYTES;
        int inflatedLength = declared + 1000;
        wire[5] = (byte) (inflatedLength >>> 24);
        wire[6] = (byte) (inflatedLength >>> 16);
        wire[7] = (byte) (inflatedLength >>> 8);
        wire[8] = (byte) inflatedLength;
        DataStreamConnection receiver = receivingConnection(wire);
        Recorder recorder = new Recorder();
        receiver.addConnectionListener(recorder);

        receiver.update();

        assertTrue(recorder.packets.isEmpty());
        assertTrue(receiver.isClosed());
        assertFalse(receiver.hasPending());
    }

    @Test
    void sendAndReceiveAgreeOnTheMarshallingTypeField() throws Exception {
        byte[] wire = wireBytesOf(new Packet(PacketCommand.SERVER_GREETING));
        byte[] typeField = new byte[] { wire[1], wire[2], wire[3], wire[4] };

        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, typeField);
    }

    /** Guards the helper itself: a stream with no bytes is an immediate, clean close. */
    @Test
    void anEmptyStreamIsACleanClose() throws Exception {
        DataStreamConnection receiver = receivingConnection(new byte[0]);
        Recorder recorder = new Recorder();
        receiver.addConnectionListener(recorder);

        receiver.update();

        assertEquals(1, recorder.disconnects);
        assertTrue(recorder.packets.isEmpty());
    }
}
