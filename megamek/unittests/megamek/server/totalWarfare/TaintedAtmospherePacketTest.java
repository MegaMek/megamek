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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.planetaryConditions.PlanetaryConditions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests that the atmospheric taint a player picks in the lobby actually reaches the server, by sending the planetary
 * conditions the way a client does rather than by calling the handler directly.
 */
class TaintedAtmospherePacketTest {

    private TWGameManager gameManager;
    private Game game;

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendServerChat(anyString());

        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        game.setPhase(GamePhase.LOUNGE);
    }

    private void sendConditionsAsPacket(AtmosphericTaint atmosphericTaint) {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setAtmosphericTaint(atmosphericTaint);
        gameManager.handlePacket(0, new Packet(PacketCommand.SENDING_PLANETARY_CONDITIONS, conditions));
    }

    @Test
    @DisplayName("The taint chosen in the lobby arrives at the server through the conditions packet")
    void taintArrivesThroughThePacket() {
        assertEquals(AtmosphericTaint.BREATHABLE, game.getPlanetaryConditions().getAtmosphericTaint(),
              "a new game should start with breathable air");

        sendConditionsAsPacket(AtmosphericTaint.RADIOLOGICAL_TOXIC);

        assertEquals(AtmosphericTaint.RADIOLOGICAL_TOXIC, game.getPlanetaryConditions().getAtmosphericTaint(),
              "the taint sent from the lobby should be the taint the server is now using");
    }

    @Test
    @DisplayName("Every taint survives the trip through the packet")
    void everyTaintSurvivesThePacket() {
        for (AtmosphericTaint atmosphericTaint : AtmosphericTaint.values()) {
            sendConditionsAsPacket(atmosphericTaint);

            assertEquals(atmosphericTaint, game.getPlanetaryConditions().getAtmosphericTaint(),
                  atmosphericTaint + " should survive being sent to the server");
        }
    }

    @Test
    @DisplayName("Conditions sent after deployment has begun are ignored, taint included")
    void taintCannotBeChangedOnceDeploymentHasStarted() {
        sendConditionsAsPacket(AtmosphericTaint.CAUSTIC_TAINTED);
        game.setPhase(GamePhase.DEPLOYMENT);

        sendConditionsAsPacket(AtmosphericTaint.FLAMMABLE_TOXIC);

        assertEquals(AtmosphericTaint.CAUSTIC_TAINTED, game.getPlanetaryConditions().getAtmosphericTaint(),
              "the server should refuse a conditions change once deployment has begun");
    }
}
