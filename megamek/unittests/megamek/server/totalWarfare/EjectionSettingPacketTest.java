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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests the packet that turns a unit's automatic ejection off after the lobby has closed.
 * <p>
 * The server's own copy of the unit decides whether a crew is thrown clear, so the change has to survive the trip
 * across the wire, and only the unit's owner may make it.
 */
class EjectionSettingPacketTest {

    private static final int OWNER_ID = 0;
    private static final int STRANGER_ID = 1;
    private static final int MEK_ID = 10;
    private static final int TANK_ID = 11;

    private TWGameManager gameManager;
    private Game game;
    private Mek mek;

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());

        game = gameManager.getGame();
        game.addPlayer(OWNER_ID, new Player(OWNER_ID, "Owner"));
        game.addPlayer(STRANGER_ID, new Player(STRANGER_ID, "Stranger"));

        mek = new BipedMek();
        mek.setId(MEK_ID);
        mek.setOwner(game.getPlayer(OWNER_ID));
        mek.setAutoEject(true);
        game.addEntity(mek);
    }

    /** Sends the setting the way the dialog does, so the handler and its ownership guard are both exercised. */
    private void sendAsPacket(int senderId, int entityId, boolean shouldEject) {
        gameManager.handlePacket(senderId,
              new Packet(PacketCommand.ENTITY_EJECTION_SETTING_CHANGE, entityId, shouldEject));
    }

    @Test
    void theOwnerCanTurnEjectionOff() {
        sendAsPacket(OWNER_ID, MEK_ID, false);

        assertFalse(mek.isAutoEject(),
              "the packet path should reach the handler and turn automatic ejection off");
    }

    @Test
    void theOwnerCanTurnEjectionBackOn() {
        mek.setAutoEject(false);

        sendAsPacket(OWNER_ID, MEK_ID, true);

        assertTrue(mek.isAutoEject(), "the setting should travel in both directions");
    }

    @Test
    void anotherPlayerCannotTurnEjectionOff() {
        sendAsPacket(STRANGER_ID, MEK_ID, false);

        assertTrue(mek.isAutoEject(),
              "a player who does not own the unit should not be able to change its ejection setting");
    }

    @Test
    void aPacketForAUnitThatIsNotInTheGameIsIgnored() {
        sendAsPacket(OWNER_ID, 999, false);

        assertTrue(mek.isAutoEject(), "an unknown unit id should leave every other unit alone");
    }

    @Test
    void aUnitWithNoEjectionSystemIsIgnored() {
        Tank tank = new Tank();
        tank.setId(TANK_ID);
        tank.setOwner(game.getPlayer(OWNER_ID));
        game.addEntity(tank);

        sendAsPacket(OWNER_ID, TANK_ID, false);

        assertTrue(mek.isAutoEject(), "a vehicle has no ejection setting, so nothing should change");
    }
}
