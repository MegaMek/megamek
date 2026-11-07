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
package megamek.common.net.marshalling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import megamek.common.Player;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for the only wire marshaller in use. They pin down what the marshaller does today - a fresh
 * {@link ObjectOutputStream} per packet, the command sent as its ordinal, the payload as one {@code Object[]} - so that
 * any later change to the encoding of hot packets is made on purpose and shows up here first.
 */
class NativeSerializationMarshallerTest {

    private static final PacketMarshaller MARSHALLER = PacketMarshallerFactory.getInstance()
          .getMarshaller(PacketMarshaller.NATIVE_SERIALIZATION_MARSHALING);

    private static byte[] marshall(Packet packet) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MARSHALLER.marshall(packet, out);
        return out.toByteArray();
    }

    private static Packet unmarshall(byte[] bytes) throws Exception {
        return MARSHALLER.unmarshall(new ByteArrayInputStream(bytes));
    }

    @Test
    void theFactoryOnlyKnowsNativeSerialization() {
        assertNotNull(MARSHALLER);
        assertInstanceOf(NativeSerializationMarshaller.class, MARSHALLER);
        assertNull(PacketMarshallerFactory.getInstance().getMarshaller(1),
              "A second marshalling type does not exist yet; the connection header's type field is always 0");
    }

    @Test
    void aPacketWithNoDataRoundTrips() throws Exception {
        Packet result = unmarshall(marshall(new Packet(PacketCommand.SERVER_GREETING)));

        assertEquals(PacketCommand.SERVER_GREETING, result.command());
        assertNotNull(result.data());
        assertEquals(0, result.data().length);
    }

    @Test
    void mixedPayloadRoundTripsByValue() throws Exception {
        List<Integer> ids = new ArrayList<>(List.of(3, 5, 8));
        Player player = new Player(4, "Kai Allard-Liao");
        Packet original = new Packet(PacketCommand.PLAYER_ADD, 4, player, "chat text", true, ids);

        Packet result = unmarshall(marshall(original));

        assertEquals(PacketCommand.PLAYER_ADD, result.command());
        assertEquals(5, result.data().length);
        assertEquals(4, result.getIntValue(0));
        assertEquals("Kai Allard-Liao", ((Player) result.getObject(1)).getName());
        assertEquals(4, ((Player) result.getObject(1)).getId());
        assertEquals("chat text", result.getStringValue(2));
        assertTrue(result.getBooleanValue(3));
        assertEquals(ids, result.getObject(4));
    }

    @Test
    void theCommandIsSentAsItsOrdinal() throws Exception {
        // The ordinal is the wire identity of a command, which is why PacketCommand values must never be reordered.
        byte[] bytes = marshall(new Packet(PacketCommand.CHAT, "hi"));
        byte[] rebuilt = handRolled(PacketCommand.CHAT.ordinal(), new Object[] { "hi" });

        assertArrayEquals(rebuilt, bytes);
    }

    @Test
    void everyPacketCarriesAFullSerializationStreamHeader() throws Exception {
        // Each packet is marshalled with its own ObjectOutputStream, so every packet repeats the stream magic and all
        // class descriptors. This is a known cost on hot packets; the test records it rather than endorsing it.
        byte[] first = marshall(new Packet(PacketCommand.CHAT, "one"));
        byte[] second = marshall(new Packet(PacketCommand.CHAT, "two"));

        for (byte[] bytes : List.of(first, second)) {
            assertEquals((byte) (ObjectStreamConstants.STREAM_MAGIC >> 8), bytes[0]);
            assertEquals((byte) ObjectStreamConstants.STREAM_MAGIC, bytes[1]);
            assertEquals((byte) (ObjectStreamConstants.STREAM_VERSION >> 8), bytes[2]);
            assertEquals((byte) ObjectStreamConstants.STREAM_VERSION, bytes[3]);
        }
        assertEquals(first.length, second.length, "Same shape, same size: nothing is shared between packets");
    }

    @Test
    void anOrdinalPastTheEndOfTheEnumIsRejected() throws Exception {
        byte[] bytes = handRolled(PacketCommand.values().length, new Object[0]);

        assertThrows(InvalidPacketCommandReceivedException.class, () -> unmarshall(bytes));
    }

    @Test
    void aNegativeOrdinalIsRejected() throws Exception {
        byte[] bytes = handRolled(-1, new Object[0]);

        assertThrows(InvalidPacketCommandReceivedException.class, () -> unmarshall(bytes));
    }

    @Test
    void aPayloadClassOutsideTheSanityFilterIsRejected() throws Exception {
        // java.util.Date is not on the SanityInputFilter allow list, so the ObjectInputStream refuses the stream.
        byte[] bytes = marshall(new Packet(PacketCommand.CHAT, new Date(0)));

        assertThrows(InvalidClassException.class, () -> unmarshall(bytes));
    }

    @Test
    void aPayloadThatIsNotAnObjectArrayFailsToUnmarshall() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeInt(PacketCommand.CHAT.ordinal());
            out.writeObject("not an array");
        }

        assertThrows(ClassCastException.class, () -> unmarshall(bos.toByteArray()));
    }

    /** Builds the exact byte sequence the marshaller is expected to produce for the given ordinal and payload. */
    private static byte[] handRolled(int ordinal, Object[] payload) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeInt(ordinal);
            out.writeObject(payload);
            out.flush();
        }
        return bos.toByteArray();
    }
}
