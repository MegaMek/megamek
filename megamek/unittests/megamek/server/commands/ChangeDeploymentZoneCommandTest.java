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
package megamek.server.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.interfaces.IStartingPositions;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.commands.arguments.PlayerArgument;
import megamek.server.commands.arguments.UnitArgument;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Verifies how the deployment zone command presents itself.
 *
 * <p>A player brought into a running game has no deployment zone, which means anywhere on the map. This is the
 * command a gamemaster uses to give them an edge, so it has to accept a player and a zone, offer every zone the
 * lobby offers, and be refused to anyone who is not the gamemaster.</p>
 */
class ChangeDeploymentZoneCommandTest {

    /** The connection the command is treated as arriving on. */
    private static final int SENDING_CONNECTION = 1;

    private final ChangeDeploymentZoneCommand command = new ChangeDeploymentZoneCommand(null, null);

    @Test
    void itIsAGamemasterCommand() {
        // GamemasterServerCommand is what refuses a sender who does not hold the role; being one is the guard
        assertTrue(command instanceof GamemasterServerCommand,
              "anyone being able to move another player's deployment zone would be a way to ruin their game");
    }

    @Test
    void itAsksForAPlayerAndAZone() {
        List<Argument<?>> arguments = command.defineArguments();

        assertEquals(2, arguments.size(), "a player to set, and the zone to set them to");
        assertTrue(arguments.getFirst() instanceof PlayerArgument, "the first argument names the player");
        assertTrue(arguments.get(1) instanceof IntegerArgument, "the second is the zone");
    }

    @Test
    void everyZoneTheLobbyOffersCanBeSet() {
        IntegerArgument zone = (IntegerArgument) command.defineArguments().get(1);

        assertEquals(0, zone.getMinValue(), "zero is the first zone, which is anywhere on the board");
        assertEquals(IStartingPositions.START_LOCATION_NAMES.length - 1, zone.getMaxValue(),
              "a gamemaster should reach every zone the lobby does, including the deep ones");
    }

    @Test
    void itDoesNotAskForAHexOrAUnit() {
        // it belongs on the game commands menu rather than a right-click, so it must not want a board position
        for (Argument<?> argument : command.defineArguments()) {
            assertFalse(argument instanceof UnitArgument, "this acts on a player, not on one of their units");
        }
    }

    /**
     * @param holdsTheRole Whether the player sending the command holds the Game Master role
     *
     * @return a command wired to a server whose sending player is or is not the gamemaster
     */
    private static ChangeDeploymentZoneCommand commandSentBy(boolean holdsTheRole) {
        Player sender = new Player(SENDING_CONNECTION, "Sender");
        sender.setGameMaster(holdsTheRole);

        Game game = new Game();
        game.addPlayer(SENDING_CONNECTION, sender);
        TWGameManager gameManager = Mockito.mock(TWGameManager.class);
        Mockito.when(gameManager.getGame()).thenReturn(game);
        Server server = Mockito.mock(Server.class);
        Mockito.when(server.getGameManager()).thenReturn(gameManager);

        return new ChangeDeploymentZoneCommand(server, gameManager);
    }

    @Test
    void aGameMasterMaySetADeploymentZone() {
        // the positive case is what keeps the one below honest: without it, a guard that refused everybody would
        // pass the negative test while making the command useless
        assertTrue(commandSentBy(true).preRun(SENDING_CONNECTION),
              "a gamemaster is exactly who this command is for");
    }

    @Test
    void aPlayerWhoIsNotGameMasterMayNot() {
        assertFalse(commandSentBy(false).preRun(SENDING_CONNECTION),
              "moving another player's deployment zone would be a way to ruin their game");
    }
}
