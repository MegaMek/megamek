/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 */

package megamek.common.net.packets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.actions.EntityAction;
import megamek.common.net.enums.PacketCommand;
import org.junit.jupiter.api.Test;

class PacketEntityActionListTest {

    @Test
    void validListPreservesEveryActionInOrder() throws InvalidPacketDataException {
        EntityAction first = mock(EntityAction.class);
        EntityAction second = mock(EntityAction.class);
        Packet packet = new Packet(PacketCommand.ACTIONS, new Vector<>(List.of(first, second)));

        assertEquals(List.of(first, second), packet.getEntityActionList(0));
    }

    @Test
    void emptyListIsValid() throws InvalidPacketDataException {
        Packet packet = new Packet(PacketCommand.ACTIONS, new ArrayList<>());

        assertTrue(packet.getEntityActionList(0).isEmpty());
    }

    @Test
    void malformedNestedElementRejectsWholeList() {
        EntityAction valid = mock(EntityAction.class);
        Packet packet = new Packet(PacketCommand.ACTIONS, List.of(valid, "invalid"));

        InvalidPacketDataException exception = assertThrows(InvalidPacketDataException.class,
              () -> packet.getEntityActionList(0));

        assertTrue(exception.getMessage().contains("list offset 1"));
    }

    @Test
    void nullNestedElementRejectsWholeList() {
        List<Object> actions = new ArrayList<>();
        actions.add(mock(EntityAction.class));
        actions.add(null);
        Packet packet = new Packet(PacketCommand.ACTIONS, actions);

        assertThrows(InvalidPacketDataException.class, () -> packet.getEntityActionList(0));
    }

    @Test
    void nonListPayloadRejects() {
        Packet packet = new Packet(PacketCommand.ACTIONS, "invalid");

        assertThrows(InvalidPacketDataException.class, () -> packet.getEntityActionList(0));
    }
}
