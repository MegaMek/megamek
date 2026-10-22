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
package megamek.server.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.event.GameToastEvent;
import megamek.common.net.packets.Packet;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Nuclear strike toasts follow the artillery rules: the aim point goes only to the firing side, and a detonation is
 * announced to everyone.
 */
class NuclearStrikeNotificationsTest {
    private static final Coords GROUND_ZERO = new Coords(24, 24);
    private static final int FIRING_PLAYER = 0;
    private static final int TEAMMATE = 1;
    private static final int ENEMY = 2;

    private TWGameManager gameManager;
    private NuclearStrikeNotifications notifications;

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(anyInt(), any(Packet.class));
        addPlayer(FIRING_PLAYER, "Firing", 1);
        addPlayer(TEAMMATE, "Teammate", 1);
        addPlayer(ENEMY, "Enemy", 2);
        notifications = new NuclearStrikeNotifications(gameManager);
    }

    private void addPlayer(int id, String name, int team) {
        Player player = new Player(id, name);
        player.setTeam(team);
        gameManager.getGame().addPlayer(id, player);
    }

    @Test
    void incomingStrikeIsToldOnlyToTheFiringSide() {
        notifications.announceIncoming(GROUND_ZERO, 4, gameManager.getGame().getPlayer(FIRING_PLAYER));

        Mockito.verify(gameManager).send(eq(FIRING_PLAYER), any(Packet.class));
        Mockito.verify(gameManager).send(eq(TEAMMATE), any(Packet.class));
        Mockito.verify(gameManager, Mockito.never()).send(eq(ENEMY), any(Packet.class));
    }

    @Test
    void incomingToastNamesTheHexAndRound() {
        notifications.announceIncoming(GROUND_ZERO, 4, gameManager.getGame().getPlayer(FIRING_PLAYER));

        ArgumentCaptor<Packet> packet = ArgumentCaptor.forClass(Packet.class);
        Mockito.verify(gameManager).send(eq(FIRING_PLAYER), packet.capture());
        assertEquals(GameToastEvent.Level.WARNING, packet.getValue().getObject(0));
        String message = (String) packet.getValue().getObject(1);
        assertTrue(message.contains("2525"), "the toast names hex 2525");
        assertTrue(message.contains("round 4"), "the toast names the round it detonates in");
    }

    @Test
    void detonationIsToldToEveryone() {
        notifications.announceDetonation("Santa Ana", GROUND_ZERO, 3);

        ArgumentCaptor<Packet> packet = ArgumentCaptor.forClass(Packet.class);
        Mockito.verify(gameManager).send(packet.capture());
        assertEquals(GameToastEvent.Level.ERROR, packet.getValue().getObject(0));
        assertEquals("Santa Ana detonated at hex 2525. 3 units destroyed.", packet.getValue().getObject(1));
    }
}
