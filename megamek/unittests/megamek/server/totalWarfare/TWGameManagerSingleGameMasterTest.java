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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import megamek.common.Player;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the single Game Master rule: only one player may hold the Game Master role at a time,
 * so a request to become Game Master is denied while another player holds the role.
 */
class TWGameManagerSingleGameMasterTest {

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
        doCallRealMethod().when(gameManager).requestGameMaster(any());
        doCallRealMethod().when(gameManager).processGameMasterRequest();
        doCallRealMethod().when(gameManager).isGameMasterRequestInProgress();
        doCallRealMethod().when(gameManager).getGameMaster();
        doCallRealMethod().when(gameManager).setGameMaster(any(), anyBoolean());
        gameManager.setGame(game);
    }

    @Test
    void grantsGameMasterWhenRoleIsFree() {
        gameManager.requestGameMaster(secondPlayer);
        gameManager.processGameMasterRequest();

        assertTrue(secondPlayer.getGameMaster());
        assertEquals(secondPlayer, gameManager.getGameMaster());
    }

    @Test
    void deniesGameMasterWhenAnotherPlayerHoldsTheRole() {
        firstPlayer.setGameMaster(true);

        gameManager.requestGameMaster(secondPlayer);
        gameManager.processGameMasterRequest();

        assertFalse(secondPlayer.getGameMaster());
        assertEquals(firstPlayer, gameManager.getGameMaster());
    }

    @Test
    void clearsPendingRequestAfterDenial() {
        firstPlayer.setGameMaster(true);

        gameManager.requestGameMaster(secondPlayer);
        gameManager.processGameMasterRequest();

        assertFalse(gameManager.isGameMasterRequestInProgress());
    }

    @Test
    void getGameMasterReturnsNullWithoutGameMaster() {
        assertNull(gameManager.getGameMaster());
    }
}
