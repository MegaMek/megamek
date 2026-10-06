/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ui.util.PlayerColour;
import megamek.common.board.Coords;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.icons.Camouflage;
import org.junit.jupiter.api.Test;

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
    void testCopyCarriesGroundObjectsToPlace() {
        // player updates sent to other clients are redacted copies - if the copy loses the ground objects,
        // designated victory hexes never show up for anyone but their owner
        Player player = new Player(0, "Test Player 3");
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("Objective 0512");
        marker.setOwnerId(0);
        marker.setLobbyPosition(new Coords(4, 11));
        player.getGroundObjectsToPlace().add(marker);

        Player copy = player.copy();

        assertTrue(copy.getGroundObjectsToPlace().contains(marker));
        // the copied list must be independent of the original
        copy.getGroundObjectsToPlace().clear();
        assertFalse(player.getGroundObjectsToPlace().isEmpty());
    }

    @Test
    void testDisplayColourFollowsTheColourChosenInTheLobby() {
        Player player = new Player(0, "Hammershome");
        // the lobby stores a chosen colour as a colour camouflage and never touches the colour field
        player.setCamouflage(Camouflage.of(PlayerColour.SPRING_GREEN));

        assertEquals(PlayerColour.BLUE, player.getColour(), "the plain field stays at its default");
        assertEquals(PlayerColour.SPRING_GREEN, player.getDisplayColour(),
              "but what the player is shown in is what they picked");
    }

    @Test
    void testDisplayColourFallsBackToTheColourFieldForAnImageCamouflage() {
        Player player = new Player(0, "Hammershome");
        player.setColour(PlayerColour.RED);
        player.setCamouflage(new Camouflage("Clans", "Wolf.jpg"));

        assertEquals(PlayerColour.RED, player.getDisplayColour());
    }
}
