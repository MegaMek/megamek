/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.victory;

import megamek.common.Game;
import megamek.common.Player;
import megamek.common.Report;

import java.util.HashSet;
import java.util.Map;

/**
 * This is a VictoryCondition that will match when friendly BV over enemy BV is at least a given
 * ratio in percent. For example, for a ratio of 150%, friendly BV must be 1.5 times enemy BV.
 * In games with more than two teams, all enemy BV is considered together, so a starting point
 * of 1:1:1 BV for three teams would place the initial ratio at 1 / (1 + 1) = 0.5.
 */
public class BVRatioVictoryCondition implements BvVictoryCondition {

    protected int ratio;

    public BVRatioVictoryCondition(int ratio) {
        this.ratio = ratio;
    }

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> ctx) {
        boolean isVictory = false;
        VictoryResult victoryResult = new VictoryResult(true);
        HashSet<Integer> doneTeams = new HashSet<>();

        for (Player player : game.getPlayersList()) {
            if (player.isObserver()) {
                continue;
            }
            int team = player.getTeam();
            if (team != Player.TEAM_NONE) {
                if (doneTeams.contains(team)) {
                    continue;
                }
                doneTeams.add(team);
            }
            int friendlyBV = getFriendlyBV(game, player);
            int enemyBV = getEnemyBV(game, player);

            if ((enemyBV == 0) || ((100 * friendlyBV) / enemyBV >= ratio)) {
                Report r = new Report(7100, Report.PUBLIC);
                isVictory = true;
                if (team == Player.TEAM_NONE) {
                    r.add(player.getName());
                    victoryResult.setPlayerScore(player.getId(), 1);
                } else {
                    r.add("Team " + team);
                    victoryResult.setTeamScore(team, 1);
                }
                r.add(enemyBV == 0 ? 9999 : (100 * friendlyBV) / enemyBV);
                victoryResult.addReport(r);
            }
        }
        return isVictory ? victoryResult : VictoryResult.noResult();
    }
}
