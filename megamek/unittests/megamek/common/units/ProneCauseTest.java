/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;

import megamek.common.game.Game;
import megamek.common.board.Coords;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.marshalling.PacketMarshaller;
import megamek.common.net.marshalling.PacketMarshallerFactory;
import megamek.common.net.packets.Packet;
import megamek.common.util.SerializationHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProneCauseTest {
    @Test
    void resolvedMovementCuesSurviveTheExistingPacketWithoutInventingLegacyPosture() throws Exception {
        var path = new Vector<UnitLocation>();
        var coords = new Coords(2, 3);
        path.add(new UnitLocation(23, coords, 0, 0, 0, ProneCause.NONE));
        path.add(new UnitLocation(23, coords, 0, 0, 0, ProneCause.FORCED));
        path.add(new UnitLocation(23, coords, 0, 0, 0, ProneCause.NONE, null, null, true));
        path.add(new UnitLocation(23, coords, 0, 0, 0, ProneCause.NONE, null, null, false));
        path.add(new UnitLocation(23, coords, 0, 0, 0, ProneCause.NONE));
        path.add(new UnitLocation(23, coords, 0, 0, 0));
        var marshaller = PacketMarshallerFactory.getInstance()
              .getMarshaller(PacketMarshaller.NATIVE_SERIALIZATION_MARSHALING);
        var bytes = new ByteArrayOutputStream();
        marshaller.marshall(new Packet(PacketCommand.ENTITY_UPDATE, 23, new BipedMek(), path), bytes);
        var packet = marshaller.unmarshall(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(path, packet.getObject(2));
        assertNull(((UnitLocation) ((Vector<?>) packet.getObject(2)).getLast()).proneCause());
        assertNull(((UnitLocation) ((Vector<?>) packet.getObject(2)).getLast()).hullDown());
    }

    @Test
    void legacyPostureWritesPreserveKnownCauseButANewTransitionIsUnknown() {
        var mek = new BipedMek();
        mek.setProne(ProneCause.VOLUNTARY);
        mek.setProne(true);
        assertEquals(ProneCause.VOLUNTARY, mek.getProneCause());
        mek.setProne(ProneCause.FORCED);
        mek.setProne(true);
        assertEquals(ProneCause.FORCED, mek.getProneCause());
        assertTrue(mek.isProne());
        mek.setProne(false);
        assertEquals(ProneCause.NONE, mek.getProneCause());
        mek.setProne(true);
        assertEquals(ProneCause.UNKNOWN, mek.getProneCause());
    }

    @Test
    void hullDownAndStandingClearCauseWithoutChangingRulesHeight() {
        var mek = new BipedMek();
        int standingHeight = mek.height();
        mek.setProne(ProneCause.VOLUNTARY);
        assertEquals(0, mek.height());
        mek.setProne(ProneCause.FORCED);
        assertEquals(0, mek.height());
        mek.setHullDown(true);
        assertFalse(mek.isProne());
        assertTrue(mek.isHullDown());
        assertEquals(ProneCause.NONE, mek.getProneCause());
        mek.setProne(ProneCause.FORCED);
        assertFalse(mek.isHullDown());
        mek.setProne(false);
        assertFalse(mek.isProne());
        assertEquals(ProneCause.NONE, mek.getProneCause());
        assertEquals(standingHeight, mek.height());
    }

    @ParameterizedTest
    @EnumSource(ProneCause.class)
    void saveGameRoundTripPreservesPostureAndCause(ProneCause cause) {
        var game = new Game();
        var mek = new BipedMek();
        mek.setId(23);
        mek.setProne(cause);
        game.addEntity(mek);
        String xml = SerializationHelper.getSaveGameXStream().toXML(game);
        var restored = (Game) SerializationHelper.getLoadSaveGameXStream().fromXML(xml);
        assertEquals(cause, restored.getEntity(23).getProneCause());
        assertEquals(mek.isProne(), restored.getEntity(23).isProne());
    }

    @ParameterizedTest
    @EnumSource(value = ProneCause.class, names = { "NONE", "FORCED" })
    void oldSaveWithoutCauseLoadsWithoutInventingATransition(ProneCause original) {
        var mek = new BipedMek();
        mek.setProne(original);
        String xml = SerializationHelper.getSaveGameXStream().toXML(mek)
              .replaceAll("<proneCause>[^<]+</proneCause>", "");
        var restored = (Entity) SerializationHelper.getLoadSaveGameXStream().fromXML(xml);
        assertEquals(mek.isProne(), restored.isProne());
        assertEquals(mek.isProne() ? ProneCause.UNKNOWN : ProneCause.NONE, restored.getProneCause());
        restored.setProne(true);
        assertEquals(ProneCause.UNKNOWN, restored.getProneCause());
        restored.setProne(false);
        assertEquals(ProneCause.NONE, restored.getProneCause());
    }

    @ParameterizedTest
    @EnumSource(ProneCause.class)
    void entityUpdateUsesTheExistingNetworkMarshaller(ProneCause cause) throws Exception {
        var mek = new BipedMek();
        mek.setId(23);
        mek.setProne(cause);
        var marshaller = PacketMarshallerFactory.getInstance()
              .getMarshaller(PacketMarshaller.NATIVE_SERIALIZATION_MARSHALING);
        assertNotNull(marshaller);
        var bytes = new ByteArrayOutputStream();
        marshaller.marshall(new Packet(PacketCommand.ENTITY_UPDATE, 23, mek, new Vector<>()), bytes);
        var packet = marshaller.unmarshall(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(PacketCommand.ENTITY_UPDATE, packet.command());
        var restored = (Entity) packet.getObject(1);
        assertEquals(cause, restored.getProneCause());
        assertEquals(mek.isProne(), restored.isProne());
    }
}
