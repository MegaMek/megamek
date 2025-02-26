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
 * This is a VictoryCondition that will match when a given percentage of all enemy BV is
 * no longer alive.
 */
public class BVDestroyedVictoryCondition implements BvVictoryCondition {

    protected int destroyedPercent;

    public BVDestroyedVictoryCondition(int destroyedPercent) {
        this.destroyedPercent = destroyedPercent;
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
            int enemyBV = getEnemyBV(game, player);
            int enemyInitialBV = getEnemyInitialBV(game, player);

            if ((enemyInitialBV != 0) && ((enemyBV * 100) / enemyInitialBV <= 100 - destroyedPercent)) {
                isVictory = true;
                Report report = new Report(7105, Report.PUBLIC);
                if (team == Player.TEAM_NONE) {
                    report.add(player.getName());
                    victoryResult.setPlayerScore(player.getId(), 1);
                } else {
                    report.add("Team " + team);
                    victoryResult.setTeamScore(team, 1);
                }
                report.add(100 - ((enemyBV * 100) / enemyInitialBV));
                victoryResult.addReport(report);
            }
        }
        return isVictory ? victoryResult : VictoryResult.noResult();
    }
}
