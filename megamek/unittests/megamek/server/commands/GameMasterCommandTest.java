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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The lobby's Game Master button asks for the role through the same path the /gm command uses, so the rules for
 * holding the role are tested here once, rather than once per way of asking.
 */
class GameMasterCommandTest {

    private TWGameManager gameManager;
    private Player firstPlayer;
    private Player secondPlayer;

    @BeforeEach
    void setUp() {
        Game game = new Game();

        firstPlayer = new Player(0, "First");
        firstPlayer.setTeam(1);
        game.addPlayer(0, firstPlayer);

        secondPlayer = new Player(1, "Second");
        secondPlayer.setTeam(2);
        game.addPlayer(1, secondPlayer);

        gameManager = mock(TWGameManager.class);
        doCallRealMethod().when(gameManager).setGame(any());
        doCallRealMethod().when(gameManager).getGameMaster();
        doCallRealMethod().when(gameManager).setGameMaster(any(), anyBoolean());
        gameManager.setGame(game);
    }

    @Test
    void noPlayerHoldsTheRoleToStartWith() {
        assertNull(gameManager.getGameMaster());
    }

    @Test
    void theRoleIsHeldByThePlayerGivenIt() {
        gameManager.setGameMaster(firstPlayer, true);

        assertTrue(firstPlayer.getGameMaster());
        assertFalse(secondPlayer.getGameMaster());
    }

    /** Giving up the role frees it for another player, which is what the button's second click does. */
    @Test
    void givingUpTheRoleFreesIt() {
        gameManager.setGameMaster(firstPlayer, true);

        gameManager.setGameMaster(firstPlayer, false);

        assertNull(gameManager.getGameMaster());
        assertFalse(firstPlayer.getGameMaster());
    }

    /**
     * Whether a game has a gamemaster is a rule of the game, so it is a game option, and it is on by default. A
     * client setting cannot decide it, since a player could otherwise take the role by typing the command whatever
     * their client showed them.
     */
    @Test
    void gamesAllowAGameMasterByDefault() {
        assertTrue(new Game().getOptions().booleanOption(OptionsConstants.GAME_MASTER_ALLOW),
              "games no longer allow a Game Master by default");
    }

    @Test
    void theGameOptionCanForbidAGameMaster() {
        Game game = new Game();
        game.getOptions().getOption(OptionsConstants.GAME_MASTER_ALLOW).setValue(false);

        assertFalse(game.getOptions().booleanOption(OptionsConstants.GAME_MASTER_ALLOW));
    }
}
