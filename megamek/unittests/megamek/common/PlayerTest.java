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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import megamek.client.ui.swing.util.PlayerColour;

class PlayerTest {

    @Test
    void testGetColorForPlayerDefault() {
        String playerName = "Test Player 1";
        Player player = new Player(0, playerName);
        assertEquals("<B><font color='8080b0'>" + playerName + "</font></B>", player.getColorForPlayer());
    }

    @Test
    void testGetColorForPlayerFuchsia() {
        String playerName = "Test Player 2";
        Player player = new Player(1, playerName);
        player.setColour(PlayerColour.FUCHSIA);
        assertEquals("<B><font color='f000f0'>" + playerName + "</font></B>", player.getColorForPlayer());
    }

    @Test
    void testGetBVDelegatesToPlayerStats() {
        Player player = new Player(1, "Refactored Player");

        // Par défaut, getBV() retourne 0 sauf si tu changes la logique dans PlayerStats
        assertEquals(0, player.getBV(), "Le BV doit être 0 par défaut pour un joueur sans unités");
    }

    @Test
    void testSetAndGetIdAndTeam() {
        Player player = new Player(42, "Team Player");
        player.setTeam(5);
        assertEquals(42, player.getId());
        assertEquals(5, player.getTeam());
    }

    @Test
    void testSetAndIsBot() {
        Player player = new Player(3, "Bot Tester");
        player.setBot(true);
        assertTrue(player.isBot(), "Le joueur doit être marqué comme bot.");
    }

    @Test
    void testSetAndGetColor() {
        Player player = new Player(7, "Colorful Player");
        player.setColour(PlayerColour.RED);
        assertEquals(PlayerColour.RED, player.getColour());
    }

    @Test
    void testPlayerTeamAndBotStatus() {
        Player player = new Player(3, "TeamBot");
        player.setTeam(15);
        player.setBot(true);

        assertEquals(15, player.getTeam());
        assertTrue(player.isBot());
    }

    @Test
    void testEqualsAndHashCode() {
        Player p1 = new Player(9, "Test");
        Player p2 = new Player(9, "Test");
        Player p3 = new Player(10, "Diff");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
    }

    @Test
    void testGetIdAndName() {
        Player player = new Player(42, "Tester");
        assertEquals(42, player.getId());
        assertEquals("Tester", player.getName());
    }

}
