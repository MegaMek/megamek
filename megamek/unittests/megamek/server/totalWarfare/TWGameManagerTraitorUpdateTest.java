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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for a pending traitor switch surviving a direct entity update. The /changeOwner command sets
 * the traitor id on the server's copy of the unit only, to be resolved in the END phase. A client-sent update
 * (for example the Edit Damage dialog's caller sending the unit back after the dialog closes) replaces the
 * server's copy wholesale, so {@code receiveEntityUpdate} must carry the pending switch over to the new copy
 * instead of letting the stale client copy erase it.
 */
class TWGameManagerTraitorUpdateTest {

    private static final int UNIT_ID = 5;
    private static final int OWNER_CONNECTION_ID = 0;
    private static final int TRAITOR_TARGET_PLAYER_ID = 2;
    private static final int NO_TRAITOR = -1;

    private TWGameManager gameManager;
    private Game game;
    private Entity serverCopy;
    private Entity clientCopy;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();

        Player owner = new Player(OWNER_CONNECTION_ID, "Owner");
        owner.setTeam(1);
        game.addPlayer(OWNER_CONNECTION_ID, owner);

        serverCopy = loadTestUnit(owner);
        game.addEntity(serverCopy);
        // a second copy of the same unit stands in for the client's local copy that an update sends back
        clientCopy = loadTestUnit(owner);

        game.setPhase(GamePhase.MOVEMENT);

        gameManager = mock(TWGameManager.class);
        doCallRealMethod().when(gameManager).setGame(any());
        doCallRealMethod().when(gameManager).handlePacket(anyInt(), any());
        gameManager.setGame(game);
    }

    private Entity loadTestUnit(Player owner) {
        Entity unit = MMTestUtilities.getEntityForUnitTesting("Enforcer III ENF-6M", false);
        assertNotNull(unit, "Test unit could not be loaded");
        unit.setId(UNIT_ID);
        unit.setOwner(owner);
        return unit;
    }

    /** Sends the client's copy of the unit back to the server the same way the Edit Damage dialog's caller does. */
    private void sendEntityUpdate() {
        gameManager.handlePacket(OWNER_CONNECTION_ID, new Packet(PacketCommand.ENTITY_UPDATE, clientCopy));
    }

    @Test
    void pendingTraitorSwitchSurvivesAnUpdateWithoutOne() {
        serverCopy.setTraitorId(TRAITOR_TARGET_PLAYER_ID);

        sendEntityUpdate();

        assertEquals(TRAITOR_TARGET_PLAYER_ID, game.getEntity(UNIT_ID).getTraitorId(),
              "The stale client copy must not erase the pending traitor switch");
    }

    @Test
    void updateCarryingATraitorSwitchOrdersOne() {
        clientCopy.setTraitorId(TRAITOR_TARGET_PLAYER_ID);

        sendEntityUpdate();

        assertEquals(TRAITOR_TARGET_PLAYER_ID, game.getEntity(UNIT_ID).getTraitorId(),
              "An update that orders a switch itself (the Traitor button) must keep it");
    }

    @Test
    void updateWithoutATraitorSwitchLeavesNone() {
        sendEntityUpdate();

        assertEquals(NO_TRAITOR, game.getEntity(UNIT_ID).getTraitorId(),
              "An update with no switch pending anywhere must not invent one");
    }
}
