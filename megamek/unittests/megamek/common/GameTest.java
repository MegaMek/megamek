/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import megamek.server.victory.VictoryResult;
import org.junit.jupiter.api.Test;

class GameTest {

    @Test
    void testCancelVictory() {
        // Default test
        Game game = new Game();
        game.cancelVictory();
        assertFalse(game.isForceVictory());
        assertSame(Player.PLAYER_NONE, game.getVictoryPlayerId());
        assertSame(Player.TEAM_NONE, game.getVictoryTeam());

        // Test with members set to specific values
        Game game2 = new Game();
        game2.setVictoryPlayerId(10);
        game2.setVictoryTeam(10);
        game2.setForceVictory(true);

        game2.cancelVictory();
        assertFalse(game.isForceVictory());
        assertSame(Player.PLAYER_NONE, game.getVictoryPlayerId());
        assertSame(Player.TEAM_NONE, game.getVictoryTeam());
    }

    @Test
    void testGetVictoryReport() {
        Game game = new Game();
        game.createVictoryConditions();
        VictoryResult victoryResult = game.getVictoryResult();
        assertNotNull(victoryResult);

        // Note: this accessors are tested in VictoryResultTest
        assertSame(Player.PLAYER_NONE, victoryResult.getWinningPlayer());
        assertSame(Player.TEAM_NONE, victoryResult.getWinningTeam());

        int winningPlayer = 2;
        int winningTeam = 5;

        // Test an actual scenario
        Game game2 = new Game();
        game2.setVictoryTeam(winningTeam);
        game2.setVictoryPlayerId(winningPlayer);
        game2.setForceVictory(true);
        game2.createVictoryConditions();
        VictoryResult victoryResult2 = game2.getVictoryResult();

        assertSame(winningPlayer, victoryResult2.getWinningPlayer());
        assertSame(winningTeam, victoryResult2.getWinningTeam());
    }
}
