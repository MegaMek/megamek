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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Player;
import org.junit.jupiter.api.Test;

/**
 * Tests the Game Master lookup and the disruptive-commands rule that decide which entries the game commands menu
 * offers.
 */
class GameCommandsMenuTest {

    private static Player createPlayer(int id, String name) {
        return new Player(id, name);
    }

    @Test
    void noGameMasterWhenNobodyHoldsTheRole() {
        List<Player> players = List.of(createPlayer(1, "Kerensky"), createPlayer(2, "Liao"));

        assertNull(GameCommandsMenu.findGameMaster(players));
    }

    @Test
    void findsThePlayerHoldingTheRole() {
        Player gameMaster = createPlayer(1, "Kerensky");
        gameMaster.setGameMaster(true);
        List<Player> players = List.of(createPlayer(2, "Liao"), gameMaster, createPlayer(3, "Davion"));

        assertEquals(gameMaster, GameCommandsMenu.findGameMaster(players));
    }

    @Test
    void noGameMasterWhenTheFlaggedPlayerIsABot() {
        // A bot may not hold the role, so its gamemaster flag must not make it the Game Master
        Player botPlayer = createPlayer(1, "Princess");
        botPlayer.setBot(true);
        botPlayer.setGameMaster(true);
        List<Player> players = List.of(botPlayer, createPlayer(2, "Liao"));

        assertNull(GameCommandsMenu.findGameMaster(players));
    }

    @Test
    void noGameMasterInAnEmptyGame() {
        assertNull(GameCommandsMenu.findGameMaster(List.of()));
    }

    @Test
    void everyPlayerGetsDisruptiveCommandsWithoutAGameMaster() {
        // No Game Master: any player may need to unstick a turn or reset an abandoned game
        Player player = createPlayer(1, "Liao");

        assertTrue(GameCommandsMenu.showsDisruptiveCommands(player, null));
    }

    @Test
    void onlyTheGameMasterGetsDisruptiveCommandsWhileTheRoleIsHeld() {
        Player gameMaster = createPlayer(1, "Kerensky");
        gameMaster.setGameMaster(true);
        Player otherPlayer = createPlayer(2, "Liao");

        assertTrue(GameCommandsMenu.showsDisruptiveCommands(gameMaster, gameMaster));
        assertFalse(GameCommandsMenu.showsDisruptiveCommands(otherPlayer, gameMaster));
    }

    @Test
    void anObserverGameMasterKeepsTheDisruptiveCommands() {
        // An observer can legitimately hold the role: the referee running a game they take no side in.
        // They keep the disruptive commands; the seated players still do not get them.
        Player observerGameMaster = createPlayer(1, "Kerensky");
        observerGameMaster.setGameMaster(true);
        observerGameMaster.setObserver(true);
        Player otherPlayer = createPlayer(2, "Liao");

        assertTrue(GameCommandsMenu.showsDisruptiveCommands(observerGameMaster, observerGameMaster));
        assertFalse(GameCommandsMenu.showsDisruptiveCommands(otherPlayer, observerGameMaster));
    }

    @Test
    void aGhostGameMasterDoesNotLockOutDisruptiveCommands() {
        // A disconnected Game Master cannot act, so the game must not be stuck without skip/reset/kick until
        // they return - the role is treated as absent and every player gets the commands
        Player ghostGameMaster = createPlayer(1, "Kerensky");
        ghostGameMaster.setGameMaster(true);
        ghostGameMaster.setGhost(true);
        Player otherPlayer = createPlayer(2, "Liao");

        assertTrue(GameCommandsMenu.showsDisruptiveCommands(otherPlayer, ghostGameMaster));
    }
}
