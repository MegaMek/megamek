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
package megamek.client.ui.clientGUI;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;

import megamek.client.ui.clientGUI.UnitOwnerColors.TeamColors;
import megamek.client.ui.util.PlayerColour;
import megamek.common.Player;
import megamek.common.icons.Camouflage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * An observer watching two bots: with team coloring on, each side must keep its lobby colour rather than both
 * being drawn in the enemy colour. A player on a side must still see own, ally and enemy colours.
 */
class UnitOwnerColorsTest {

    private static final Color OWN_COLOR = Color.BLUE;
    private static final Color ALLY_COLOR = Color.CYAN;
    private static final Color ENEMY_COLOR = Color.RED;
    private static final TeamColors TEAM_COLORS = new TeamColors(OWN_COLOR, ALLY_COLOR, ENEMY_COLOR);

    private Player princess;
    private Player caspar;
    private Player teammate;

    @BeforeEach
    void setUp() {
        princess = createPlayer(1, "Princess", 1, PlayerColour.GREEN);
        caspar = createPlayer(2, "CASPAR", 2, PlayerColour.YELLOW);
        teammate = createPlayer(3, "Teammate", 1, PlayerColour.PURPLE);
    }

    private static Player createPlayer(int id, String name, int team, PlayerColour lobbyColour) {
        Player player = new Player(id, name);
        player.setTeam(team);
        player.setCamouflage(Camouflage.of(lobbyColour));
        return player;
    }

    @Test
    void observerSeesEachOwnersLobbyColourWithTeamColoringOn() {
        Player observer = createPlayer(4, "Observer", 3, PlayerColour.BLUE);
        observer.setObserver(true);

        assertEquals(PlayerColour.GREEN.getColour(false),
              UnitOwnerColors.colorFor(princess, observer, true, TEAM_COLORS));
        assertEquals(PlayerColour.YELLOW.getColour(false),
              UnitOwnerColors.colorFor(caspar, observer, true, TEAM_COLORS));
    }

    @Test
    void noLocalPlayerSeesEachOwnersLobbyColour() {
        assertEquals(PlayerColour.GREEN.getColour(false),
              UnitOwnerColors.colorFor(princess, null, true, TEAM_COLORS));
    }

    @Test
    void playerOnASideSeesOwnAllyAndEnemyColours() {
        assertEquals(OWN_COLOR, UnitOwnerColors.colorFor(princess, princess, true, TEAM_COLORS));
        assertEquals(ALLY_COLOR, UnitOwnerColors.colorFor(teammate, princess, true, TEAM_COLORS));
        assertEquals(ENEMY_COLOR, UnitOwnerColors.colorFor(caspar, princess, true, TEAM_COLORS));
    }

    @Test
    void teamColoringOffUsesTheLobbyColourNotThePlainColourField() {
        // The lobby sets the colour camouflage only; the plain colour field stays at its default
        caspar.setColour(PlayerColour.BLUE);

        assertEquals(PlayerColour.YELLOW.getColour(false),
              UnitOwnerColors.colorFor(caspar, princess, false, TEAM_COLORS));
    }
}
