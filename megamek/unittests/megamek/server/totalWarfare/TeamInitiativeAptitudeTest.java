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

import static megamek.common.game.Game.TEAM_HAS_COMBAT_SENSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Player;
import megamek.common.Team;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.turns.TurnOrdered;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests that every team's commander ability reaches the team initiative roll. Regression for #9284: the roll ran once
 * per team inside the loop that built the ability map, so only the last team's ability was in the roll that stuck.
 */
class TeamInitiativeAptitudeTest {

    @Test
    void everyTeamWithCombatSenseRollsWithIt() {
        Game game = spy(new Game());
        game.getOptions().getOption(OptionsConstants.BASE_TEAM_INITIATIVE).setValue(true);
        for (int playerId = 0; playerId < 3; playerId++) {
            Player player = new Player(playerId, "Player " + playerId);
            player.setTeam(playerId + 1);
            game.addPlayer(playerId, player);
        }
        game.setupTeams();
        doReturn(true).when(game).commanderHasCombatSense(any());

        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(game);
        doNothing().when(gameManager).transmitAllPlayerUpdates();

        List<Map<Team, Integer>> rolls = new ArrayList<>();
        try (MockedStatic<TurnOrdered> turnOrdered = Mockito.mockStatic(TurnOrdered.class)) {
            turnOrdered.when(() -> TurnOrdered.rollInitiative(anyList(), anyBoolean(), any()))
                  .thenAnswer(invocation -> {
                      rolls.add(new HashMap<>(invocation.getArgument(2)));
                      return null;
                  });

            gameManager.rollInitiative();
        }

        assertEquals(1, rolls.size(), "The teams roll initiative once");
        Map<Team, Integer> aptitudes = rolls.get(0);
        assertEquals(3, aptitudes.size(), "Every team is in the roll");
        for (Team team : game.getTeams()) {
            assertEquals(TEAM_HAS_COMBAT_SENSE, aptitudes.get(team), "Team " + team.getId() + " keeps Combat Sense");
        }
    }
}
