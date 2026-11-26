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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.TimeUnit;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.moves.MovePath;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.VTOL;
import megamek.server.Server;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * A movement packet the server refuses must hand the turn back to its sender (issue #9390). Before the fix the server
 * logged the error and returned, so the sender thought its turn was over while the server kept waiting on it.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class RefusedMoveResendsTurnTest extends GameBoardTestCase {

    private static final int PLAYER_ID = 0;
    private static final int REFUSED_ENTITY_ID = 1;
    private static final int TURN_ENTITY_ID = 2;

    private Server server;

    static {
        initializeBoard("REFUSED_MOVE_CLEAR", """
              size 1 1
              hex 0101 0 "" ""
              end""");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.die();
        }
    }

    @Test
    void aRefusedMoveResendsTheTurnToItsSender() throws Exception {
        setBoard("REFUSED_MOVE_CLEAR");
        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        server = ServerFactory.createServer(gameManager);
        Player player = new Player(PLAYER_ID, "Test");
        getGame().addPlayer(PLAYER_ID, player);

        VTOL refused = new VTOL();
        refused.setChassis("Test");
        refused.setModel("Refused");
        refused.setMovementMode(EntityMovementMode.VTOL);
        refused.setCrew(new Crew(CrewType.CREW));
        refused.setOwner(player);
        refused.setId(REFUSED_ENTITY_ID);
        getGame().addEntity(refused);

        // The turn belongs to a different unit, so a move for this one is refused
        getGame().setPhase(GamePhase.MOVEMENT);
        getGame().setTurnVector(List.of(new SpecificEntityTurn(PLAYER_ID, TURN_ENTITY_ID)));
        getGame().setTurnIndex(0, PLAYER_ID);

        gameManager.handlePacket(PLAYER_ID,
              new Packet(PacketCommand.ENTITY_MOVE, REFUSED_ENTITY_ID, new MovePath(getGame(), refused)));

        verify(gameManager).send(eq(PLAYER_ID), argThat(packet -> packet.command() == PacketCommand.SENDING_TURNS));
        verify(gameManager).send(eq(PLAYER_ID), argThat(packet -> (packet.command() == PacketCommand.TURN)
              && Integer.valueOf(PLAYER_ID).equals(packet.getObject(1))));
    }
}
