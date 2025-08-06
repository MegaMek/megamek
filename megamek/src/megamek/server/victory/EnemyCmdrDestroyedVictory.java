/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
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

package megamek.server.victory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;

import megamek.common.Game;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.options.OptionsConstants;

/**
 * Implements a victory condition that checks if all enemy commanders have been killed.
 */
public class EnemyCmdrDestroyedVictory implements VictoryCondition, Serializable {

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> ctx) {
        VictoryResult victoryResult = new VictoryResult(true);
        // check all players/teams for killing enemy commanders
        // score is 1.0 when enemy commanders are dead
        boolean isVictory = false;
        HashSet<Integer> doneTeams = new HashSet<>();

        for (Player player : game.getPlayersList()) {
            int team = player.getTeam();
            if (team != Player.TEAM_NONE) {
                if (doneTeams.contains(team)) {
                    // only consider each team once
                    continue;
                }
                doneTeams.add(team);
            }
            boolean killedAllCommanders = false;
            if (game.getOptions().booleanOption(OptionsConstants.VICTORY_COMMANDER_KILLED)) {
                killedAllCommanders = game.getPlayersList()
                      .stream()
                      .filter(p -> p.isEnemyOf(player))
                      .mapToInt(game::getLiveCommandersOwnedBy)
                      .sum() == 0;
            }

            if (killedAllCommanders) {
                Report r = new Report(7110, Report.PUBLIC);
                if (team == Player.TEAM_NONE) {
                    r.add(player.getName());
                    victoryResult.setPlayerScore(player.getId(), 1);
                } else {
                    r.add("Team " + team);
                    victoryResult.setTeamScore(team, 1);
                }
                victoryResult.addReport(r);
                isVictory = true;
            }
        }
        return isVictory ? victoryResult : VictoryResult.noResult();
    }
}
