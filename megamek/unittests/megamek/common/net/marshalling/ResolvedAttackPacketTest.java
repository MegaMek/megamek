/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.common.net.marshalling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Targetable;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Test;

class ResolvedAttackPacketTest {
    @Test
    void resultSurvivesTheActualNetworkSerializationFilter() throws Exception {
        var marshaller = new NativeSerializationMarshaller();
        for (var kind : ResolvedAttack.Kind.values()) {
            var result = new ResolvedAttack(UUID.randomUUID(), kind,
                  new UnitLocation(1, new Coords(2, 3), 4, 0, 0),
                  new UnitLocation(2, new Coords(3, 4), 1, 0, 0), Targetable.TYPE_ENTITY, 5, "ISPPC", 4, true,
                  List.of(new ResolvedAttack.Mount(11, 0), new ResolvedAttack.Mount(12, 3,
                        new ResolvedAttack.Shot("Indirect", Set.of("M_STANDARD"), false, false, 1, 20, true, 12))),
                  new ResolvedAttack.Shot("Indirect", Set.of("M_STANDARD"), false, false, 1, 20, true, 12)
                        .withTrajectory(new UnitLocation(1, new Coords(1, 2), 0, 0, 0),
                              new UnitLocation(-1, new Coords(4, 5), 0, 2, 0)));
            var bytes = new ByteArrayOutputStream();
            marshaller.marshall(new Packet(PacketCommand.ENTITY_ATTACK_RESOLVED, result), bytes);
            var packet = marshaller.unmarshall(new ByteArrayInputStream(bytes.toByteArray()));
            assertEquals(PacketCommand.ENTITY_ATTACK_RESOLVED, packet.command());
            assertEquals(result, packet.getObject(0));
        }
    }
}
