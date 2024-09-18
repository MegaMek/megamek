/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import megamek.common.enums.GamePhase;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.verifier.TestEntity;
import megamek.server.totalwarfare.TWGameManager;

class DropShipMekLoadTest {

    @Test
    void test() throws Exception {
        MekSummaryCache instance = MekSummaryCache.getInstance(true);
        Mek atlas = (Mek) instance.getMek("Atlas AS7-D").loadEntity();
        atlas.setId(2);
        Dropship leopard = (Dropship) instance.getMek("Leopard (2537)").loadEntity();
        leopard.setId(1);

        Game game = new Game();
        game.setPhase(GamePhase.LOUNGE);
        game.addPlayer(0, new Player(0, "TestPlayer"));
        game.addEntity(atlas);
        game.addEntity(leopard);

        TWGameManager gm = mock(TWGameManager.class);
        doNothing().when(gm).entityUpdate(anyInt());
        when(gm.getGame()).thenReturn(game);
        doCallRealMethod().when(gm).setGame(any(Game.class));
        doCallRealMethod().when(gm).handlePacket(anyInt(), any(Packet.class));
        doCallRealMethod().when(gm).loadUnit(any(Entity.class), any(Entity.class), anyInt());
        gm.setGame(game);

        Packet packet = new Packet(PacketCommand.ENTITY_LOAD, atlas.getId(), leopard.getId(), -1);
        gm.handlePacket(0, packet);

        doAssertions(leopard, atlas);

        leopard.setAltitude(0);

        doAssertions(leopard, atlas);
    }

    private void doAssertions(Entity leopard, Entity atlas) {
        StringBuffer errors = new StringBuffer();
        assertTrue(isValid(leopard, errors), "Leopard is not valid after loading Atlas, errors: " + errors);
        assertTrue(isValid(atlas, errors), "Atlas is not valid after loading onto Leopard, errors: " + errors);
        assertEquals(atlas.getTransportId(), leopard.getId(),
                "Carrier ID " + atlas.getTransportId() + " wrong, should be " + leopard.getId());
        assertEquals(1, leopard.getLoadedUnits().size(),
                "Loaded unit list size" + leopard.getLoadedUnits().size() + " wrong, should be 1");
        assertNull(atlas.getPosition(), "Loaded Atlas position is " + atlas.getPosition() + ", should be null");
    }

    private boolean isValid(Entity entity, StringBuffer errors) {
        return TestEntity.getEntityVerifier(entity).correctEntity(errors);
    }
}
