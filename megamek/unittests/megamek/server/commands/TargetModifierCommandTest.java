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
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.commands.arguments.UnitArgument;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TargetModifierCommandTest {

    /** The connection the command is treated as arriving on. */
    private static final int SENDING_CONNECTION = 1;

    private final TargetModifierCommand command = new TargetModifierCommand(null, null);

    @Test
    void itAsksForAUnitAndADeltaWithinTheTableRange() {
        List<Argument<?>> arguments = command.defineArguments();

        assertEquals(2, arguments.size(), "the unit, and the delta to its target movement modifier");
        assertTrue(arguments.getFirst() instanceof UnitArgument, "the first argument names the unit");
        assertTrue(arguments.get(1) instanceof IntegerArgument, "the second is the delta");
        IntegerArgument delta = (IntegerArgument) arguments.get(1);
        assertEquals(-Compute.MAX_GAMEMASTER_TARGET_MODIFIER, delta.getMinValue(),
              "a delta below the table's whole range could never do more than reach the floor");
        assertEquals(Compute.MAX_GAMEMASTER_TARGET_MODIFIER, delta.getMaxValue());
    }

    /**
     * @param holdsTheRole Whether the player sending the command holds the Game Master role
     *
     * @return a command wired to a server whose sending player is or is not the gamemaster
     */
    private static TargetModifierCommand commandSentBy(boolean holdsTheRole) {
        Player sender = new Player(SENDING_CONNECTION, "Sender");
        sender.setGameMaster(holdsTheRole);

        Game game = new Game();
        game.addPlayer(SENDING_CONNECTION, sender);
        TWGameManager gameManager = Mockito.mock(TWGameManager.class);
        Mockito.when(gameManager.getGame()).thenReturn(game);
        Server server = Mockito.mock(Server.class);
        Mockito.when(server.getGameManager()).thenReturn(gameManager);

        return new TargetModifierCommand(server, gameManager);
    }

    @Test
    void aGameMasterMayChangeATargetModifier() {
        // the positive case keeps the one below honest: a guard that refused everybody would pass the negative
        // test while making the command useless
        assertTrue(commandSentBy(true).preRun(SENDING_CONNECTION),
              "a gamemaster is exactly who this command is for");
    }

    @Test
    void aPlayerWhoIsNotGameMasterMayNot() {
        assertFalse(commandSentBy(false).preRun(SENDING_CONNECTION),
              "making another player's unit easier to hit would be a way to ruin their game");
    }
}
