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

package megamek.server.sbf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import megamek.common.Player;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.server.Server;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/** Tests recipient filtering and empty-envelope suppression in SBF packet batching. */
class SBFGameManagerPendingPacketTest {

    @Test
    void targetedPacketDoesNotSendEmptyEnvelopeToOtherPlayers() {
        SBFGameManager gameManager = new SBFGameManager();
        SBFGame game = new SBFGame();
        game.addPlayer(0, new Player(0, "Recipient"));
        game.addPlayer(1, new Player(1, "Other"));
        gameManager.setGame(game);
        Packet targetedPacket = new Packet(PacketCommand.ROUND_UPDATE, 3);
        gameManager.addPendingPacket(0, targetedPacket);
        Server server = mock(Server.class);

        try (MockedStatic<Server> serverStatic = mockStatic(Server.class)) {
            serverStatic.when(Server::getServerInstance).thenReturn(server);

            gameManager.handlePacket(0, new Packet(PacketCommand.LOCAL_PN));
            gameManager.handlePacket(0, new Packet(PacketCommand.LOCAL_PN));

            ArgumentCaptor<Packet> envelopeCaptor = ArgumentCaptor.forClass(Packet.class);
            verify(server, times(1)).send(eq(0), envelopeCaptor.capture());
            verify(server, never()).send(eq(1), any(Packet.class));
            Packet envelope = envelopeCaptor.getValue();
            assertEquals(PacketCommand.MULTI_PACKET, envelope.command());
            assertEquals(List.of(targetedPacket), envelope.getObject(0));
        }
    }
}
