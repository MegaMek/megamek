/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.verifier.TestEntity;
import megamek.server.totalwarfare.TWGameManager;
import org.junit.jupiter.api.Test;

class DropShipMekLoadTest {
    @Test
    void test() throws Exception {
        Entity entity = getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity, "Atlas AS7-D not found");
        Mek atlas = (Mek) entity;
        atlas.setId(2);

        entity = getEntityForUnitTesting("Leopard (2537)", true);
        assertNotNull(entity, "Leopard (2537) not found");
        Dropship leopard = (Dropship) entity;
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
