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
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Tank;
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
                              new UnitLocation(-1, new Coords(4, 5), 0, 2, 0)))
                  .withImpacts(List.of(new ResolvedAttack.Impact("LA", false, 5), new ResolvedAttack.Impact("RT", true, 7)));
            var bytes = new ByteArrayOutputStream();
            marshaller.marshall(new Packet(PacketCommand.ENTITY_ATTACK_RESOLVED, result), bytes);
            var packet = marshaller.unmarshall(new ByteArrayInputStream(bytes.toByteArray()));
            assertEquals(PacketCommand.ENTITY_ATTACK_RESOLVED, packet.command());
            assertEquals(result, packet.getObject(0));
        }
    }

    @Test
    void cannonCalibreAndPpcIdentitySurviveResolutionAndTheNetworkFilter() throws Exception {
        var cannon = ResolvedAttack.Shot.capture(Mounted.createMounted(new Tank(), EquipmentType.get("ISLongTomCannon")));
        var origin = new UnitLocation(1, new Coords(2, 3), 0, 0, 0);
        var landing = new UnitLocation(2, new Coords(2, 1), 0, 0, 0);
        var shot = cannon.withResolution(null, null).withTrajectory(origin, landing).asDefensive();
        assertEquals(true, shot.ballistic());
        assertEquals(20, shot.rackSize());
        var ppc = ResolvedAttack.Shot.capture(Mounted.createMounted(new Tank(), EquipmentType.get("ISPPC")))
              .withResolution(null, null).asDefensive();
        assertEquals(true, ppc.ppc());
        assertEquals(false, ppc.ballistic());
        var result = new ResolvedAttack(UUID.randomUUID(), ResolvedAttack.Kind.SHOT, origin, landing, Targetable.TYPE_ENTITY,
              0, "ISLongTomCannon", 0, true, List.of(new ResolvedAttack.Mount(1, 0, shot), new ResolvedAttack.Mount(1, 1, ppc)), shot);
        var marshaller = new NativeSerializationMarshaller();
        var bytes = new ByteArrayOutputStream();
        marshaller.marshall(new Packet(PacketCommand.ENTITY_ATTACK_RESOLVED, result), bytes);
        assertEquals(result, marshaller.unmarshall(new ByteArrayInputStream(bytes.toByteArray())).getObject(0));
    }
}
